package com.tritv.photo.slideshow

import com.tritv.photo.data.MediaItem
import java.util.Calendar
import java.util.TimeZone

/** The words under a photo: when and where it was taken, and how long ago. */
object Caption {

    private val UTC = TimeZone.getTimeZone("UTC")

    /** "2021 年 10 月 23 日 · 莫干山", or null when the photo has no time. */
    fun line(item: MediaItem): String? {
        val date = date(item) ?: return null
        return if (item.place != null) "$date · ${item.place}" else date
    }

    /** "2021 年 10 月 23 日", or null when the photo has no time. */
    fun date(item: MediaItem): String? {
        if (item.takenAt <= 0) return null
        val c = Calendar.getInstance(UTC).apply { timeInMillis = item.takenAt * 1000 }
        return "${c.get(Calendar.YEAR)} 年 ${c.get(Calendar.MONTH) + 1} 月 ${c.get(Calendar.DAY_OF_MONTH)} 日"
    }

    /**
     * "3 年前" once a full year has passed since the photo's date, by the TV's clock and zone;
     * null for photos from the last year.
     */
    fun yearsAgo(item: MediaItem, nowMillis: Long, zone: TimeZone = TimeZone.getDefault()): String? {
        if (item.takenAt <= 0) return null
        val taken = Calendar.getInstance(UTC).apply { timeInMillis = item.takenAt * 1000 }
        val now = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
        var years = now.get(Calendar.YEAR) - taken.get(Calendar.YEAR)
        val beforeAnniversary = now.get(Calendar.MONTH) < taken.get(Calendar.MONTH) ||
            (now.get(Calendar.MONTH) == taken.get(Calendar.MONTH) && now.get(Calendar.DAY_OF_MONTH) < taken.get(Calendar.DAY_OF_MONTH))
        if (beforeAnniversary) years--
        return if (years >= 1) "$years 年前" else null
    }
}
