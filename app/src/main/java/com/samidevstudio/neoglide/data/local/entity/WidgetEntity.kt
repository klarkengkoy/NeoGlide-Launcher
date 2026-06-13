package com.samidevstudio.neoglide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widgets")
data class WidgetEntity(
    @PrimaryKey val widgetId: Int,
    val providerPackage: String,
    val providerClass: String,
    val label: String,
    val row: Float = 0f,
    val column: Float = 0f,
    val spanX: Float = 4f,
    val spanY: Float = 2f
)
