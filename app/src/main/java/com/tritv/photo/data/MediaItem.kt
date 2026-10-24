package com.tritv.photo.data

/**
 * One photo or video in the album, as listed by Synology Photos.
 *
 * [takenAt] is the `time` field: seconds since the epoch for the wall-clock time the photo was
 * taken, written as if that wall-clock time were UTC (Synology Photos keeps EXIF time without a
 * zone). Read it with a UTC calendar to get the date and time the camera showed.
 * [width] and [height] are as displayed, with the EXIF orientation already applied.
 */
data class MediaItem(
    val id: Long,
    val kind: Kind,
    val filename: String,
    val takenAt: Long,
    val width: Int,
    val height: Int,
    val thumbnailKey: String,
    val camera: String? = null,
    val place: String? = null,
    /** Synology's transcoded versions of a video, best first, e.g. "high", "medium". */
    val videoQualities: List<String> = emptyList(),
    val durationMs: Long = 0,
) {
    enum class Kind { PHOTO, VIDEO, LIVE }

    val isVideo: Boolean get() = kind == Kind.VIDEO

    /** Taller than wide; two of these share the screen in the slideshow. */
    val isPortrait: Boolean get() = width > 0 && height > width
}

/** An album read from a share link. [items] are in the album's order. */
data class Album(val name: String, val items: List<MediaItem>, val fetchedAt: Long, val fromCache: Boolean = false)
