package com.lunahub.android.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_tasks ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTask(taskId: String): DownloadTaskEntity?

    @Query("SELECT * FROM download_tasks WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getTaskByMediaId(mediaId: String): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: DownloadTaskEntity)

    @Query(
        """
        UPDATE download_tasks
        SET progress = :progress,
            status = :status,
            speed = :speed,
            errorMessage = :errorMessage,
            localPath = :localPath,
            updatedAt = :updatedAt
        WHERE id = :taskId
        """,
    )
    suspend fun updateState(
        taskId: String,
        progress: Float,
        status: String,
        speed: Long,
        errorMessage: String?,
        localPath: String?,
        updatedAt: Long,
    )
}
