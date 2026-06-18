package com.samidevstudio.neoglide.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samidevstudio.neoglide.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM custom_categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM custom_categories")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("UPDATE custom_categories SET name = :newName, iconName = :newIcon WHERE name = :oldName")
    suspend fun updateCategory(oldName: String, newName: String, newIcon: String?)

    @Query("DELETE FROM custom_categories WHERE name = :name")
    suspend fun deleteCategory(name: String)
}
