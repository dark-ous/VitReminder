package com.vitreminder.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Premium OLED Obsidian Canvas (Spotify / Discord / Pinterest inspired)
val BgDark = Color(0xFF0B0D14)          // Deep OLED obsidian canvas
val SurfaceDark = Color(0xFF121520)     // Elevated surface (Top/Bottom bars, sheets)
val CardDark = Color(0xFF181B28)        // Rich card container
val CardDarkHover = Color(0xFF22273A)   // Interactive active/hover state
val GlassBorder = Color(0x18FFFFFF)     // 10% translucent glassmorphism edge
val GlassBorderActive = Color(0x3DFFFFFF)// 24% translucent active edge

// High-Contrast Crisp Typography
val TextPrimary = Color(0xFFF8FAFC)     // Crisp pure white headline
val TextSecondary = Color(0xFF94A3B8)   // Refined slate subtitle
val TextMuted = Color(0xFF64748B)       // Muted metadata & caption

// Flagship Accent Palette
val AccentGreen = Color(0xFF10B981)     // Spotify vibrant neon emerald
val AccentBlue = Color(0xFF6366F1)      // Discord modern blurple/indigo
val AccentPurple = Color(0xFFA855F7)    // Neon violet for labs
val AccentYellow = Color(0xFFF59E0B)    // Cyber amber for upcoming / breaks
val AccentPeach = Color(0xFFFB923C)     // Warm peach
val AccentRed = Color(0xFFF43F5E)       // Pinterest vibrant rose red

// Schedule Session Types
val LabColor = Color(0xFFA855F7)        // Violet
val TheoryColor = Color(0xFF38BDF8)     // Sky Blue
val TutorialColor = Color(0xFF10B981)   // Emerald
val BreakColor = Color(0xFFF59E0B)      // Warm Amber

// Spotify-inspired Ambient Meshes for Hero & Highlights
val HeroOngoingBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF064E3B), Color(0xFF0F172A), Color(0xFF181B28))
)
val HeroUpcomingBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF311042), Color(0xFF0F172A), Color(0xFF181B28))
)
val HeroBreakBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF451A03), Color(0xFF1A1714), Color(0xFF181B28))
)
val HeroFreeDayBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF062D3E), Color(0xFF0F172A), Color(0xFF181B28))
)
