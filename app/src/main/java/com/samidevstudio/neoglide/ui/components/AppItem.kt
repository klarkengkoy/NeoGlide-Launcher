package com.samidevstudio.neoglide.ui.components

import android.app.ActivityOptions
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samidevstudio.neoglide.domain.model.AppModel
import com.samidevstudio.neoglide.domain.model.AppShortcut

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun AppItem(
    app: AppModel,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    useMonochrome: Boolean = false,
    iconPackPackageName: String? = null,
    isHidden: Boolean = false,
    hasNotification: Boolean = false,
    notificationCount: Int = 0,
    sharedElementKeyPrefix: String = "drawer",
    showLabel: Boolean = true,
    isLongClickEnabled: Boolean = true,
    isHovered: Boolean = false,
    isBlocked: Boolean = false,
    refreshTrigger: Int = 0,
    getShortcuts: suspend (String) -> List<AppShortcut> = { emptyList() },
    onShortcutClick: (AppShortcut) -> Unit = {},
    onHideToggle: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onClick: (android.os.Bundle?) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var shortcuts by remember { mutableStateOf<List<AppShortcut>>(emptyList()) }
    val view = LocalView.current
    var coords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    val hoverScale by animateFloatAsState(
        targetValue = if (isHovered) 0.8f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "hoverScale"
    )

    LaunchedEffect(showMenu) {
        if (showMenu) {
            shortcuts = getShortcuts(app.packageName)
        }
    }

    val boundsTransform = BoundsTransform { _, _ ->
        spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow
        )
    }

    with(sharedTransitionScope) {
        val sharedIconModifier = if (sharedElementKeyPrefix != "drawer") {
            Modifier.sharedElement(
                rememberSharedContentState(key = "$sharedElementKeyPrefix-icon-${app.packageName}"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = boundsTransform
            )
        } else Modifier

        val sharedLabelModifier = if (sharedElementKeyPrefix == "dock") {
            Modifier.sharedElement(
                rememberSharedContentState(key = "$sharedElementKeyPrefix-label-${app.packageName}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        } else Modifier

        val labelStyle = if (sharedElementKeyPrefix == "dock") {
            MaterialTheme.typography.labelSmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.6f),
                    offset = Offset(0f, 2f),
                    blurRadius = 8f
                )
            )
        } else {
            MaterialTheme.typography.labelSmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = modifier
                .onGloballyPositioned { coords = it }
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = hoverScale
                    scaleY = hoverScale
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isBlocked) Color.Red.copy(alpha = 0.2f) else Color.Transparent)
                    .combinedClickable(
                        onClick = {
                            val bundle = coords?.let {
                                val pos = it.positionInWindow()
                                ActivityOptions.makeScaleUpAnimation(
                                    view,
                                    pos.x.toInt(),
                                    pos.y.toInt(),
                                    it.size.width,
                                    it.size.height
                                ).toBundle()
                            }
                            onClick(bundle)
                        },
                        onLongClick = if (isLongClickEnabled && onLongClick != null) { 
                            { 
                                onLongClick()
                            } 
                        } else null
                    )
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = sharedIconModifier) {
                    AppIcon(
                        packageName = app.packageName,
                        contentDescription = app.label,
                        useMonochrome = useMonochrome,
                        iconPackPackageName = iconPackPackageName,
                        refreshTrigger = refreshTrigger
                    )
                    
                    // NOTIFICATION DOT
                    if (hasNotification) {
                        val dotColor = MaterialTheme.colorScheme.primary
                        val borderColor = if (sharedElementKeyPrefix != "drawer") Color.White else MaterialTheme.colorScheme.surface
                        
                        if (notificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .background(dotColor, CircleShape)
                                    .padding(1.dp)
                                    .background(borderColor, CircleShape)
                                    .padding(1.dp)
                                    .background(dotColor, CircleShape)
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(10.dp)
                                    .background(dotColor, CircleShape)
                                    .padding(1.dp)
                                    .background(borderColor, CircleShape)
                                    .padding(1.dp)
                                    .background(dotColor, CircleShape)
                            )
                        }
                    }
                    
                    if (onLongClick == null) {
                        AppContextMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            packageName = app.packageName,
                            label = app.label,
                            shortcuts = shortcuts,
                            isHidden = isHidden,
                            onShortcutClick = onShortcutClick,
                            onHideToggle = onHideToggle
                        )
                    }
                }
                if (showLabel) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = app.label,
                        style = labelStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = sharedLabelModifier
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SearchAppItem(
    app: AppModel,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    useMonochrome: Boolean = false,
    iconPackPackageName: String? = null,
    isHidden: Boolean = false,
    hasNotification: Boolean = false,
    notificationCount: Int = 0,
    sharedElementKeyPrefix: String = "search",
    showLabel: Boolean = true,
    refreshTrigger: Int = 0,
    onLongClick: (() -> Unit)? = null,
    getShortcuts: suspend (String) -> List<AppShortcut> = { emptyList() },
    onShortcutClick: (AppShortcut) -> Unit = {},
    onHideToggle: () -> Unit = {},
    onClick: (android.os.Bundle?) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var shortcuts by remember { mutableStateOf<List<AppShortcut>>(emptyList()) }
    val view = LocalView.current
    var coords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    LaunchedEffect(showMenu) {
        if (showMenu) {
            shortcuts = getShortcuts(app.packageName)
        }
    }

    val boundsTransform = BoundsTransform { _, _ ->
        spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow
        )
    }

    with(sharedTransitionScope) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords = it }
                .wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClick = {
                            val bundle = coords?.let {
                                val pos = it.positionInWindow()
                                ActivityOptions.makeScaleUpAnimation(
                                    view,
                                    pos.x.toInt(),
                                    pos.y.toInt(),
                                    it.size.width,
                                    it.size.height
                                ).toBundle()
                            }
                            onClick(bundle)
                        },
                        onLongClick = { 
                            if (onLongClick != null) {
                                onLongClick()
                            } else {
                                showMenu = true
                            }
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "$sharedElementKeyPrefix-icon-${app.packageName}"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = boundsTransform
                    )
                ) {
                    AppIcon(
                        packageName = app.packageName,
                        contentDescription = app.label,
                        useMonochrome = useMonochrome,
                        iconPackPackageName = iconPackPackageName,
                        refreshTrigger = refreshTrigger
                    )
                    
                    // NOTIFICATION DOT
                    if (hasNotification) {
                        val dotColor = MaterialTheme.colorScheme.primary
                        val borderColor = Color.White
                        
                        if (notificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .background(dotColor, CircleShape)
                                    .padding(1.dp)
                                    .background(borderColor, CircleShape)
                                    .padding(1.dp)
                                    .background(dotColor, CircleShape)
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(10.dp)
                                    .background(dotColor, CircleShape)
                                    .padding(1.dp)
                                    .background(borderColor, CircleShape)
                                    .padding(1.dp)
                                    .background(dotColor, CircleShape)
                            )
                        }
                    }
                    
                    if (onLongClick == null) {
                        AppContextMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            packageName = app.packageName,
                            label = app.label,
                            shortcuts = shortcuts,
                            isHidden = isHidden,
                            onShortcutClick = onShortcutClick,
                            onHideToggle = onHideToggle
                        )
                    }
                }
                if (showLabel) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState(key = "$sharedElementKeyPrefix-label-${app.packageName}"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = boundsTransform
                        )
                    )
                }
            }
        }
    }
}
