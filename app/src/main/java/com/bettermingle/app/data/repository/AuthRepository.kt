package com.bettermingle.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val settingsManager: com.bettermingle.app.data.preferences.SettingsManager? = null) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun loginWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Přihlášení selhalo"))
            syncUserToFirestore(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Registrace selhala"))
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                displayName = name
            }
            user.updateProfile(profileUpdates).await()
            syncUserToFirestore(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return Result.failure(Exception("Google přihlášení selhalo"))
            syncUserToFirestore(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    private suspend fun syncUserToFirestore(user: FirebaseUser) {
        try {
            val userData = mapOf(
                "displayName" to (user.displayName ?: ""),
                "email" to (user.email ?: ""),
                "avatarUrl" to (user.photoUrl?.toString() ?: ""),
                "lastLogin" to System.currentTimeMillis()
            )
            firestore.collection("users")
                .document(user.uid)
                .set(userData, SetOptions.merge())
                .await()

            // Read premium status from Firestore and sync to local settings
            val userDoc = firestore.collection("users")
                .document(user.uid)
                .get()
                .await()
            val isPremium = userDoc.getBoolean("isPremium") ?: false
            val premiumUntil = userDoc.getTimestamp("premiumUntil")?.toDate()?.time
                ?: userDoc.getLong("premiumUntil")
            val tier = try {
                userDoc.getString("premiumTier")?.let { com.bettermingle.app.data.preferences.PremiumTier.valueOf(it) }
            } catch (_: Exception) { null }
            settingsManager?.updatePremiumStatus(isPremium, premiumUntil, tier)
            settingsManager?.updateUserInfo(
                name = user.displayName ?: "",
                email = user.email ?: "",
                avatarUrl = user.photoUrl?.toString() ?: ""
            )
        } catch (_: Exception) {
            // Non-critical, continue
        }
    }
}
