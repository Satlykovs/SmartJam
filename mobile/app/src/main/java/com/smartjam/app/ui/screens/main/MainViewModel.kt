package com.smartjam.app.ui.screens.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartjam.app.domain.repository.AuthRepository
import com.smartjam.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination = _startDestination.asStateFlow()

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            try {
                Log.d("SmartJam_Auth", "Starting auth verification...")

                val isValid = authRepository.verifyAuthentication()

                Log.d("SmartJam_Auth", "Verification result: $isValid")
                _startDestination.value = if (isValid) Screen.Home.route else Screen.Login.route
            } catch (e: Exception) {
                Log.e("SmartJam_Auth", "Auth verification failed", e)
                _startDestination.value = Screen.Login.route
            }
        }
    }
}
