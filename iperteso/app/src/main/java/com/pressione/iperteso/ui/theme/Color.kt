package com.pressione.iperteso.ui.theme

import androidx.compose.ui.graphics.Color

// ── Medical Green Palette ──────────────────────────────────
// Matching the web app: #006C4C (light), #4DD9A0 (dark)
val MedicalGreen = Color(0xFF006C4C)
val MedicalGreenLight = Color(0xFF4DD9A0)
val MedicalGreenBg = Color(0xFFE8F5E9)
val MedicalGreenDarkBg = Color(0xFF1B3A2D)
val MedicalGreenVariant = Color(0xFF00855C)

// ── Status Colors ──────────────────────────────────────────
val ErrorRed = Color(0xFFBA1A1A)
val ErrorRedContainer = Color(0xFFFFDAD6)
val WarningOrange = Color(0xFFEF6C00)
val CriticalRed = Color(0xFFD32F2F)

// ── Light Theme ────────────────────────────────────────────
val LightPrimary = MedicalGreen
val LightOnPrimary = Color.White
val LightPrimaryContainer = MedicalGreenBg
val LightOnPrimaryContainer = Color(0xFF002114)
val LightSecondary = Color(0xFF4D6356)
val LightOnSecondary = Color.White
val LightSecondaryContainer = Color(0xFFCFE9D8)
val LightOnSecondaryContainer = Color(0xFF0A1F15)
val LightTertiary = Color(0xFF3D6473)
val LightOnTertiary = Color.White
val LightTertiaryContainer = Color(0xFFC1E8FB)
val LightOnTertiaryContainer = Color(0xFF001F29)
val LightError = ErrorRed
val LightOnError = Color.White
val LightErrorContainer = ErrorRedContainer
val LightOnErrorContainer = Color(0xFF410002)
val LightBackground = Color(0xFFF8FDF8)
val LightOnBackground = Color(0xFF191C1A)
val LightSurface = Color(0xFFF8FDF8)
val LightOnSurface = Color(0xFF191C1A)
val LightSurfaceVariant = Color(0xFFDBE4DD)
val LightOnSurfaceVariant = Color(0xFF404943)
val LightOutline = Color(0xFF707973)
val LightOutlineVariant = Color(0xFFBFC9C2)

// ── Dark Theme ─────────────────────────────────────────────
val DarkPrimary = MedicalGreenLight
val DarkOnPrimary = Color(0xFF003825)
val DarkPrimaryContainer = Color(0xFF005238)
val DarkOnPrimaryContainer = MedicalGreenBg
val DarkSecondary = Color(0xFFB3CDBD)
val DarkOnSecondary = Color(0xFF1F352A)
val DarkSecondaryContainer = Color(0xFF354B40)
val DarkOnSecondaryContainer = Color(0xFFCFE9D8)
val DarkTertiary = Color(0xFFA5CCDF)
val DarkOnTertiary = Color(0xFF063543)
val DarkTertiaryContainer = Color(0xFF244C5A)
val DarkOnTertiaryContainer = Color(0xFFC1E8FB)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = ErrorRedContainer
val DarkBackground = Color(0xFF191C1A)
val DarkOnBackground = Color(0xFFE1E3DF)
val DarkSurface = Color(0xFF191C1A)
val DarkOnSurface = Color(0xFFE1E3DF)
val DarkSurfaceVariant = Color(0xFF404943)
val DarkOnSurfaceVariant = Color(0xFFBFC9C2)
val DarkOutline = Color(0xFF89938C)
val DarkOutlineVariant = Color(0xFF404943)

// ── ESC/ESH Category Colors ────────────────────────────────
// Matching the web app category badge colors
val CategoryOptimal = Color(0xFF2E7D32)          // Green
val CategoryNormal = Color(0xFF66BB6A)           // Light green
val CategoryHighNormal = Color(0xFFFFA726)       // Orange
val CategoryGrade1 = Color(0xFFEF6C00)           // Dark orange
val CategoryGrade2 = Color(0xFFD32F2F)           // Red
val CategoryGrade3 = Color(0xFFBA1A1A)           // Deep red
val CategoryCrisis = Color(0xFF880E4F)           // Purple-red
