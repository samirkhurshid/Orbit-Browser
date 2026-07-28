package com.orbit.browser.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.orbit.browser.ui.WeatherKind

// ═══════════════════════════════════════════════════════════════════════════
// TIME SLOT
// ═══════════════════════════════════════════════════════════════════════════

enum class TimeSlot { Dawn, Morning, Noon, Afternoon, Evening, Night }

fun getTimeSlot(hour: Int): TimeSlot = when {
    hour in 5..6  -> TimeSlot.Dawn
    hour in 7..11 -> TimeSlot.Morning
    hour in 12..13 -> TimeSlot.Noon
    hour in 14..17 -> TimeSlot.Afternoon
    hour in 18..20 -> TimeSlot.Evening
    else           -> TimeSlot.Night
}

// ═══════════════════════════════════════════════════════════════════════════
// TIME-SLOT PALETTE  (exact values from App.tsx TIME_DARK / TIME_LIGHT)
// ═══════════════════════════════════════════════════════════════════════════

data class TimeSlotPalette(
    val orb1: Color,
    val orb2: Color,
    val a1:   Color,
    val a2:   Color,
    // Background is rendered as a Box gradient; stored as color stops here
    val bgBase:    Color,   // solid base colour
    val bgOrb1Clr: Color,   // top-left ellipse color
    val bgOrb2Clr: Color,   // bottom-right ellipse color
)

// ── DARK palettes ────────────────────────────────────────────────────────────

private val DARK_DAWN = TimeSlotPalette(
    orb1 = Color(0xFFFF6B9D), orb2 = Color(0xFFC44DFF),
    a1   = Color(0xFFFF6B9D), a2   = Color(0xFFC44DFF),
    bgBase = Color(0xFF09030F),
    bgOrb1Clr = Color(0x8C3D1A78),  // rgba(61,26,120,0.55)
    bgOrb2Clr = Color(0x731A0533),  // rgba(26,5,51,0.45)
)
private val DARK_MORNING = TimeSlotPalette(
    orb1 = Color(0xFFF5A623), orb2 = Color(0xFFFF8C42),
    a1   = Color(0xFFF5A623), a2   = Color(0xFFE07316),
    bgBase = Color(0xFF090C18),
    bgOrb1Clr = Color(0x802D1A00),
    bgOrb2Clr = Color(0x730F1B35),
)
private val DARK_NOON = TimeSlotPalette(
    orb1 = Color(0xFFFFD200), orb2 = Color(0xFFF7971E),
    a1   = Color(0xFFF7971E), a2   = Color(0xFFFFD200),
    bgBase = Color(0xFF0A0800),
    bgOrb1Clr = Color(0x802D1E00),
    bgOrb2Clr = Color(0x661A1000),
)
private val DARK_AFTERNOON = TimeSlotPalette(
    orb1 = Color(0xFF4FACFE), orb2 = Color(0xFF00C6FB),
    a1   = Color(0xFF4FACFE), a2   = Color(0xFF00C6FB),
    bgBase = Color(0xFF010D1A),
    bgOrb1Clr = Color(0x8C002244),
    bgOrb2Clr = Color(0x73001225),
)
private val DARK_EVENING = TimeSlotPalette(
    orb1 = Color(0xFFF5576C), orb2 = Color(0xFFF97316),
    a1   = Color(0xFFF5576C), a2   = Color(0xFFF97316),
    bgBase = Color(0xFF0D0310),
    bgOrb1Clr = Color(0x8C2D0E10),
    bgOrb2Clr = Color(0x731A0519),
)
private val DARK_NIGHT = TimeSlotPalette(
    orb1 = Color(0xFF1A6FFF), orb2 = Color(0xFF7C3AED),
    a1   = Color(0xFF1A6FFF), a2   = Color(0xFF7C3AED),
    bgBase = Color(0xFF03040A),
    bgOrb1Clr = Color(0x1F1A6FFF),  // rgba(26,111,255,0.12)
    bgOrb2Clr = Color(0x1A7C3AED),  // rgba(124,58,237,0.10)
)

// ── LIGHT palettes ───────────────────────────────────────────────────────────

private val LIGHT_DAWN = TimeSlotPalette(
    orb1 = Color(0xFFC44DFF), orb2 = Color(0xFFFF6B9D),
    a1   = Color(0xFFC44DFF), a2   = Color(0xFFFF6B9D),
    bgBase = Color(0xFFF0E6FF),
    bgOrb1Clr = Color(0xCCD4A8FF),  // #d4a8ffcc
    bgOrb2Clr = Color(0xBBFFB3D4),
)
private val LIGHT_MORNING = TimeSlotPalette(
    orb1 = Color(0xFFF5A623), orb2 = Color(0xFFFF8C42),
    a1   = Color(0xFFD97706), a2   = Color(0xFFF97316),
    bgBase = Color(0xFFFFF5DD),
    bgOrb1Clr = Color(0xCCFFB347),
    bgOrb2Clr = Color(0xAAFFE066),
)
private val LIGHT_NOON = TimeSlotPalette(
    orb1 = Color(0xFFFFD200), orb2 = Color(0xFFF7971E),
    a1   = Color(0xFFD97706), a2   = Color(0xFFCA8A04),
    bgBase = Color(0xFFFFFACC),
    bgOrb1Clr = Color(0xCCFFE033),
    bgOrb2Clr = Color(0xAAFFA500),
)
private val LIGHT_AFTERNOON = TimeSlotPalette(
    orb1 = Color(0xFF4FACFE), orb2 = Color(0xFF00C6FB),
    a1   = Color(0xFF0284C7), a2   = Color(0xFF0EA5E9),
    bgBase = Color(0xFFDAEEFF),
    bgOrb1Clr = Color(0xE05BAAFF),
    bgOrb2Clr = Color(0xCC00D4FF),
)
private val LIGHT_EVENING = TimeSlotPalette(
    orb1 = Color(0xFFF5576C), orb2 = Color(0xFFF97316),
    a1   = Color(0xFFE11D48), a2   = Color(0xFFF97316),
    bgBase = Color(0xFFFFE8E4),
    bgOrb1Clr = Color(0xCCFF6B6B),
    bgOrb2Clr = Color(0xBBFF9F43),
)
private val LIGHT_NIGHT = TimeSlotPalette(
    orb1 = Color(0xFF1A6FFF), orb2 = Color(0xFF7C3AED),
    a1   = Color(0xFF1A6FFF), a2   = Color(0xFF7C3AED),
    bgBase = Color(0xFF0F172A),
    bgOrb1Clr = Color(0x611A6FFF),
    bgOrb2Clr = Color(0x4D7C3AED),
)

val TIME_DARK: Map<TimeSlot, TimeSlotPalette> = mapOf(
    TimeSlot.Dawn      to DARK_DAWN,
    TimeSlot.Morning   to DARK_MORNING,
    TimeSlot.Noon      to DARK_NOON,
    TimeSlot.Afternoon to DARK_AFTERNOON,
    TimeSlot.Evening   to DARK_EVENING,
    TimeSlot.Night     to DARK_NIGHT,
)

val TIME_LIGHT: Map<TimeSlot, TimeSlotPalette> = mapOf(
    TimeSlot.Dawn      to LIGHT_DAWN,
    TimeSlot.Morning   to LIGHT_MORNING,
    TimeSlot.Noon      to LIGHT_NOON,
    TimeSlot.Afternoon to LIGHT_AFTERNOON,
    TimeSlot.Evening   to LIGHT_EVENING,
    TimeSlot.Night     to LIGHT_NIGHT,
)

// ═══════════════════════════════════════════════════════════════════════════
// STATIC THEMES  (matches STATIC_THEME_COLORS in App.tsx)
// ═══════════════════════════════════════════════════════════════════════════

enum class OBThemePreset(
    val displayName: String,
    val isDynamic: Boolean = false,
    // accent colors used when static theme is selected
    val staticA1: Color = Color.Unspecified,
    val staticA2: Color = Color.Unspecified,
) {
    Dynamic(
        displayName = "Dynamic",
        isDynamic   = true,
        staticA1    = Color(0xFF1A6FFF),   // fallback only
        staticA2    = Color(0xFF7C3AED),
    ),
    BlueFrost(
        displayName = "Blue Frost",
        staticA1    = Color(0xFF1A6FFF),
        staticA2    = Color(0xFF7C3AED),
    ),
    PurpleAurora(
        displayName = "Purple Aurora",
        staticA1    = Color(0xFF9333EA),
        staticA2    = Color(0xFFC026D3),
    ),
    OceanGlass(
        displayName = "Ocean Glass",
        staticA1    = Color(0xFF0891B2),
        staticA2    = Color(0xFF0E7490),
    ),
    EmeraldCrystal(
        displayName = "Emerald",
        staticA1    = Color(0xFF059669),
        staticA2    = Color(0xFF047857),
    ),
    SunsetGlow(
        displayName = "Sunset Glow",
        staticA1    = Color(0xFFF97316),
        staticA2    = Color(0xFFDC2626),
    );
}

// Gradient for each theme swatch in the customise panel
fun OBThemePreset.swatchGradientColors(effectiveA1: Color, effectiveA2: Color): Pair<Color, Color> =
    when (this) {
        OBThemePreset.Dynamic       -> Pair(effectiveA1, effectiveA2)
        OBThemePreset.BlueFrost     -> Pair(Color(0xFF1A6FFF), Color(0xFF7C3AED))
        OBThemePreset.PurpleAurora  -> Pair(Color(0xFF9333EA), Color(0xFFC026D3))
        OBThemePreset.OceanGlass    -> Pair(Color(0xFF0891B2), Color(0xFF0E7490))
        OBThemePreset.EmeraldCrystal -> Pair(Color(0xFF059669), Color(0xFF047857))
        OBThemePreset.SunsetGlow    -> Pair(Color(0xFFF97316), Color(0xFFDC2626))
    }

// ═══════════════════════════════════════════════════════════════════════════
// WEATHER KIND (mirrors WeatherKind in ViewModel — duplicated here for theme use)
// ═══════════════════════════════════════════════════════════════════════════

fun resolveAccentColors(
    preset: OBThemePreset,
    timeSlot: TimeSlot,
    weatherKind: WeatherKind,
    isDark: Boolean,
): Pair<Color, Color> {
    if (!preset.isDynamic) return Pair(preset.staticA1, preset.staticA2)

    val timePalette = if (isDark) TIME_DARK[timeSlot]!! else TIME_LIGHT[timeSlot]!!
    val a1 = when (weatherKind) {
        WeatherKind.Thunderstorm          -> Color(0xFF9B59B6)
        WeatherKind.Rain, WeatherKind.Drizzle -> Color(0xFF4FACFE)
        WeatherKind.Snow                  -> Color(0xFFA8D8F0)
        else                              -> timePalette.a1
    }
    val a2 = when (weatherKind) {
        WeatherKind.Thunderstorm          -> Color(0xFF6C3483)
        WeatherKind.Rain, WeatherKind.Drizzle -> Color(0xFF00C6FB)
        WeatherKind.Snow                  -> Color(0xFFC5E8FF)
        else                              -> timePalette.a2
    }
    return Pair(a1, a2)
}

fun resolveWeatherTint(weatherKind: WeatherKind, isDark: Boolean): Color = when (weatherKind) {
    WeatherKind.Cloudy      -> if (isDark) Color(0x2E505082) else Color(0x1E6478A0)
    WeatherKind.Fog         -> if (isDark) Color(0x4D78909C) else Color(0x40889AAA)
    WeatherKind.Drizzle     -> if (isDark) Color(0x333250A0) else Color(0x1E4664B4)
    WeatherKind.Rain        -> if (isDark) Color(0x471E3C8C) else Color(0x293C5AB4)
    WeatherKind.Thunderstorm -> if (isDark) Color(0x730F0A23) else Color(0x401E143C)
    WeatherKind.Snow        -> if (isDark) Color(0x1EDCE8FF) else Color(0x2EDCEBFF)
    else                    -> Color.Transparent
}

// ═══════════════════════════════════════════════════════════════════════════
// GLASS TOKENS  (mirrors the glassBg/glassBorder variables in App.tsx)
// ═══════════════════════════════════════════════════════════════════════════

data class OBGlassTokens(
    val isDark: Boolean,
    // backgrounds
    val glassBg:   Color,   // card default
    val glassBg2:  Color,   // medium
    val glassBg3:  Color,   // heavy
    // borders
    val glassBorder:  Color,
    val glassBorder2: Color,
    // text
    val txColor:  Color,
    val tx2Color: Color,
    val tx3Color: Color,
    // surface background colours
    val phoneBg:  Color,
    val phoneBg2: Color,
    val islandBg: Color,
    // semantic
    val green:  Color,
    val red:    Color,
    val amber:  Color,
    val violet: Color,
)

fun buildGlassTokens(isDark: Boolean) = OBGlassTokens(
    isDark       = isDark,
    glassBg      = if (isDark) Color(0x12FFFFFF) else Color(0x8CFFFFFF),
    glassBg2     = if (isDark) Color(0x1FFFFFFF) else Color(0xB8FFFFFF),
    glassBg3     = if (isDark) Color(0x29FFFFFF) else Color(0xD1FFFFFF),
    glassBorder  = if (isDark) Color(0x1FFFFFFF) else Color(0xB3FFFFFF),
    glassBorder2 = if (isDark) Color(0x33FFFFFF) else Color(0xD9FFFFFF),
    txColor      = if (isDark) Color(0xFFEEF0FF) else Color(0xFF12122A),
    tx2Color     = if (isDark) Color(0x8CEEF0FF) else Color(0x9912122A),
    tx3Color     = if (isDark) Color(0x47EEF0FF) else Color(0x5912122A),
    phoneBg      = if (isDark) Color(0xFF06070F) else Color(0xFFEEF1FF),
    phoneBg2     = if (isDark) Color(0xFF0B0D1A) else Color(0xFFE2E6F8),
    islandBg     = if (isDark) Color(0x8C080A16) else Color(0x6BFFFFFF),
    green        = Color(0xFF00DDA0),
    red          = Color(0xFFFF4D6D),
    amber        = Color(0xFFFFB74D),
    violet       = Color(0xFFC084FC),
)

// ═══════════════════════════════════════════════════════════════════════════
// FULL THEME CONFIG  (all resolved values, computed once per recomposition)
// ═══════════════════════════════════════════════════════════════════════════

data class OBThemeConfig(
    val preset:       OBThemePreset,
    val isDark:       Boolean,
    val timeSlot:     TimeSlot,
    val weatherKind:  WeatherKind,
    val effectiveA1:  Color,
    val effectiveA2:  Color,
    val orb1:         Color,
    val orb2:         Color,
    val orb3:         Color,
    val orb4:         Color,
    val orb5:         Color,
    val bgBase:       Color,
    val bgOrb1Clr:    Color,
    val bgOrb2Clr:    Color,
    val weatherTint:  Color,
    val glass:        OBGlassTokens,
) {
    // Convenience shortcuts used throughout composables
    val glow: Color   get() = effectiveA1.copy(alpha = 0.33f)
    val pill: Color   get() = effectiveA1.copy(alpha = 0.12f)
    val pillBorder: Color get() = effectiveA1.copy(alpha = 0.25f)
}

// ═══════════════════════════════════════════════════════════════════════════
// COMPOSITION LOCAL
// ═══════════════════════════════════════════════════════════════════════════

val LocalOBTheme = compositionLocalOf {
    buildDefaultConfig()
}

private fun buildDefaultConfig(): OBThemeConfig {
    val glass = buildGlassTokens(isDark = true)
    return OBThemeConfig(
        preset       = OBThemePreset.BlueFrost,
        isDark       = true,
        timeSlot     = TimeSlot.Night,
        weatherKind  = WeatherKind.Clear,
        effectiveA1  = DARK_NIGHT.a1,
        effectiveA2  = DARK_NIGHT.a2,
        orb1         = DARK_NIGHT.orb1,
        orb2         = DARK_NIGHT.orb2,
        orb3         = DARK_NIGHT.orb1,
        orb4         = DARK_NIGHT.orb2,
        orb5         = DARK_NIGHT.orb2,
        bgBase       = DARK_NIGHT.bgBase,
        bgOrb1Clr    = DARK_NIGHT.bgOrb1Clr,
        bgOrb2Clr    = DARK_NIGHT.bgOrb2Clr,
        weatherTint  = Color.Transparent,
        glass        = glass,
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// DEPRECATED SHIM  (keeps existing code compiling until it's rewritten)
// Remove once all screens are migrated to LocalOBTheme / OBThemeConfig.
// ═══════════════════════════════════════════════════════════════════════════

object OBColors {
    val Background    = Color(0xFF06070F)
    val Surface       = Color(0x12FFFFFF)
    val Surface2      = Color(0x1FFFFFFF)
    val SurfaceHigh   = Color(0x29FFFFFF)
    val Border        = Color(0x1FFFFFFF)
    val Border2       = Color(0x33FFFFFF)
    val TextPrimary   = Color(0xFFEEF0FF)
    val TextSecondary = Color(0x8CEEF0FF)
    val TextTertiary  = Color(0x47EEF0FF)
    val Green  = Color(0xFF00DDA0)
    val Red    = Color(0xFFFF4D6D)
    val Amber  = Color(0xFFFFB74D)
    val Violet = Color(0xFFC084FC)
    val Blue   = Color(0xFF5B8FFF)
    val Purple = Color(0xFF7C4DFF)
}

// kept for backward-compat with old screens not yet rewritten
class OBThemeColors(
    val preset:         OBThemePreset,
    val accent1:        Color,
    val accent2:        Color,
    val glowColor:      Color,
    val pillBackground: Color,
)

val LocalOBThemeLegacy = compositionLocalOf {
    OBThemeColors(
        preset         = OBThemePreset.BlueFrost,
        accent1        = Color(0xFF1A6FFF),
        accent2        = Color(0xFF7C3AED),
        glowColor      = Color(0x591A6FFF),
        pillBackground = Color(0x1F1A6FFF),
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// OBTheme COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun OBTheme(
    preset:        OBThemePreset = OBThemePreset.BlueFrost,
    isDarkOverride: Boolean? = null,
    weatherKind:   WeatherKind = WeatherKind.Clear,
    timeSlot:      TimeSlot = TimeSlot.Night,
    content:       @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = isDarkOverride ?: systemDark

    val glass = remember(isDark) { buildGlassTokens(isDark) }

    val (effectiveA1, effectiveA2) = remember(preset, timeSlot, weatherKind, isDark) {
        resolveAccentColors(preset, timeSlot, weatherKind, isDark)
    }

    val timePalette = remember(preset, timeSlot, isDark) {
        if (preset.isDynamic) {
            if (isDark) TIME_DARK[timeSlot]!! else TIME_LIGHT[timeSlot]!!
        } else {
            // static theme uses accent colours for orbs; bg is simple dark/light surface
            TimeSlotPalette(
                orb1 = preset.staticA1,
                orb2 = preset.staticA2,
                a1   = preset.staticA1,
                a2   = preset.staticA2,
                bgBase    = if (isDark) Color(0xFF03040A) else Color(0xFFEEF1FF),
                bgOrb1Clr = preset.staticA1.copy(alpha = if (isDark) 0.18f else 0.38f),
                bgOrb2Clr = preset.staticA2.copy(alpha = if (isDark) 0.14f else 0.30f),
            )
        }
    }

    val weatherTint = remember(weatherKind, isDark) { resolveWeatherTint(weatherKind, isDark) }

    // Smooth gradual color animations for theme switching & dark/light mode transitions
    val animSpec = tween<Color>(550, easing = FastOutSlowInEasing)

    val animA1          by animateColorAsState(effectiveA1, animSpec, label = "a1")
    val animA2          by animateColorAsState(effectiveA2, animSpec, label = "a2")
    val animOrb1        by animateColorAsState(timePalette.orb1, animSpec, label = "orb1")
    val animOrb2        by animateColorAsState(timePalette.orb2, animSpec, label = "orb2")
    val animBgBase      by animateColorAsState(timePalette.bgBase, animSpec, label = "bgBase")
    val animBgOrb1Clr   by animateColorAsState(timePalette.bgOrb1Clr, animSpec, label = "bgOrb1")
    val animBgOrb2Clr   by animateColorAsState(timePalette.bgOrb2Clr, animSpec, label = "bgOrb2")
    val animWeatherTint by animateColorAsState(weatherTint, animSpec, label = "weatherTint")

    val animGlassBg     by animateColorAsState(glass.glassBg, animSpec, label = "gBg")
    val animGlassBg2    by animateColorAsState(glass.glassBg2, animSpec, label = "gBg2")
    val animGlassBg3    by animateColorAsState(glass.glassBg3, animSpec, label = "gBg3")
    val animGlassBorder by animateColorAsState(glass.glassBorder, animSpec, label = "gBorder")
    val animGlassBorder2 by animateColorAsState(glass.glassBorder2, animSpec, label = "gBorder2")
    val animTxColor     by animateColorAsState(glass.txColor, animSpec, label = "tx")
    val animTx2Color    by animateColorAsState(glass.tx2Color, animSpec, label = "tx2")
    val animTx3Color    by animateColorAsState(glass.tx3Color, animSpec, label = "tx3")
    val animPhoneBg     by animateColorAsState(glass.phoneBg, animSpec, label = "pBg")
    val animPhoneBg2    by animateColorAsState(glass.phoneBg2, animSpec, label = "pBg2")
    val animIslandBg    by animateColorAsState(glass.islandBg, animSpec, label = "iBg")

    val animatedGlass = OBGlassTokens(
        isDark       = isDark,
        glassBg      = animGlassBg,
        glassBg2     = animGlassBg2,
        glassBg3     = animGlassBg3,
        glassBorder  = animGlassBorder,
        glassBorder2 = animGlassBorder2,
        txColor      = animTxColor,
        tx2Color     = animTx2Color,
        tx3Color     = animTx3Color,
        phoneBg      = animPhoneBg,
        phoneBg2     = animPhoneBg2,
        islandBg     = animIslandBg,
        green        = glass.green,
        red          = glass.red,
        amber        = glass.amber,
        violet       = glass.violet,
    )

    val config = OBThemeConfig(
        preset       = preset,
        isDark       = isDark,
        timeSlot     = timeSlot,
        weatherKind  = weatherKind,
        effectiveA1  = animA1,
        effectiveA2  = animA2,
        orb1         = animOrb1,
        orb2         = animOrb2,
        orb3         = animOrb1,
        orb4         = animOrb2,
        orb5         = animOrb2,
        bgBase       = animBgBase,
        bgOrb1Clr    = animBgOrb1Clr,
        bgOrb2Clr    = animBgOrb2Clr,
        weatherTint  = animWeatherTint,
        glass        = animatedGlass,
    )

    val legacyColors = OBThemeColors(
        preset         = preset,
        accent1        = animA1,
        accent2        = animA2,
        glowColor      = animA1.copy(alpha = 0.33f),
        pillBackground = animA1.copy(alpha = 0.12f),
    )

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary        = effectiveA1,
            secondary      = effectiveA2,
            background     = glass.phoneBg,
            surface        = glass.glassBg,
            surfaceVariant = glass.glassBg2,
            onPrimary      = Color.White,
            onSecondary    = Color.White,
            onBackground   = glass.txColor,
            onSurface      = glass.txColor,
            error          = glass.red,
        )
    } else {
        lightColorScheme(
            primary        = effectiveA1,
            secondary      = effectiveA2,
            background     = glass.phoneBg,
            surface        = glass.glassBg,
            surfaceVariant = glass.glassBg2,
            onPrimary      = Color.White,
            onSecondary    = Color.White,
            onBackground   = glass.txColor,
            onSurface      = glass.txColor,
            error          = glass.red,
        )
    }

    CompositionLocalProvider(
        LocalOBTheme       provides config,
        LocalOBThemeLegacy provides legacyColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content     = content,
        )
    }
}

// Greet text & emoji for each time slot
val GREET_TEXT: Map<TimeSlot, String> = mapOf(
    TimeSlot.Dawn      to "Good dawn",
    TimeSlot.Morning   to "Good morning",
    TimeSlot.Noon      to "Good noon",
    TimeSlot.Afternoon to "Good afternoon",
    TimeSlot.Evening   to "Good evening",
    TimeSlot.Night     to "Good night",
)

val GREET_EMOJI: Map<TimeSlot, String> = mapOf(
    TimeSlot.Dawn      to "🌅",
    TimeSlot.Morning   to "☀️",
    TimeSlot.Noon      to "🌞",
    TimeSlot.Afternoon to "⛅",
    TimeSlot.Evening   to "🌇",
    TimeSlot.Night     to "🌙",
)

val WEATHER_ICON: Map<WeatherKind, String> = mapOf(
    WeatherKind.Clear       to "☀️",
    WeatherKind.Cloudy      to "☁️",
    WeatherKind.Fog         to "🌫️",
    WeatherKind.Drizzle     to "🌦️",
    WeatherKind.Rain        to "🌧️",
    WeatherKind.Thunderstorm to "⛈️",
    WeatherKind.Snow        to "❄️",
)

val WEATHER_LABEL: Map<WeatherKind, String> = mapOf(
    WeatherKind.Clear       to "Clear",
    WeatherKind.Cloudy      to "Cloudy",
    WeatherKind.Fog         to "Foggy",
    WeatherKind.Drizzle     to "Drizzle",
    WeatherKind.Rain        to "Rainy",
    WeatherKind.Thunderstorm to "Thunderstorm",
    WeatherKind.Snow        to "Snowing",
)
