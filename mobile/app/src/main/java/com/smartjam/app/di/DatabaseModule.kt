package com.smartjam.app.di

import android.content.Context
import androidx.room.Room
import com.smartjam.app.data.local.AudioFileStore
import com.smartjam.app.data.local.SmartJamDatabase
import com.smartjam.app.data.local.TokenStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartJamDatabase =
        Room.databaseBuilder(context, SmartJamDatabase::class.java, "smartjam_database")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideConnectionDao(db: SmartJamDatabase) = db.connectionDao()

    @Provides fun provideAssignmentDao(db: SmartJamDatabase) = db.assignmentDao()

    @Provides fun provideSubmissionResultDao(db: SmartJamDatabase) = db.submissionResultDao()

    @Provides
    @Singleton
    fun provideTokenStorage(@ApplicationContext context: Context) = TokenStorage(context)

    @Provides
    @Singleton
    fun provideAudioFileStore(@ApplicationContext context: Context) = AudioFileStore(context)
}
