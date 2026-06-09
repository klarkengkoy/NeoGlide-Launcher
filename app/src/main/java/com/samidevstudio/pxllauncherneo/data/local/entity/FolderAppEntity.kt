package com.samidevstudio.pxllauncherneo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "folder_apps",
    primaryKeys = ["folderId", "packageName"],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class FolderAppEntity(
    val folderId: Int,
    val packageName: String,
    val displayOrder: Int
)
