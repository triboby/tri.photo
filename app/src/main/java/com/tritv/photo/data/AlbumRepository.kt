package com.tritv.photo.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * The saved share link and the last copy of its album.
 *
 * One [ShareSession] is kept per link for the life of the process, so the main screen and the
 * screensaver share the NAS session, the image cache and the video player's connection.
 */
class AlbumRepository(context: Context) {

    private val prefs = context.getSharedPreferences("album", Context.MODE_PRIVATE)
    private val cache = File(context.filesDir, "album.json")

    var shareLink: ShareLink?
        get() = prefs.getString(KEY_LINK, null)?.let(ShareLink::parse)
        private set(value) {
            prefs.edit().putString(KEY_LINK, value?.sharingPage).apply()
        }

    @Volatile private var session: ShareSession? = null

    /** The session for the saved link, or null when no album is connected. */
    fun session(): ShareSession? {
        val link = shareLink ?: return null
        session?.let { if (it.link.sharingPage == link.sharingPage) return it }
        return synchronized(this) {
            session?.takeIf { it.link.sharingPage == link.sharingPage } ?: ShareSession(link).also { session = it }
        }
    }

    /**
     * Tries [link] and saves it only if the album can be read and has something to show.
     * Throws with a message for the connect screen otherwise.
     */
    suspend fun connect(link: ShareLink): Album {
        val trial = ShareSession(link)
        val album = try {
            trial.fetchAlbum()
        } catch (e: SynologyException) {
            throw ConnectFailure(if (e.code == 119 || e.code == 105) "这个分享链接无效或已过期，请在 Synology Photos 里重新分享。" else "群晖返回错误 ${e.code}。")
        } catch (e: IOException) {
            throw ConnectFailure("连不上 ${link.baseUrl}，请确认电视和群晖在同一个网络。")
        }
        if (album.items.isEmpty()) throw ConnectFailure("这个相册里还没有照片。")
        withContext(Dispatchers.IO) { save(link, album) }
        synchronized(this) { session = trial }
        shareLink = link
        return album
    }

    fun disconnect() {
        shareLink = null
        session = null
        cache.delete()
    }

    /** The album as last downloaded, or null. */
    suspend fun cached(): Album? = withContext(Dispatchers.IO) {
        val link = shareLink ?: return@withContext null
        if (prefs.getString(KEY_CACHE_LINK, null) != link.sharingPage || !cache.exists()) return@withContext null
        runCatching { AlbumCodec.decode(cache.readText()) }.getOrNull()
    }

    /**
     * Downloads the album again. Network errors are retried a few times, since right after the TV
     * wakes from standby its network takes a while to come up; after that the cached copy is used.
     */
    suspend fun refresh(): Album? {
        val s = session() ?: return null
        var attempt = 0
        while (true) {
            try {
                val album = s.fetchAlbum()
                withContext(Dispatchers.IO) { save(s.link, album) }
                return album
            } catch (e: IOException) {
                if (attempt >= RETRY_DELAYS_MS.size) return cached()
                delay(RETRY_DELAYS_MS[attempt++])
            } catch (e: SynologyException) {
                return cached()
            }
        }
    }

    private fun save(link: ShareLink, album: Album) {
        runCatching {
            cache.writeText(AlbumCodec.encode(album))
            prefs.edit().putString(KEY_CACHE_LINK, link.sharingPage).apply()
        }
    }

    private companion object {
        const val KEY_LINK = "share_link"
        const val KEY_CACHE_LINK = "cache_link"
        val RETRY_DELAYS_MS = longArrayOf(2_000, 5_000, 10_000)
    }
}

class ConnectFailure(message: String) : Exception(message)
