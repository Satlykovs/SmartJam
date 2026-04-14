package com.smartjam.app.data.local

import androidx.room.*
import com.smartjam.app.data.local.dao.ConnectionDao
import com.smartjam.app.data.local.entity.ConnectionEntity

@Database(entities = [ConnectionEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class SmartJamDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao
}