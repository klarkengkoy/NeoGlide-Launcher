package com.samidevstudio.neoglide.ui.utils

import android.appwidget.AppWidgetProviderInfo
import android.os.Build

object WidgetUtils {
    /**
     * Calculates the span (cell count) for a widget based on its provider info.
     * Uses Android 12+ target cell sizes if available, otherwise falls back to minWidth/Height.
     * 
     * @param unitWidthDp The width of a single grid cell in DP
     * @param unitHeightDp The height of a single grid cell in DP
     * @return Pair of (spanX, spanY)
     */
    fun calculateWidgetSpan(
        info: AppWidgetProviderInfo,
        unitWidthDp: Float,
        unitHeightDp: Float
    ): Pair<Int, Int> {
        // Find the absolute minimum the developer says the widget can handle
        val effectiveMinWidth = if (info.minResizeWidth > 0 && info.minResizeWidth < info.minWidth) 
            info.minResizeWidth else info.minWidth
        val effectiveMinHeight = if (info.minResizeHeight > 0 && info.minResizeHeight < info.minHeight) 
            info.minResizeHeight else info.minHeight

        // PRIORITY 1: Use Android 12+ target cell sizes if available.
        val targetSpanX = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0) {
            info.targetCellWidth
        } else -1

        val targetSpanY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellHeight > 0) {
            info.targetCellHeight
        } else -1

        // PRIORITY 2: Fallback to min size calculation using the actual grid unit size.
        // We use a small buffer (16dp instead of 12dp) to better account for margins on modern grids.
        val minSpanX = Math.ceil(((effectiveMinWidth + 16) / unitWidthDp).toDouble()).toInt().coerceAtLeast(1)
        val minSpanY = Math.ceil(((effectiveMinHeight + 16) / unitHeightDp).toDouble()).toInt().coerceAtLeast(1)

        val spanX = (if (targetSpanX > 0) targetSpanX else minSpanX).coerceIn(1, 10)
        
        // Use targetSpanY if valid, otherwise use minSpanY
        val spanY = (if (targetSpanY > 0) targetSpanY else minSpanY).coerceIn(1, 15)

        return spanX to spanY
    }
}
