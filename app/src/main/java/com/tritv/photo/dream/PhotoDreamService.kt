package com.tritv.photo.dream

import android.service.dreams.DreamService
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.tv.material3.Text
import com.tritv.photo.PhotoApp
import com.tritv.photo.data.Album
import com.tritv.photo.slideshow.Slideshow
import com.tritv.photo.slideshow.SlideshowMode
import com.tritv.photo.slideshow.SlideshowState
import com.tritv.photo.ui.PhotoColors
import com.tritv.photo.ui.PhotoTheme
import com.tritv.photo.ui.PhotoType

/**
 * The screensaver: the album's slideshow on black. Not interactive, so any key on the remote ends
 * it and returns to what was on screen. Plays from the TV's cache first and refreshes the album
 * from the NAS behind it, so it still runs while the NAS is asleep or unreachable.
 */
class PhotoDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    override fun onCreate() {
        super.onCreate()
        savedState.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = true

        val app = PhotoApp.from(this)
        val view = ComposeView(this).apply {
            setContent {
                PhotoTheme {
                    val session = remember { app.albums.session() }
                    var album by remember { mutableStateOf<Album?>(null) }
                    var loaded by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        album = app.albums.cached()
                        loaded = true
                        app.albums.refresh()?.let { album = it }
                    }
                    val a = album
                    if (session != null && a != null && a.items.isNotEmpty()) {
                        val state = remember { SlideshowState(a.items, app.settings.includeVideos) }
                        Slideshow(a, session, app.settings, SlideshowMode.Screensaver, state)
                    } else if (loaded) {
                        Box(Modifier.fillMaxSize().background(Color.Black).padding(64.dp), contentAlignment = Alignment.Center) {
                            Text("打开「回忆」应用，连接一个群晖相册后，这里会播放照片。", style = PhotoType.Body, color = PhotoColors.OnPhotoMuted)
                        }
                    }
                }
            }
        }
        window.decorView.let {
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
            it.setViewTreeViewModelStoreOwner(this)
        }
        setContentView(view)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        store.clear()
        super.onDetachedFromWindow()
    }
}
