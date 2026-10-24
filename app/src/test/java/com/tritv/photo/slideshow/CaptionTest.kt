package com.tritv.photo.slideshow

import com.tritv.photo.data.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CaptionTest {

    private val shanghai = TimeZone.getTimeZone("Asia/Shanghai")

    private fun takenAt(y: Int, m: Int, d: Int, h: Int = 12): Long {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        c.clear(); c.set(y, m - 1, d, h, 0, 0)
        return c.timeInMillis / 1000
    }

    private fun now(y: Int, m: Int, d: Int): Long {
        val c = Calendar.getInstance(shanghai)
        c.clear(); c.set(y, m - 1, d, 9, 0, 0)
        return c.timeInMillis
    }

    private fun item(takenAt: Long, place: String? = null) =
        MediaItem(1, MediaItem.Kind.PHOTO, "a.jpg", takenAt, 4000, 3000, "k", place = place)

    @Test
    fun writesTheCameraDateAndPlace() {
        assertEquals("2021 年 10 月 23 日 · 莫干山", Caption.line(item(takenAt(2021, 10, 23, 23), "莫干山")))
        assertEquals("2021 年 1 月 2 日", Caption.line(item(takenAt(2021, 1, 2, 0))))
        assertNull(Caption.line(item(0)))
    }

    @Test
    fun countsFullYears() {
        val photo = item(takenAt(2021, 10, 24))
        assertEquals("5 年前", Caption.yearsAgo(photo, now(2026, 10, 24), shanghai))
        assertEquals("4 年前", Caption.yearsAgo(photo, now(2026, 10, 23), shanghai))
        assertNull(Caption.yearsAgo(item(takenAt(2026, 3, 1)), now(2026, 10, 24), shanghai))
    }
}
