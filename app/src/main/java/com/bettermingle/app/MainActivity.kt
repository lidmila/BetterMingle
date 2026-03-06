package com.bettermingle.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import com.bettermingle.app.data.preferences.SettingsManager
import com.bettermingle.app.navigation.BetterMingleNavigation
import com.bettermingle.app.ui.theme.BetterMingleTheme
import com.bettermingle.app.utils.ActivityLogger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ActivityLogger.initialize(this)

        val screenWidthDp = resources.configuration.screenWidthDp
        if (screenWidthDp < 600) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        }

        setContent {
            val settingsManager = remember { SettingsManager(this) }
            val settings by settingsManager.settingsFlow.collectAsState(
                initial = com.bettermingle.app.data.preferences.AppSettings()
            )

            // Apply app language
            LaunchedEffect(settings.appLanguage) {
                val localeList = if (settings.appLanguage == "system") {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(settings.appLanguage)
                }
                AppCompatDelegate.setApplicationLocales(localeList)
            }

            BetterMingleTheme(themeMode = settings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BetterMingleNavigation()
                }
            }
        }
    }
}
