package com.tritv.photo.data

import android.content.Context

/** Slideshow and screensaver options. */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Seconds each photo stays on screen. */
    var intervalSeconds: Int
        get() = prefs.getInt("interval", 12)
        set(value) = prefs.edit().putInt("interval", value).apply()

    var showClock: Boolean
        get() = prefs.getBoolean("clock", true)
        set(value) = prefs.edit().putBoolean("clock", value).apply()

    /** Date, place and "N 年前" under each photo. */
    var showCaption: Boolean
        get() = prefs.getBoolean("caption", true)
        set(value) = prefs.edit().putBoolean("caption", value).apply()

    /** Slow zoom and pan on landscape photos. */
    var kenBurns: Boolean
        get() = prefs.getBoolean("ken_burns", true)
        set(value) = prefs.edit().putBoolean("ken_burns", value).apply()

    var includeVideos: Boolean
        get() = prefs.getBoolean("videos", true)
        set(value) = prefs.edit().putBoolean("videos", value).apply()

    /** Sound for videos in the screensaver; the slideshow opened from the app always plays sound. */
    var screensaverVideoSound: Boolean
        get() = prefs.getBoolean("dream_sound", false)
        set(value) = prefs.edit().putBoolean("dream_sound", value).apply()

    companion object {
        val INTERVALS = listOf(6, 12, 20, 30, 60)
    }
}
