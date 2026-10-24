package com.tritv.photo.data

import org.json.JSONArray
import org.json.JSONObject

/** Parses Synology Photos API responses. Kept free of Android so it runs in unit tests. */
object SynologyJson {

    /** Fails with [SynologyException] when the response says `"success": false`. */
    fun data(body: String): JSONObject {
        val root = JSONObject(body)
        if (!root.optBoolean("success", false)) {
            val code = root.optJSONObject("error")?.optInt("code", -1) ?: -1
            throw SynologyException(code)
        }
        return root.optJSONObject("data") ?: JSONObject()
    }

    /** `SYNO.API.Info` query: API name to its CGI path and highest version. */
    fun apiInfo(body: String): Map<String, ApiEndpoint> {
        val data = data(body)
        return data.keys().asSequence().associateWith { name ->
            val o = data.getJSONObject(name)
            ApiEndpoint(o.optString("path", "entry.cgi"), o.optInt("maxVersion", 1))
        }
    }

    /** `SYNO.Foto.Browse.Item` list. Items the NAS has no large thumbnail for are skipped. */
    fun items(body: String): List<MediaItem> {
        val list = data(body).optJSONArray("list") ?: return emptyList()
        return (0 until list.length()).mapNotNull { item(list.getJSONObject(it)) }
    }

    /** Album name from `SYNO.Foto.Browse.Album` get, or null. */
    fun albumName(body: String): String? {
        val list = data(body).optJSONArray("list") ?: return null
        if (list.length() == 0) return null
        return list.getJSONObject(0).optString("name").takeIf { it.isNotBlank() }
    }

    fun item(o: JSONObject): MediaItem? {
        val extra = o.optJSONObject("additional") ?: JSONObject()
        val thumb = extra.optJSONObject("thumbnail") ?: return null
        val key = thumb.optString("cache_key")
        if (key.isEmpty() || thumb.optString("xl") == "broken") return null

        val kind = when (o.optString("type")) {
            "video" -> MediaItem.Kind.VIDEO
            "live" -> MediaItem.Kind.LIVE
            else -> MediaItem.Kind.PHOTO
        }
        val res = extra.optJSONObject("resolution")
        var w = res?.optInt("width") ?: 0
        var h = res?.optInt("height") ?: 0
        // EXIF orientations 5–8 are rotated a quarter turn: the stored width is the shown height.
        if (extra.optInt("orientation", 1) in 5..8) w = h.also { h = w }

        val exif = extra.optJSONObject("exif")
        val camera = exif?.optString("camera")?.takeIf { it.isNotBlank() }
        val videoMeta = extra.optJSONObject("video_meta")
        return MediaItem(
            id = o.getLong("id"),
            kind = kind,
            filename = o.optString("filename"),
            takenAt = o.optLong("time"),
            width = w,
            height = h,
            thumbnailKey = key,
            camera = camera,
            place = place(extra.optJSONObject("address")),
            videoQualities = qualities(extra.optJSONArray("video_convert")),
            durationMs = videoMeta?.optLong("duration") ?: 0,
        )
    }

    /** The most specific of district, city and county that is set, e.g. "西湖区" or "杭州市". */
    private fun place(address: JSONObject?): String? {
        if (address == null) return null
        return listOf("district", "city", "county", "state", "country")
            .map { address.optString(it) }
            .firstOrNull { it.isNotBlank() }
    }

    private val QUALITY_ORDER = listOf("high", "medium", "low")

    private fun qualities(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val names = (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("quality")?.takeIf(String::isNotBlank) }
        return names.distinct().sortedBy { QUALITY_ORDER.indexOf(it).let { i -> if (i < 0) QUALITY_ORDER.size else i } }
    }
}

data class ApiEndpoint(val path: String, val maxVersion: Int)

/**
 * An error the NAS reported. 119 and 106 mean the session is missing or expired;
 * 120 means a parameter was not accepted.
 */
class SynologyException(val code: Int) : Exception("Synology error $code") {
    val sessionExpired: Boolean get() = code == 106 || code == 119
}
