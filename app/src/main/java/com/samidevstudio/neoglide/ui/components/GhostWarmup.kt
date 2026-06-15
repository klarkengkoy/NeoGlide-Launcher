package com.samidevstudio.neoglide.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.samidevstudio.neoglide.domain.model.AppModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GhostWarmup(
    apps: List<AppModel>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    if (apps.isEmpty()) return

    // recycling index to touch every app icon/item without a massive composition
    var currentAppIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(apps) {
        // Touch every app one-by-one to trigger JIT and GPU uploads
        for (i in apps.indices) {
            currentAppIndex = i
            // Minimal delay to ensure each one gets a "draw" pass without blocking the main thread
            delay(10) 
        }
        
        currentAppIndex = -1
    }

    // Render a tiny invisible slot that swaps through all apps
    Box(
        modifier = Modifier
            .size(1.dp)
            .alpha(0.01f)
    ) {
        if (currentAppIndex != -1 && currentAppIndex < apps.size) {
            val app = apps[currentAppIndex]
            key(app.packageName) {
                AppItem(
                    app = app,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = { }
                )
            }
        }
    }
}
