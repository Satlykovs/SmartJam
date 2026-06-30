package com.smartjam.app.di

import android.content.Context
import com.smartjam.app.BuildConfig
import com.smartjam.app.api.AssignmentsApi
import com.smartjam.app.api.AuthApi
import com.smartjam.app.api.ConnectionsApi
import com.smartjam.app.api.DevicesApi
import com.smartjam.app.api.ProfileApi
import com.smartjam.app.api.SubmissionsApi
import com.smartjam.app.data.api.AuthAuthenticator
import com.smartjam.app.data.api.InstantAdapter
import com.smartjam.app.data.local.TokenStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.infrastructure.Serializer

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideCallFactory(okHttpClient: OkHttpClient): okhttp3.Call.Factory = okHttpClient

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenStorage: TokenStorage,
        authApiProvider: dagger.Lazy<AuthApi>,
        @ApplicationContext context: Context,
    ): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val url = originalRequest.url

            val isS3Request = url.queryParameter("X-Amz-Algorithm") != null

            val isAuthPath = url.encodedPath.contains("/auth/")

            val token =
                if (!isAuthPath && !isS3Request) {
                    runBlocking { tokenStorage.accessToken.first() }
                } else null

            val request =
                if (token != null) {
                    originalRequest.newBuilder().header("Authorization", "Bearer $token").build()
                } else {
                    originalRequest
                }

            chain.proceed(request)
        }

        val emulatorLocalhostInterceptor = Interceptor { chain ->
            var request = chain.request()
            val host = request.url.host

            if (host == "localhost" || host == "127.0.0.1") {
                val newUrl = request.url.newBuilder().host("10.0.2.2").build()

                request =
                    request
                        .newBuilder()
                        .url(newUrl)
                        .header(
                            "Host",
                            if (request.url.port == -1) host else "$host:${request.url.port}",
                        )
                        .build()
            }
            chain.proceed(request)
        }

        val authenticator = AuthAuthenticator(tokenStorage, authApiProvider)

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(emulatorLocalhostInterceptor)
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideApiClient(okHttpClient: OkHttpClient): ApiClient {
        val serializerBuilder =
            Serializer.gsonBuilder.registerTypeAdapter(Instant::class.java, InstantAdapter())

        return ApiClient(
            baseUrl = BuildConfig.BASE_URL,
            okHttpClientBuilder = okHttpClient.newBuilder(),
            serializerBuilder = serializerBuilder,
        )
    }

    @Provides
    fun provideAuthApi(apiClient: ApiClient): AuthApi = apiClient.createService(AuthApi::class.java)

    @Provides
    fun provideConnectionsApi(apiClient: ApiClient): ConnectionsApi =
        apiClient.createService(ConnectionsApi::class.java)

    @Provides
    fun provideAssignmentsApi(apiClient: ApiClient): AssignmentsApi =
        apiClient.createService(AssignmentsApi::class.java)

    @Provides
    fun provideSubmissionsApi(apiClient: ApiClient): SubmissionsApi =
        apiClient.createService(SubmissionsApi::class.java)

    @Provides
    fun provideDevicesApi(apiClient: ApiClient): DevicesApi =
        apiClient.createService(DevicesApi::class.java)

    @Provides
    fun provideProfileApi(apiClient: ApiClient): ProfileApi =
        apiClient.createService(ProfileApi::class.java)
}
