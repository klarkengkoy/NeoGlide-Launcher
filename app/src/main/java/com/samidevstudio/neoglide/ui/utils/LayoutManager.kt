package com.samidevstudio.neoglide.ui.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samidevstudio.neoglide.data.repository.GridSize
import kotlin.math.floor

// UNIVERSAL BASELINE (Design Intent)
private const val TRAY_MARGIN = 16f // dp from screen edge (Side/Bottom fallback)
private const val MIN_SPACING = 10f // dp

// OPTICAL WEIGHTING (48dp top safety / 16dp bottom safety)
private const val TOP_MARGIN = 48f
private const val BOTTOM_MARGIN = 16f
private const val VERTICAL_INSET_TOTAL = TOP_MARGIN + BOTTOM_MARGIN

// FIXED SIZE LOOKUP TABLE (Standard Launcher Dimensions)
private const val TINY_ICON_SIZE = 48f
private const val TINY_FONT_SIZE = 11f

private const val SMALL_ICON_SIZE = 60f
private const val SMALL_FONT_SIZE = 12f

private const val MEDIUM_ICON_SIZE = 72f
private const val MEDIUM_FONT_SIZE = 14f

private const val LARGE_ICON_SIZE = 84f
private const val LARGE_FONT_SIZE = 16f

object LayoutManager {
    const val SNAP_FACTOR = 4f

    data class LayoutConfig(
        val columns: Int, // Whole columns
        val rows: Int,    // Whole rows
        val totalColumns: Float, // Full capacity including subgrids (e.g. 5.5)
        val totalRows: Float,
        val iconSize: Dp,
        val spacing: Dp,
        val fontSize: TextUnit,
        val sidePadding: Dp,
        val topPadding: Dp,
        val trayWidth: Dp,
        val trayHeight: Dp,
        val unitWidth: Dp,
        val unitHeight: Dp,
        val actualGridWidth: Dp,
        val actualGridHeight: Dp,
        val snapUnitWidth: Float,
        val snapUnitHeight: Float,
        val expansionOffsetW: Float,
        val expansionOffsetH: Float,
    )

    fun calculateConfig(screenWidthDp: Dp, screenHeightDp: Dp, densitySetting: GridSize): LayoutConfig {
        val screenWidth = screenWidthDp.value
        val screenHeight = screenHeightDp.value
        
        // 1. Define the Fixed Tray Dimensions
        val trayWidthBase = screenWidth - (2 * TRAY_MARGIN)
        val trayHeightBase = screenHeight - VERTICAL_INSET_TOTAL
        
        // 2. Select Icon Size (Downgrade to fit at least 5 core columns)
        val sizesToTry = GridSize.entries.asSequence().filter { it.ordinal <= densitySetting.ordinal }.sortedByDescending { it.ordinal }
        
        var selectedIconSize = TINY_ICON_SIZE
        var selectedFontSize = TINY_FONT_SIZE
        
        for (setting in sizesToTry) {
            val (icon, font) = when (setting) {
                GridSize.TINY -> TINY_ICON_SIZE to TINY_FONT_SIZE
                GridSize.SMALL -> SMALL_ICON_SIZE to SMALL_FONT_SIZE
                GridSize.MEDIUM -> MEDIUM_ICON_SIZE to MEDIUM_FONT_SIZE
                GridSize.LARGE -> LARGE_ICON_SIZE to LARGE_FONT_SIZE
            }
            
            // Check if 5 core columns fit (5 icons + 4 spaces)
            val coreWidth = (5 * icon) + (4 * MIN_SPACING)
            if (coreWidth <= trayWidthBase) {
                selectedIconSize = icon
                selectedFontSize = font
                break
            }
        }

        // 3. Balanced Adaptive Expansion
        val iconSizeDp = selectedIconSize.dp
        val spacingDp = MIN_SPACING.dp
        val fontSizeSp = selectedFontSize.sp

        val unitWidthBase = selectedIconSize + MIN_SPACING
        val unitHeightBase = unitWidthBase + (selectedFontSize * 1.5f)
        val snapUnitW = unitWidthBase / SNAP_FACTOR
        val snapUnitH = unitHeightBase / SNAP_FACTOR

        // Calculate horizontal expansion (in pairs of snap units)
        val coreWidth = (5 * selectedIconSize) + (4 * MIN_SPACING)
        val remainingW = trayWidthBase - coreWidth
        val pairsW = floor(remainingW / (2 * snapUnitW)).toInt()
        val expansionOffsetW = pairsW.toFloat() / SNAP_FACTOR
        val finalColsFloat = 5f + ((2f * pairsW) / SNAP_FACTOR)
        val actualGridWidth = finalColsFloat * unitWidthBase

        // Calculate vertical expansion (in pairs of snap units)
        val baseRows = floor(trayHeightBase / unitHeightBase).toInt().coerceAtLeast(1)
        val remainingH = trayHeightBase - ((baseRows * unitHeightBase) - MIN_SPACING)
        val pairsH = floor(remainingH / (2 * snapUnitH)).toInt()
        val expansionOffsetH = pairsH.toFloat() / SNAP_FACTOR
        val finalRowsFloat = baseRows.toFloat() + (2f * pairsH / SNAP_FACTOR)
        val actualGridHeight = finalRowsFloat * unitHeightBase

        // 4. Center Everything with Optical Weighting
        val finalSidePadding = TRAY_MARGIN + (trayWidthBase - actualGridWidth) / 2f
        
        // The mesh centers itself within the offset tray (48dp top, 16dp bottom)
        val finalTopPadding = TOP_MARGIN + (trayHeightBase - actualGridHeight) / 2f

        // 5. Symmetric Core Anchors
        val dockRow = (floor(finalRowsFloat) - 1f).coerceAtLeast(0f)

        android.util.Log.d("LayoutManager", 
            """
            SYMMETRY_CHECK:
            Screen: ${screenWidth}dp x ${screenHeight}dp
            Mesh: ${actualGridWidth}dp x ${actualGridHeight}dp
            Expansion Offset: W=$expansionOffsetW, H=$expansionOffsetH
            Final Padding: Side=${finalSidePadding}dp, Top=${finalTopPadding}dp
            Dock Row: $dockRow
        """.trimIndent())

        return LayoutConfig(
            columns = 5, // Core Columns
            rows = floor(finalRowsFloat).toInt(),
            totalColumns = finalColsFloat,
            totalRows = finalRowsFloat,
            iconSize = iconSizeDp,
            spacing = spacingDp,
            fontSize = fontSizeSp,
            sidePadding = finalSidePadding.dp,
            topPadding = finalTopPadding.dp,
            trayWidth = trayWidthBase.dp,
            trayHeight = trayHeightBase.dp,
            unitWidth = unitWidthBase.dp,
            unitHeight = unitHeightBase.dp,
            actualGridWidth = actualGridWidth.dp,
            actualGridHeight = actualGridHeight.dp,
            snapUnitWidth = snapUnitW,
            snapUnitHeight = snapUnitH,
            expansionOffsetW = expansionOffsetW,
            expansionOffsetH = expansionOffsetH
        )
    }

    /**
     * Standardized Dock Row calculation to ensure ViewModel and UI are in sync.
     */
    fun getDockRow(totalRows: Float): Float {
        return (floor(totalRows) - 1f).coerceAtLeast(0f)
    }
}
