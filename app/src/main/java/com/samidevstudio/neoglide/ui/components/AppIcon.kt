package com.samidevstudio.neoglide.ui.components

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.Log
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
import java.util.Calendar

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
    val colorScheme = MaterialTheme.colorScheme
    val TAG = "AppIcon"
    
    // Load icon asynchronously to avoid blocking the main thread
    val iconResult by produceState<Pair<Drawable?, Boolean>>(
        initialValue = null to false,
        packageName,
        useMonochrome,
        iconPackPackageName,
        refreshTrigger
    ) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading icon for $packageName")
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val userHandle = Process.myUserHandle()
                val activities = launcherApps.getActivityList(packageName, userHandle)
                
                if (activities.isNotEmpty()) {
                    val info = activities[0]
                    var icon = info.getIcon(0)
                    Log.d(TAG, "Found activity for $packageName, icon type: ${icon?.javaClass?.simpleName}")

                    // Metadata keys for dynamic icons
                    val CALENDAR_METADATA = "com.google.android.calendar.dynamic_icons"
                    val CALENDAR_LEVEL_METADATA = "com.android.launcher3.LEVEL_PER_DAY"
                    
                    // Compatibility fix for metadata access - Always use PM to ensure metadata is fetched
                    val pm = context.packageManager
                    val activityInfo = pm.getActivityInfo(info.componentName, PackageManager.GET_META_DATA)
                    val metaData = activityInfo.metaData

                    if (!useMonochrome && metaData != null) {
                        // Check for Calendar Dynamic Icon
                        if (metaData.containsKey(CALENDAR_METADATA)) {
                            val arrayResId = metaData.getInt(CALENDAR_METADATA)
                            Log.d(TAG, "Calendar metadata found for $packageName, arrayResId: $arrayResId")
                            if (arrayResId != 0) {
                                try {
                                    val resources = pm.getResourcesForApplication(packageName)
                                    val dayArray = resources.obtainTypedArray(arrayResId)
                                    val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                                    val iconResId = dayArray.getResourceId(day - 1, 0)
                                    Log.d(TAG, "Calendar day: $day, iconResId: $iconResId")
                                    dayArray.recycle()
                                    if (iconResId != 0) {
                                        @Suppress("DEPRECATION")
                                        icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            resources.getDrawable(iconResId, null)
                                        } else {
                                            resources.getDrawable(iconResId)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error loading calendar icon for $packageName", e)
                                }
                            }
                        } else if (metaData.containsKey(CALENDAR_LEVEL_METADATA)) {
                            Log.d(TAG, "AOSP Calendar metadata found for $packageName")
                            val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                            icon?.level = day
                        }
                    }

                    if (useMonochrome && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) && 
                        (icon is AdaptiveIconDrawable) && (icon.monochrome != null)) {
                        Log.d(TAG, "Using monochrome layer for $packageName")
                        value = icon.monochrome to true
                    } else {
                        value = icon to false
                    }
                } else {
                    Log.w(TAG, "No activities found for $packageName")
                    value = null to false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading icon for $packageName", e)
                value = null to false
            }
        }
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
                Modifier.size(if (isActuallyMonochrome) 38.dp else 52.dp)
            },
            colorFilter = if (isActuallyMonochrome) ColorFilter.tint(colorScheme.onPrimaryContainer) else null
        )
    }
}
