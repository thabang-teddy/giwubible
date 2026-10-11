package com.giwu.bible.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Brand palette ──────────────────────────────────────────────────────────

val BrandRed = Color(0xFFE30613)
val BrandBlack = Color(0xFF0A0A0A)
val BrandGray = Color(0xFF6D6E71)

// Light surfaces
private val LightDivider = Color(0xFFE5E7EB)
private val LightSidebar = Color(0xFFF9FAFB)
private val LightText = Color(0xFF1F1F1F)

// Dark surfaces, kept neutral rather than blue-grey
private val DarkBackground = Color(0xFF0A0A0A)
private val DarkSurface = Color(0xFF1A1A1A)
private val DarkBorder = Color(0xFF2A2A2A)
private val DarkBorderStrong = Color(0xFF3D3D3D)
private val DarkText = Color(0xFFE5E5E5)

/** Width at which the reader switches to the three-column layout. */
val DesktopBreakpoint: Dp = 768.dp
val SidebarWidth: Dp = 260.dp
val PanelWidth: Dp = 340.dp

/**
 * Brand tokens Material's own scheme has no slot for.
 *
 * The reader leans on hairline rules and a muted label colour in several
 * places; keeping them here stops every call site from branching on the
 * current theme.
 */
@Immutable
data class GiwuColors(
    val divider: Color,
    val sidebar: Color,
    val muted: Color,
    val borderStrong: Color,
    val verseText: Color,
    val speakingHighlight: Color,
    val selectedHighlight: Color,
)

private val LocalGiwuColors = staticCompositionLocalOf {
    GiwuColors(
        divider = LightDivider,
        sidebar = LightSidebar,
        muted = BrandGray,
        borderStrong = LightDivider,
        verseText = LightText,
        speakingHighlight = BrandRed.copy(alpha = 0.14f),
        selectedHighlight = BrandRed.copy(alpha = 0.07f),
    )
}

val MaterialTheme.giwu: GiwuColors
    @Composable
    @ReadOnlyComposable
    get() = LocalGiwuColors.current

private val LightScheme = lightColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1E1),
    onPrimaryContainer = Color(0xFF5C0207),
    secondary = BrandGray,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = LightText,
    surface = Color.White,
    onSurface = LightText,
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = BrandGray,
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEFEFEF),
    outline = Color(0xFFD1D5DB),
    outlineVariant = LightDivider,
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFF6B70),
    onPrimary = Color(0xFF4A0004),
    primaryContainer = Color(0xFF8C0510),
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = Color(0xFFBFBFC1),
    onSecondary = BrandBlack,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFA8A8A8),
    surfaceContainerLow = Color(0xFF141414),
    surfaceContainer = Color(0xFF1F1F1F),
    surfaceContainerHigh = Color(0xFF262626),
    outline = DarkBorderStrong,
    outlineVariant = DarkBorder,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val GiwuTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.W700),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.W600),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.W600),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.W700),
)

@Composable
fun GiwuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extras = if (darkTheme) {
        GiwuColors(
            divider = DarkBorder,
            sidebar = DarkSurface,
            muted = BrandGray,
            borderStrong = DarkBorderStrong,
            verseText = DarkText,
            speakingHighlight = DarkScheme.primary.copy(alpha = 0.26f),
            selectedHighlight = DarkScheme.primary.copy(alpha = 0.15f),
        )
    } else {
        GiwuColors(
            divider = LightDivider,
            sidebar = LightSidebar,
            muted = BrandGray,
            borderStrong = LightDivider,
            verseText = LightText,
            speakingHighlight = BrandRed.copy(alpha = 0.14f),
            selectedHighlight = BrandRed.copy(alpha = 0.07f),
        )
    }

    CompositionLocalProvider(LocalGiwuColors provides extras) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = GiwuTypography,
            content = content,
        )
    }
}
