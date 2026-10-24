package com.tritv.photo.slideshow

import com.tritv.photo.data.MediaItem
import kotlin.random.Random

/** What fills the screen at one time. */
sealed interface Slide {
    val items: List<MediaItem>

    data class Single(val item: MediaItem) : Slide { override val items get() = listOf(item) }

    /** Two portrait photos side by side. */
    data class Pair(val left: MediaItem, val right: MediaItem) : Slide { override val items get() = listOf(left, right) }

    data class Video(val item: MediaItem) : Slide { override val items get() = listOf(item) }
}

/**
 * Turns an album into an endless run of slides: the album is shuffled into a deck, played through,
 * then shuffled again, so every photo comes up once per round. A portrait photo is paired with
 * another portrait from the same day if one is coming up soon, otherwise with the next portrait
 * close behind it, so portraits rarely stand alone between black bars.
 */
class SlidePlanner(
    items: List<MediaItem>,
    includeVideos: Boolean,
    private val random: Random = Random.Default,
) {
    private val pool = items.filter { includeVideos || !it.isVideo }
    private val deck = ArrayDeque<MediaItem>()
    private var last: MediaItem? = null

    val isEmpty: Boolean get() = pool.isEmpty()

    fun next(): Slide? {
        if (pool.isEmpty()) return null
        if (deck.isEmpty()) refill()
        val item = deck.removeFirst()
        last = item
        return when {
            item.isVideo -> Slide.Video(item)
            item.isPortrait -> partnerFor(item)?.let { Slide.Pair(item, it) } ?: Slide.Single(item)
            else -> Slide.Single(item)
        }
    }

    private fun refill() {
        val shuffled = pool.shuffled(random).toMutableList()
        // Don't start a round with the photo that ended the last one.
        if (shuffled.size > 1 && shuffled.first() == last) shuffled.add(shuffled.removeAt(0))
        deck.addAll(shuffled)
    }

    private fun partnerFor(item: MediaItem): MediaItem? {
        val day = dayOf(item)
        val window = deck.take(SAME_DAY_LOOKAHEAD)
        val match = window.firstOrNull { it.pairable() && dayOf(it) == day }
            ?: window.take(ANY_LOOKAHEAD).firstOrNull { it.pairable() }
            ?: return null
        deck.remove(match)
        return match
    }

    private fun MediaItem.pairable() = isPortrait && !isVideo

    private companion object {
        const val SAME_DAY_LOOKAHEAD = 24
        const val ANY_LOOKAHEAD = 3
        const val DAY_SECONDS = 86_400L

        /** [MediaItem.takenAt] is wall-clock time written as UTC, so whole days divide evenly. */
        fun dayOf(item: MediaItem) = Math.floorDiv(item.takenAt, DAY_SECONDS)
    }
}
