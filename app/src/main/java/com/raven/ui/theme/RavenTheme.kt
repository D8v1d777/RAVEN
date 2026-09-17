package com.raven.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * Raven is a dark-first product. There is deliberately no light scheme: a washed-out
 * light mode would contradict the identity, and a fake one would be worse than none.
 */
private val RavenColorScheme = darkColorScheme(
    primary = RavenColors.Primary,
    onPrimary = RavenColors.OnPrimary,
    primaryContainer = RavenColors.AccentGlow,
    onPrimaryContainer = RavenColors.SecondaryAccent,
    secondary = RavenColors.SecondaryAccent,
    onSecondary = RavenColors.OnSecondary,
    tertiary = RavenColors.MutedCrimson,
    onTertiary = RavenColors.OnPrimary,
    background = RavenColors.Background,
    onBackground = RavenColors.TextPrimary,
    surface = RavenColors.SurfaceDefault,
    onSurface = RavenColors.TextPrimary,
    surfaceVariant = RavenColors.SurfaceVariant,
    onSurfaceVariant = RavenColors.TextSecondary,
    error = RavenColors.ErrorText,
    onError = RavenColors.Background,
    errorContainer = RavenColors.ErrorContainer,
    onErrorContainer = RavenColors.ErrorText,
    outline = RavenColors.Divider,
)

/** Cinematic headings use a serif face; body text stays neutral and highly readable. */
val RavenSerif = FontFamily.Serif

@Composable
fun RavenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RavenColorScheme,
        typography = RavenMaterialTypography,
        content = content,
    )
}

private val RavenMaterialTypography = Typography(
    displayLarge = RavenTypography.DisplayLarge.serifDisplay(),
    displayMedium = RavenTypography.DisplayMedium.serifDisplay(),
    displaySmall = RavenTypography.DisplaySmall.serifDisplay(),
    headlineLarge = RavenTypography.HeadlineLarge.serifDisplay(),
    headlineMedium = RavenTypography.HeadlineMedium.serifDisplay(),
    headlineSmall = RavenTypography.HeadlineSmall.serifDisplay(),
    titleLarge = RavenTypography.TitleLarge.serifDisplay(),
    titleMedium = RavenTypography.TitleMedium,
    titleSmall = RavenTypography.TitleSmall,
    bodyLarge = RavenTypography.BodyLarge,
    bodyMedium = RavenTypography.BodyMedium,
    bodySmall = RavenTypography.BodySmall,
    labelLarge = RavenTypography.LabelLarge,
    labelMedium = RavenTypography.LabelMedium,
    labelSmall = RavenTypography.LabelSmall,
)

private fun TextStyle.serifDisplay(): TextStyle = copy(
    fontFamily = RavenSerif,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.2.sp,
)