package com.bettermingle.app.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bettermingle.app.data.model.UserProfile
import com.bettermingle.app.data.preferences.AppSettings
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val userAvatarUrl: String = "",
    val phone: String = "",
    val contactEmail: String = "",
    val department: String = "",
    val bio: String = "",
    val isPremium: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false
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

    fun refreshProfile() = loadProfile()

    private fun loadProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        // Set initial values from Firebase Auth
        _uiState.value = _uiState.value.copy(
            userName = user.displayName ?: "",
            userEmail = user.email ?: "",
            userAvatarUrl = user.photoUrl?.toString() ?: ""
        )
        // Then load full profile from Firestore
        viewModelScope.launch {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users").document(user.uid).get().await()
                if (doc.exists()) {
                    _uiState.value = _uiState.value.copy(
                        userName = doc.getString("displayName")?.ifEmpty { null }
                            ?: user.displayName ?: "",
                        userEmail = doc.getString("email")?.ifEmpty { null }
                            ?: user.email ?: "",
                        userAvatarUrl = doc.getString("avatarUrl")?.ifEmpty { null }
                            ?: user.photoUrl?.toString() ?: "",
                        phone = doc.getString("phone") ?: "",
                        contactEmail = doc.getString("contactEmail") ?: "",
                        department = doc.getString("department") ?: "",
                        bio = doc.getString("bio") ?: ""
                    )
                }
            } catch (e: Exception) {
                Log.w("ProfileViewModel", "Failed to load profile from Firestore", e)
            }
        }
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

    fun updateProfile(
        name: String,
        contactEmail: String,
        phone: String,
        department: String,
        bio: String
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val data = mapOf(
                    "displayName" to name,
                    "contactEmail" to contactEmail,
                    "phone" to phone,
                    "department" to department,
                    "bio" to bio
                )
                FirebaseFirestore.getInstance()
                    .collection("users").document(user.uid)
                    .set(data, SetOptions.merge())
                    .await()

                // Update Firebase Auth displayName if changed
                if (name != user.displayName) {
                    user.updateProfile(userProfileChangeRequest { displayName = name }).await()
                }

                // Update local DataStore
                settingsManager.updateUserInfo(
                    name = name,
                    email = user.email ?: "",
                    avatarUrl = _uiState.value.userAvatarUrl
                )

                _uiState.value = _uiState.value.copy(
                    userName = name,
                    contactEmail = contactEmail,
                    phone = phone,
                    department = department,
                    bio = bio,
                    isSaving = false
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update profile", e)
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }

    fun uploadAvatar(uri: Uri) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingAvatar = true)
            try {
                val storageRef = FirebaseStorage.getInstance()
                    .reference.child("profile_photos/${user.uid}.jpg")
                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                // Update Firestore
                FirebaseFirestore.getInstance()
                    .collection("users").document(user.uid)
                    .set(mapOf("avatarUrl" to downloadUrl), SetOptions.merge())
                    .await()

                // Update Firebase Auth photoUrl
                user.updateProfile(
                    userProfileChangeRequest { photoUri = Uri.parse(downloadUrl) }
                ).await()

                // Update local DataStore
                settingsManager.updateUserInfo(
                    name = _uiState.value.userName,
                    email = user.email ?: "",
                    avatarUrl = downloadUrl
                )

                _uiState.value = _uiState.value.copy(
                    userAvatarUrl = downloadUrl,
                    isUploadingAvatar = false
                )
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to upload avatar", e)
                _uiState.value = _uiState.value.copy(isUploadingAvatar = false)
            }
        }
    }

    fun selectAvatar(avatarUrl: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users").document(user.uid)
                    .set(mapOf("avatarUrl" to avatarUrl), SetOptions.merge())
                    .await()

                settingsManager.updateUserInfo(
                    name = _uiState.value.userName,
                    email = user.email ?: "",
                    avatarUrl = avatarUrl
                )

                _uiState.value = _uiState.value.copy(userAvatarUrl = avatarUrl)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to select avatar", e)
            }
        }
    }

    fun loadUserProfile(uid: String, onResult: (UserProfile?) -> Unit) {
        viewModelScope.launch {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val profile = UserProfile(
                        id = uid,
                        displayName = doc.getString("displayName") ?: "",
                        email = doc.getString("email") ?: "",
                        avatarUrl = doc.getString("avatarUrl") ?: "",
                        phone = doc.getString("phone") ?: "",
                        contactEmail = doc.getString("contactEmail") ?: "",
                        department = doc.getString("department") ?: "",
                        bio = doc.getString("bio") ?: "",
                        isPremium = doc.getBoolean("isPremium") ?: false
                    )
                    onResult(profile)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                Log.w("ProfileViewModel", "Failed to load user profile $uid", e)
                onResult(null)
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
