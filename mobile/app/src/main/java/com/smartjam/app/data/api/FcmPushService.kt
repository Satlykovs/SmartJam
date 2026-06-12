package com.smartjam.app.data.api

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartjam.app.api.DevicesApi
import com.smartjam.app.model.DeviceRegistrationRequest
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FcmPushService : FirebaseMessagingService() {

    @Inject lateinit var devicesApi: DevicesApi

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("SmartJam_FCM", "New token from Google received")

        scope.launch {
            try {
                devicesApi.registerDevice(DeviceRegistrationRequest(token = token))
                Log.i("SmartJam_FCM", "FCM token updated successfully")
            } catch (e: Exception) {
                Log.e("SmartJam_FCM", "Error during FCM token update", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("SmartJam_FCM", "Push notification received: ${message.notification?.body}")
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
