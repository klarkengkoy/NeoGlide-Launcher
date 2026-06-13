package com.samidevstudio.neoglide.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A high-quality frosted glass component that uses hardware-accelerated blur on Android 12+.
 * Provides a luminous, layered look similar to iOS Liquid Glass.
 */
@Composable
fun FrostedGlass(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 20.dp,
    tintColor: Color? = null,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color.Black else Color.White
    val resolvedTintColor = tintColor ?: if (isDark) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)
    
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.graphicsLayer {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            blurRadius.toPx(),
                            blurRadius.toPx(),
                            android.graphics.Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                } else {
                    Modifier.background(if (isDark) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f))
                }
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = if (isDark) 0.15f else 0.25f),
                        baseColor.copy(alpha = if (isDark) 0.05f else 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        baseColor.copy(alpha = if (isDark) 0.2f else 0.5f),
                        baseColor.copy(alpha = if (isDark) 0.05f else 0.1f),
                        baseColor.copy(alpha = if (isDark) 0.1f else 0.3f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        // Luminous tint overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(resolvedTintColor)
        )
        content()
    }
}
