package com.tritv.photo.slideshow

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.SuccessResult
import com.tritv.photo.data.Album
import com.tritv.photo.data.MediaItem
import com.tritv.photo.data.Settings
import com.tritv.photo.data.ShareSession
import com.tritv.photo.ui.PhotoColors
import com.tritv.photo.ui.PhotoType
import com.tritv.photo.ui.tabular
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/** How the slideshow is being watched: opened from the app, or as the screensaver. */
enum class SlideshowMode { App, Screensaver }

/**
 * Full-screen slideshow on black, shared by the app and the screensaver. Each slide stays for the
 * set interval (a video until it ends); meanwhile the next slide's photos load, and a slide whose
 * photos cannot be loaded (NAS asleep and not in the cache) is skipped.
 */
@Composable
fun Slideshow(
    album: Album,
    session: ShareSession,
    settings: Settings,
    mode: SlideshowMode,
    state: SlideshowState,
    showDetails: Boolean = false,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(album) { state.replaceItems(album.items) }

    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        val context = LocalContext.current
        val requests = remember(session, constraints.maxWidth, constraints.maxHeight) {
            PhotoRequests(context, session, max(1, constraints.maxWidth), max(1, constraints.maxHeight))
        }
        val loader = remember(context) { SingletonImageLoader.get(context) }
        val intervalMs = settings.intervalSeconds * 1000L
        val current = state.current

        LaunchedEffect(current) {
            if (current == null) return@LaunchedEffect
            coroutineScope {
                // Find and load the next slide while this one is on screen.
                val ready = async {
                    repeat(MAX_SKIPS) {
                        val next = state.peekNext() ?: return@async
                        val ok = requests.forSlide(next).all { loader.execute(it) is SuccessResult }
                        if (ok) return@async
                        state.skipUpcoming()
                    }
                }
                val stay = if (current is Slide.Video) videoTimeout(current.item) else intervalMs
                delay(stay)
                withTimeoutOrNull(LOAD_GRACE_MS) { ready.await() }
                state.next()
            }
        }

        Crossfade(targetState = current, animationSpec = tween(FADE_MS), label = "slide") { slide ->
            when (slide) {
                null -> Unit
                is Slide.Single -> SinglePhoto(slide.item, requests, kenBurns = settings.kenBurns, durationMs = intervalMs + FADE_MS)
                is Slide.Pair -> PairedPhotos(slide, requests)
                is Slide.Video -> VideoSlide(
                    item = slide.item,
                    session = session,
                    poster = requests.full(slide.item),
                    muted = mode == SlideshowMode.Screensaver && !settings.screensaverVideoSound,
                    onFinished = { if (state.current === slide) state.next() },
                )
            }
        }

        if (current != null && settings.showCaption) {
            Captions(current, showDetails)
        }
        if (settings.showClock) {
            Clock(Modifier.align(Alignment.TopEnd).padding(top = 32.dp, end = 48.dp))
        }
    }
}

@Composable
private fun SinglePhoto(item: MediaItem, requests: PhotoRequests, kenBurns: Boolean, durationMs: Long) {
    Box(Modifier.fillMaxSize()) {
        if (requests.needsBackdrop(item)) {
            AsyncImage(
                model = requests.backdrop(item),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.55f },
            )
            AsyncImage(
                model = requests.full(item),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val progress = remember(item.id) { Animatable(0f) }
            if (kenBurns) {
                LaunchedEffect(item.id) { progress.animateTo(1f, tween(durationMs.toInt(), easing = LinearEasing)) }
            }
            // Alternate zoom direction and drift by photo, so consecutive photos don't move alike.
            val zoomIn = item.id % 2 == 0L
            val drift = if (item.id % 3 == 0L) -1f else 1f
            AsyncImage(
                model = requests.full(item),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val t = progress.value
                    // Start from 1 + DRIFT so the drift (at most DRIFT / 2 of the width each way)
                    // never pulls an edge into view.
                    val s = 1f + KEN_BURNS_DRIFT + KEN_BURNS_ZOOM * (if (zoomIn) t else 1f - t)
                    scaleX = s
                    scaleY = s
                    translationX = drift * size.width * KEN_BURNS_DRIFT * (t - 0.5f)
                },
            )
        }
    }
}

@Composable
private fun PairedPhotos(slide: Slide.Pair, requests: PhotoRequests) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(PAIR_GAP)) {
        for (item in slide.items) {
            AsyncImage(
                model = requests.half(item),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

/** Date, place and "N 年前" at the bottom, one block per photo; with [showDetails], file and camera too. */
@Composable
private fun Captions(slide: Slide, showDetails: Boolean) {
    val now = remember(slide) { System.currentTimeMillis() }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(0.62f to Color.Transparent, 1f to Color(0x99000000)),
        ),
    )
    Row(
        Modifier.fillMaxSize().padding(start = 48.dp, end = 48.dp, bottom = 36.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(PAIR_GAP + 96.dp),
    ) {
        for (item in slide.items) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Caption.yearsAgo(item, now)?.let { ago ->
                    Text(
                        ago,
                        style = PhotoType.Tag,
                        color = PhotoColors.OnPhoto,
                        modifier = Modifier
                            .background(PhotoColors.PhotoChip, RoundedCornerShape(percent = 50))
                            .padding(horizontal = 14.dp, vertical = 5.dp),
                    )
                }
                Caption.line(item)?.let { Text(it, style = PhotoType.Body.tabular(), color = PhotoColors.OnPhotoMuted) }
                if (showDetails) {
                    val details = listOfNotNull(item.filename, item.camera).joinToString(" · ")
                    Text(details, style = PhotoType.Caption, color = PhotoColors.OnPhotoMuted)
                }
            }
        }
        if (slide.items.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun Clock(modifier: Modifier) {
    val time by produceState(initialValue = clockText()) {
        while (true) {
            delay(1000L * (60 - (System.currentTimeMillis() / 1000) % 60))
            value = clockText()
        }
    }
    Text(time, style = PhotoType.Clock, color = PhotoColors.OnPhoto, modifier = modifier)
}

private fun clockText() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

private fun videoTimeout(item: MediaItem): Long =
    if (item.durationMs > 0) (item.durationMs + 5_000).coerceAtMost(MAX_VIDEO_MS) else MAX_VIDEO_MS

private const val FADE_MS = 1200
private const val MAX_SKIPS = 8
private const val LOAD_GRACE_MS = 15_000L
private const val MAX_VIDEO_MS = 3 * 60_000L
private const val KEN_BURNS_ZOOM = 0.08f
private const val KEN_BURNS_DRIFT = 0.04f
private val PAIR_GAP = 6.dp
