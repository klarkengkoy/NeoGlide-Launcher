package com.samidevstudio.neoglide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val iconName: String? = null,
    val label: String? = null
)
