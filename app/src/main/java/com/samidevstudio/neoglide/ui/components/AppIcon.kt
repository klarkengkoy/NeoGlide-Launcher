package com.samidevstudio.neoglide.ui.components

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
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
import com.samidevstudio.neoglide.ui.utils.LocalIconCache
import com.samidevstudio.neoglide.ui.utils.IconLoader
import com.samidevstudio.neoglide.ui.utils.LocalIconLoader
import androidx.compose.runtime.remember

import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

@Composable
fun rememberDrawablePainter(drawable: Drawable?): Painter {
    return remember(drawable) {
        object : Painter() {
            override val intrinsicSize: Size
                get() = drawable?.let { 
                    Size(it.intrinsicWidth.toFloat(), it.intrinsicHeight.toFloat()) 
                } ?: Size.Unspecified

            override fun DrawScope.onDraw() {
                drawable?.let {
                    drawIntoCanvas { canvas ->
                        it.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                        it.draw(canvas.nativeCanvas)
                    }
                }
            }
        }
    }
}

@Composable
fun AppIcon(
    packageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    useMonochrome: Boolean = false,
    iconPackPackageName: String? = null,
    refreshTrigger: Int = 0, // Used to force re-fetch from LauncherApps
) {
    val context = LocalContext.current
    val iconCache = LocalIconCache.current
    val iconLoader = LocalIconLoader.current
    val colorScheme = MaterialTheme.colorScheme

    val cacheKey = "$packageName-$useMonochrome-$iconPackPackageName"
    
    val initialValue = remember(cacheKey) { 
        iconCache.get(cacheKey) ?: (null to false) 
    }

    val iconResult by produceState(
        initialValue = initialValue,
        packageName,
        useMonochrome,
        iconPackPackageName,
        if (iconCache.isDynamic(packageName) || iconCache.get(cacheKey) == null) refreshTrigger else 0
    ) {
        value = iconLoader.loadIcon(packageName, useMonochrome, iconPackPackageName, refreshTrigger)
    }
    
    val iconData = iconResult.first
    val isActuallyMonochrome = iconResult.second

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                if (isActuallyMonochrome) colorScheme.primaryContainer 
                else colorScheme.surfaceVariant.copy(alpha = 0.1f)
            ),
        contentAlignment = Alignment.Center
    ) {
        val imageModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && (iconData is AdaptiveIconDrawable)) {
            Modifier.fillMaxSize()
        } else {
            Modifier.size(if (isActuallyMonochrome) 38.dp else 52.dp)
        }

        if (iconData != null) {
            // Bypass Coil's state machine for cached icons
            Image(
                painter = rememberDrawablePainter(iconData),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = imageModifier,
                colorFilter = if (isActuallyMonochrome) ColorFilter.tint(colorScheme.onPrimaryContainer) else null
            )
        } else {
            // Fallback to Coil only if data is not yet available
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconData)
                    .crossfade(enable = true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = imageModifier,
                colorFilter = if (isActuallyMonochrome) ColorFilter.tint(colorScheme.onPrimaryContainer) else null
            )
        }
    }
}
