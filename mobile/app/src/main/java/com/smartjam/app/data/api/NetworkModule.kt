package com.smartjam.app.data.api

import org.openapitools.client.auth.HttpBearerAuth
import com.smartjam.app.BuildConfig
import com.smartjam.app.data.local.TokenStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object NetworkModule {

    fun createRetrofit(tokenStorage: TokenStorage): Retrofit {
        val bearerAuth = HttpBearerAuth("bearer")

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(bearerAuth)
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else{
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

}