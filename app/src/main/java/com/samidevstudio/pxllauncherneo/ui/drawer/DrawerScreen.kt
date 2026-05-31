package com.samidevstudio.pxllauncherneo.ui.drawer

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samidevstudio.pxllauncherneo.data.repository.*
import com.samidevstudio.pxllauncherneo.domain.model.AppCategory
import com.samidevstudio.pxllauncherneo.domain.model.AppModel
import com.samidevstudio.pxllauncherneo.domain.model.AppShortcut
import com.samidevstudio.pxllauncherneo.ui.components.AppItem
import com.samidevstudio.pxllauncherneo.ui.components.SearchAppItem
import com.samidevstudio.pxllauncherneo.ui.settings.SettingsSheet
import com.samidevstudio.pxllauncherneo.ui.settings.SettingsViewModel
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

internal class DragTargetInfo {
    var isDragging: Boolean by mutableStateOf(false)
    var dragPosition by mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
    var dragOffset by mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
    var draggableItem by mutableStateOf<AppModel?>(null)
}

internal val LocalDragTargetInfo = compositionLocalOf { DragTargetInfo() }

enum class CategoryOrientation {
    HORIZONTAL_BOTTOM,
    VERTICAL_LEFT,
    VERTICAL_RIGHT
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DrawerScreen(
    modifier: Modifier = Modifier,
    viewModel: DrawerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAppClick: (String, android.os.Bundle?) -> Unit,
) {
    val categorizedApps by viewModel.categorizedApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val recentlyUsedApps by viewModel.recentlyUsedApps.collectAsStateWithLifecycle()
    val webSuggestions by viewModel.webSuggestions.collectAsStateWithLifecycle()
    val activeNotifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val preferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<AppCategory?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    
    val orientation = remember(preferences.categoryBarType) {
        when (preferences.categoryBarType) {
            CategoryBarType.LEFT -> CategoryOrientation.VERTICAL_LEFT
            CategoryBarType.RIGHT -> CategoryOrientation.VERTICAL_RIGHT
            CategoryBarType.BOTTOM -> CategoryOrientation.HORIZONTAL_BOTTOM
            CategoryBarType.NONE -> CategoryOrientation.VERTICAL_RIGHT
        }
    }
    
    val showCategoryBar = preferences.categoryBarType != CategoryBarType.NONE
    
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val dragInfo = remember { DragTargetInfo() }

    CompositionLocalProvider(LocalDragTargetInfo provides dragInfo) {
        BackHandler(isSearchActive || showSettings) {
            if (showSettings) {
                showSettings = false
            } else if (isSearchActive) {
                isSearchActive = false
                viewModel.onSearchQueryChanged("")
                focusManager.clearFocus()
            }
        }

        val headerContent = @Composable {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedCategory?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "All Apps",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                    
                    IconButton(
                        onClick = { isSearchActive = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = "Search", 
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(
                        onClick = { 
                            val intent = context.packageManager.getLaunchIntentForPackage("com.android.vending")
                            if (intent != null) {
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Play Store not found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Storefront, 
                            contentDescription = "Play Store", 
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Tune, 
                            contentDescription = "Pxl Settings", 
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    IconButton(
                        onClick = { 
                            context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings, 
                            contentDescription = "System Settings", 
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isSearchActive,
                    enter = scaleIn(
                        animationSpec = tween(400),
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.8f, 0.5f)
                    ) + fadeIn(animationSpec = tween(400)),
                    exit = scaleOut(
                        animationSpec = tween(400),
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.8f, 0.5f)
                    ) + fadeOut(animationSpec = tween(400))
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .focusRequester(focusRequester),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { 
                                    viewModel.launchFirstResult()
                                    isSearchActive = false
                                    viewModel.onSearchQueryChanged("")
                                    focusManager.clearFocus()
                                }),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    "Search",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                }
                            )
                            
                            IconButton(onClick = { 
                                isSearchActive = false
                                viewModel.onSearchQueryChanged("")
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    Icons.Default.Close, 
                                    contentDescription = "Cancel",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    LaunchedEffect(isSearchActive) {
                        if (isSearchActive) {
                            focusRequester.requestFocus()
                        }
                    }
                }
            }
        }

        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(top = 16.dp),
            ) {
                headerContent()
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxSize()) {
                    if (searchQuery.isNotBlank() || isSearchActive) {
                        SearchResults(
                            filteredApps = filteredApps,
                            recentlyUsedApps = if (searchQuery.isBlank()) recentlyUsedApps else emptyList(),
                            webSuggestions = webSuggestions,
                            modifier = Modifier.fillMaxSize(),
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onAppClick = onAppClick,
                            useMonochrome = preferences.useMonochromeIcons,
                            iconPackPackageName = preferences.iconPackPackageName,
                            hiddenPackages = preferences.hiddenPackages,
                            activeNotifications = activeNotifications,
                            showNotificationDots = preferences.notificationDotMode in listOf(NotificationDotMode.APP_ICON, NotificationDotMode.BOTH),
                            getShortcuts = { viewModel.getShortcuts(it) },
                            onShortcutClick = { viewModel.launchShortcut(it) },
                            onHideToggle = { packageName, isHidden ->
                                if (isHidden) viewModel.unhideApp(packageName) else viewModel.hideApp(packageName)
                            }
                        ) { query ->
                            val intent = Intent(Intent.ACTION_VIEW, "https://www.google.com/search?q=$query".toUri())
                            context.startActivity(intent)
                        }
                    } else {
                        val appsToDisplay = remember(categorizedApps, selectedCategory) {
                            getFilteredApps(categorizedApps, selectedCategory).sortedBy { it.label }
                        }

                        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                            val categories = remember(categorizedApps) {
                                categorizedApps.keys.sortedWith(
                                    compareBy<AppCategory> { it == AppCategory.HIDDEN }
                                        .thenBy { it.name }
                                )
                            }
                            val allCategories = remember(categories) { listOf(null) + categories }
                            
                            val isVertical = orientation != CategoryOrientation.HORIZONTAL_BOTTOM
                            val barWidth = if (!showCategoryBar) 0.dp else if (isVertical) 56.dp else 0.dp

                            Row(modifier = Modifier.fillMaxSize()) {
                                if (showCategoryBar && orientation == CategoryOrientation.VERTICAL_LEFT) {
                                    Box(
                                        modifier = Modifier
                                            .width(barWidth)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CategorySelector(
                                            allCategories = allCategories,
                                            selectedCategory = selectedCategory,
                                            orientation = orientation,
                                            onCategorySelected = { selectedCategory = it },
                                            onDrop = { app, category -> 
                                                category?.let { viewModel.moveAppToCategory(app.packageName, it) }
                                            },
                                            activeNotifications = activeNotifications,
                                            categorizedApps = categorizedApps,
                                            showNotificationDots = preferences.notificationDotMode in listOf(NotificationDotMode.CATEGORY_BAR, NotificationDotMode.BOTH)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    AppGrid(
                                        apps = appsToDisplay,
                                        columns = 4,
                                        bottomPadding = if (showCategoryBar && orientation == CategoryOrientation.HORIZONTAL_BOTTOM) 8.dp else 20.dp,
                                        modifier = Modifier.weight(1f),
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        useMonochrome = preferences.useMonochromeIcons,
                                        iconPackPackageName = preferences.iconPackPackageName,
                                        hiddenPackages = preferences.hiddenPackages,
                                        activeNotifications = activeNotifications,
                                        showNotificationDots = preferences.notificationDotMode in listOf(NotificationDotMode.APP_ICON, NotificationDotMode.BOTH),
                                        getShortcuts = { viewModel.getShortcuts(it) },
                                        onShortcutClick = { viewModel.launchShortcut(it) },
                                        onHideToggle = { packageName, isHidden ->
                                            if (isHidden) viewModel.unhideApp(packageName) else viewModel.hideApp(packageName)
                                        },
                                        onAppClick = onAppClick
                                    )

                                    if (showCategoryBar && orientation == CategoryOrientation.HORIZONTAL_BOTTOM) {
                                        CategorySelector(
                                            allCategories = allCategories,
                                            selectedCategory = selectedCategory,
                                            orientation = orientation,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(64.dp),
                                            onCategorySelected = { selectedCategory = it },
                                            onDrop = { app, category -> 
                                                category?.let { viewModel.moveAppToCategory(app.packageName, it) }
                                            },
                                            activeNotifications = activeNotifications,
                                            categorizedApps = categorizedApps,
                                            showNotificationDots = preferences.notificationDotMode in listOf(NotificationDotMode.CATEGORY_BAR, NotificationDotMode.BOTH)
                                        )
                                    }
                                }

                                if (showCategoryBar && orientation == CategoryOrientation.VERTICAL_RIGHT) {
                                    Box(
                                        modifier = Modifier
                                            .width(barWidth)
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CategorySelector(
                                            allCategories = allCategories,
                                            selectedCategory = selectedCategory,
                                            orientation = orientation,
                                            onCategorySelected = { selectedCategory = it },
                                            onDrop = { app, category -> 
                                                category?.let { viewModel.moveAppToCategory(app.packageName, it) }
                                            },
                                            activeNotifications = activeNotifications,
                                            categorizedApps = categorizedApps,
                                            showNotificationDots = preferences.notificationDotMode in listOf(NotificationDotMode.CATEGORY_BAR, NotificationDotMode.BOTH)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showSettings) {
                SettingsSheet(
                    onDismiss = { showSettings = false },
                    viewModel = settingsViewModel
                )
            }

            // FLOATING DRAG ICON
            if (dragInfo.isDragging && dragInfo.draggableItem != null) {
                val app = dragInfo.draggableItem!!
                val density = androidx.compose.ui.platform.LocalDensity.current
                val iconSizePx = with(density) { 80.dp.toPx() }
                
                Box(
                    modifier = Modifier
                        .offset { 
                            androidx.compose.ui.unit.IntOffset(
                                (dragInfo.dragPosition.x + dragInfo.dragOffset.x - iconSizePx / 2).toInt(),
                                (dragInfo.dragPosition.y + dragInfo.dragOffset.y - iconSizePx / 2).toInt()
                            )
                        }
                        .size(80.dp)
                        .graphicsLayer {
                            alpha = 0.7f
                            scaleX = 1.2f
                            scaleY = 1.2f
                        }
                ) {
                    com.samidevstudio.pxllauncherneo.ui.components.AppIcon(
                        packageName = app.packageName,
                        contentDescription = app.label,
                        useMonochrome = preferences.useMonochromeIcons
                    )
                }
            }
        }
    }
}

private fun getFilteredApps(
    categorizedApps: Map<AppCategory, List<AppModel>>,
    selectedCategory: AppCategory?
): List<AppModel> {
    return if (selectedCategory == null) {
        categorizedApps.values.flatten()
    } else {
        categorizedApps[selectedCategory] ?: emptyList()
    }
}

@Composable
fun CategorySelector(
    allCategories: List<AppCategory?>,
    selectedCategory: AppCategory?,
    orientation: CategoryOrientation,
    modifier: Modifier = Modifier,
    onCategorySelected: (AppCategory?) -> Unit,
    onDrop: (AppModel, AppCategory?) -> Unit = { _, _ -> },
    activeNotifications: Set<String> = emptySet(),
    categorizedApps: Map<AppCategory, List<AppModel>> = emptyMap(),
    showNotificationDots: Boolean = true
) {
    val isVertical = orientation != CategoryOrientation.HORIZONTAL_BOTTOM
    val density = androidx.compose.ui.platform.LocalDensity.current
    val dragInfo = LocalDragTargetInfo.current
    
    // Constants for calculation
    val iconSize = 40.dp
    val defaultSpacing = 8.dp
    
    var containerSize by remember { mutableFloatStateOf(0f) }
    var containerOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val handleGesture = { offset: androidx.compose.ui.geometry.Offset ->
        val count = allCategories.size
        if (count > 0 && containerSize > 0) {
            val iconSizePx = with(density) { iconSize.toPx() }
            val spacingPx = with(density) { defaultSpacing.toPx() }
            val itemTotalSize = iconSizePx + spacingPx
            
            // Calculate total content size to find centering offset
            val totalContentSize = (iconSizePx * count) + (spacingPx * (count - 1))
            val startOffset = (containerSize - totalContentSize) / 2

            val touchPos = if (isVertical) offset.y else offset.x
            val relativeTouch = touchPos - startOffset
            
            val index = (relativeTouch / itemTotalSize).toInt().coerceIn(0, count - 1)
            onCategorySelected(allCategories[index])
        }
    }

    if (isVertical) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .onGloballyPositioned { 
                    containerSize = it.size.height.toFloat()
                    containerOffset = it.positionInWindow()
                }
                .pointerInput(allCategories) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        handleGesture(down.position)
                        drag(down.id) { change ->
                            change.consume()
                            handleGesture(change.position)
                        }
                    }
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            allCategories.forEachIndexed { index, category ->
                val itemSizePx = with(density) { iconSize.toPx() }
                val spacingPx = with(density) { defaultSpacing.toPx() }
                val itemTop = index * (itemSizePx + spacingPx)
                val itemBottom = itemTop + itemSizePx
                
                val globalTouchPos = dragInfo.dragPosition + dragInfo.dragOffset
                val localTouchY = globalTouchPos.y - containerOffset.y
                val isHovered = dragInfo.isDragging && 
                                globalTouchPos.x >= containerOffset.x && 
                                globalTouchPos.x <= containerOffset.x + with(density) { 56.dp.toPx() } &&
                                localTouchY >= itemTop && localTouchY <= itemBottom
                
                val categoryHasNotif = remember(category, activeNotifications, categorizedApps) {
                    if (category == null) {
                        activeNotifications.isNotEmpty()
                    } else {
                        val appPackages = categorizedApps[category]?.map { it.packageName } ?: emptyList()
                        appPackages.any { it in activeNotifications }
                    }
                }

                LaunchedEffect(dragInfo.isDragging) {
                    if (!dragInfo.isDragging && isHovered) {
                        dragInfo.draggableItem?.let { onDrop(it, category) }
                    }
                }

                CategoryIconItem(
                    isSelected = selectedCategory == category,
                    icon = category?.toIcon() ?: Icons.Default.AllInclusive,
                    isHovered = isHovered,
                    hasNotification = categoryHasNotif && showNotificationDots,
                    size = iconSize
                )
                
                if (index < allCategories.size - 1) {
                    Spacer(modifier = Modifier.height(defaultSpacing))
                }
            }
        }
    } else {
        // Horizontal Rail
        Row(
            modifier = modifier
                .fillMaxWidth()
                .onGloballyPositioned { 
                    containerSize = it.size.width.toFloat()
                    containerOffset = it.positionInWindow()
                }
                .pointerInput(allCategories) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        handleGesture(down.position)
                        drag(down.id) { change ->
                            change.consume()
                            handleGesture(change.position)
                        }
                    }
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            allCategories.forEachIndexed { index, category ->
                val categoryHasNotif = remember(category, activeNotifications, categorizedApps) {
                    if (category == null) {
                        activeNotifications.isNotEmpty()
                    } else {
                        val appPackages = categorizedApps[category]?.map { it.packageName } ?: emptyList()
                        appPackages.any { it in activeNotifications }
                    }
                }

                CategoryIconItem(
                    isSelected = selectedCategory == category,
                    icon = category?.toIcon() ?: Icons.Default.AllInclusive,
                    hasNotification = categoryHasNotif && showNotificationDots,
                    size = iconSize
                )

                if (index < allCategories.size - 1) {
                    Spacer(modifier = Modifier.width(defaultSpacing))
                }
            }
        }
    }
}

@Composable
private fun CategoryIconItem(
    isSelected: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isHovered: Boolean = false,
    hasNotification: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    val scale by animateFloatAsState(if (isHovered) 1.2f else 1f)
    val color = if (isHovered) MaterialTheme.colorScheme.secondaryContainer else if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .then(if (isSelected && !isHovered) Modifier.padding(4.dp) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = color,
            contentColor = if (isHovered) MaterialTheme.colorScheme.onSecondaryContainer else if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null)
            }
        }
        
        if (hasNotification) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

fun AppCategory.toIcon(): ImageVector {
    return when (this) {
        AppCategory.COMMUNICATION -> Icons.AutoMirrored.Filled.Chat
        AppCategory.GAMES -> Icons.Default.SportsEsports
        AppCategory.MEDIA -> Icons.Default.PlayCircle
        AppCategory.UTILITIES -> Icons.Default.Build
        AppCategory.SOCIAL -> Icons.Default.Groups
        AppCategory.SHOPPING -> Icons.Default.ShoppingCart
        AppCategory.EDUCATION -> Icons.Default.School
        AppCategory.LIFESTYLE -> Icons.Default.Favorite
        AppCategory.SYSTEM -> Icons.Default.Settings
        AppCategory.HIDDEN -> Icons.Default.VisibilityOff
        else -> Icons.Default.Apps
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppGrid(
    apps: List<AppModel>,
    columns: Int,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 20.dp,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    useMonochrome: Boolean = false,
    iconPackPackageName: String? = null,
    hiddenPackages: Set<String> = emptySet(),
    activeNotifications: Set<String> = emptySet(),
    showNotificationDots: Boolean = true,
    getShortcuts: suspend (String) -> List<AppShortcut> = { emptyList() },
    onShortcutClick: (AppShortcut) -> Unit = {},
    onHideToggle: (String, Boolean) -> Unit = { _, _ -> },
    onAppClick: (String, android.os.Bundle?) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        reverseLayout = false,
        contentPadding = PaddingValues(bottom = bottomPadding, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
        modifier = modifier
    ) {
        items(
            count = apps.size,
            key = { index -> apps[index].packageName }
        ) { index ->
            val app = apps[index]
            val dragInfo = LocalDragTargetInfo.current
            var itemPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
            Box(
                modifier = Modifier
                    .onGloballyPositioned { itemPosition = it.positionInWindow() }
                    .pointerInput(app) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                dragInfo.draggableItem = app
                                dragInfo.isDragging = true
                                dragInfo.dragPosition = itemPosition + offset
                                dragInfo.dragOffset = androidx.compose.ui.geometry.Offset.Zero
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragInfo.dragOffset += dragAmount
                            },
                            onDragEnd = {
                                dragInfo.isDragging = false
                            },
                            onDragCancel = {
                                dragInfo.isDragging = false
                                dragInfo.draggableItem = null
                            }
                        )
                    }
            ) {
                AppItem(
                    app = app,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    useMonochrome = useMonochrome,
                    iconPackPackageName = iconPackPackageName,
                    isHidden = app.packageName in hiddenPackages,
                    hasNotification = showNotificationDots && app.packageName in activeNotifications,
                    sharedElementKeyPrefix = "drawer",
                    getShortcuts = getShortcuts,
                    onShortcutClick = onShortcutClick,
                    onHideToggle = { onHideToggle(app.packageName, app.packageName in hiddenPackages) }
                ) { options ->
                    onAppClick(app.packageName, options)
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchResults(
    filteredApps: List<AppModel>,
    webSuggestions: List<String>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAppClick: (String, android.os.Bundle?) -> Unit,
    modifier: Modifier = Modifier,
    recentlyUsedApps: List<AppModel> = emptyList(),
    useMonochrome: Boolean = false,
    iconPackPackageName: String? = null,
    hiddenPackages: Set<String> = emptySet(),
    activeNotifications: Set<String> = emptySet(),
    showNotificationDots: Boolean = true,
    getShortcuts: suspend (String) -> List<AppShortcut> = { emptyList() },
    onShortcutClick: (AppShortcut) -> Unit = {},
    onHideToggle: (String, Boolean) -> Unit = { _, _ -> },
    onWebSearch: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (recentlyUsedApps.isNotEmpty()) {
            item {
                Text(
                    text = "Recently Used",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    recentlyUsedApps.forEach { app ->
                        Box(modifier = Modifier.weight(1f)) {
                            AppItem(
                                app = app,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                useMonochrome = useMonochrome,
                                iconPackPackageName = iconPackPackageName,
                                isHidden = app.packageName in hiddenPackages,
                                hasNotification = showNotificationDots && app.packageName in activeNotifications,
                                sharedElementKeyPrefix = "recent",
                                showLabel = true,
                                getShortcuts = getShortcuts,
                                onShortcutClick = onShortcutClick,
                                onHideToggle = { onHideToggle(app.packageName, app.packageName in hiddenPackages) }
                            ) { options ->
                                onAppClick(app.packageName, options)
                            }
                        }
                    }
                    // Fill remaining space if less than 5 apps
                    repeat(5 - recentlyUsedApps.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (filteredApps.isNotEmpty()) {
            item {
                Text("Apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(filteredApps) { app ->
                SearchAppItem(
                    app = app,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    useMonochrome = useMonochrome,
                    iconPackPackageName = iconPackPackageName,
                    isHidden = app.packageName in hiddenPackages,
                    hasNotification = showNotificationDots && app.packageName in activeNotifications,
                    sharedElementKeyPrefix = "search",
                    getShortcuts = getShortcuts,
                    onShortcutClick = onShortcutClick,
                    onHideToggle = { onHideToggle(app.packageName, app.packageName in hiddenPackages) },
                    onClick = { options -> onAppClick(app.packageName, options) }
                )
            }
        }

        if (webSuggestions.isNotEmpty()) {
            item {
                Text("Web Suggestions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(webSuggestions) { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onWebSearch(suggestion) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(suggestion)
                }
            }
        }
    }
}
