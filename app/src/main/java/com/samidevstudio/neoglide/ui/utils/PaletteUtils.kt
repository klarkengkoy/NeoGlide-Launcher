package com.samidevstudio.neoglide.ui.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils

object PaletteUtils {

    /**
     * Extracts the dominant Hue (0-360) from a drawable.
     * Uses a scaled-down bitmap and simple averaging for maximum speed.
     */
    fun extractDominantHue(drawable: Drawable?): Float {
        if (drawable == null) return 0f
        
        return try {
            val bitmap = drawableToBitmap(drawable) ?: return 0f
            
            // Scale down drastically for speed (16x16 is plenty for dominant color)
            val scaled = Bitmap.createScaledBitmap(bitmap, 16, 16, false)
            
            var totalHue = 0f
            var count = 0
            val hsl = FloatArray(3)

            for (x in 0 until scaled.width) {
                for (y in 0 until scaled.height) {
                    val color = scaled.getPixel(x, y)
                    if (Color.alpha(color) > 128) { // Ignore transparent pixels
                        ColorUtils.colorToHSL(color, hsl)
                        // Ignore near-grayscale colors for "Rainbow" flow
                        if (hsl[1] > 0.15f && hsl[2] > 0.15f && hsl[2] < 0.85f) {
                            totalHue += hsl[0]
                            count++
                        }
                    }
                }
            }

            if (scaled != bitmap) scaled.recycle()
            if (count > 0) totalHue / count else 0f
        } catch (e: Exception) {
            0f
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        return try {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
