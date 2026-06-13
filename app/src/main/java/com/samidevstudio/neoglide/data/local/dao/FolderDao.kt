package com.samidevstudio.neoglide.data.local.dao

import androidx.room.*
import com.samidevstudio.neoglide.data.local.entity.FolderAppEntity
import com.samidevstudio.neoglide.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

data class FolderWithApps(
    @Embedded val folder: FolderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "folderId"
    )
    val apps: List<FolderAppEntity>
)

@Dao
interface FolderDao {
    @Transaction
    @Query("SELECT * FROM folders")
    fun getAllFoldersWithApps(): Flow<List<FolderWithApps>>

    @Transaction
    @Query("SELECT * FROM folders WHERE category = :category")
    fun getFoldersWithAppsByCategory(category: String): Flow<List<FolderWithApps>>

    @Transaction
    @Query("SELECT * FROM folders WHERE category IS NULL")
    fun getHomeScreenFoldersWithApps(): Flow<List<FolderWithApps>>

    @Transaction
    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderWithAppsById(id: Int): FolderWithApps?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("UPDATE folders SET `row` = :row, `column` = :col WHERE id = :folderId")
    suspend fun updateFolderPosition(folderId: Int, row: Float, col: Float)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderApp(folderApp: FolderAppEntity)

    @Query("DELETE FROM folder_apps WHERE folderId = :folderId AND packageName = :packageName")
    suspend fun removeAppFromFolder(folderId: Int, packageName: String)

    @Query("DELETE FROM folder_apps WHERE folderId = :folderId")
    suspend fun deleteAllAppsFromFolder(folderId: Int)

    @Query("UPDATE folders SET label = :label WHERE id = :folderId")
    suspend fun updateFolderLabel(folderId: Int, label: String)

    @Query("UPDATE folders SET category = :category WHERE id = :folderId")
    suspend fun updateFolderCategory(folderId: Int, category: String?)

    @Query("DELETE FROM folder_apps WHERE packageName = :packageName")
    suspend fun removeAppFromAllFolders(packageName: String)

    @Query("SELECT DISTINCT folderId FROM folder_apps WHERE packageName = :packageName")
    suspend fun getFoldersContainingApp(packageName: String): List<Int>

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: Int)

    @Query("SELECT MAX(displayOrder) FROM folder_apps WHERE folderId = :folderId")
    suspend fun getMaxDisplayOrder(folderId: Int): Int?

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    @Query("DELETE FROM folders WHERE category IS NULL")
    suspend fun deleteHomeScreenFolders()

    @Query("DELETE FROM folders WHERE category IS NOT NULL")
    suspend fun deleteAppDrawerFolders()

    @Query("DELETE FROM folder_apps")
    suspend fun deleteAllFolderApps()
}
