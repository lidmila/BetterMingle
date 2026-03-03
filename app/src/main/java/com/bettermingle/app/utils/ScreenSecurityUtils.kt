package com.bettermingle.app.utils

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Composable effect that enables FLAG_SECURE on the current window
 * when [enabled] is true. This prevents screenshots and screen recording.
 * The flag is automatically removed when the composable leaves composition.
 */
@Composable
fun SecureScreen(enabled: Boolean) {
    val context = LocalContext.current

    DisposableEffect(enabled) {
        val activity = context as? Activity
        if (enabled) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
