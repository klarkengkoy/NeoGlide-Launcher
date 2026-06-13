package com.samidevstudio.neoglide.ui.components

import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppIcon(
    packageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    useMonochrome: Boolean = false,
    iconPackPackageName: String? = null,
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Load icon asynchronously to avoid blocking the main thread
    val iconResult by produceState<Pair<android.graphics.drawable.Drawable?, Boolean>>(
        initialValue = null to false,
        key1 = packageName,
        key2 = useMonochrome,
        key3 = iconPackPackageName
    ) {
        value = withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val icon = pm.getApplicationIcon(packageName)
                if (useMonochrome && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) && 
                (icon is AdaptiveIconDrawable) && (icon.monochrome != null)) {
                    icon.monochrome to true
                } else {
                    icon to false
                }
            } catch (_: Exception) {
                null to false
            }
        }
    }
    
    val iconData = iconResult.first
    val isActuallyMonochrome = iconResult.second

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isActuallyMonochrome) colorScheme.primaryContainer 
                else colorScheme.surfaceVariant.copy(alpha = 0.1f)
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(iconData)
                .crossfade(enable = true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && (iconData is AdaptiveIconDrawable)) {
                Modifier.fillMaxSize()
            } else {
                // Scaling for monochrome layer or legacy icons
                // 32dp (66% of 48dp) matches the standard adaptive icon safe zone weight
                Modifier.size(if (isActuallyMonochrome) 32.dp else 44.dp)
            },
            colorFilter = if (isActuallyMonochrome) ColorFilter.tint(colorScheme.onPrimaryContainer) else null
        )
    }
}
