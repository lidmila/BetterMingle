package com.bettermingle.app.ui.screen.auth

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bettermingle.app.R
import com.bettermingle.app.ui.component.BetterMingleButton
import com.bettermingle.app.ui.component.BetterMingleOutlinedButton
import com.bettermingle.app.ui.component.BetterMingleTextButton
import com.bettermingle.app.ui.component.BetterMingleTextField
import com.bettermingle.app.ui.theme.BetterMingleMotion
import com.bettermingle.app.ui.theme.PrimaryBlue
import com.bettermingle.app.ui.theme.Spacing
import com.bettermingle.app.ui.theme.TextOnColor
import com.bettermingle.app.ui.theme.TextSecondary
import com.bettermingle.app.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }
    val logoScale = remember { Animatable(0.3f) }

    val uiState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(Unit) {
        formVisible = true
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryBlue)
                .padding(horizontal = Spacing.lg)
                .padding(top = Spacing.xxl, bottom = Spacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.bettermingle_logo),
                    contentDescription = stringResource(R.string.login_logo_description),
                    modifier = Modifier
                        .size(120.dp)
                        .scale(logoScale.value)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                AnimatedVisibility(
                    visible = formVisible,
                    enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 100)) +
                            slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 100)) { it / 3 }
                ) {
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextOnColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xxl))

        AnimatedVisibility(
            visible = formVisible,
            enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 200)) +
                    slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 200)) { it / 3 }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = Spacing.lg)
            ) {
                BetterMingleTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = stringResource(R.string.login_email),
                    enabled = !uiState.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.formFieldSpacing))

                BetterMingleTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.login_password),
                    enabled = !uiState.isLoading,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) stringResource(R.string.login_password_hide) else stringResource(R.string.login_password_show)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                BetterMingleTextButton(
                    text = stringResource(R.string.login_forgot_password),
                    onClick = onNavigateToForgotPassword,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        AnimatedVisibility(
            visible = formVisible,
            enter = fadeIn(tween(BetterMingleMotion.STANDARD, delayMillis = 350)) +
                    slideInVertically(tween(BetterMingleMotion.STANDARD, delayMillis = 350)) { it / 2 }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = Spacing.lg)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = PrimaryBlue)
                } else {
                    BetterMingleButton(
                        text = stringResource(R.string.login_button),
                        onClick = { authViewModel.login(email, password) },
                        isCta = true
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                BetterMingleOutlinedButton(
                    text = stringResource(R.string.login_google),
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(com.bettermingle.app.BuildConfig.GOOGLE_CLIENT_ID)
                                    .build()

                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val result = credentialManager.getCredential(context, request)
                                val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data)
                                authViewModel.signInWithGoogle(googleIdToken.idToken)
                            } catch (_: GetCredentialCancellationException) {
                                // User cancelled
                            } catch (e: Exception) {
                                Log.e("LoginScreen", "Google Sign-In failed", e)
                                snackbarHostState.showSnackbar(context.getString(R.string.login_google_failed))
                            }
                        }
                    },
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                BetterMingleTextButton(
                    text = stringResource(R.string.login_no_account),
                    onClick = onNavigateToRegister
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(horizontal = Spacing.lg)
        )
    }
}
