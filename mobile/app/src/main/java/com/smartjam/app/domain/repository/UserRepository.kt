package com.smartjam.app.domain.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.smartjam.app.api.ProfileApi
import com.smartjam.app.model.UserProfileUpdateRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.openapitools.client.infrastructure.ApiClient

@Singleton
class UserRepository
@Inject
constructor(
    private val profileApi: ProfileApi,
    private val httpClient: okhttp3.OkHttpClient,
    private val apiClient: ApiClient,
    @param:ApplicationContext private val context: Context,
) {
    suspend fun getProfile() = profileApi.getCurrentUserProfile()

    suspend fun updateProfile(
        username: String?,
        firstName: String?,
        lastName: String?,
        newAvatarUri: Uri?,
    ): Result<Unit> {
        return try {
            if (newAvatarUri != null) {
                val mimeType = context.contentResolver.getType(newAvatarUri)
                if (
                    mimeType != "image/jpeg" && mimeType != "image/png" && mimeType != "image/jpg"
                ) {
                    return Result.failure(Exception("Допустимы только форматы JPG и PNG"))
                }
            }

            val response =
                profileApi.updateCurrentUserProfile(
                    UserProfileUpdateRequest(
                        username = username,
                        firstName = firstName,
                        lastName = lastName,
                        avatarUpdated = newAvatarUri != null,
                    )
                )

            if (!response.isSuccessful) {
                return Result.failure(Exception("Ошибка сервера: ${response.code()}"))
            }

            val uploadUrl = response.body()?.avatarUrl?.toString()
            if (uploadUrl != null && newAvatarUri != null) {
                val uploadSuccess = uploadFileToS3(uploadUrl, newAvatarUri)
                if (!uploadSuccess)
                    return Result.failure(Exception("Ошибка при передаче файла в хранилище"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Update error", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadFileToS3(url: String, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readBytes()
                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                    val requestBody =
                        bytes.toRequestBody(mimeType.toMediaTypeOrNull(), 0, bytes.size)

                    val request = okhttp3.Request.Builder().url(url).put(requestBody).build()

                    httpClient.newCall(request).execute().use { response -> response.isSuccessful }
                } ?: false
            } catch (e: Exception) {
                Log.e("UserRepository", "S3 Upload Error", e)
                false
            }
        }
    }
}
