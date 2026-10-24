package com.tritv.photo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.tritv.photo.data.Settings

/** Slideshow and screensaver options, the album, and how to turn the screensaver on. */
@Composable
fun SettingsScreen(
    settings: Settings,
    albumName: String?,
    onChangeAlbum: () -> Unit,
    onOpenScreensaverSettings: () -> Unit,
) {
    // Settings are plain preferences; bump this to redraw after a change.
    var version by remember { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    fun onOff(b: Boolean) = if (b) "开" else "关"

    Box(Modifier.fillMaxSize().background(PhotoColors.CanvasSoft)) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = ScreenMargin, vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.width(720.dp),
        ) {
            item { Text("设置", style = PhotoType.Display, modifier = Modifier.padding(bottom = 16.dp)) }
            item(key = version) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingRow("每张照片停留", "${settings.intervalSeconds} 秒", Modifier.focusRequester(first)) {
                        val i = Settings.INTERVALS.indexOf(settings.intervalSeconds)
                        settings.intervalSeconds = Settings.INTERVALS[(i + 1) % Settings.INTERVALS.size]
                        version++
                    }
                    SettingRow("显示时钟", onOff(settings.showClock)) { settings.showClock = !settings.showClock; version++ }
                    SettingRow("显示日期、地点和「N 年前」", onOff(settings.showCaption)) { settings.showCaption = !settings.showCaption; version++ }
                    SettingRow("横图缓慢缩放", onOff(settings.kenBurns)) { settings.kenBurns = !settings.kenBurns; version++ }
                    SettingRow("播放视频", onOff(settings.includeVideos)) { settings.includeVideos = !settings.includeVideos; version++ }
                    SettingRow("屏保里的视频有声音", onOff(settings.screensaverVideoSound)) {
                        settings.screensaverVideoSound = !settings.screensaverVideoSound; version++
                    }
                }
            }
            item {
                Text("相册", style = PhotoType.RowTitle, modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
            }
            item { SettingRow(albumName ?: "未连接", "更换相册", onClick = onChangeAlbum) }
            item {
                Text("屏保", style = PhotoType.RowTitle, modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
            }
            item { SettingRow("打开系统的屏幕保护程序设置", "", onClick = onOpenScreensaverSettings) }
            item { ScreensaverHelp() }
        }
    }
}

/** How to pick the screensaver, with the adb command for TVs whose settings hide third-party ones. */
@Composable
fun ScreensaverHelp(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(PhotoColors.Cream, Radius.Card)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("在电视的「设置 → 设备偏好设置 → 屏幕保护程序」里选「回忆」。", style = PhotoType.Body, color = PhotoColors.Ink)
        Text("如果列表里没有，可以在电脑上用 adb 设置：", style = PhotoType.Caption, color = PhotoColors.InkSecondary)
        Text(ADB_COMMAND, style = PhotoType.Caption, color = PhotoColors.InkSecondary)
    }
}

const val ADB_COMMAND = "adb shell settings put secure screensaver_components com.tritv.photo/.dream.PhotoDreamService"

@Composable
private fun SettingRow(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(60.dp),
        shape = ClickableSurfaceDefaults.shape(Radius.Card),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = PhotoColors.Canvas,
            contentColor = PhotoColors.Ink,
            focusedContainerColor = PhotoColors.Canvas,
            focusedContentColor = PhotoColors.Ink,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, PhotoColors.Hairline), shape = Radius.Card),
            focusedBorder = focusRing(PhotoColors.Primary, width = 2.5.dp, gap = 3.dp, shape = Radius.Card),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = PhotoType.Body)
            if (value.isNotEmpty()) Text(value, style = PhotoType.Body.tabular(), color = PhotoColors.Primary)
        }
    }
}
