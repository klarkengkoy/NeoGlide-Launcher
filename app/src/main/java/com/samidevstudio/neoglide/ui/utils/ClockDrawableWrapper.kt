package com.samidevstudio.neoglide.ui.utils

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
class ClockDrawableWrapper(
    private val baseIcon: AdaptiveIconDrawable,
    private val hourIndex: Int,
    private val minuteIndex: Int,
    @Suppress("UNUSED_PARAMETER") secondIndex: Int // Kept for metadata compatibility, but unused for Pixel-style clock
) : Drawable() {

    private val foreground = baseIcon.foreground as? LayerDrawable
    private val background = baseIcon.background

    override fun draw(canvas: Canvas) {
        // Draw background
        if (background != null) {
            background.bounds = bounds
            background.draw(canvas)
        }

        if (foreground == null) {
            baseIcon.foreground?.let {
                it.bounds = bounds
                it.draw(canvas)
            }
            return
        }

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)

        // Rotation angles: 360 degrees / 12 hours = 30 deg/hr; 360 degrees / 60 mins = 6 deg/min
        val hourRotation = (hour + minute / 60f) * 30f
        val minuteRotation = minute * 6f

        // Draw foreground layers
        for (i in 0 until foreground.numberOfLayers) {
            val layer = foreground.getDrawable(i)
            layer.bounds = bounds
            
            val rotation = when (i) {
                hourIndex -> hourRotation
                minuteIndex -> minuteRotation
                else -> 0f
            }

            if (rotation != 0f) {
                canvas.save()
                canvas.rotate(rotation, bounds.centerX().toFloat(), bounds.centerY().toFloat())
                layer.draw(canvas)
                canvas.restore()
            } else {
                layer.draw(canvas)
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        baseIcon.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        baseIcon.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        baseIcon.bounds = bounds
    }
}
