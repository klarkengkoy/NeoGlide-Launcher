package com.samidevstudio.neoglide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_apps")
data class HomeAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val row: Float,
    val column: Float,
    val spanX: Float = 1f,
    val spanY: Float = 1f
)
