package com.bettermingle.app.ui.theme

import androidx.compose.ui.graphics.Color

// ===============================================
// BAREVNÁ PALETA "BITTERBAL"
// ===============================================

// === HLAVNÍ BARVY ===
val PrimaryBlue = Color(0xFF4346DE)           // Primární akce, hlavičky, navigace, vybraný tab
val AccentPink = Color(0xFFF884BD)            // Sekundární akce, tagy, chipy, zvýraznění
val AccentOrange = Color(0xFFD84D35)          // CTA tlačítka, FAB, důležité odznaky
val AccentGold = Color(0xFFFCB13F)            // Varování, progress bary, hvězdičky hodnocení
val SurfacePeach = Color(0xFFF0CDC6)          // Pozadí karet, jemné sekce, input pozadí

// === POZADÍ ===
val BackgroundPrimary = Color(0xFFFFFFFF)     // Pozadí obrazovek
val BackgroundSecondary = Color(0xFFFAF5F4)   // Pozadí sekcí (lehce teplé)

// === TEXTOVÉ BARVY ===
val TextPrimary = Color(0xFF1E293B)           // Hlavní text
val TextSecondary = Color(0xFF64748B)         // Sekundární text
val TextOnColor = Color(0xFFFFFFFF)           // Bílý text na barevném pozadí

// === FUNKČNÍ BARVY ===
val Success = Color(0xFF22C55E)               // Potvrzení, splněno
val Error = Color(0xFFD84D35)                 // Chyby (sdílí barvu s AccentOrange)
val Warning = Color(0xFFFCB13F)               // Upozornění (sdílí barvu s AccentGold)

// === ODVOZENÉ BARVY ===
val BorderColor = Color(0xFFE2E8F0)           // Ohraničení
val SurfaceSecondary = Color(0xFFF1F5F9)      // Světle šedé pozadí sekcí

// Pozadí pro stavové barvy
val ErrorBackground = Color(0xFFFEF2F2)
val WarningBackground = Color(0xFFFFFBEB)
val SuccessBackground = Color(0xFFF0FDF4)

// === INTERAKČNÍ BARVY ===
val SelectionPrimary = PrimaryBlue.copy(alpha = 0.12f)
val SelectionSecondary = AccentPink.copy(alpha = 0.12f)

// === GRADIENT BARVY ===
val GradientPrimary = listOf(PrimaryBlue, AccentPink)
val GradientSecondary = listOf(AccentOrange, AccentGold)
val GradientSurface = listOf(BackgroundPrimary, BackgroundSecondary)
