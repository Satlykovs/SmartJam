package com.smartjam.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartjam.app.data.local.entity.ConnectionEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface ConnectionDao {
    @Query("SELECT * FROM connections WHERE myRole = :role ORDER BY createdAt DESC")
    fun getConnectionsFlow(role: String): Flow<List<ConnectionEntity>>

    @Query("SELECT * FROM connections WHERE connectionId IN (:ids)")
    suspend fun getConnectionsByIds(ids: List<java.util.UUID>): List<ConnectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnections(connections: List<ConnectionEntity>): List<Long>

    @Query("DELETE FROM connections WHERE myRole = :role")
    suspend fun clearConnections(role: String): Int

    @Query("DELETE FROM connections")
    suspend fun clearAllConnections(): Int
}