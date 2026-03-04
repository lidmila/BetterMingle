package com.bettermingle.app.utils

private var lastClickTimestamp = 0L

fun debouncedClick(debounceMs: Long = 400L, action: () -> Unit) {
    val now = System.currentTimeMillis()
    if (now - lastClickTimestamp >= debounceMs) {
        lastClickTimestamp = now
        action()
    }
}
