package com.tritv.photo.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Reads one Synology Photos album through its public share link, with no account.
 *
 * Opening the share page hands out a `sharing_sid` cookie; after that every API call, thumbnail
 * and video request carries the cookie plus the passphrase (as the `X-SYNO-SHARING` header and as
 * a parameter). [client] is the one OkHttp client that does this, shared with the image loader
 * and the video player so they are authorised too.
 */
class ShareSession(val link: ShareLink) {

    private val cookies = MemoryCookieJar()
    private val mutex = Mutex()
    private var apis: Map<String, ApiEndpoint> = emptyMap()
    @Volatile private var opened = false

    val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookies)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val req = chain.request()
            // Only API calls get the header: DSM withholds the sharing_sid cookie from a share page
            // request that already carries it, and every later call then fails with error 101.
            val ours = req.url.host == baseUrl.host && req.url.port == baseUrl.port &&
                req.url.encodedPath.contains("/webapi/")
            chain.proceed(if (ours) req.newBuilder().header("X-SYNO-SHARING", link.passphrase).build() else req)
        })
        .build()

    private val baseUrl: HttpUrl = link.baseUrl.toHttpUrl()

    /** Album name and every item, oldest first. Opens the share first if needed. */
    suspend fun fetchAlbum(): Album = withContext(Dispatchers.IO) {
        retryingSession {
            val items = mutableListOf<MediaItem>()
            var offset = 0
            while (true) {
                val page = listPage(offset)
                items += page.items
                if (page.rawCount < PAGE) break
                offset += PAGE
            }
            Album(albumName() ?: DEFAULT_NAME, items.sortedBy { it.takenAt }, System.currentTimeMillis())
        }
    }

    /** Large thumbnail (about 1920 px on the long side) for [item]. */
    fun thumbnailUrl(item: MediaItem, size: String = "xl"): String =
        apiUrl(THUMBNAIL, "get")
            .addQueryParameter("id", item.id.toString())
            .addQueryParameter("cache_key", quoted(item.thumbnailKey))
            .addQueryParameter("type", quoted("unit"))
            .addQueryParameter("size", quoted(size))
            .addQueryParameter("passphrase", quoted(link.passphrase))
            .build().toString()

    /**
     * Where to play [item] from: Synology's transcoded stream when it has made one, otherwise
     * the original file. Not yet tried against the NAS with a video (the test album has none).
     */
    fun videoUrl(item: MediaItem): String {
        val quality = item.videoQualities.firstOrNull { it == "medium" } ?: item.videoQualities.firstOrNull()
        return if (quality != null) {
            apiUrl(STREAMING, "streaming")
                .addQueryParameter("item_id", item.id.toString())
                .addQueryParameter("quality", quoted(quality))
                .addQueryParameter("passphrase", quoted(link.passphrase))
                .build().toString()
        } else {
            apiUrl(DOWNLOAD, "download")
                .addQueryParameter("unit_id", "[${item.id}]")
                .addQueryParameter("passphrase", quoted(link.passphrase))
                .build().toString()
        }
    }

    /**
     * Reopens the share before images and videos are requested, e.g. after the TV wakes up and
     * the cookie may have expired. Cheap when the session is already open.
     */
    suspend fun ensureOpen() = withContext(Dispatchers.IO) { mutex.withLock { if (!opened) open() } }

    private suspend fun <T> retryingSession(block: () -> T): T {
        ensureOpen()
        return try {
            block()
        } catch (e: SynologyException) {
            if (!e.sessionExpired) throw e
            mutex.withLock { open() }
            block()
        }
    }

    /** Must hold [mutex]. */
    private fun open() {
        cookies.clear()
        execute(Request.Builder().url(link.sharingPage).build())
        apis = runCatching { queryApis() }.getOrDefault(emptyMap())
        opened = true
    }

    private fun queryApis(): Map<String, ApiEndpoint> {
        val url = baseUrl.newBuilder().addPathSegments("webapi/query.cgi")
            .addQueryParameter("api", "SYNO.API.Info")
            .addQueryParameter("version", "1")
            .addQueryParameter("method", "query")
            .addQueryParameter("query", listOf(BROWSE_ITEM, BROWSE_ALBUM, THUMBNAIL, STREAMING, DOWNLOAD).joinToString(","))
            .build()
        return SynologyJson.apiInfo(execute(Request.Builder().url(url).build()))
    }

    private class Page(val items: List<MediaItem>, val rawCount: Int)

    private fun listPage(offset: Int): Page {
        // Some DSM versions refuse extras they do not know (error 120); retry with the basics.
        for (extras in listOf(EXTRAS_FULL, EXTRAS_BASIC)) {
            try {
                val body = post(BROWSE_ITEM, "list", mapOf(
                    "offset" to offset.toString(),
                    "limit" to PAGE.toString(),
                    "additional" to extras,
                ))
                val raw = SynologyJson.data(body).optJSONArray("list")?.length() ?: 0
                return Page(SynologyJson.items(body), raw)
            } catch (e: SynologyException) {
                if (e.code != 120 || extras == EXTRAS_BASIC) throw e
            }
        }
        error("unreachable")
    }

    private fun albumName(): String? = runCatching {
        SynologyJson.albumName(post(BROWSE_ALBUM, "get", emptyMap()))
    }.getOrNull()

    private fun post(api: String, method: String, params: Map<String, String>): String {
        val ep = apis[api]
        val form = FormBody.Builder()
            .add("api", api)
            .add("method", method)
            .add("version", (ep?.maxVersion ?: 1).coerceAtMost(MAX_VERSION[api] ?: 1).toString())
            .add("passphrase", quoted(link.passphrase))
        params.forEach { (k, v) -> form.add(k, v) }
        val url = baseUrl.newBuilder().addPathSegments("webapi/" + (ep?.path ?: "entry.cgi")).build()
        val body = execute(Request.Builder().url(url).post(form.build()).build())
        SynologyJson.data(body) // throws on "success": false
        return body
    }

    private fun apiUrl(api: String, method: String): HttpUrl.Builder {
        val ep = apis[api]
        return baseUrl.newBuilder().addPathSegments("webapi/" + (ep?.path ?: "entry.cgi"))
            .addQueryParameter("api", api)
            .addQueryParameter("method", method)
            .addQueryParameter("version", (ep?.maxVersion ?: 1).coerceAtMost(MAX_VERSION[api] ?: 1).toString())
    }

    private fun execute(request: Request): String = client.newCall(request).execute().use { r ->
        if (!r.isSuccessful) throw IOException("HTTP ${r.code} for ${request.url.encodedPath}")
        r.body?.string().orEmpty()
    }

    /** Synology's web client sends string parameters as JSON strings. */
    private fun quoted(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private class MemoryCookieJar : CookieJar {
        private val store = ConcurrentHashMap<String, Cookie>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookies.forEach { store[it.domain + it.path + it.name] = it }
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            return store.values.filter { it.expiresAt > now && it.matches(url) }
        }
        fun clear() = store.clear()
    }

    companion object {
        const val DEFAULT_NAME = "共享相册"
        private const val PAGE = 500

        private const val BROWSE_ITEM = "SYNO.Foto.Browse.Item"
        private const val BROWSE_ALBUM = "SYNO.Foto.Browse.Album"
        private const val THUMBNAIL = "SYNO.Foto.Thumbnail"
        private const val STREAMING = "SYNO.Foto.Streaming"
        private const val DOWNLOAD = "SYNO.Foto.Download"

        /** Highest version this code was written against; the NAS's own maximum is used if lower. */
        private val MAX_VERSION = mapOf(
            BROWSE_ITEM to 1, BROWSE_ALBUM to 1, THUMBNAIL to 2, STREAMING to 1, DOWNLOAD to 1,
        )

        private const val EXTRAS_BASIC = """["thumbnail","resolution","orientation","video_convert","video_meta"]"""
        private const val EXTRAS_FULL = """["thumbnail","resolution","orientation","video_convert","video_meta","exif","address"]"""
    }
}
