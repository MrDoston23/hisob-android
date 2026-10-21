package uz.hisob.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

data class Palette(
    val bg: Color, val surface: Color, val surface2: Color, val ink: Color, val muted: Color, val line: Color,
    val lapis: Color, val lapisDeep: Color, val gold: Color, val onLapis: Color,
    val good: Color, val goodBg: Color, val warn: Color, val warnBg: Color, val bad: Color, val badBg: Color, val infoBg: Color,
)

val LightPalette = Palette(
    bg = Color(0xFFF2F4FA), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFE9EDF7), ink = Color(0xFF101A3B),
    muted = Color(0xFF5A6588), line = Color(0xFFDCE1EE), lapis = Color(0xFF2443C4), lapisDeep = Color(0xFF152C8F),
    gold = Color(0xFFC08A1E), onLapis = Color(0xFFF5F7FF), good = Color(0xFF17845A), goodBg = Color(0xFFDDF3E9),
    warn = Color(0xFFB86A0C), warnBg = Color(0xFFFBEBD3), bad = Color(0xFFBE2F45), badBg = Color(0xFFFADDE2), infoBg = Color(0xFFE2E8FB),
)

val DarkPalette = Palette(
    bg = Color(0xFF0A0F21), surface = Color(0xFF131A31), surface2 = Color(0xFF1B2442), ink = Color(0xFFECEFFA),
    muted = Color(0xFF97A1C2), line = Color(0xFF253052), lapis = Color(0xFF5B78F0), lapisDeep = Color(0xFF1A2C80),
    gold = Color(0xFFE6BA52), onLapis = Color(0xFFF5F7FF), good = Color(0xFF4FCB93), goodBg = Color(0xFF12382A),
    warn = Color(0xFFF0A54A), warnBg = Color(0xFF3A2A12), bad = Color(0xFFF2798B), badBg = Color(0xFF3E1A23), infoBg = Color(0xFF1E2A55),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val NumStyle = TextStyle(fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold)

@Composable
fun HisobTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme(
        primary = p.lapis, onPrimary = p.onLapis, background = p.bg, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink, surfaceVariant = p.surface2, onSurfaceVariant = p.muted,
        outline = p.line, outlineVariant = p.line, secondaryContainer = p.infoBg, onSecondaryContainer = p.ink,
        surfaceContainer = p.surface, surfaceContainerLow = p.surface, surfaceContainerHigh = p.surface,
        error = p.bad,
    ) else lightColorScheme(
        primary = p.lapis, onPrimary = p.onLapis, background = p.bg, onBackground = p.ink,
        surface = p.surface, onSurface = p.ink, surfaceVariant = p.surface2, onSurfaceVariant = p.muted,
        outline = p.line, outlineVariant = p.line, secondaryContainer = p.infoBg, onSecondaryContainer = p.ink,
        surfaceContainer = p.surface, surfaceContainerLow = p.surface, surfaceContainerHigh = p.surface,
        error = p.bad,
    )
    val base = Typography()
    val typo = base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif),
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = typo, content = content)
    }
}
