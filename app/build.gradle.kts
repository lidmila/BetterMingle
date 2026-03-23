import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { this@apply.load(it) }
    }
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val phApiKey = (localProps["POSTHOG_API_KEY"] as? String) ?: ""
val phHost = (localProps["POSTHOG_HOST"] as? String) ?: "https://eu.i.posthog.com"
val geoApiKey = (localProps["GEO_API_KEY"] as? String) ?: ""

android {
    namespace = "com.bettermingle.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bettermingle.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 7
        versionName = "1.3.1"

        androidResources.localeFilters += listOf("en", "cs", "de", "pl", "fr", "es")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "POSTHOG_API_KEY", "\"$phApiKey\"")
        buildConfigField("String", "POSTHOG_HOST", "\"$phHost\"")
        val googleClientId = localProps.getProperty("GOOGLE_CLIENT_ID") ?: ""
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
        manifestPlaceholders["GEO_API_KEY"] = geoApiKey
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("upload") {
                val ks = keystoreProperties
                storeFile = file(ks["storeFile"]?.toString() ?: "")
                storePassword = ks["storePassword"]?.toString() ?: ""
                keyAlias = ks["keyAlias"]?.toString() ?: ""
                keyPassword = ks["keyPassword"]?.toString() ?: ""
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("upload")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md"
            )
        }
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.material3)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")

    // Icons
    implementation("androidx.compose.material:material-icons-extended:1.5.8")

    // Preferences DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Google Sign-In (Credential Manager)
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Legacy Google Sign-In (fallback for emulators)
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    // Google AdMob
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // Google Places
    implementation("com.google.android.libraries.places:places:4.4.1")

    // QR Code generation
    implementation("com.google.zxing:core:3.5.3")

    // Jetpack Glance (Widget)
    implementation("androidx.glance:glance-appwidget:1.1.1")

    // PostHog Analytics
    implementation("com.posthog:posthog-android:3.7.4")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
