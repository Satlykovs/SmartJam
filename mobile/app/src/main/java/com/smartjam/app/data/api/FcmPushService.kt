package com.smartjam.app.data.api

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartjam.app.BuildConfig
import com.smartjam.app.api.DevicesApi
import com.smartjam.app.data.local.TokenStorage
import com.smartjam.app.model.DeviceRegistrationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.openapitools.client.infrastructure.ApiClient

class FcmPushService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("SmartJam_FCM", "New token from Google received")

        scope.launch {
            try {
                val tokenStorage = TokenStorage(applicationContext)
                val baseUrl = BuildConfig.BASE_URL
                val authenticator = AuthAuthenticator(tokenStorage, baseUrl)

                val okHttpClientBuilder = OkHttpClient.Builder().authenticator(authenticator)

                val apiClient =
                    ApiClient(baseUrl = baseUrl, okHttpClientBuilder = okHttpClientBuilder)
                authenticator.apiClient = apiClient

                val devicesApi = apiClient.createService(DevicesApi::class.java)

                devicesApi.registerDevice(DeviceRegistrationRequest(token = token))
                Log.i("SmartJam_FCM", "FCM token updated successfully")
            } catch (e: Exception) {
                Log.e("SmartJam_FCM", "Error during FCM token update", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val body = message.notification?.body

        Log.d("SmartjJam_FCM", "Push notification received: $body")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
