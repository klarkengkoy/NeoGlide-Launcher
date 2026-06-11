package com.samidevstudio.pxllauncherneo.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samidevstudio.pxllauncherneo.domain.model.AppModel
import com.samidevstudio.pxllauncherneo.ui.utils.HapticEngine

@Composable
fun FolderItem(
    label: String,
    apps: List<AppModel>,
    modifier: Modifier = Modifier,
    useMonochrome: Boolean = false,
    showLabel: Boolean = true,
    onHapticFeedback: (HapticEngine.HapticType) -> Unit = {},
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable { 
                onHapticFeedback(HapticEngine.HapticType.FOLDER_OPEN)
                onClick() 
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            // 2x2 Grid of mini icons
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    MiniAppIcon(apps.getOrNull(0), useMonochrome)
                    MiniAppIcon(apps.getOrNull(1), useMonochrome)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    MiniAppIcon(apps.getOrNull(2), useMonochrome)
                    MiniAppIcon(apps.getOrNull(3), useMonochrome)
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
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
private fun MiniAppIcon(app: AppModel?, useMonochrome: Boolean) {
    Box(modifier = Modifier.size(22.dp)) {
        if (app != null) {
            AppIcon(
                packageName = app.packageName,
                contentDescription = null,
                useMonochrome = useMonochrome,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
