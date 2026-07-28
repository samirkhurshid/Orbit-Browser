package com.orbit.browser.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.orbit.browser.ui.WeatherKind
import com.orbit.browser.ui.theme.LocalOBTheme
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════════════
// RAIN PARTICLES
// Mirrors App.tsx RainParticles:
//   - light: 45 drops, 1px wide, height 8–22px, dur 0.9–1.5s
//   - heavy: 80 drops, 1.5px wide, height 12–34px, dur 0.5–1.0s
//   - 12° slant (CSS rotate(12deg) → slantFactor ≈ tan12° = 0.213)
//   - color: rgba(160, 200, 255, alpha)  — bluish-white streaks
// ═══════════════════════════════════════════════════════════════════════════

private class RainDrop(
    var x: Float,          // 0..1  (% of screen width)
    var y: Float,          // 0..1+ (may start above screen)
    val speed: Float,      // pixels-per-frame (normalised)
    val length: Float,     // dp equivalent px (unnormalised, raw px)
    val alpha: Float,
    val strokeDp: Float,
)

@Composable
fun RainParticles(
    intensity: String = "light",
    modifier: Modifier = Modifier,
) {
    val count = if (intensity == "heavy") 45 else 22
    val strokeDp = 1f

    // Stable random seed per composition — minimal aesthetic rain drops
    val drops = remember(intensity) {
        List(count) {
            RainDrop(
                x       = (-0.02f + Math.random().toFloat() * 1.05f).coerceIn(-0.02f, 1.03f),
                y       = Math.random().toFloat() * -1.5f,
                speed   = 0.014f + Math.random().toFloat() * 0.010f,
                length  = 7f + Math.random().toFloat() * 11f,
                alpha   = 0.12f + Math.random().toFloat() * 0.18f,
                strokeDp = strokeDp,
            )
        }
    }

    // Frame-loop: advance every drop each frame
    LaunchedEffect(intensity) {
        while (true) {
            withFrameMillis {
                for (d in drops) {
                    d.y += d.speed
                    if (d.y > 1.05f) {
                        d.y = -0.1f
                        d.x = (-0.02f + Math.random().toFloat() * 1.05f).coerceIn(-0.02f, 1.03f)
                    }
                }
            }
        }
    }

    // Read mutableState so Canvas recomposes every frame
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(intensity) { while (true) { withFrameMillis { tick = it } } }

    @Suppress("UNUSED_EXPRESSION") tick  // force recompose

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val slant = 0.213f // tan(12°)

        for (d in drops) {
            val sx = d.x * w
            val sy = d.y * h
            val lenPx = d.length   // already in raw px from reference (no dp scaling needed here)
            val ex = sx + lenPx * slant
            val ey = sy + lenPx

            if (sy > -lenPx && sy < h) {
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(160, 200, 255, (d.alpha * 255).toInt()),
                        ),
                        startY = sy,
                        endY   = ey,
                    ),
                    start       = Offset(sx, sy),
                    end         = Offset(ex, ey),
                    strokeWidth = d.strokeDp * density,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SNOW PARTICLES
// Mirrors App.tsx SnowParticles:
//   55 flakes, size 3–12px, dur 5–12s, drift ±40px, rgba(220,235,255,0.85)
//   glow: box-shadow 0 0 ${size}px rgba(200,220,255,0.5) → Canvas radial gradient
// ═══════════════════════════════════════════════════════════════════════════

private class SnowFlake(
    var x: Float,       // 0..1
    var y: Float,       // 0..1+
    val speed: Float,
    val sizePx: Float,
    val drift: Float,   // half-amplitude of sinusoidal X drift
    var phase: Float,   // radians, advances per frame
)

@Composable
fun SnowParticles(modifier: Modifier = Modifier) {
    val flakes = remember {
        List(55) {
            SnowFlake(
                x      = (-0.05f + Math.random().toFloat() * 1.10f).coerceIn(-0.05f, 1.05f),
                y      = Math.random().toFloat() * -1f,
                speed  = 0.0012f + Math.random().toFloat() * 0.0015f,
                sizePx = 3f + Math.random().toFloat() * 9f,
                drift  = (Math.random().toFloat() - 0.5f) * 80f,  // ±40 px
                phase  = Math.random().toFloat() * 6.283f,
            )
        }
    }

    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { t ->
                tick = t
                for (f in flakes) {
                    f.y     += f.speed
                    f.phase += 0.015f
                    if (f.y > 1.05f) {
                        f.y = -0.05f
                        f.x = (-0.05f + Math.random().toFloat() * 1.10f).coerceIn(-0.05f, 1.05f)
                    }
                }
            }
        }
    }

    @Suppress("UNUSED_EXPRESSION") tick

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (f in flakes) {
            val cy = f.y * h
            if (cy < -f.sizePx || cy > h + f.sizePx) continue

            // X with sinusoidal drift (CSS --drift translateX)
            val driftPx = kotlin.math.sin(f.phase.toDouble()).toFloat() * f.drift
            val cx = f.x * w + driftPx

            val r = (f.sizePx / 2f) * density

            // Core flake: rgba(220, 235, 255, 0.85)
            drawCircle(
                color  = Color(220, 235, 255, 217),
                radius = r,
                center = Offset(cx, cy),
            )
            // Glow halo: rgba(200, 220, 255, 0.5) spread = r*2
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(
                        Color(200, 220, 255, 128),
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = r * 2.2f,
                ),
                radius = r * 2.2f,
                center = Offset(cx, cy),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LIGHTNING FLASH
// Mirrors App.tsx LightningFlash:
//   double-flash: 120 ms on → off → 80 ms on → off, interval 4–12 s
//   color: rgba(200, 220, 255, 0.18)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun LightningFlash(modifier: Modifier = Modifier) {
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000L + (Math.random() * 8000).toLong())
            // Flash 1
            flashAlpha = 0.18f
            delay(120)
            flashAlpha = 0f
            delay(40)
            // Flash 2 (shorter)
            flashAlpha = 0.14f
            delay(80)
            flashAlpha = 0f
        }
    }

    if (flashAlpha > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(200, 220, 255, (flashAlpha * 255).toInt()))
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FROST GLASS
// Mirrors App.tsx FrostGlass:
//   rain/drizzle/thunderstorm: 28 teardrop drops, rgba(180,210,255) + white highlight
//   snow: 20 circular ice crystals, rgba(230,240,255)
//   global backdrop-filter blur(0.5px) → Modifier.blur(0.5.dp) on wrapper
// ═══════════════════════════════════════════════════════════════════════════

private class FrostDrop(val x: Float, val y: Float, val size: Float, val alpha: Float)

@Composable
fun FrostGlass(
    weather: String,   // "rain" | "snow"
    modifier: Modifier = Modifier,
) {
    val isSnow = weather == "snow"
    val count = if (isSnow) 20 else 28

    val drops = remember(weather) {
        List(count) {
            FrostDrop(
                x     = 0.05f + Math.random().toFloat() * 0.90f,
                y     = 0.05f + Math.random().toFloat() * 0.90f,
                size  = if (isSnow) 4f + Math.random().toFloat() * 8f
                        else        2f + Math.random().toFloat() * 7f,
                alpha = 0.25f + Math.random().toFloat() * 0.35f,
            )
        }
    }

    // backdrop blur(0.5px) — the very subtle global surface wetness
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .blur(0.5.dp),
    ) {
        val w = size.width
        val h = size.height

        for (d in drops) {
            val cx = d.x * w
            val cy = d.y * h
            val r  = d.size * density

            if (isSnow) {
                // Ice crystal — circle, rgba(230,240,255)
                drawCircle(
                    color  = Color(230, 240, 255, (d.alpha * 255).toInt()),
                    radius = r,
                    center = Offset(cx, cy),
                )
            } else {
                // Raindrop teardrop — oval, with white highlight spec
                // Main droplet body: radial-gradient(ellipse at 30% 30%, white, rgba(180,210,255))
                drawOval(
                    brush     = Brush.radialGradient(
                        colors  = listOf(
                            Color(255, 255, 255, ((d.alpha + 0.1f) * 255).toInt()),
                            Color(180, 210, 255, (d.alpha * 255).toInt()),
                        ),
                        center  = Offset(cx - r * 0.2f, cy - r * 0.2f),
                        radius  = r * 1.4f,
                    ),
                    topLeft   = Offset(cx - r / 2f, cy - r * 0.8f),
                    size      = Size(r, r * 1.6f),
                )
                // Specular highlight dot
                drawOval(
                    color   = Color(255, 255, 255, ((d.alpha + 0.1f) * 255 * 0.7f).toInt()),
                    topLeft = Offset(cx - r / 5f, cy - r * 0.6f),
                    size    = Size(r / 2.5f, r * 0.55f),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FOG OVERLAY
// App.tsx weatherTint for fog = rgba(120,140,160,0.3) dark / rgba(140,155,170,0.25) light
// + a gentle pulse to feel like drifting mist
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun FogOverlay(modifier: Modifier = Modifier) {
    val theme = LocalOBTheme.current
    val fogColor = if (theme.isDark) Color(120, 140, 160, 76) else Color(140, 155, 170, 64)

    val infiniteTransition = rememberInfiniteTransition(label = "fog_pulse")
    val fogAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.6f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fog_alpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .blur(8.dp)
            .background(fogColor.copy(alpha = fogColor.alpha * fogAlpha)),
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// WEATHER OVERLAY LAYER  (convenience composable for MainActivity)
// Draws: weatherTint box → fog blur → rain/snow/lightning/frost
// Reads weatherKind directly from LocalOBTheme — no parameters needed.
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun WeatherOverlayLayer(modifier: Modifier = Modifier) {
    val theme = LocalOBTheme.current

    // ── 1. Weather tint ──────────────────────────────────────────────────
    if (theme.weatherTint != Color.Transparent) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(theme.weatherTint),
        )
    }

    // ── 2. Fog: extra blur overlay with pulse ────────────────────────────
    if (theme.weatherKind == WeatherKind.Fog) {
        FogOverlay(modifier = modifier)
    }

    // ── 3. Wallpaper particles (Rain removed from wallpaper as requested) ──
    when (theme.weatherKind) {
        WeatherKind.Snow -> SnowParticles(modifier)
        else             -> Unit
    }

    // ── 4. Lightning flash ───────────────────────────────────────────────
    if (theme.weatherKind == WeatherKind.Thunderstorm) {
        LightningFlash(modifier = modifier)
    }

    // ── 5. Frost (Only for Snow on wallpaper) ───────────────────────────
    if (theme.weatherKind == WeatherKind.Snow) {
        FrostGlass(
            weather  = "snow",
            modifier = modifier,
        )
    }
}
