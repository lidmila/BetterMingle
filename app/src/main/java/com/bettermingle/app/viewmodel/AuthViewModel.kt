package com.bettermingle.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null,
    val passwordResetSent: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)
    private val repository = AuthRepository(settingsManager)

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
            _uiState.value = _uiState.value.copy(error = "Vyplň e-mail a heslo")
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
            _uiState.value = _uiState.value.copy(error = "Vyplň všechna pole")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(error = "Hesla se neshodují")
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(error = "Heslo musí mít alespoň 6 znaků")
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
            _uiState.value = _uiState.value.copy(error = "Zadej e-mail")
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

    suspend fun logout() {
        try {
            settingsManager.clearAll()
            com.bettermingle.app.data.database.AppDatabase
                .getDatabase(getApplication())
                .clearAllTables()
        } catch (_: Exception) { }
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
                msg.contains("credential is incorrect", ignoreCase = true) -> "Nesprávný e-mail nebo heslo"
            msg.contains("EMAIL_EXISTS", ignoreCase = true) ||
                msg.contains("email address is already in use", ignoreCase = true) -> "Účet s tímto e-mailem už existuje"
            msg.contains("WEAK_PASSWORD", ignoreCase = true) -> "Heslo je příliš slabé"
            msg.contains("INVALID_EMAIL", ignoreCase = true) ||
                msg.contains("badly formatted", ignoreCase = true) -> "Neplatný formát e-mailu"
            msg.contains("USER_NOT_FOUND", ignoreCase = true) ||
                msg.contains("no user record", ignoreCase = true) -> "Účet nenalezen"
            msg.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ||
                msg.contains("unusual activity", ignoreCase = true) -> "Příliš mnoho pokusů. Zkus to později."
            msg.contains("NETWORK", ignoreCase = true) -> "Chyba připojení. Zkontroluj internet."
            else -> "Něco se pokazilo: ${e.localizedMessage ?: "neznámá chyba"}"
        }
    }
}
