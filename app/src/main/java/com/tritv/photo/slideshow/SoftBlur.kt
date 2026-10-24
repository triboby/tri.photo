package com.tritv.photo.slideshow

import android.graphics.Bitmap
import coil3.size.Size
import coil3.transform.Transformation

/**
 * A heavy blur for the backdrop behind a portrait photo. Android 10 has no blur for views
 * (RenderEffect needs Android 12), so the photo is shrunk to a few dozen pixels and scaled back up
 * with filtering, which is cheap and soft enough for a dimmed background.
 */
class SoftBlur : Transformation() {
    override val cacheKey: String = "soft_blur_v1"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val tinyW = 24
        val tinyH = (input.height * tinyW / input.width.toFloat()).toInt().coerceAtLeast(1)
        val tiny = Bitmap.createScaledBitmap(input, tinyW, tinyH, true)
        val mid = Bitmap.createScaledBitmap(tiny, tinyW * 4, tinyH * 4, true)
        val out = Bitmap.createScaledBitmap(mid, tinyW * 16, tinyH * 16, true)
        if (tiny !== input) tiny.recycle()
        if (mid !== out) mid.recycle()
        return out
    }
}
