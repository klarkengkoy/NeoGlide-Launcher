package com.smyddevstudio.neoglidelauncher.ui.components.folder

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.smyddevstudio.neoglidelauncher.ui.theme.NeoGlideLauncherTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smyddevstudio.neoglidelauncher.domain.model.AppModel
import com.smyddevstudio.neoglidelauncher.ui.theme.BadgeRed
import com.smyddevstudio.neoglidelauncher.ui.utils.system.HapticEngine
import com.smyddevstudio.neoglidelauncher.ui.utils.system.LocalWallpaperIsLight
import com.smyddevstudio.neoglidelauncher.ui.components.AppIcon

@Composable
fun FolderItem(
    label: String,
    apps: List<AppModel>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
    fontSize: TextUnit = 11.sp,
    useMonochrome: Boolean = false,
    showLabel: Boolean = true,
    isHovered: Boolean = false,
    isBlocked: Boolean = false,
    hasNotification: Boolean = false,
    notificationCount: Int = 0,
    onHapticFeedback: (HapticEngine.HapticType) -> Unit = {},
    onClick: (() -> Unit)? = null
) {
    val isWallpaperLight = LocalWallpaperIsLight.current
    val hoverScale by animateFloatAsState(
        targetValue = if (isHovered) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "hoverScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (onClick != null) {
                    Modifier.clickable { 
                        onHapticFeedback(HapticEngine.HapticType.FOLDER_OPEN)
                        onClick.invoke() 
                    }
                } else Modifier
            )
            .padding(4.dp)
            .graphicsLayer {
                scaleX = hoverScale
                scaleY = hoverScale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(iconSize),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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

            // NOTIFICATION BADGE
            com.smyddevstudio.neoglidelauncher.ui.components.NotificationBadge(
                hasNotification = hasNotification,
                notificationCount = notificationCount,
                iconSize = iconSize,
                borderColor = Color.White,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = fontSize,
                    color = if (isWallpaperLight) Color.Black.copy(alpha = 0.8f) else Color.White,
                    shadow = if (isWallpaperLight) {
                        Shadow(
                            color = Color.White.copy(alpha = 0.5f),
                            offset = Offset(0f, 1f),
                            blurRadius = 2f
                        )
                    } else {
                        Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                            blurRadius = 4f
                        )
                    }
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
private fun MiniAppIcon(app: AppModel?, useMonochrome: Boolean, size: Dp) {
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

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FolderItemPreview() {
    NeoGlideLauncherTheme {
        Surface {
            Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                FolderItem(
                    label = "Social",
                    apps = emptyList(),
                    hasNotification = true,
                    notificationCount = 3
                )
            }
        }
    }
}
