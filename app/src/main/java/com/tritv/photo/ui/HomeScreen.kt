package com.tritv.photo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.tritv.photo.data.Album
import com.tritv.photo.data.MediaItem
import com.tritv.photo.data.ShareSession
import com.tritv.photo.slideshow.Caption
import java.util.Calendar
import java.util.TimeZone

/**
 * The start screen: the album over the pastel band, one filled "播放" button, and a row of the
 * album's photos; picking one starts the slideshow from it.
 */
@Composable
fun HomeScreen(
    album: Album?,
    session: ShareSession?,
    onPlay: (MediaItem?) -> Unit,
    onScreensaver: () -> Unit,
    onSettings: () -> Unit,
) {
    val play = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { play.requestFocus() } }
    val hero = remember(album?.items?.size) { album?.items?.filter { !it.isVideo && !it.isPortrait }?.randomOrNull() ?: album?.items?.firstOrNull() }

    Box(Modifier.fillMaxSize().background(PhotoColors.Canvas)) {
        MeshBand()
        Column(Modifier.fillMaxSize().padding(top = 48.dp), verticalArrangement = Arrangement.spacedBy(36.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("共享相册", style = PhotoType.Tag, color = PhotoColors.InkSecondary)
                    Text(album?.name ?: "正在读取相册…", style = PhotoType.Display, maxLines = 2)
                    album?.let { Text(summary(it), style = PhotoType.Body.tabular(), color = PhotoColors.InkMute) }
                    Spacer(Modifier.height(10.dp))
                    ButtonRow {
                        PillButton("▶ 播放", { onPlay(null) }, PillKind.Primary, Modifier.focusRequester(play))
                        PillButton("设为屏保", onScreensaver)
                        PillButton("设置", onSettings, PillKind.Dark)
                    }
                }
                if (hero != null && session != null) {
                    AsyncImage(
                        model = session.thumbnailUrl(hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(400.dp)
                            .aspectRatio(16f / 10f)
                            .shadow(18.dp, Radius.Card, ambientColor = SHADOW, spotColor = SHADOW)
                            .clip(Radius.Card)
                            .background(PhotoColors.CanvasSoft),
                    )
                }
            }
            if (album != null && session != null && album.items.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("相册里的照片", style = PhotoType.RowTitle, modifier = Modifier.padding(horizontal = ScreenMargin))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = ScreenMargin, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        items(album.items.asReversed(), key = { it.id }) { item ->
                            PhotoTile(item, session) { onPlay(item) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoTile(item: MediaItem, session: ShareSession, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(width = 208.dp, height = 130.dp),
        shape = ClickableSurfaceDefaults.shape(Radius.Card),
        colors = ClickableSurfaceDefaults.colors(containerColor = PhotoColors.CanvasSoft, focusedContainerColor = PhotoColors.CanvasSoft),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, PhotoColors.Primary), inset = (-6).dp, shape = Radius.Card),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
    ) {
        AsyncImage(
            model = session.thumbnailUrl(item, size = "m"),
            contentDescription = Caption.line(item),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.isVideo) {
            Text(
                "▶",
                style = PhotoType.Caption,
                color = PhotoColors.OnPhoto,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(Color(0x66000000), Radius.Pill)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
}

private val SHADOW = Color(0x33003770)

/** "4 张照片 · 1 段视频 · 2021–2023". */
private fun summary(album: Album): String {
    val videos = album.items.count { it.isVideo }
    val photos = album.items.size - videos
    val years = album.items.filter { it.takenAt > 0 }.map { year(it.takenAt) }
    val parts = mutableListOf("$photos 张照片")
    if (videos > 0) parts += "$videos 段视频"
    if (years.isNotEmpty()) {
        val (lo, hi) = years.min() to years.max()
        parts += if (lo == hi) "$lo" else "$lo–$hi"
    }
    return parts.joinToString(" · ")
}

private fun year(takenAt: Long) =
    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = takenAt * 1000 }.get(Calendar.YEAR)
