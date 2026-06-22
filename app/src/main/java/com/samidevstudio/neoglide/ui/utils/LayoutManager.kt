package com.samidevstudio.neoglide.ui.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samidevstudio.neoglide.data.repository.GridSize
import kotlin.math.floor

object LayoutManager {
    // UNIVERSAL BASELINE (Design Intent)
    private const val DEFAULT_COLUMNS = 5
    private val DEFAULT_ICON_SIZE = 56.dp
    private val DEFAULT_LABEL_SIZE = 12.sp
    private val DEFAULT_SPACING = 16.dp
    private val DEFAULT_SIDE_PADDING = 16.dp
    private val MIN_ICON_SIZE = 40.dp

    data class LayoutConfig(
        val columns: Int,
        val iconSize: Dp,
        val spacing: Dp,
        val fontSize: TextUnit,
        val sidePadding: Dp,
        val unitWidth: Dp,
        val unitHeight: Dp
    )

    fun calculateConfig(screenWidthDp: Dp, densitySetting: GridSize): LayoutConfig {
        val screenWidth = screenWidthDp.value
        
        // 1. Calculate space needed for baseline 5 columns
        val baselineSpaceNeeded = (DEFAULT_COLUMNS * DEFAULT_ICON_SIZE.value) +
                ((DEFAULT_COLUMNS - 1) * DEFAULT_SPACING.value) +
                (2 * DEFAULT_SIDE_PADDING.value)

        var adaptedColumns = DEFAULT_COLUMNS
        
        if (baselineSpaceNeeded <= screenWidth) {
            // PHASE 3A: Surplus (Wide Screen)
            val surplus = screenWidth - baselineSpaceNeeded
            // How many extra "cells" (icon + spacing) fit in the surplus?
            val extraCols = floor(surplus / (DEFAULT_ICON_SIZE.value + DEFAULT_SPACING.value)).toInt()
            adaptedColumns = DEFAULT_COLUMNS + extraCols
        } else {
            // PHASE 3B: Compression (Narrow Screen)
            var currentCols = DEFAULT_COLUMNS
            var found = false
            while (currentCols >= 1 && !found) {
                val spaceForN = (currentCols * DEFAULT_ICON_SIZE.value) +
                        ((currentCols - 1) * DEFAULT_SPACING.value) +
                        (2 * DEFAULT_SIDE_PADDING.value)
                val scaleFactor = screenWidth / spaceForN
                val scaledIcon = DEFAULT_ICON_SIZE.value * scaleFactor
                
                if (scaledIcon >= MIN_ICON_SIZE.value || currentCols == 1) {
                    adaptedColumns = currentCols
                    found = true
                } else {
                    currentCols--
                }
            }
        }

        // PHASE 5: Apply Density Modifier
        val finalColumns = when (densitySetting) {
            GridSize.MEDIUM -> adaptedColumns
            GridSize.SMALL -> adaptedColumns + 1
            GridSize.LARGE -> (adaptedColumns - 1).coerceAtLeast(3)
        }

        // RECALCULATE FINAL SIZES to fill width perfectly
        // available = cols * icon + (cols - 1) * spacing + 2 * side_padding
        // Keep the ratio between icon and spacing the same as baseline
        val spacingRatio = DEFAULT_SPACING.value / DEFAULT_ICON_SIZE.value
        val paddingRatio = DEFAULT_SIDE_PADDING.value / DEFAULT_ICON_SIZE.value
        
        // screenWidth = iconSize * (cols + (cols - 1) * spacingRatio + 2 * paddingRatio)
        val totalUnits = finalColumns + (finalColumns - 1) * spacingRatio + 2 * paddingRatio
        val finalIconSizeValue = screenWidth / totalUnits
        
        val finalIconSize = finalIconSizeValue.dp
        val finalSpacing = (finalIconSizeValue * spacingRatio).dp
        val finalSidePadding = (finalIconSizeValue * paddingRatio).dp
        val finalFontSize = (DEFAULT_LABEL_SIZE.value * (finalIconSizeValue / DEFAULT_ICON_SIZE.value)).sp

        // unitWidth is the logical bounding box of one "slot" in the grid
        // It should include the icon width and the gap to the next icon
        val unitWidth = finalIconSize + finalSpacing
        
        // For a square grid, unitHeight = unitWidth
        // If we want more vertical space for labels, we add it here
        val unitHeight = unitWidth + (finalFontSize.value.dp * 1.5f) // Approximate space for label + padding

        return LayoutConfig(
            columns = finalColumns,
            iconSize = finalIconSize,
            spacing = finalSpacing,
            fontSize = finalFontSize,
            sidePadding = finalSidePadding,
            unitWidth = unitWidth,
            unitHeight = unitHeight
        )
    }

    /**
     * Calculates a distributed column index for items in the bottom dock (row 99).
     * This spreads a fixed number of items (usually 5) across the entire grid width.
     */
    fun getDistributedColumn(index: Int, totalItems: Int, columns: Int): Float {
        if (totalItems <= 1) return (columns - 1) / 2f
        return index.toFloat() * (columns - 1) / (totalItems - 1).toFloat()
    }
}
