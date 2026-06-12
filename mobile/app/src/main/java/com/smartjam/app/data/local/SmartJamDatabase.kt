package com.smartjam.app.data.local

import androidx.room.*
import com.smartjam.app.data.local.dao.AssignmentDao
import com.smartjam.app.data.local.dao.ConnectionDao
import com.smartjam.app.data.local.dao.SubmissionResultDao
import com.smartjam.app.data.local.entity.AssignmentEntity
import com.smartjam.app.data.local.entity.ConnectionEntity
import com.smartjam.app.data.local.entity.SubmissionResultEntity

@Database(
    entities = [ConnectionEntity::class, AssignmentEntity::class, SubmissionResultEntity::class],
    version = 8,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class SmartJamDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao

    abstract fun assignmentDao(): AssignmentDao

    abstract fun submissionResultDao(): SubmissionResultDao
}
