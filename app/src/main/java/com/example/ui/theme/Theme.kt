package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
  primary = MintPrimary,
  onPrimary = MintOnPrimary,
  primaryContainer = MintContainer,
  onPrimaryContainer = MintOnContainer,
  secondary = CyanSecondary,
  onSecondary = CyanOnSecondary,
  secondaryContainer = CyanContainer,
  onSecondaryContainer = CyanOnContainer,
  tertiary = AmberTertiary,
  onTertiary = AmberOnTertiary,
  tertiaryContainer = AmberContainer,
  onTertiaryContainer = AmberOnContainer,
  error = CrimsonError,
  onError = CrimsonOnError,
  errorContainer = CrimsonContainer,
  onErrorContainer = CrimsonOnContainer,
  background = ObsidianBg,
  onBackground = DarkOnSurface,
  surface = ObsidianSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = ObsidianSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant,
  surfaceContainerHighest = ObsidianSurfaceHigh,
  outline = ObsidianOutline,
)

private val LightColorScheme = lightColorScheme(
  primary = LightMintPrimary,
  onPrimary = LightMintOnPrimary,
  primaryContainer = LightMintContainer,
  onPrimaryContainer = LightMintOnContainer,
  secondary = LightCyanSecondary,
  onSecondary = LightCyanOnSecondary,
  secondaryContainer = LightCyanContainer,
  onSecondaryContainer = LightCyanOnContainer,
  tertiary = LightAmberTertiary,
  onTertiary = LightAmberOnTertiary,
  tertiaryContainer = LightAmberContainer,
  onTertiaryContainer = LightAmberOnContainer,
  error = LightCrimsonError,
  onError = LightCrimsonOnError,
  errorContainer = LightCrimsonContainer,
  onErrorContainer = LightCrimsonOnContainer,
  background = LightBg,
  onBackground = LightOnSurface,
  surface = LightSurface,
  onSurface = LightOnSurface,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightOnSurfaceVariant,
  outline = LightOutline,
)

val MailVaultShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MailVaultTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = MailVaultShapes,
    content = content,
  )
}
