@file:Suppress("DEPRECATION")
package com.bettermingle.app.ui.screen.auth

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.request.repeatCount
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
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

import com.bettermingle.app.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToEmailLink: () -> Unit,
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }

    val uiState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // Legacy Google Sign-In fallback (for emulators / devices without Credential Manager)
    val legacyGoogleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    authViewModel.signInWithGoogle(token)
                }
            } catch (e: ApiException) {
                if (com.bettermingle.app.BuildConfig.DEBUG) {
                    Log.e("LoginScreen", "Legacy Google Sign-In failed", e)
                }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.login_google_failed))
                }
            }
        }
    }

    val gifImageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    LaunchedEffect(Unit) {
        formVisible = true
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
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
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(R.drawable.bettermingle_gif)
                        .repeatCount(0)
                        .build(),
                    imageLoader = gifImageLoader,
                    contentDescription = stringResource(R.string.login_logo_description),
                    modifier = Modifier
                        .size(160.dp)
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
                        isCta = true,
                        enabled = email.isNotBlank() && password.isNotBlank()
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                BetterMingleOutlinedButton(
                    text = stringResource(R.string.login_email_link),
                    onClick = onNavigateToEmailLink,
                    enabled = !uiState.isLoading
                )

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
                            } catch (_: NoCredentialException) {
                                // Credential Manager unavailable – fall back to legacy Google Sign-In
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(com.bettermingle.app.BuildConfig.GOOGLE_CLIENT_ID)
                                    .requestEmail()
                                    .build()
                                val client = GoogleSignIn.getClient(context, gso)
                                legacyGoogleSignInLauncher.launch(client.signInIntent)
                            } catch (e: Exception) {
                                if (com.bettermingle.app.BuildConfig.DEBUG) {
                                    Log.e("LoginScreen", "Google Sign-In failed", e)
                                }
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

    }
    }
}
