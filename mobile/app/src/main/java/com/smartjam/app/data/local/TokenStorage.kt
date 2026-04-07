package com.smartjam.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore : DataStore<Preferences> by preferencesDataStore(
        name = "auth_preferences"
        )
class TokenStorage(private val context: Context) {
        private companion object Keys{
                val ACCESS_TOKEN = stringPreferencesKey("access_token")
                val REFRESH_TOKEN = stringPreferencesKey("refresh_token")

                val ACCESS_EXPIRED_AT = longPreferencesKey("access_expires_at")

                val REFRESH_EXPIRED_AT = longPreferencesKey("refresh_expired_at")
        }

        suspend fun saveToken(accessToken: String, refreshToken: String, accessExpiredAt: Long, refreshExpiredAt: Long){
                context.dataStore.edit { preferences ->
                        preferences[ACCESS_TOKEN] = accessToken
                        preferences[REFRESH_TOKEN] = refreshToken
                        preferences[ACCESS_EXPIRED_AT] = accessExpiredAt
                        preferences[REFRESH_EXPIRED_AT] = refreshExpiredAt
                }
        }

        val accessToken : Flow<String?> = context.dataStore.data
                .map { preferences -> preferences[ACCESS_TOKEN] }

        val refreshToken : Flow<String?> = context.dataStore.data
                .map { preferences -> preferences[REFRESH_TOKEN] }

        val accessExpiredAt : Flow<Long?> = context.dataStore.data
                .map {preferences -> preferences[ACCESS_EXPIRED_AT]}

        val refreshExpiredAt : Flow<Long?> = context.dataStore.data
                .map {preferences -> preferences[REFRESH_EXPIRED_AT]}

        suspend fun clearTokens(){
                context.dataStore.edit { preferences ->
                        preferences.remove(ACCESS_TOKEN)
                        preferences.remove(REFRESH_TOKEN)
                        preferences.remove(ACCESS_EXPIRED_AT)
                        preferences.remove(REFRESH_EXPIRED_AT)
                }
        }

        suspend fun isAccessTokenExpired(): Boolean {
                val expires = context.dataStore.data
                        .map { preferences -> preferences[ACCESS_EXPIRED_AT] }
                        .first()
                val currentTime = System.currentTimeMillis() / 1000
                return (expires == null) || (currentTime > expires)

        }

        suspend fun isRefreshTokenExpired(): Boolean {
                val expires = context.dataStore.data
                        .map { preferences -> preferences[REFRESH_EXPIRED_AT] }
                        .first()
                val currentTime = System.currentTimeMillis() / 1000
                return (expires == null) || (currentTime > expires)

        }

        suspend fun isAuthenticated(): Boolean {
                return !isRefreshTokenExpired() && !isAccessTokenExpired()
        }

}