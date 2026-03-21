package com.bettermingle.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bettermingle.app.R
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null,
    val passwordResetSent: Boolean = false,
    val emailLinkSent: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)
    private val repository = AuthRepository(settingsManager)

    private fun str(@StringRes id: Int): String = getApplication<Application>().getString(id)
    private fun str(@StringRes id: Int, vararg args: Any): String = getApplication<Application>().getString(id, *args)

    companion object {
        private const val PREFS_NAME = "email_link_auth"
        private const val KEY_PENDING_EMAIL = "pending_email"
    }

    private val emailLinkPrefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(AuthUiState(
        isLoggedIn = repository.isLoggedIn,
        user = repository.currentUser
    ))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.authStateFlow.collect { user ->
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = user != null,
                    user = user
                )
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = str(R.string.auth_error_fill_email_password))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.loginWithEmail(email, password)
            _uiState.value = result.fold(
                onSuccess = { user ->
                    _uiState.value.copy(isLoading = false, isLoggedIn = true, user = user)
                },
                onFailure = { e ->
                    _uiState.value.copy(isLoading = false, error = mapAuthError(e))
                }
            )
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = str(R.string.auth_error_fill_all_fields))
            return
        }
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(error = str(R.string.auth_error_passwords_mismatch))
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(error = str(R.string.auth_error_password_too_short))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.registerWithEmail(name, email, password)
            _uiState.value = result.fold(
                onSuccess = { user ->
                    _uiState.value.copy(isLoading = false, isLoggedIn = true, user = user)
                },
                onFailure = { e ->
                    _uiState.value.copy(isLoading = false, error = mapAuthError(e))
                }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.signInWithGoogleCredential(idToken)
            _uiState.value = result.fold(
                onSuccess = { user ->
                    _uiState.value.copy(isLoading = false, isLoggedIn = true, user = user)
                },
                onFailure = { e ->
                    _uiState.value.copy(isLoading = false, error = mapAuthError(e))
                }
            )
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = str(R.string.auth_error_enter_email))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.sendPasswordResetEmail(email)
            _uiState.value = result.fold(
                onSuccess = {
                    _uiState.value.copy(isLoading = false, passwordResetSent = true)
                },
                onFailure = { e ->
                    _uiState.value.copy(isLoading = false, error = mapAuthError(e))
                }
            )
        }
    }

    fun sendEmailLink(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = str(R.string.auth_error_enter_email))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, emailLinkSent = false)
            val result = repository.sendSignInLink(email)
            _uiState.value = result.fold(
                onSuccess = {
                    emailLinkPrefs.edit().putString(KEY_PENDING_EMAIL, email).apply()
                    _uiState.value.copy(isLoading = false, emailLinkSent = true)
                },
                onFailure = { e ->
                    _uiState.value.copy(isLoading = false, error = mapAuthError(e))
                }
            )
        }
    }

    fun signInWithEmailLink(email: String, link: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = repository.signInWithEmailLink(email, link)
            _uiState.value = result.fold(
                onSuccess = { user ->
                    emailLinkPrefs.edit().remove(KEY_PENDING_EMAIL).apply()
                    _uiState.value.copy(isLoading = false, isLoggedIn = true, user = user)
                },
                onFailure = { e ->
                    _uiState.value.copy(isLoading = false, error = mapAuthError(e))
                }
            )
        }
    }

    fun getPendingEmail(): String? {
        return emailLinkPrefs.getString(KEY_PENDING_EMAIL, null)
    }

    fun isSignInWithEmailLink(link: String): Boolean {
        return repository.isSignInWithEmailLink(link)
    }

    suspend fun logout() {
        try {
            settingsManager.clearAll()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.bettermingle.app.data.database.AppDatabase
                    .getDatabase(getApplication())
                    .clearAllTables()
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Failed to clear local data on logout", e)
        }
        repository.logout()
        _uiState.value = AuthUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun mapAuthError(e: Throwable): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                msg.contains("credential is incorrect", ignoreCase = true) -> str(R.string.auth_error_invalid_credentials)
            msg.contains("EMAIL_EXISTS", ignoreCase = true) ||
                msg.contains("email address is already in use", ignoreCase = true) -> str(R.string.auth_error_email_exists)
            msg.contains("WEAK_PASSWORD", ignoreCase = true) -> str(R.string.auth_error_weak_password)
            msg.contains("INVALID_EMAIL", ignoreCase = true) ||
                msg.contains("badly formatted", ignoreCase = true) -> str(R.string.auth_error_invalid_email)
            msg.contains("USER_NOT_FOUND", ignoreCase = true) ||
                msg.contains("no user record", ignoreCase = true) -> str(R.string.auth_error_user_not_found)
            msg.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ||
                msg.contains("unusual activity", ignoreCase = true) -> str(R.string.auth_error_too_many_attempts)
            msg.contains("NETWORK", ignoreCase = true) -> str(R.string.auth_error_network)
            else -> str(R.string.auth_error_generic, e.localizedMessage ?: "unknown")
        }
    }
}
