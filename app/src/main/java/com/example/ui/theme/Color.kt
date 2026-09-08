package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Elegant Dark Palette Tokens
val VaultBackground = Color(0xFF121212)
val VaultCardBackground = Color(0xFF1E1E1E)
val VaultCardBorder = Color(0xFF333333)
val VaultSurfaceVariant = Color(0xFF2D2D2D)
val VaultTextPrimary = Color(0xFFE6E1E5)
val VaultTextSecondary = Color(0xFF938F99)
val VaultTextTertiary = Color(0xFFCAC4D0)
val VaultDivider = Color(0xFF333333)

// Elegant Dark Accent Tokens
val ElegantDarkPrimary = Color(0xFFD0BCFF)
val ElegantDarkOnPrimary = Color(0xFF381E72)
val ElegantDarkPrimaryContainer = Color(0xFF381E72)
val ElegantDarkOnPrimaryContainer = Color(0xFFD0BCFF)

// Calculator Disguise Keypad Colors
val CalcNumKeyBg = Color(0xFF1E1E1E)
val CalcNumKeyText = Color(0xFFE6E1E5)
val CalcOperatorKeyBg = Color(0xFF2D2D2D)
val CalcActionKeyBg = Color(0xFF381E72)

// Accent Colors Available for Customization
val AccentLilac = Color(0xFFD0BCFF)
val AccentEmerald = Color(0xFF10B981)
val AccentBlue = Color(0xFF3B82F6)
val AccentPurple = Color(0xFF8B5CF6)
val AccentAmber = Color(0xFFF59E0B)
val AccentRed = Color(0xFFEF4444)
val AccentCyan = Color(0xFF06B6D4)

data class VaultAccentTheme(
    val name: String,
    val primary: Color,
    val onPrimary: Color = Color.White,
    val container: Color,
    val onContainer: Color = Color.White
)

val AvailableAccents = listOf(
    VaultAccentTheme("Elegant Lilac", AccentLilac, ElegantDarkOnPrimary, ElegantDarkPrimaryContainer, ElegantDarkOnPrimaryContainer),
    VaultAccentTheme("Emerald Green", AccentEmerald, Color.White, Color(0xFF064E3B), Color(0xFFA7F3D0)),
    VaultAccentTheme("Electric Blue", AccentBlue, Color.White, Color(0xFF1E3A8A), Color(0xFFBFDBFE)),
    VaultAccentTheme("Cyber Purple", AccentPurple, Color.White, Color(0xFF4C1D95), Color(0xFFDDD6FE)),
    VaultAccentTheme("Amber Sunset", AccentAmber, Color.Black, Color(0xFF78350F), Color(0xFFFDE68A)),
    VaultAccentTheme("Crimson Red", AccentRed, Color.White, Color(0xFF7F1D1D), Color(0xFFFECACA)),
    VaultAccentTheme("Neon Cyan", AccentCyan, Color.Black, Color(0xFF164E63), Color(0xFFA5F3FC))
)

val AccentPalettes = AvailableAccents


