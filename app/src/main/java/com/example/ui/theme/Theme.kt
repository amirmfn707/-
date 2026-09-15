package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val AppDarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = OnAmber,
    primaryContainer = AmberContainer,
    onPrimaryContainer = AmberPrimary,
    secondary = CyanAccent,
    onSecondary = NavyDeep,
    tertiary = CoralAccent,
    onTertiary = NavyDeep,
    background = NavyDark,
    onBackground = InkLight,
    surface = PanelBackground,
    onSurface = InkLight,
    surfaceVariant = PanelRaised,
    onSurfaceVariant = TextMuted,
    outline = BorderLine
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We provide RTL layout direction for Persian UI
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = AppDarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
