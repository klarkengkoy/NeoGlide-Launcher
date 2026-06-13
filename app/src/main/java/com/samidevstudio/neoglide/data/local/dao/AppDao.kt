package com.samidevstudio.neoglide.data.local.dao

import androidx.room.*
import com.samidevstudio.neoglide.data.local.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY label ASC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps")
    suspend fun getAllAppsList(): List<AppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity)

    @Query("SELECT * FROM apps WHERE category = :category ORDER BY label ASC")
    fun getAppsByCategory(category: String): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): AppEntity?

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("UPDATE apps SET category = :category WHERE packageName = :packageName")
    suspend fun updateAppCategory(packageName: String, category: String)

    @Query("UPDATE apps SET category = 'FOLDER' WHERE packageName = :packageName")
    suspend fun markAppAsInFolder(packageName: String)

    @Query("UPDATE apps SET lastUsedTime = :time WHERE packageName = :packageName")
    suspend fun updateLastUsedTime(packageName: String, time: Long)

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteAppByPackageName(packageName: String)

    @Query("DELETE FROM apps")
    suspend fun deleteAllApps()
}
