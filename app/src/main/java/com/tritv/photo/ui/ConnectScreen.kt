package com.tritv.photo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.tritv.photo.data.Album
import com.tritv.photo.data.AlbumRepository
import com.tritv.photo.data.ConnectFailure
import com.tritv.photo.data.ShareLink
import kotlinx.coroutines.launch

/**
 * First run (and "更换相册"): paste a Synology Photos share link. The link is tried before it is
 * saved, so a typo shows an error here instead of an empty slideshow.
 */
@Composable
fun ConnectScreen(albums: AlbumRepository, onConnected: (Album) -> Unit, onCancel: (() -> Unit)?) {
    var text by rememberSaveable { mutableStateOf(albums.shareLink?.sharingPage ?: "http://192.168.50.94:5000/mo/sharing/") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val field = remember { FocusRequester() }
    val connectButton = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { field.requestFocus() } }

    fun connect() {
        if (busy) return
        val link = ShareLink.parse(text)
        if (link == null) {
            error = "这不是 Synology Photos 的分享链接。链接里应该有 /mo/sharing/。"
            return
        }
        busy = true
        error = null
        scope.launch {
            try {
                onConnected(albums.connect(link))
            } catch (e: ConnectFailure) {
                error = e.message
            } finally {
                busy = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(PhotoColors.Canvas)) {
        MeshBand()
        Row(
            Modifier.fillMaxSize().padding(start = ScreenMargin, end = ScreenMargin, top = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(56.dp),
        ) {
            // Scrolls rather than squashing the button when an error line makes the column too tall.
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("第一步", style = PhotoType.Label, color = PhotoColors.InkSecondary)
                Text("连接家里的相册", style = PhotoType.Display)
                Text(
                    "在群晖 Photos 里给相册建一个共享链接，把链接填在下面。不需要账号密码。",
                    style = PhotoType.Body,
                    color = PhotoColors.InkMute,
                    modifier = Modifier.widthIn(max = 490.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text("共享链接", style = PhotoType.Label, color = PhotoColors.InkSecondary)
                LinkField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    onDone = ::connect,
                    next = connectButton,
                    placeholder = "http://群晖地址:5000/mo/sharing/…",
                    modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().focusRequester(field),
                )
                Text("按 OK 输入，按 ↓ 收起键盘", style = PhotoType.Caption, color = PhotoColors.InkMute)
                error?.let { Text(it, style = PhotoType.Body, color = PhotoColors.Error) }
                Spacer(Modifier.height(4.dp))
                ButtonRow {
                    PillButton(
                        text = if (busy) "正在连接…" else "完成连接",
                        onClick = ::connect,
                        kind = PillKind.Primary,
                        modifier = Modifier.focusRequester(connectButton),
                    )
                    if (onCancel != null) PillButton("取消", onCancel)
                }
            }
            Column(
                Modifier
                    .padding(top = 100.dp)
                    .width(300.dp)
                    .background(PhotoColors.Cream, Radius.Panel)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("怎么拿到链接", style = PhotoType.Body)
                Text(
                    "1. 手机打开群晖 Photos\n2. 长按相册 → 共享\n3. 打开「共享链接」，复制\n4. 在这里粘贴或输入",
                    style = PhotoType.Caption.copy(lineHeight = 22.sp),
                    color = PhotoColors.InkSecondary,
                )
                Text("每次打开或进入屏保时，会同步相册里新加的照片。", style = PhotoType.Caption, color = PhotoColors.InkMute)
            }
        }
    }
}
