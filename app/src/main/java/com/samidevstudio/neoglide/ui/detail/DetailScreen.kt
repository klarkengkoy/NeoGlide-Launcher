package com.samidevstudio.neoglide.ui.detail

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samidevstudio.neoglide.ui.components.AppIcon

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    label: String,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    with(sharedTransitionScope) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.sharedElement(
                    rememberSharedContentState(key = "icon-$packageName"),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            ) {
                AppIcon(
                    packageName = packageName,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.sharedElement(
                    rememberSharedContentState(key = "label-$packageName"),
                    animatedVisibilityScope = animatedVisibilityScope,
                ),
            )
        }
    }
}
