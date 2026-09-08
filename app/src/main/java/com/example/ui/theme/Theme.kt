package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    accentIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val selectedAccent = AvailableAccents.getOrElse(accentIndex) { AvailableAccents[0] }

    val darkColorScheme = darkColorScheme(
        primary = selectedAccent.primary,
        onPrimary = selectedAccent.onPrimary,
        primaryContainer = selectedAccent.container,
        onPrimaryContainer = selectedAccent.onContainer,
        secondary = selectedAccent.primary,
        onSecondary = selectedAccent.onPrimary,
        tertiary = VaultTextTertiary,
        background = VaultBackground,
        onBackground = VaultTextPrimary,
        surface = VaultCardBackground,
        onSurface = VaultTextPrimary,
        surfaceVariant = VaultSurfaceVariant,
        onSurfaceVariant = VaultTextSecondary,
        outline = VaultCardBorder,
        outlineVariant = VaultDivider
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = Typography,
        content = content
    )
}
