package com.samidevstudio.neoglide.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.samidevstudio.neoglide.domain.model.AppCategory

fun AppCategory.toIcon(): ImageVector {
    return resolveIcon(this.iconName ?: "apps")
}

fun resolveIcon(iconName: String): ImageVector {
    return when (iconName) {
        "chat" -> Icons.AutoMirrored.Filled.Chat
        "build" -> Icons.Default.Build
        "play_circle" -> Icons.Default.PlayCircle
        "sports_esports" -> Icons.Default.SportsEsports
        "account_balance" -> Icons.Default.AccountBalance
        "shopping_cart" -> Icons.Default.ShoppingCart
        "camera_alt" -> Icons.Default.CameraAlt
        "favorite" -> Icons.Default.Favorite
        "directions_car" -> Icons.Default.DirectionsCar
        "restaurant" -> Icons.Default.Restaurant
        "school" -> Icons.Default.School
        "settings" -> Icons.Default.Settings
        "newspaper" -> Icons.Default.Newspaper
        "music_note" -> Icons.Default.MusicNote
        "flight" -> Icons.Default.Flight
        "chat_bubble" -> Icons.Default.ChatBubble
        "health_and_safety" -> Icons.Default.HealthAndSafety
        "wb_sunny" -> Icons.Default.WbSunny
        "style" -> Icons.Default.Style
        "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
        "business_center" -> Icons.Default.BusinessCenter
        "handyman" -> Icons.Default.Handyman
        "map" -> Icons.Default.Map
        "sports_soccer" -> Icons.Default.SportsSoccer
        "home" -> Icons.Default.Home
        "child_care" -> Icons.Default.ChildCare
        "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
        "visibility_off" -> Icons.Default.VisibilityOff
        "folder" -> Icons.Default.Folder
        "star" -> Icons.Default.Star
        "local_shipping" -> Icons.Default.LocalShipping
        "savings" -> Icons.Default.Savings
        "gamepad" -> Icons.Default.Gamepad
        "movie" -> Icons.Default.Movie
        "brush" -> Icons.Default.Brush
        "lightbulb" -> Icons.Default.Lightbulb
        "public" -> Icons.Default.Public
        "group" -> Icons.Default.Group
        "shopping_bag" -> Icons.Default.ShoppingBag
        "work" -> Icons.Default.Work
        "event" -> Icons.Default.Event
        "mail" -> Icons.Default.Mail
        "call" -> Icons.Default.Call
        "lock" -> Icons.Default.Lock
        "security" -> Icons.Default.Security
        "fitness_center" -> Icons.Default.FitnessCenter
        "vibration" -> Icons.Default.Vibration
        "volume_up" -> Icons.AutoMirrored.Filled.VolumeUp
        "wifi" -> Icons.Default.Wifi
        "bluetooth" -> Icons.Default.Bluetooth
        "battery_full" -> Icons.Default.BatteryFull
        "calculate" -> Icons.Default.Calculate
        "schedule" -> Icons.Default.Schedule
        "explore" -> Icons.Default.Explore
        "landscape" -> Icons.Default.Landscape
        "fastfood" -> Icons.Default.Fastfood
        "local_pizza" -> Icons.Default.LocalPizza
        "local_cafe" -> Icons.Default.LocalCafe
        "coffee" -> Icons.Default.Coffee
        "directions_run" -> Icons.AutoMirrored.Filled.DirectionsRun
        "directions_bike" -> Icons.AutoMirrored.Filled.DirectionsBike
        "self_improvement" -> Icons.Default.SelfImprovement
        "psychology" -> Icons.Default.Psychology
        "auto_stories" -> Icons.Default.AutoStories
        "extension" -> Icons.Default.Extension
        "memory" -> Icons.Default.Memory
        "terminal" -> Icons.Default.Terminal
        "code" -> Icons.Default.Code
        else -> Icons.Default.Apps
    }
}
