package com.samidevstudio.pxllauncherneo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val row: Float,
    val column: Float
)
