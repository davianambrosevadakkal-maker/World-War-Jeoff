package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TacticalColorScheme = darkColorScheme(
  primary = TacticalAmber,
  onPrimary = TacticalDarkBg,
  primaryContainer = TacticalSurfaceVariant,
  onPrimaryContainer = TacticalAmber,
  secondary = TacticalCyan,
  onSecondary = TacticalDarkBg,
  secondaryContainer = TacticalCard,
  onSecondaryContainer = TacticalCyan,
  tertiary = TacticalCrimson,
  onTertiary = TacticalTextPrimary,
  background = TacticalDarkBg,
  onBackground = TacticalTextPrimary,
  surface = TacticalSurface,
  onSurface = TacticalTextPrimary,
  surfaceVariant = TacticalSurfaceVariant,
  onSurfaceVariant = TacticalTextSecondary,
  outline = TacticalBorder,
  outlineVariant = TacticalBorderHighlight
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = TacticalColorScheme,
    typography = Typography,
    content = content
  )
}
