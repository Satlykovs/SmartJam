package com.smartjam.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartjam.app.data.local.dao.ConnectionDao
import com.smartjam.app.data.local.entity.ConnectionEntity

@Database(entities = [ConnectionEntity::class], version = 1, exportSchema = false)
abstract class SmartJamDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao
}