package com.tritv.photo.slideshow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tritv.photo.data.MediaItem

/**
 * Which slide is on screen, the one being prepared to follow it, and the slides already shown so
 * the remote can step back through them.
 */
class SlideshowState(
    items: List<MediaItem>,
    private val includeVideos: Boolean,
    /** Shown first, e.g. the photo picked on the home screen. */
    first: MediaItem? = null,
) {

    private var planner = SlidePlanner(items, includeVideos)
    private val back = ArrayDeque<Slide>()
    private val forward = ArrayDeque<Slide>()
    private var upcoming: Slide? = null
    private var itemIds = items.map { it.id }

    var current: Slide? by mutableStateOf(
        first?.let { if (it.isVideo) Slide.Video(it) else Slide.Single(it) } ?: planner.next(),
    )
        private set

    /** The slide [next] will show, picked now so its photos can load while this one is shown. */
    fun peekNext(): Slide? {
        forward.firstOrNull()?.let { return it }
        if (upcoming == null) upcoming = planner.next()
        return upcoming
    }

    /** Drops the prepared slide (its photos would not load) so [peekNext] picks another. */
    fun skipUpcoming() {
        if (forward.isNotEmpty()) forward.removeFirst() else upcoming = null
    }

    fun next() {
        val n = forward.removeFirstOrNull() ?: upcoming ?: planner.next()
        if (n === upcoming) upcoming = null
        current?.let { back.addLast(it); if (back.size > HISTORY) back.removeFirst() }
        current = n
    }

    fun previous(): Boolean {
        val p = back.removeLastOrNull() ?: return false
        current?.let { forward.addFirst(it) }
        current = p
        return true
    }

    /** Takes a refreshed album. The slide on screen stays; what follows comes from the new items. */
    fun replaceItems(items: List<MediaItem>) {
        val ids = items.map { it.id }
        if (ids == itemIds) return
        itemIds = ids
        planner = SlidePlanner(items, includeVideos)
        upcoming = null
        forward.clear()
        val known = ids.toSet()
        back.removeAll { slide -> slide.items.any { it.id !in known } }
        if (current == null || current!!.items.any { it.id !in known }) current = planner.next()
    }

    private companion object {
        const val HISTORY = 30
    }
}
