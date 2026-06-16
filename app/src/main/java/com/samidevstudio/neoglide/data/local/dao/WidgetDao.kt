package com.samidevstudio.neoglide.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samidevstudio.neoglide.data.local.entity.WidgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetDao {
    @Query("SELECT * FROM widgets")
    fun getAllWidgets(): Flow<List<WidgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(widget: WidgetEntity)

    @Delete
    suspend fun deleteWidget(widget: WidgetEntity)

    @Query("DELETE FROM widgets WHERE widgetId = :widgetId")
    suspend fun deleteWidgetById(widgetId: Int)

    @Query("UPDATE widgets SET spanY = :spanY WHERE widgetId = :widgetId")
    suspend fun updateWidgetSpanY(widgetId: Int, spanY: Float)

    @Query("UPDATE widgets SET spanX = :spanX WHERE widgetId = :widgetId")
    suspend fun updateWidgetSpanX(widgetId: Int, spanX: Float)

    @Query("UPDATE widgets SET `row` = :row, `column` = :column, spanX = :spanX, spanY = :spanY WHERE widgetId = :widgetId")
    suspend fun updateWidgetBounds(widgetId: Int, row: Float, column: Float, spanX: Float, spanY: Float)

    @Query("DELETE FROM widgets")
    suspend fun deleteAllWidgets()
}
