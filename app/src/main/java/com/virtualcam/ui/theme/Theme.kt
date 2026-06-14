package com.virtualcam.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ──────────────────────────────────────────────────────────────────
// Deep navy-black backgrounds with electric cyan as the primary action color.
// Warm orange is reserved for the "LIVE / recording" indicator only.

val BgDeep      = Color(0xFF0A0F1A)   // root background
val BgSurface   = Color(0xFF141C2C)   // card / sheet background
val BgCard      = Color(0xFF1B2540)   // slightly lighter card

val CyanBright  = Color(0xFF00D9FF)   // primary action / live indicator
val CyanMuted   = Color(0xFF007A99)   // secondary / inactive tabs

val OrangeLive  = Color(0xFFFF5C35)   // LIVE badge dot
val OrangeMuted = Color(0xFF7A2C18)

val TextPrimary   = Color(0xFFE2EAF8)
val TextSecondary = Color(0xFF7A90B8)
val TextDisabled  = Color(0xFF3D4F6E)

val DividerColor  = Color(0xFF1F2E4A)

// ── Color scheme ─────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary          = CyanBright,
    onPrimary        = BgDeep,
    primaryContainer = CyanMuted,
    secondary        = OrangeLive,
    onSecondary      = Color.White,
    background       = BgDeep,
    onBackground     = TextPrimary,
    surface          = BgSurface,
    onSurface        = TextPrimary,
    surfaceVariant   = BgCard,
    onSurfaceVariant = TextSecondary,
    outline          = DividerColor,
    error            = Color(0xFFFF4C5B)
)

// ── Theme entry point ─────────────────────────────────────────────────────────

@Composable
fun VirtualCamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content     = content
    )
}
