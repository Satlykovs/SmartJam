package com.smartjam.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartjam.app.data.local.entity.SubmissionResultEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

@Dao
interface SubmissionResultDao {
    @Query(
        "SELECT * FROM submission_results WHERE assignmentId = :assignmentId ORDER BY createdAt DESC"
    )
    fun getResultsForAssignment(assignmentId: UUID): Flow<List<SubmissionResultEntity>>

    @Query("SELECT * FROM submission_results WHERE assignmentId = :assignmentId")
    suspend fun getResultsForAssignmentOnce(assignmentId: UUID): List<SubmissionResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<SubmissionResultEntity>)

    @Query("DELETE FROM submission_results WHERE assignmentId = :assignmentId")
    suspend fun clearForAssignment(assignmentId: UUID)
}
