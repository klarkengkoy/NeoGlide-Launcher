package com.samidevstudio.pxllauncherneo.data.local.dao

import androidx.room.*
import com.samidevstudio.pxllauncherneo.data.local.entity.FolderAppEntity
import com.samidevstudio.pxllauncherneo.data.local.entity.FolderEntity
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

    @Query("DELETE FROM folder_apps WHERE packageName = :packageName")
    suspend fun removeAppFromAllFolders(packageName: String)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: Int)

    @Query("SELECT MAX(displayOrder) FROM folder_apps WHERE folderId = :folderId")
    suspend fun getMaxDisplayOrder(folderId: Int): Int?
}
