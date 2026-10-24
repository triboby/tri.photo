package com.tritv.photo.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Row(
        Modifier.fillMaxSize().background(PhotoColors.CanvasSoft).padding(horizontal = ScreenMargin),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.width(520.dp),
        ) {
            item { Text("设置", style = PhotoType.Display, modifier = Modifier.padding(start = 20.dp)) }
            item(key = version) {
                SettingsCard {
                    SectionLabel("播放")
                    SettingRow(
                        "每张照片停留",
                        "${settings.intervalSeconds} 秒",
                        Modifier.focusRequester(first),
                        onStep = { step ->
                            val i = Settings.INTERVALS.indexOf(settings.intervalSeconds).coerceAtLeast(0)
                            settings.intervalSeconds = Settings.INTERVALS[(i + step).coerceIn(0, Settings.INTERVALS.lastIndex)]
                            version++
                        },
                    ) {
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
                    SectionLabel("相册")
                    SettingRow(albumName ?: "未连接", "更换链接", onClick = onChangeAlbum)
                    SectionLabel("屏保")
                    SettingRow("打开系统的屏幕保护程序设置", "", onClick = onOpenScreensaverSettings)
                }
            }
        }
        Column(
            Modifier.weight(1f).padding(top = 48.dp + 62.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreensaverHelp()
            Text("← → 改数值，OK 切换开关", style = PhotoType.Caption, color = PhotoColors.InkMute, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

/** The white card that groups the rows; rows sit inside its padding so a focused row never touches its edge. */
@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(PhotoColors.Canvas, Radius.Panel)
            .border(1.dp, PhotoColors.Hairline, Radius.Panel)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = PhotoType.Label, color = PhotoColors.InkMute, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp))
}

/** How to pick the screensaver, with the adb command for TVs whose settings hide third-party ones. */
@Composable
fun ScreensaverHelp(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(PhotoColors.Cream, Radius.Panel)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("设为电视屏保", style = PhotoType.Body, color = PhotoColors.Ink)
        Text("设置 → 设备偏好设置 → 屏幕保护程序 → 选「回忆」。", style = PhotoType.Caption, color = PhotoColors.InkSecondary)
        Text("如果列表里没有，用电脑执行：", style = PhotoType.Caption, color = PhotoColors.InkMute)
        Text(
            ADB_COMMAND,
            style = PhotoType.Caption.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp),
            color = PhotoColors.InkSecondary,
            modifier = Modifier.fillMaxWidth().background(PhotoColors.Canvas, Radius.Input).padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

const val ADB_COMMAND = "adb shell settings put secure screensaver_components com.tritv.photo/.dream.PhotoDreamService"

/**
 * One setting: filled indigo when focused, like the primary pill. With [onStep], Left and Right
 * change the value and the focused row shows "‹ 12 秒 ›".
 */
@Composable
private fun SettingRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onStep: ((Int) -> Unit)? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                val step = when (event.key) {
                    Key.DirectionLeft -> -1
                    Key.DirectionRight -> 1
                    else -> 0
                }
                if (onStep == null || step == 0) return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyDown) onStep(step)
                true
            },
        shape = ClickableSurfaceDefaults.shape(Radius.Card),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = PhotoColors.Ink,
            focusedContainerColor = PhotoColors.Primary,
            focusedContentColor = PhotoColors.OnPrimary,
            pressedContainerColor = PhotoColors.PrimaryPress,
            pressedContentColor = PhotoColors.OnPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f, pressedScale = 0.99f),
        glow = ClickableSurfaceDefaults.glow(glow = Glow.None, focusedGlow = Glow.None),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = PhotoType.Body.copy(fontSize = 17.sp))
            if (value.isNotEmpty()) {
                Text(
                    if (focused && onStep != null) "‹ $value ›" else value,
                    style = PhotoType.Caption,
                    color = if (focused) PhotoColors.OnPrimary else PhotoColors.InkMute,
                )
            }
        }
    }
}
