package com.samidevstudio.neoglide.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samidevstudio.neoglide.domain.model.AppModel
import com.samidevstudio.neoglide.ui.utils.HapticEngine

@Composable
fun FolderItem(
    label: String,
    apps: List<AppModel>,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 56.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    useMonochrome: Boolean = false,
    showLabel: Boolean = true,
    isHovered: Boolean = false,
    isBlocked: Boolean = false,
    onHapticFeedback: (HapticEngine.HapticType) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val hoverScale by animateFloatAsState(
        targetValue = if (isHovered) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "hoverScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable { 
                onHapticFeedback(HapticEngine.HapticType.FOLDER_OPEN)
                onClick() 
            }
            .padding(4.dp)
            .graphicsLayer {
                scaleX = hoverScale
                scaleY = hoverScale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .background(if (isBlocked) Color.Red.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            // 2x2 Grid of mini icons
            val miniIconSize = (iconSize.value * 0.4f).dp
            Column(
                modifier = Modifier.padding((iconSize.value * 0.125f).dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    MiniAppIcon(apps.getOrNull(0), useMonochrome, miniIconSize)
                    MiniAppIcon(apps.getOrNull(1), useMonochrome, miniIconSize)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    MiniAppIcon(apps.getOrNull(2), useMonochrome, miniIconSize)
                    MiniAppIcon(apps.getOrNull(3), useMonochrome, miniIconSize)
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = fontSize,
                    color = Color.White,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                        blurRadius = 4f
                    )
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MiniAppIcon(app: AppModel?, useMonochrome: Boolean, size: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.size(size)) {
        if (app != null) {
            AppIcon(
                packageName = app.packageName,
                contentDescription = null,
                useMonochrome = useMonochrome,
                size = size,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
