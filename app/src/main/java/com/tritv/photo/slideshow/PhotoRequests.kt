package com.tritv.photo.slideshow

import android.content.Context
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.size.Precision
import com.tritv.photo.data.MediaItem
import com.tritv.photo.data.ShareSession

/**
 * The image requests for a slide. The slideshow loads these ahead of time and then draws them, so
 * both sides must build them the same way to hit the same memory-cache entry.
 */
class PhotoRequests(
    private val context: Context,
    private val session: ShareSession,
    /** Screen size in pixels. */
    private val width: Int,
    private val height: Int,
) {
    fun full(item: MediaItem): ImageRequest = request(item, width, height)

    /** One half of a [Slide.Pair]. */
    fun half(item: MediaItem): ImageRequest = request(item, width / 2, height)

    /** Blurred, dimmed copy behind a portrait photo shown alone. */
    fun backdrop(item: MediaItem): ImageRequest =
        ImageRequest.Builder(context)
            .data(session.thumbnailUrl(item, size = "m"))
            .size(width / 4, height / 4)
            .transformations(SoftBlur())
            .build()

    fun forSlide(slide: Slide): List<ImageRequest> = when (slide) {
        is Slide.Pair -> listOf(half(slide.left), half(slide.right))
        is Slide.Single -> if (needsBackdrop(slide.item)) listOf(full(slide.item), backdrop(slide.item)) else listOf(full(slide.item))
        is Slide.Video -> listOf(full(slide.item))
    }

    /**
     * Photos close to the screen's shape fill it (a 3:2 photo loses a little top and bottom);
     * anything narrower is shown whole over a blurred copy of itself.
     */
    fun fillsScreen(item: MediaItem): Boolean {
        if (item.width <= 0 || item.height <= 0) return true
        val ratio = item.width.toFloat() / item.height
        return ratio >= MIN_FILL_RATIO
    }

    fun needsBackdrop(item: MediaItem) = !fillsScreen(item)

    private fun request(item: MediaItem, w: Int, h: Int) =
        ImageRequest.Builder(context)
            .data(session.thumbnailUrl(item))
            .size(w, h)
            .precision(Precision.INEXACT)
            .build()

    private companion object {
        /** 4:3 and wider fill the screen. */
        const val MIN_FILL_RATIO = 1.3f
    }
}
