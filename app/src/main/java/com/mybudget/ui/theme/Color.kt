package com.mybudget.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════ Dark Mode ═══════════
val DarkBg       = Color(0xFF0D1117)     // near-black with blue tint
val DarkCard     = Color(0xFF161B22)     // GitHub-style dark card
val DarkCardAlt  = Color(0xFF21262D)     // slightly lighter surface
val DarkBorder   = Color(0xFF30363D)     // subtle borders

// ═══════════ Light Mode ═══════════
val LightBg      = Color(0xFFF6F8FA)     // warm off-white
val LightCard    = Color(0xFFFFFFFF)
val LightCardAlt = Color(0xFFEEF2F6)

// ═══════════ Accent Palette ═══════════
val AccentPurple      = Color(0xFF6C5CE7)  // primary (dark mode)
val AccentPurpleLight = Color(0xFF4A3ABA)  // primary (light mode)
val AccentTeal        = Color(0xFF00CEC9)  // secondary
val AccentGreen       = Color(0xFF00E676)  // income / positive
val AccentRed         = Color(0xFFFF6B6B)  // expense / negative
val AccentOrange      = Color(0xFFFFA62F)  // warnings
val AccentPink        = Color(0xFFFF85A2)  // tertiary accent

// ═══════════ Text ═══════════
val TextPrimaryDark   = Color(0xFFF0F6FC)
val TextSecondaryDark = Color(0xFF8B949E)

val TextPrimaryLight   = Color(0xFF1C1C1E)
val TextSecondaryLight = Color(0xFF6E7781)

// Legacy aliases for backward compat
val Slate900       = DarkBg
val Slate800       = DarkCard
val Slate700       = DarkCardAlt
val MintGreen      = AccentGreen
val MintGreenDark  = Color(0xFF00B25E)
val OceanBlue      = AccentPurpleLight
val OceanBlueLight = AccentPurple
val CoralPink      = AccentRed