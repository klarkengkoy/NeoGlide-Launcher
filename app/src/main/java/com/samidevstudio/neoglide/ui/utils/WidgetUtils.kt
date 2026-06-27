package com.samidevstudio.neoglide.ui.utils

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Rect
import android.os.Build

object WidgetUtils {
    /**
     * Calculates the span (cell count) for a widget based on its provider info.
     * Uses Android 12+ target cell sizes if available, otherwise falls back to minWidth/Height.
     * 
     * @param context Context to access system resources and getDefaultPaddingForWidget
     * @param info The AppWidgetProviderInfo
     * @param unitWidthDp The width of a single grid cell in DP
     * @param unitHeightDp The height of a single grid cell in DP
     * @return Pair of (spanX, spanY)
     */
    fun calculateWidgetSpan(
        context: Context,
        info: AppWidgetProviderInfo,
        unitWidthDp: Float,
        unitHeightDp: Float
    ): Pair<Float, Float> {
        // Get default padding added by the system for this widget
        val defaultPadding = Rect()
        val padding = AppWidgetHostView.getDefaultPaddingForWidget(context, info.provider, null)
        defaultPadding.set(padding)

        // Find the absolute minimum the developer says the widget can handle
        val effectiveMinWidth = if (info.minResizeWidth > 0 && info.minResizeWidth < info.minWidth) 
            info.minResizeWidth else info.minWidth
        val effectiveMinHeight = if (info.minResizeHeight > 0 && info.minResizeHeight < info.minHeight) 
            info.minResizeHeight else info.minHeight

        // PRIORITY 1: Use Android 12+ target cell sizes if available.
        val targetSpanX = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0) {
            info.targetCellWidth.toFloat()
        } else -1f

        val targetSpanY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellHeight > 0) {
            info.targetCellHeight.toFloat()
        } else -1f

        // PRIORITY 2: Fallback to min size calculation using the actual grid unit size.
        // ULTRA-TIGHT LOGIC: Since we call setPadding(0,0,0,0) on the host view,
        // we subtract system padding from the requirement to calculate content-only span.
        val totalMinWidth = (effectiveMinWidth.toFloat() - defaultPadding.left - defaultPadding.right).coerceAtLeast(40f)
        val totalMinHeight = (effectiveMinHeight.toFloat() - defaultPadding.top - defaultPadding.bottom).coerceAtLeast(40f)

        // "Tight Fit" Logic: 
        // We allow up to 8dp of "squeezing" if it allows us to snap to a 0.5 unit smaller span.
        // This is safe because most widgets have internal transparent space.
        val tightFitThreshold = 8f

        fun calculateTightSpan(minSize: Float, unitSize: Float): Float {
            val snapFactor = LayoutManager.SNAP_FACTOR
            val rawSpan = minSize / unitSize
            // Round to nearest snap increment
            val snappedSpan = kotlin.math.round(rawSpan * snapFactor) / snapFactor
            
            // If we are over a boundary by less than the threshold, snap down anyway
            val incrementSize = unitSize / snapFactor
            val overflow = minSize % incrementSize
            if (overflow > 0 && overflow <= tightFitThreshold && snappedSpan > (1f / snapFactor)) {
                return kotlin.math.floor((rawSpan * snapFactor).toDouble()).toFloat() / snapFactor
            }
            
            return snappedSpan.coerceAtLeast(1f / snapFactor)
        }

        val minSpanX = calculateTightSpan(totalMinWidth, unitWidthDp)
        val minSpanY = calculateTightSpan(totalMinHeight, unitHeightDp)

        val spanX = (if (targetSpanX > 0) targetSpanX else minSpanX).coerceIn(1f, 10f)
        val spanY = (if (targetSpanY > 0) targetSpanY else minSpanY).coerceIn(0.5f, 15f)

        return spanX to spanY
    }

    /**
     * Calculates the projected span for a widget, considering the current grid's column limit.
     * This ensures the dimensions shown in the picker match what will actually be placed.
     */
    fun calculateProjectedWidgetSpan(
        context: Context,
        info: AppWidgetProviderInfo,
        unitWidthDp: Float,
        unitHeightDp: Float,
        maxColumns: Int
    ): Pair<Float, Float> {
        val (originalSpanX, originalSpanY) = calculateWidgetSpan(context, info, unitWidthDp, unitHeightDp)
        
        val spanX = originalSpanX.coerceAtMost(maxColumns.toFloat())
        val scaleFactor = spanX / originalSpanX
        val rawSpanY = originalSpanY * scaleFactor
        
        val tightFitThreshold = 12f
        val rawHeightDp = rawSpanY * unitHeightDp
        val snapFactor = LayoutManager.SNAP_FACTOR
        val incrementDp = unitHeightDp / snapFactor
        val overflow = rawHeightDp % incrementDp
        
        val spanY = if (overflow > 0 && overflow <= tightFitThreshold && rawSpanY > (1f / snapFactor)) {
            (kotlin.math.floor((rawSpanY * snapFactor).toDouble()) / snapFactor).toFloat()
        } else {
            kotlin.math.round(rawSpanY * snapFactor) / snapFactor
        }.coerceIn(0.5f, 15f)

        return spanX to spanY
    }
}
