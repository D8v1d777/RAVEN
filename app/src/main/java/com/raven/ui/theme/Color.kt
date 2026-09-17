package com.raven.ui.theme

import androidx.compose.ui.graphics.Color

// Raven Dark Theme Colors - Based on Raven Product & Design Bible
object RavenColors {
    // Background
    val Background = Color(0xFF0A0A0B) // #0A0A0B

    // Primary surfaces
    val SurfaceDefault = Color(0xFF121214) // #121214
    val SurfaceElevated = Color(0xFF18181B) // #18181B

    // Accents
    val Primary = Color(0xFF7B2CBF) // #7B2CBF
    val SecondaryAccent = Color(0xFF9B4DCA) // #9B4DCA

    // Muted crimson
    val MutedCrimson = Color(0xFFC23A5A) // #C23A5A

    // Text colors
    val OnPrimary = Color.White
    val OnSecondary = Color.Black
    val OnBackground = Color.White
    val OnSurface = Color.White
    var SurfaceVariant = Color(0xFF1E1E20) // Slightly elevated surface

    // Neutral colors for text hierarchy
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFAEAEAE) // Light gray for secondary text
    val TextTertiary = Color(0xFF8E8E93) // Muted text

    // Divider
    val Divider = Color(0xFF2C2C2E) // Subtle divider

    // Conversation surfaces
    val AssistantBubble = Color(0xFF141417) // Raven's voice: matte, near-black
    val UserBubble = Color(0xFF1B1230) // User's voice: deep violet tint

    // Semantic states
    val Success = Color(0xFF5FBF8F)
    val Warning = Color(0xFFD9A441)
    val ErrorContainer = Color(0xFF2A1119)
    val ErrorText = Color(0xFFFF8FA3)

    // Presence / glow
    val AccentGlow = Color(0xFF3A1560) // Low-alpha violet used behind active elements
}