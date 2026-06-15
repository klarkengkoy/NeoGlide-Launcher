package com.samidevstudio.neoglide.ui.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

object PaletteUtils {

    /**
     * Extracts a representative Hue value for ultra-concise sorting.
     * Uses the Palette library to identify prominent colors.
     * Order: Rainbow -> Brown -> Grayscale -> Black -> White.
     */
    fun extractDominantHue(drawable: Drawable?): Float {
        if (drawable == null) return 0f
        
        return try {
            val bitmap = drawableToBitmap(drawable) ?: return 0f
            val palette = Palette.from(bitmap).generate()
            
            // Priority for selecting the "representative" color
            val swatch = palette.vibrantSwatch 
                ?: palette.dominantSwatch 
                ?: palette.mutedSwatch
                ?: palette.swatches.maxByOrNull { it.population }
                ?: return 0f

            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(swatch.rgb, hsl)
            
            val h = hsl[0]
            val s = hsl[1]
            val l = hsl[2]

            return when {
                // 1. Grayscale: Low Saturation
                s < 0.15f -> {
                    // Range: 500 to 600. Lighter grays come first.
                    500f + (1f - l) * 100f
                }

                // 2. Black: Very Low Lightness
                l < 0.15f -> {
                    // Range: 700 to 800. Lighter blacks come first.
                    700f + (1f - l) * 100f
                }

                // 3. White: High Lightness
                l > 0.88f -> {
                    // Range: 900 to 1000. Pure white comes last.
                    900f + l * 100f
                }

                // 4. Brown: Specific Hue range with low-mid Saturation/Lightness
                (h in 20f..45f) && s in 0.15f..0.65f && l in 0.1f..0.5f -> {
                    // Range: 400 to 450.
                    400f + h
                }

                // 5. Rainbow: Vibrant Colors
                else -> {
                    // Normalize Red: Shift high-hue reds (330-360) to negative values (-30-0)
                    // so they sort together with low-hue reds at the start of the spectrum.
                    // Range: -30 to 330.
                    if (h > 330f) h - 360f else h
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
