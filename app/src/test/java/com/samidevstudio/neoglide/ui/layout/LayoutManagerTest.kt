package com.samidevstudio.neoglide.ui.layout

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
        // coreColumns = 5. minSpacing = 16.
        // TINY_ICON_SIZE = 48. coreWidth = (5 * 48) + (4 * 16) = 240 + 64 = 304.
        // 304 <= 328 -> TINY selected.
        // unitWidth = 48 + 16 = 64.
        // snapUnit = 64 / 4 = 16.
        // remaining = 328 - 304 = 24.
        // pairs = floor( 24 / 32 ) = 0.
        // totalCols = 5 + 0 = 5.0.
        
        assertEquals(5.0f, config.totalColumns)
        assertEquals(48f, config.iconSize.value)
        assertEquals(328f, config.trayWidth.value)
    }

    @Test
    fun `calculateConfig tablet width 800dp fills tray with balanced extra columns`() {
        val screenWidth = 800.dp
        val screenHeight = 1200.dp
        val config = LayoutManager.calculateConfig(screenWidth, screenHeight, GridSize.MEDIUM)
        
        // trayWidth = 800 - 32 = 768.
        // MEDIUM_ICON_SIZE = 66. coreColumns = 5. minSpacing = 16.
        // coreWidth = (5 * 66) + (4 * 16) = 330 + 64 = 394.
        // 394 <= 768 -> MEDIUM selected.
        // unitWidth = 66 + 16 = 82.
        // snapUnit = 82 / 4 = 20.5.
        // remaining = 768 - 394 = 374.
        // pairs = floor( 374 / 41 ) = 9.
        // extraCols = 2 * 9 / 4 = 4.5.
        // totalCols = 5 + 4.5 = 9.5.
        
        assertEquals(9.5f, config.totalColumns)
        assertEquals(66f, config.iconSize.value)
    }
}
