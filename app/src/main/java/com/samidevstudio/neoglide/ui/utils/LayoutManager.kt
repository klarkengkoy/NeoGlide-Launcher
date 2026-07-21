package com.samidevstudio.neoglide.ui.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samidevstudio.neoglide.data.repository.GridSize
import kotlin.math.floor

// UNIVERSAL BASELINE (Design Intent)
private const val TRAY_MARGIN = 16f // dp from screen edge (Side fallback)
private const val MIN_SPACING = 16f // dp

// FIXED SIZE LOOKUP TABLE (Standard Launcher Dimensions)
private const val TINY_ICON_SIZE = 48f
private const val TINY_FONT_SIZE = 11f

private const val SMALL_ICON_SIZE = 60f
private const val SMALL_FONT_SIZE = 12f

private const val MEDIUM_ICON_SIZE = 66f
private const val MEDIUM_FONT_SIZE = 14f

private const val LARGE_ICON_SIZE = 78f
private const val LARGE_FONT_SIZE = 16f

private const val RESERVE_NAV_DP = 48f
private const val MIN_STATUS_BAR_DP = 24f

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
        val bottomPadding: Dp,
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

    fun calculateConfig(
        screenWidthDp: Dp, 
        screenHeightDp: Dp, 
        densitySetting: GridSize, 
        forcedWidth: Dp? = null,
        topInset: Dp = 48.dp,
        bottomInset: Dp = 48.dp,
        minSpacing: Float = MIN_SPACING,
        coreColumns: Int = 5
    ): LayoutConfig {
        val screenWidth = screenWidthDp.value
        val screenHeight = screenHeightDp.value
        
        // 1. Precise Tray Dimensions
        val trayWidthBase = forcedWidth?.value ?: (screenWidth - (2 * TRAY_MARGIN))
        val actualTrayHeight = screenHeight - topInset.value - bottomInset.value
        
        // STABILITY CLAMP: If topInset is reported as 0 (during transitions), 
        // fallback to a minimum safe value to prevent grid capacity from flickering.
        val stableTopInset = topInset.value.coerceAtLeast(MIN_STATUS_BAR_DP)
        
        // CAPACITY REFERENCE: Always calculate row count based on a 3-button nav height (RESERVE_NAV_DP)
        // to ensure the grid doesn't shrink/grow when toggling gesture navigation.
        val capacityTrayHeight = screenHeight - stableTopInset - RESERVE_NAV_DP
        
        // 2. Select Icon Size (Downgrade to fit core columns)
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

            // Check if core columns fit (N icons + (N-1) spaces)
            val coreWidth = (coreColumns * icon) + ((coreColumns - 1) * minSpacing)
            if (coreWidth <= trayWidthBase) {
                selectedIconSize = icon
                selectedFontSize = font
                break
            }
        }

        // 3. Balanced Adaptive Expansion
        val iconSizeDp = selectedIconSize.dp
        val spacingDp = minSpacing.dp
        val fontSizeSp = selectedFontSize.sp

        val unitWidthBase = selectedIconSize + minSpacing
        val unitHeightBase = unitWidthBase + (selectedFontSize * 1.5f)
        val snapUnitW = unitWidthBase / SNAP_FACTOR
        val snapUnitH = unitHeightBase / SNAP_FACTOR

        // Calculate horizontal expansion (in pairs of snap units)
        val coreWidth = (coreColumns * selectedIconSize) + ((coreColumns - 1) * minSpacing)
        val remainingW = trayWidthBase - coreWidth
        val pairsW = floor(remainingW / (2 * snapUnitW)).toInt()
        val expansionOffsetW = pairsW.toFloat() / SNAP_FACTOR
        val finalColsFloat = coreColumns.toFloat() + ((2f * pairsW) / SNAP_FACTOR)
        val actualGridWidth = finalColsFloat * unitWidthBase

        // Calculate vertical expansion (in pairs of snap units) based on CAPACITY height
        val baseRows = floor(capacityTrayHeight / unitHeightBase).toInt().coerceAtLeast(1)
        val remainingH = capacityTrayHeight - ((baseRows * unitHeightBase) - minSpacing)
        val pairsH = floor(remainingH / (2 * snapUnitH)).toInt()
        val expansionOffsetH = pairsH.toFloat() / SNAP_FACTOR
        val finalRowsFloat = baseRows.toFloat() + (2f * pairsH / SNAP_FACTOR)
        val actualGridHeight = finalRowsFloat * unitHeightBase

        // 4. Center Everything with Optical Weighting
        val finalSidePadding = ((if (forcedWidth != null) 0f else TRAY_MARGIN) + (trayWidthBase - actualGridWidth) / 2f).coerceAtLeast(0f)

        // Provide the base insets as padding. The UI's align(Alignment.Center) will handle the grid centering.
        val finalTopPadding = topInset.value.coerceAtLeast(0f)
        val finalBottomPadding = bottomInset.value.coerceAtLeast(0f)

        val columns = floor(finalColsFloat).toInt()

        return LayoutConfig(
            columns = columns,
            rows = floor(finalRowsFloat).toInt(),
            totalColumns = finalColsFloat,
            totalRows = finalRowsFloat,
            iconSize = iconSizeDp,
            spacing = spacingDp,
            fontSize = fontSizeSp,
            sidePadding = finalSidePadding.dp,
            topPadding = finalTopPadding.dp,
            bottomPadding = finalBottomPadding.dp,
            trayWidth = trayWidthBase.dp,
            trayHeight = actualTrayHeight.dp,
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
     * Sitting 3 subgrids above the absolute bottom edge.
     */
    fun getDockRow(totalRows: Float): Float {
        return (totalRows - 1f).coerceAtLeast(0f)
    }
}
