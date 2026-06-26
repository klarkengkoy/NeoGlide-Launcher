package com.samidevstudio.neoglide.ui.utils

import androidx.compose.ui.unit.dp
import com.samidevstudio.neoglide.data.repository.GridSize
import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutManagerTest {

    @Test
    fun `calculateConfig phone width 360dp medium density downgrades size to fit 5`() {
        val screenWidth = 360.dp
        val screenHeight = 800.dp
        val config = LayoutManager.calculateConfig(screenWidth, screenHeight, GridSize.MEDIUM)
        
        // trayWidth = 360 - 32 = 328.
        // 48dp icon: (328 + 10) / (48 + 10) = 5.82 -> 5 cols.
        // snapUnit = 58 / 4 = 14.5.
        // pairs = floor( (328 - 280) / 29 ) = floor(48 / 29) = 1 pair.
        // totalCols = 5 + 0.5 = 5.5.
        
        assertEquals(5.5f, config.totalColumns)
        assertEquals(48f, config.iconSize.value)
        assertEquals(328f, config.trayWidth.value)
    }

    @Test
    fun `calculateConfig tablet width 800dp fills tray with balanced extra columns`() {
        val screenWidth = 800.dp
        val screenHeight = 1200.dp
        val config = LayoutManager.calculateConfig(screenWidth, screenHeight, GridSize.MEDIUM)
        
        // trayWidth = 768.
        // 72dp icon. snapUnit = 82 / 4 = 20.5.
        // baseWidth = 5 * 72 + 4 * 10 = 400.
        // remaining = 768 - 400 = 368.
        // pairs = floor( 368 / 41 ) = 12 pairs.
        // extraCols = 2 * 12 / 4 = 6.
        // totalCols = 5 + 6 = 11.
        
        assertEquals(11.0f, config.totalColumns)
        assertEquals(72f, config.iconSize.value)
    }
}
