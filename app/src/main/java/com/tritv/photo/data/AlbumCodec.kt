package com.tritv.photo.data

import org.json.JSONArray
import org.json.JSONObject

/** Stores an [Album] as JSON on the TV, so the slideshow can start before the NAS answers. */
object AlbumCodec {

    fun encode(album: Album): String = JSONObject()
        .put("name", album.name)
        .put("fetchedAt", album.fetchedAt)
        .put("items", JSONArray().apply {
            album.items.forEach { i ->
                put(JSONObject()
                    .put("id", i.id)
                    .put("kind", i.kind.name)
                    .put("filename", i.filename)
                    .put("takenAt", i.takenAt)
                    .put("w", i.width)
                    .put("h", i.height)
                    .put("key", i.thumbnailKey)
                    .putOpt("camera", i.camera)
                    .putOpt("place", i.place)
                    .put("vq", JSONArray(i.videoQualities))
                    .put("dur", i.durationMs))
            }
        })
        .toString()

    fun decode(text: String): Album {
        val o = JSONObject(text)
        val arr = o.getJSONArray("items")
        val items = (0 until arr.length()).map { n ->
            val i = arr.getJSONObject(n)
            val vq = i.optJSONArray("vq")
            MediaItem(
                id = i.getLong("id"),
                kind = runCatching { MediaItem.Kind.valueOf(i.getString("kind")) }.getOrDefault(MediaItem.Kind.PHOTO),
                filename = i.optString("filename"),
                takenAt = i.optLong("takenAt"),
                width = i.optInt("w"),
                height = i.optInt("h"),
                thumbnailKey = i.getString("key"),
                camera = i.optString("camera").takeIf { it.isNotEmpty() },
                place = i.optString("place").takeIf { it.isNotEmpty() },
                videoQualities = if (vq == null) emptyList() else (0 until vq.length()).map { vq.getString(it) },
                durationMs = i.optLong("dur"),
            )
        }
        return Album(o.optString("name", ShareSession.DEFAULT_NAME), items, o.optLong("fetchedAt"), fromCache = true)
    }
}
