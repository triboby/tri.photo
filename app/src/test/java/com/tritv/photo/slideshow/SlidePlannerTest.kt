package com.tritv.photo.slideshow

import com.tritv.photo.data.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SlidePlannerTest {

    private val day = 86_400L

    private fun photo(id: Long, portrait: Boolean, takenAt: Long = id * 60) =
        MediaItem(id, MediaItem.Kind.PHOTO, "$id.jpg", takenAt, if (portrait) 3000 else 4000, if (portrait) 4000 else 3000, "$id")

    private fun video(id: Long) = MediaItem(id, MediaItem.Kind.VIDEO, "$id.mp4", id * 60, 1920, 1080, "$id")

    @Test
    fun showsEveryItemOncePerRound() {
        val items = (1L..10L).map { photo(it, portrait = false) }
        val planner = SlidePlanner(items, includeVideos = true, random = Random(1))
        val shown = (1..10).flatMap { planner.next()!!.items }.map { it.id }
        assertEquals(items.map { it.id }.toSet(), shown.toSet())
        assertEquals(10, shown.size)
    }

    @Test
    fun pairsPortraitsFromTheSameDay() {
        val items = listOf(
            photo(1, portrait = true, takenAt = 10 * day + 100),
            photo(2, portrait = false, takenAt = 10 * day + 200),
            photo(3, portrait = true, takenAt = 20 * day),
            photo(4, portrait = true, takenAt = 10 * day + 300),
        )
        val planner = SlidePlanner(items, includeVideos = true, random = Random(7))
        val slides = generateSequence { planner.next() }.take(3).toList()
        val pairs = slides.filterIsInstance<Slide.Pair>()
        val samePair = pairs.any { setOf(it.left.id, it.right.id) == setOf(1L, 4L) }
        val pairedWith3 = pairs.any { 3L in setOf(it.left.id, it.right.id) }
        // Either 1 and 4 (same day) are paired, or 3 came first and took its nearest portrait.
        assertTrue(samePair || pairedWith3)
        assertTrue(pairs.isNotEmpty())
    }

    @Test
    fun aLonePortraitIsShownAlone() {
        val planner = SlidePlanner(listOf(photo(1, portrait = true), photo(2, portrait = false)), includeVideos = true)
        val slides = listOf(planner.next()!!, planner.next()!!)
        assertTrue(slides.all { it is Slide.Single })
    }

    @Test
    fun videosAreTheirOwnSlidesAndCanBeLeftOut() {
        val items = listOf(video(1), photo(2, portrait = false))
        val withVideos = SlidePlanner(items, includeVideos = true, random = Random(3))
        assertTrue((1..2).map { withVideos.next() }.any { it is Slide.Video })
        val withoutVideos = SlidePlanner(items, includeVideos = false)
        assertTrue((1..4).map { withoutVideos.next() }.none { it is Slide.Video })
    }

    @Test
    fun anEmptyAlbumHasNoSlides() {
        assertNull(SlidePlanner(emptyList(), includeVideos = true).next())
        assertNull(SlidePlanner(listOf(video(1)), includeVideos = false).next())
    }
}
