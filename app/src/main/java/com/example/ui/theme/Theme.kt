package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalVaultCornerStyle = staticCompositionLocalOf { VaultCornerStyle.SOFT }

@Composable
fun MyApplicationTheme(
    accentIndex: Int = 0,
    cornerStyle: VaultCornerStyle = VaultCornerStyle.SOFT,
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

    CompositionLocalProvider(LocalVaultCornerStyle provides cornerStyle) {
        MaterialTheme(
            colorScheme = darkColorScheme,
            typography = Typography,
            shapes = cornerStyle.toMaterialShapes(),
            content = content
        )
    }
}

