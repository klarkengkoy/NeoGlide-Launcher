package com.samidevstudio.neoglide.ui.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils

object PaletteUtils {

    /**
     * Extracts a representative Hue value for sorting.
     * Uses a "Most Vibrant" strategy to pick the dominant color.
     * Maps grayscale colors to pseudo-hues for specific ordering:
     * White (-100), Rainbow (0-360), Gray (500), Black (1000).
     */
    fun extractDominantHue(drawable: Drawable?): Float {
        if (drawable == null) return 0f
        
        return try {
            val bitmap = drawableToBitmap(drawable) ?: return 0f
            
            // 24x24 is enough for accuracy without sacrificing performance
            val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, false)
            
            var maxS = -1f
            var hueOfMaxS = 0f
            var totalL = 0f
            var pixelCount = 0
            val hsl = FloatArray(3)

            for (x in 0 until scaled.width) {
                for (y in 0 until scaled.height) {
                    val color = scaled.getPixel(x, y)
                    if (Color.alpha(color) > 160) { // Ignore semi-transparent pixels
                        ColorUtils.colorToHSL(color, hsl)
                        val h = hsl[0]
                        val s = hsl[1]
                        val l = hsl[2]

                        pixelCount++
                        totalL += l

                        // Strategy: Find the most saturated (vibrant) pixel
                        if (s > maxS) {
                            maxS = s
                            hueOfMaxS = h
                        }
                    }
                }
            }

            if (scaled != bitmap) scaled.recycle()
            
            if (pixelCount == 0) return 0f
            val avgL = totalL / pixelCount

            return when {
                // Grayscale detection
                maxS < 0.12f -> {
                    when {
                        avgL > 0.85f -> -100f // White
                        avgL < 0.15f -> 1000f // Black
                        else -> 500f         // Gray
                    }
                }
                // Near-white icons with slight saturation
                avgL > 0.92f && maxS < 0.3f -> -100f
                // Near-black icons with slight saturation
                avgL < 0.1f -> 1000f
                // Rainbow sequence
                else -> {
                    // Normalize Red: Shift high-hue reds (330-360) to negative values (-30-0)
                    // so they sort together with low-hue reds at the start of the spectrum.
                    if (hueOfMaxS > 330f) hueOfMaxS - 360f else hueOfMaxS
                }
            }
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
