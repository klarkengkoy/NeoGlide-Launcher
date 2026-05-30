package com.samidevstudio.pxllauncherneo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val category: String,
    val isFavorite: Boolean = false,
    val installTime: Long,
    val lastUsedTime: Long = 0L
)
