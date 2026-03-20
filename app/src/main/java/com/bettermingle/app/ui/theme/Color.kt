package com.bettermingle.app.ui.theme

import androidx.compose.ui.graphics.Color

// ===============================================
// BAREVNÁ PALETA — GLASSMORPHISM PREMIUM
// ===============================================

// === HLAVNÍ BARVY ===
val PrimaryBlue = Color(0xFF5B5FEF)           // Hlubší indigo-fialová
val AccentPink = Color(0xFFE879B8)            // Měkčí rose
val AccentOrange = Color(0xFFE85D3A)          // Teplejší coral
val AccentPurple = Color(0xFF8B5CF6)          // Fialová pro nápoje/kategorie
val AccentGold = Color(0xFFF5A623)            // Sytější amber
val AccentTeal = Color(0xFF14B8A6)            // Teal pro moduly
val AccentRed = Color(0xFFEF4444)             // Akcentová červená
val AccentLime = Color(0xFF84CC16)            // Lime pro moduly
val SurfacePeach = Color(0xFFEDE4F0)          // Lavender tint

// === POZADÍ ===
val BackgroundPrimary = Color(0xFFF8F7FC)     // Jemný lavender-white
val BackgroundSecondary = Color(0xFFF1EFF6)   // Chladnější off-white

// === TEXTOVÉ BARVY ===
val TextPrimary = Color(0xFF1A1B3D)           // Hlubší blue-black
val TextSecondary = Color(0xFF6E7191)         // Muted blue-gray
val TextOnColor = Color(0xFFFFFFFF)           // Bílý text na barevném pozadí

// === FUNKČNÍ BARVY ===
val Success = Color(0xFF34D399)               // Měkčí emerald
val Error = Color(0xFFEF4444)                 // Samostatná červená
val Warning = Color(0xFFF5A623)               // Upozornění (sdílí barvu s AccentGold)

// === ODVOZENÉ BARVY ===
val BorderColor = Color(0xFFE0DFF0)           // Lavender border
val SurfaceSecondary = Color(0xFFECEAF4)      // Lavender-gray

// Pozadí pro stavové barvy
val ErrorBackground = Color(0xFFFEF2F2)
val WarningBackground = Color(0xFFFFFBEB)
val SuccessBackground = Color(0xFFF0FDF4)
val BalancePositive = Color(0xFF86EFAC)       // Světle zelená pro kladný zůstatek
val BalanceNegative = Color(0xFFFED7AA)       // Světle oranžová pro záporný zůstatek

// === PASTELOVÉ BARVY (pro FeatureModuleCard pozadí) ===
val PastelBlue = Color(0xFFDCD9F5)            // z PrimaryBlue
val PastelPink = Color(0xFFF5D8EA)            // z AccentPink
val PastelOrange = Color(0xFFFADDD4)          // z AccentOrange
val PastelGreen = Color(0xFFD0F0E4)           // z Success
val PastelGold = Color(0xFFFAF0D8)            // z AccentGold
val PastelPurple = Color(0xFFE8D5FC)          // z AccentPurple
val PastelTeal = Color(0xFFCCF0ED)            // z AccentTeal
val PastelRed = Color(0xFFFDD9D9)             // z AccentRed
val PastelLime = Color(0xFFE4F5C5)            // z AccentLime
val PastelGray = Color(0xFFE8E7F0)            // z TextSecondary

// === GLASS BARVY ===
val GlassWhite = Color(0xFFFFFFFF)
val GlassShadow = Color(0x1A5B5FEF)          // 10% primary pro barevný stín
val CardShadow = Color(0x265B5FEF)            // 15% primary pro viditelný stín

// === INTERAKČNÍ BARVY ===
val SelectionPrimary = PrimaryBlue.copy(alpha = 0.12f)
val SelectionSecondary = AccentPink.copy(alpha = 0.12f)

// ===============================================
// DARK MODE BARVY
// ===============================================
val DarkBackgroundPrimary = Color(0xFF121218)
val DarkBackgroundSecondary = Color(0xFF1C1C24)
val DarkSurfaceCard = Color(0xFF242430)
val DarkTextPrimary = Color(0xFFE8E8F0)
val DarkTextSecondary = Color(0xFF9D9DB5)
val DarkBorderColor = Color(0xFF35354A)
val DarkSurfacePeach = Color(0xFF2A2A3A)
val DarkErrorBackground = Color(0xFF2D1515)
val DarkWarningBackground = Color(0xFF2D2815)
val DarkSuccessBackground = Color(0xFF152D1A)
val DarkPastelBlue = Color(0xFF2A2A50)
val DarkPastelPink = Color(0xFF3A2535)
val DarkPastelOrange = Color(0xFF3A2820)
val DarkPastelGreen = Color(0xFF1A3028)
val DarkPastelGold = Color(0xFF332D1A)
val DarkPastelPurple = Color(0xFF2D2540)
val DarkPastelTeal = Color(0xFF1A302E)
val DarkPastelRed = Color(0xFF2D1A1A)
val DarkPastelLime = Color(0xFF252D1A)
val DarkPastelGray = Color(0xFF28283A)
val DarkGlassShadow = Color(0x1A8080FF)
val DarkCardShadow = Color(0x26000000)

// ===============================================
// ROZŠÍŘENÁ BAREVNÁ PALETA (pro dark mode aware pastely)
// ===============================================
data class BetterMingleExtendedColors(
    val pastelBlue: Color,
    val pastelPink: Color,
    val pastelOrange: Color,
    val pastelGreen: Color,
    val pastelGold: Color,
    val pastelPurple: Color,
    val pastelTeal: Color,
    val pastelRed: Color,
    val pastelLime: Color,
    val pastelGray: Color,
    val glassShadow: Color,
    val cardShadow: Color,
    val surfacePeach: Color,
)

val LightExtendedColors = BetterMingleExtendedColors(
    pastelBlue = PastelBlue,
    pastelPink = PastelPink,
    pastelOrange = PastelOrange,
    pastelGreen = PastelGreen,
    pastelGold = PastelGold,
    pastelPurple = PastelPurple,
    pastelTeal = PastelTeal,
    pastelRed = PastelRed,
    pastelLime = PastelLime,
    pastelGray = PastelGray,
    glassShadow = GlassShadow,
    cardShadow = CardShadow,
    surfacePeach = SurfacePeach,
)

val DarkExtendedColors = BetterMingleExtendedColors(
    pastelBlue = DarkPastelBlue,
    pastelPink = DarkPastelPink,
    pastelOrange = DarkPastelOrange,
    pastelGreen = DarkPastelGreen,
    pastelGold = DarkPastelGold,
    pastelPurple = DarkPastelPurple,
    pastelTeal = DarkPastelTeal,
    pastelRed = DarkPastelRed,
    pastelLime = DarkPastelLime,
    pastelGray = DarkPastelGray,
    glassShadow = DarkGlassShadow,
    cardShadow = DarkCardShadow,
    surfacePeach = DarkSurfacePeach,
)

// ===============================================
// PALETA BAREV MODULŮ + HELPERY
// ===============================================
data class ModuleColorOption(val key: String, val color: Color, val hex: String)

val MODULE_COLOR_PALETTE = listOf(
    ModuleColorOption("blue", PrimaryBlue, "#5B5FEF"),
    ModuleColorOption("pink", AccentPink, "#E879B8"),
    ModuleColorOption("orange", AccentOrange, "#E85D3A"),
    ModuleColorOption("green", Success, "#34D399"),
    ModuleColorOption("gold", AccentGold, "#F5A623"),
    ModuleColorOption("purple", AccentPurple, "#8B5CF6"),
    ModuleColorOption("teal", AccentTeal, "#14B8A6"),
    ModuleColorOption("red", AccentRed, "#EF4444"),
    ModuleColorOption("lime", AccentLime, "#84CC16"),
)

fun pastelForColor(color: Color, ext: BetterMingleExtendedColors): Color = when (color) {
    PrimaryBlue -> ext.pastelBlue
    AccentPink -> ext.pastelPink
    AccentOrange -> ext.pastelOrange
    Success -> ext.pastelGreen
    AccentGold -> ext.pastelGold
    AccentPurple -> ext.pastelPurple
    AccentTeal -> ext.pastelTeal
    AccentRed -> ext.pastelRed
    AccentLime -> ext.pastelLime
    else -> ext.pastelGray
}

fun hexToColor(hex: String): Color? = MODULE_COLOR_PALETTE.find { it.hex == hex }?.color

fun colorToHex(color: Color): String? = MODULE_COLOR_PALETTE.find { it.color == color }?.hex

