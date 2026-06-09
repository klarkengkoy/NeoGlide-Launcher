package com.samidevstudio.pxllauncherneo.data.local.dao

import androidx.room.*
import com.samidevstudio.pxllauncherneo.data.local.entity.HomeAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeAppDao {
    @Query("SELECT * FROM home_apps")
    fun getAllHomeApps(): Flow<List<HomeAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeApp(homeApp: HomeAppEntity)

    @Delete
    suspend fun deleteHomeApp(homeApp: HomeAppEntity)

    @Query("UPDATE home_apps SET `row` = :row, `column` = :column WHERE id = :id")
    suspend fun updateHomeAppPosition(id: Int, row: Float, column: Float)

    @Query("DELETE FROM home_apps WHERE packageName = :packageName")
    suspend fun deleteHomeAppByPackageName(packageName: String)

    @Query("DELETE FROM home_apps WHERE id = :id")
    suspend fun deleteHomeAppById(id: Int)
}
