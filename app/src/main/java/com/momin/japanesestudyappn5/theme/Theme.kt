package com.momin.japanesestudyappn5.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary             = JpIndigo,
    onPrimary           = JpOnPrimary,
    primaryContainer    = JpIndigoLight,
    onPrimaryContainer  = JpIndigoDark,
    secondary           = JpRed,
    onSecondary         = JpOnSecondary,
    secondaryContainer  = JpRedLight,
    onSecondaryContainer = JpRed,
    tertiary            = JpSakura,
    onTertiary          = JpOnPrimary,
    tertiaryContainer   = JpSakuraLight,
    onTertiaryContainer = JpSakura,
    background          = JpBackground,
    onBackground        = JpOnBackground,
    surface             = JpSurface,
    onSurface           = JpOnSurface,
    surfaceVariant      = JpSurfaceVar,
    onSurfaceVariant    = Color(0xFF44474F),
)

private val DarkColorScheme = darkColorScheme(
    primary             = JpIndigoDarkScheme,
    onPrimary           = JpOnPrimaryDark,
    primaryContainer    = JpIndigoContainerDark,
    onPrimaryContainer  = JpIndigoDarkScheme,
    secondary           = JpRedDark,
    onSecondary         = JpOnSecondaryDark,
    secondaryContainer  = JpRedContainerDark,
    onSecondaryContainer = JpRedDark,
    tertiary            = JpSakuraDark,
    onTertiary          = Color(0xFF40001F),
    tertiaryContainer   = JpSakuraContainerDark,
    onTertiaryContainer = JpSakuraDark,
    background          = JpBackgroundDark,
    onBackground        = JpOnBackgroundDark,
    surface             = JpSurfaceDark,
    onSurface           = JpOnSurfaceDark,
    surfaceVariant      = JpSurfaceVarDark,
    onSurfaceVariant    = Color(0xFFC5C4D4),
)

// AMOLED = Dark but with pure-black backgrounds
private val AmoledColorScheme = DarkColorScheme.copy(
    background     = JpAmoledBackground,
    surface        = JpAmoledSurface,
    surfaceVariant = JpAmoledSurfaceVar,
)

// 🌸 Sakura — soft cherry-blossom pink theme
private val SakuraColorScheme = lightColorScheme(
    primary             = Color(0xFFC2185B),
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFFCE4EC),
    onPrimaryContainer  = Color(0xFF880E4F),
    secondary           = Color(0xFFAD1457),
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFF8BBD0),
    onSecondaryContainer = Color(0xFF880E4F),
    tertiary            = Color(0xFF6A1B9A),
    onTertiary          = Color.White,
    tertiaryContainer   = Color(0xFFE1BEE7),
    onTertiaryContainer = Color(0xFF4A148C),
    background          = Color(0xFFFFF0F5),
    onBackground        = Color(0xFF1A1A1A),
    surface             = Color(0xFFFFF8FB),
    onSurface           = Color(0xFF1A1A1A),
    surfaceVariant      = Color(0xFFFCE4EC),
    onSurfaceVariant    = Color(0xFF880E4F),
)
// 🌌 Tokyo Night — deep neon dark theme
private val TokyoNightColorScheme = darkColorScheme(
    primary             = Color(0xFF7AA2F7), // Neon Blue
    onPrimary           = Color(0xFF0F101A),
    primaryContainer    = Color(0xFF24283B), // Indigo/dark primary container
    onPrimaryContainer  = Color(0xFF7AA2F7),
    secondary           = Color(0xFFBB9AF7), // Neon Purple
    onSecondary         = Color(0xFF0F101A),
    secondaryContainer  = Color(0xFF3B4252),
    onSecondaryContainer = Color(0xFFBB9AF7),
    tertiary            = Color(0xFF2AC3DE), // Neon Teal/Cyan
    onTertiary          = Color(0xFF0F101A),
    tertiaryContainer   = Color(0xFF1F2335),
    onTertiaryContainer = Color(0xFF2AC3DE),
    background          = Color(0xFF1A1B26), // Dark background
    onBackground        = Color(0xFFC0CAF5), // Light text
    surface             = Color(0xFF1F2335),
    onSurface           = Color(0xFFC0CAF5),
    surfaceVariant      = Color(0xFF24283B),
    onSurfaceVariant    = Color(0xFFA9B1D6),
)

// 🕹️ Retro Arcade — high contrast neon green / cybernetic CRT aesthetic
private val RetroArcadeColorScheme = darkColorScheme(
    primary             = Color(0xFF00FF00), // Neon Green
    onPrimary           = Color.Black,
    primaryContainer    = Color(0xFF003300), // Very dark green
    onPrimaryContainer  = Color(0xFF00FF00),
    secondary           = Color(0xFF33FF33),
    onSecondary         = Color.Black,
    secondaryContainer  = Color(0xFF002200),
    onSecondaryContainer = Color(0xFF33FF33),
    tertiary            = Color(0xFF00AA00),
    onTertiary          = Color.Black,
    tertiaryContainer   = Color(0xFF111111),
    onTertiaryContainer = Color(0xFF00FF00),
    background          = Color(0xFF000000), // Pure Black background
    onBackground        = Color(0xFF33FF33), // Green text
    surface             = Color(0xFF0D0D0D),
    onSurface           = Color(0xFF33FF33),
    surfaceVariant      = Color(0xFF151515),
    onSurfaceVariant    = Color(0xFF00FF00),
)

@Composable
fun JapaneseStudyAppN5Theme(
    themeMode: String = "system",   // "light", "dark", "amoled", "sakura", "tokyonight", "retroarcade", "system"
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themeMode) {
        "light"        -> LightColorScheme
        "dark"         -> DarkColorScheme
        "amoled"       -> AmoledColorScheme
        "sakura"       -> SakuraColorScheme
        "tokyonight"   -> TokyoNightColorScheme
        "retroarcade"  -> RetroArcadeColorScheme
        else           -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
