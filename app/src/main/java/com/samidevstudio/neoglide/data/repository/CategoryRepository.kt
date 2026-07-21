package com.samidevstudio.neoglide.data.repository

import com.samidevstudio.neoglide.data.local.dao.CategoryDao
import com.samidevstudio.neoglide.data.local.entity.CategoryEntity
import com.samidevstudio.neoglide.domain.model.AppCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun getCustomCategoriesMap(): Map<AppCategory, List<String>> = withContext(Dispatchers.IO) {
        categoryDao.getAllCategoriesList().associate { 
            AppCategory(it.name, isCustom = true, iconName = it.iconName, label = it.label) to emptyList()
        }
    }

    suspend fun addCategory(name: String, iconName: String?) = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(CategoryEntity(name, iconName))
    }

    suspend fun updateCategory(oldName: String, newName: String, newIcon: String?, newLabel: String?) = withContext(Dispatchers.IO) {
        categoryDao.updateCategory(oldName, newName, newIcon, newLabel)
    }

    suspend fun upsertCategoryOverride(name: String, label: String?, icon: String?) = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(CategoryEntity(name, icon, label))
    }

    suspend fun removeCategory(name: String) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategory(name)
    }
}
