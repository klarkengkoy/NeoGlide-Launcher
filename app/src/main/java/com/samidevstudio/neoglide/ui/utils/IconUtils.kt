package com.samidevstudio.neoglide.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.samidevstudio.neoglide.domain.model.AppCategory

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
