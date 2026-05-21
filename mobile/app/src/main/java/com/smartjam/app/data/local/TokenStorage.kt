package com.smartjam.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore : DataStore<Preferences> by preferencesDataStore( //TODO: make encrypted storage
        name = "auth_preferences"
        )
class TokenStorage(private val context: Context) {
        private companion object Keys{
                val ACCESS_TOKEN = stringPreferencesKey("access_token")
                val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
                val USER_ROLE = stringPreferencesKey("user_role")
        }

        suspend fun saveToken(accessToken: String, refreshToken: String, role: String? = null){
                context.dataStore.edit { preferences ->
                        preferences[ACCESS_TOKEN] = accessToken
                        preferences[REFRESH_TOKEN] = refreshToken
                        if (role != null) {
                                preferences[USER_ROLE] = role
                        }
                }
        }

        val accessToken : Flow<String?> = context.dataStore.data
                .map { preferences -> preferences[ACCESS_TOKEN] }

        val refreshToken : Flow<String?> = context.dataStore.data
                .map { preferences -> preferences[REFRESH_TOKEN] }

        val userRole : Flow<String?> = context.dataStore.data
                .map { preferences -> preferences[USER_ROLE] }

        suspend fun saveRole(role: String){
                context.dataStore.edit { preferences ->
                        preferences[USER_ROLE] = role
                }
        }

        suspend fun clearTokens(){
                context.dataStore.edit { preferences ->
                        preferences.remove(ACCESS_TOKEN)
                        preferences.remove(REFRESH_TOKEN)
                }
        }

        suspend fun isAuthenticated(): Boolean {
                val token = refreshToken.first()
                return token != null && token.isNotEmpty()
        }

}