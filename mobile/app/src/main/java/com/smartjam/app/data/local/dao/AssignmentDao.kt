package com.smartjam.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartjam.app.data.local.entity.AssignmentEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

@Dao
interface AssignmentDao {
    @Query("SELECT * FROM assignments WHERE connectionId = :connectionId ORDER BY createdAt DESC")
    fun getAssignmentsForConnection(connectionId: UUID): Flow<List<AssignmentEntity>>

    @Query("SELECT * FROM assignments WHERE id = :assignmentId")
    suspend fun getAssignmentById(assignmentId: UUID): AssignmentEntity?

    @Query("SELECT * FROM assignments WHERE id IN (:ids)")
    suspend fun getAssignmentsByIds(ids: List<UUID>): List<AssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(assignments: List<AssignmentEntity>)

    @Query("DELETE FROM assignments WHERE connectionId = :connectionId")
    suspend fun clearForConnection(connectionId: UUID)
}
