package com.smartjam.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Call

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlayerEntryPoint {
    fun callFactory(): Call.Factory
}
