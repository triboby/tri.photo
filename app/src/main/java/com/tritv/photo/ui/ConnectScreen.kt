package com.tritv.photo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
        Column(
            Modifier.padding(horizontal = ScreenMargin, vertical = 56.dp).widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("连接家里的相册", style = PhotoType.Display)
            Text(
                "在 Synology Photos 里把一个相册设为公开分享，把链接填在下面。按 OK 开始输入。",
                style = PhotoType.Body,
                color = PhotoColors.InkSecondary,
            )
            LinkField(
                value = text,
                onValueChange = { text = it; error = null },
                onDone = ::connect,
                next = connectButton,
                placeholder = "http://群晖地址:5000/mo/sharing/…",
                modifier = Modifier.width(600.dp).focusRequester(field),
            )
            ButtonRow {
                PillButton(
                    text = if (busy) "正在连接…" else "完成连接",
                    onClick = ::connect,
                    kind = PillKind.Primary,
                    modifier = Modifier.focusRequester(connectButton),
                )
                if (onCancel != null) PillButton("取消", onCancel)
            }
            error?.let { Text(it, style = PhotoType.Body, color = PhotoColors.Error) }
            Text(
                "分享链接只能看这一个相册。想要「那年今日」等全库回忆，之后可以再加 DSM 账号。",
                style = PhotoType.Caption,
                color = PhotoColors.InkSecondary,
                modifier = Modifier
                    .background(PhotoColors.Cream, Radius.Card)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            )
        }
    }
}
