package com.bettermingle.app.utils

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Haptic feedback utilities for key interactions.
 * Usage: view.performHapticClick() or view.performHapticConfirm()
 */

fun View.performHapticClick() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}

fun View.performHapticConfirm() {
    performHapticFeedback(HapticFeedbackConstants.CONFIRM)
}

fun View.performHapticReject() {
    performHapticFeedback(HapticFeedbackConstants.REJECT)
}

fun View.performHapticLongPress() {
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}
