package com.bettermingle.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bettermingle.app.data.preferences.AppSettings
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val userAvatarUrl: String = "",
    val isPremium: Boolean = false,
    val settings: AppSettings = AppSettings()
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadSettings()
        syncPremiumFromCloud()
    }

    private fun loadProfile() {
        val user = FirebaseAuth.getInstance().currentUser
        Log.d("ProfileViewModel", "loadProfile: uid=${user?.uid}, displayName=${user?.displayName}, email=${user?.email}")
        _uiState.value = _uiState.value.copy(
            userName = user?.displayName ?: "",
            userEmail = user?.email ?: "",
            userAvatarUrl = user?.photoUrl?.toString() ?: ""
        )
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsManager.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(
                    isPremium = settings.isPremium,
                    settings = settings
                )
            }
        }
    }

    private fun syncPremiumFromCloud() {
        viewModelScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val doc = FirebaseFirestore.getInstance()
                    .collection("users").document(uid).get().await()
                val isPremium = doc.getBoolean("isPremium") ?: false
                val premiumUntil = doc.getTimestamp("premiumUntil")?.toDate()?.time
                    ?: doc.getLong("premiumUntil")
                settingsManager.updatePremiumStatus(isPremium, premiumUntil)
                Log.d("ProfileViewModel", "syncPremiumFromCloud: isPremium=$isPremium")
            } catch (e: Exception) {
                Log.w("ProfileViewModel", "Failed to sync premium status", e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            settingsManager.clearAll()
        }
        authRepository.logout()
    }
}
