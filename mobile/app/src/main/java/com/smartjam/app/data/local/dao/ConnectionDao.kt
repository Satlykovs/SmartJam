package com.smartjam.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartjam.app.data.local.entity.ConnectionEntity
import com.smartjam.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow


@Dao
interface ConnectionDao {
    @Query("SELECT * FROM connections WHERE myRole = :role")
    fun getConnectionsFlow(role: String): Flow<List<ConnectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnections(connections: List<ConnectionEntity>): List<Long>

    @Query("DELETE FROM connections WHERE myRole = :role")
    suspend fun clearConnections(role: String): Int
}