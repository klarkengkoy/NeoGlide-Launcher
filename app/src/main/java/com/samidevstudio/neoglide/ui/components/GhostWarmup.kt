package com.samidevstudio.neoglide.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.samidevstudio.neoglide.domain.model.AppModel

@Composable
fun GhostWarmup(
    apps: List<AppModel>
) {
    if (apps.isEmpty()) return

    LaunchedEffect(Unit) {
        android.util.Log.d("NeoGlideSplash", "Ghost UI Warmup started for ${apps.size} apps")
    }

    // Render an invisible 1px grid to force JIT and GPU uploads
    Box(
        modifier = Modifier
            .size(1.dp)
            .alpha(0.01f)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier.size(1.dp)
        ) {
            items(apps) { app ->
                AppIcon(
                    packageName = app.packageName,
                    contentDescription = null
                )
            }
        }
    }

    SideEffect {
        // This will run on every recomposition, but after the first one we know we've "touched" the UI
        android.util.Log.v("NeoGlideSplash", "Ghost UI Warmup pass completed")
    }
}
