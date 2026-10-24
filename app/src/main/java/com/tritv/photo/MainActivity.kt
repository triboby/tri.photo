package com.tritv.photo

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings as SystemSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import com.tritv.photo.data.Album
import com.tritv.photo.data.MediaItem
import com.tritv.photo.data.ShareSession
import com.tritv.photo.slideshow.Slideshow
import com.tritv.photo.slideshow.SlideshowMode
import com.tritv.photo.slideshow.SlideshowState
import com.tritv.photo.ui.ConnectScreen
import com.tritv.photo.ui.HomeScreen
import com.tritv.photo.ui.PhotoTheme
import com.tritv.photo.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = PhotoApp.from(this)
        setContent {
            PhotoTheme { PhotoAppUi(app, ::openScreensaverSettings) }
        }
    }

    /** The system screensaver picker; Sony's Android 10 settings have one. */
    private fun openScreensaverSettings() {
        try {
            startActivity(Intent(SystemSettings.ACTION_DREAM_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(SystemSettings.ACTION_SETTINGS))
        }
    }
}

private enum class Screen { Connect, Home, Play, Settings }

@Composable
private fun PhotoAppUi(app: PhotoApp, openScreensaverSettings: () -> Unit) {
    val albums = app.albums
    var screen by rememberSaveable { mutableStateOf(if (albums.shareLink == null) Screen.Connect else Screen.Home) }
    var album by remember { mutableStateOf<Album?>(null) }
    var linkVersion by remember { mutableStateOf(0) }
    var startWith by remember { mutableStateOf<MediaItem?>(null) }

    // Cached copy first, so the screen fills at once; then the NAS's current list.
    LaunchedEffect(linkVersion) {
        if (albums.shareLink == null) return@LaunchedEffect
        albums.cached()?.let { album = it }
        albums.refresh()?.let { album = it }
    }

    BackHandler(enabled = screen == Screen.Play || screen == Screen.Settings) { screen = Screen.Home }

    when (screen) {
        Screen.Connect -> ConnectScreen(
            albums = albums,
            onConnected = {
                album = it
                linkVersion++
                screen = Screen.Home
            },
            onCancel = if (albums.shareLink != null) ({ screen = Screen.Home }) else null,
        )
        Screen.Home -> HomeScreen(
            album = album,
            session = albums.session(),
            onPlay = {
                startWith = it
                screen = Screen.Play
            },
            onScreensaver = openScreensaverSettings,
            onSettings = { screen = Screen.Settings },
        )
        Screen.Settings -> SettingsScreen(
            settings = app.settings,
            albumName = album?.name,
            onChangeAlbum = { screen = Screen.Connect },
            onOpenScreensaverSettings = openScreensaverSettings,
        )
        Screen.Play -> {
            val a = album
            val session = albums.session()
            if (a == null || session == null || a.items.isEmpty()) {
                LaunchedEffect(Unit) { screen = Screen.Home }
            } else {
                PlayScreen(a, session, app, startWith)
            }
        }
    }
}

/** The slideshow opened from the app: Left / Right step through slides, OK shows file details. */
@Composable
private fun PlayScreen(album: Album, session: ShareSession, app: PhotoApp, startWith: MediaItem?) {
    val state = remember { SlideshowState(album.items, app.settings.includeVideos, first = startWith) }
    var details by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Slideshow(
        album = album,
        session = session,
        settings = app.settings,
        mode = SlideshowMode.App,
        state = state,
        showDetails = details,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionRight -> { state.next(); true }
                    Key.DirectionLeft -> { state.previous(); true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { details = !details; true }
                    else -> false
                }
            },
    )
}
