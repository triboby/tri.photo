package com.tritv.photo

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.tritv.photo.data.AlbumRepository
import com.tritv.photo.data.Settings
import okhttp3.Call
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath

/** Holds the repositories shared by the main screen and the screensaver. */
class PhotoApp : Application(), SingletonImageLoader.Factory {

    val albums by lazy { AlbumRepository(this) }
    val settings by lazy { Settings(this) }

    private val plainClient by lazy { OkHttpClient() }

    /** Requests go through the current album's session, so they carry its cookie and passphrase. */
    val callFactory = Call.Factory { request -> (albums.session()?.client ?: plainClient).newCall(request) }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { callFactory })) }
            // A few full-screen photos fit; the rest come back from disk in a few milliseconds.
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.15).build() }
            // Big enough to keep a few thousand photos, so the screensaver runs while the NAS sleeps.
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("photos").toOkioPath())
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }
            .build()

    companion object {
        const val DISK_CACHE_BYTES = 800L * 1024 * 1024

        fun from(context: Context) = context.applicationContext as PhotoApp
    }
}
