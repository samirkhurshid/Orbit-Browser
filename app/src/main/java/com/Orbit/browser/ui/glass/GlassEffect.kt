package com.orbit.browser.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

object GlassTokens {
    val BlurLight   = 16.dp
    val BlurMedium  = 24.dp
    val BlurHeavy   = 36.dp

    const val TintDark  = 0.55f
    const val TintLight = 0.50f
}

/**
 * The [HazeState] that backdrop-blur consumers (frostedGlass surfaces) read from.
 * Provided once, high up in the tree (see OrbitBrowserApp), over the layer that
 * contains the actual background content (web pages, mesh background, screens)
 * that should be visible - blurred - behind glass surfaces.
 *
 * Defaults to null so previews / call sites without a haze source still compile
 * and simply fall back to a plain tinted surface instead of crashing.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Universal Figma-grade Frosted Glass Surface Modifier.
 * Blurs whatever is actually drawn behind the surface (via Haze backdrop blur),
 * then layers a translucent tint and a specular top edge light on top.
 * Keeps all nested text, icons, and graphics crisp, sharp, and ultra-legible.
 */
fun Modifier.frostedGlass(
    isDark: Boolean = true,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    blurRadius: Dp = 32.dp,
    borderWidth: Dp = 0.5.dp,
    accentColor: Color? = null,
    isFocused: Boolean = false,
): Modifier = composed {
    val hazeState = LocalHazeState.current

    val tintColor = if (isDark) Color(0xFF070914) else Color(0xFFE8EEFC)
    val tintAlpha = if (isDark) 0.62f else 0.72f

    // Crystalline Glassmorphism background fill - deeper opacity for rich frosted glass
    val glassmorphismBg = if (isDark) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.White.copy(alpha = 0.16f),
                0.5f to Color.White.copy(alpha = 0.08f),
                1.0f to Color(0x40000000)
            )
        )
    } else {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color.White.copy(alpha = 0.85f),
                0.5f to Color.White.copy(alpha = 0.68f),
                1.0f to Color.White.copy(alpha = 0.55f)
            )
        )
    }

    // Specular Glass Rim Reflection & Border
    val glassBorder = if (accentColor != null) {
        Brush.verticalGradient(
            listOf(
                accentColor.copy(alpha = 0.65f),
                accentColor.copy(alpha = 0.20f)
            )
        )
    } else if (isFocused) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF60A5FA).copy(alpha = 0.75f),
                Color(0xFF3B82F6).copy(alpha = 0.30f)
            )
        )
    } else if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.06f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.90f),
                Color.White.copy(alpha = 0.40f)
            )
        )
    }

    var m = this.clip(shape)

    m = if (hazeState != null) {
        m.hazeEffect(state = hazeState) {
            this.blurRadius = blurRadius
            backgroundColor = tintColor
            tints = listOf(HazeTint(tintColor.copy(alpha = tintAlpha)))
            noiseFactor = 0f
        }.background(brush = glassmorphismBg, shape = shape)
    } else {
        m.background(brush = glassmorphismBg, shape = shape)
    }

    m = m.border(width = borderWidth, brush = glassBorder, shape = shape)

    return@composed m
}

@Composable
fun GlassWaterDroplets(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val droplets = listOf(
            Triple(0.12f, 0.28f, 5.0f),
            Triple(0.78f, 0.22f, 4.2f),
            Triple(0.88f, 0.72f, 5.8f),
            Triple(0.32f, 0.76f, 3.8f),
            Triple(0.58f, 0.32f, 4.5f),
        )
        for ((rx, ry, rDp) in droplets) {
            val cx = rx * w
            val cy = ry * h
            val r = rDp * density

            // Shadow
            drawCircle(
                color = Color.Black.copy(alpha = if (isDark) 0.28f else 0.15f),
                radius = r * 1.08f,
                center = androidx.compose.ui.geometry.Offset(cx + 0.8f * density, cy + 1.2f * density)
            )
            // Bead body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.40f else 0.65f),
                        Color(0xFF90B0D0).copy(alpha = if (isDark) 0.18f else 0.35f),
                    ),
                    center = androidx.compose.ui.geometry.Offset(cx - r * 0.3f, cy - r * 0.3f),
                    radius = r * 1.3f
                ),
                radius = r,
                center = androidx.compose.ui.geometry.Offset(cx, cy)
            )
            // Highlight dot
            drawCircle(
                color = Color.White.copy(alpha = 0.90f),
                radius = r * 0.32f,
                center = androidx.compose.ui.geometry.Offset(cx - r * 0.35f, cy - r * 0.35f)
            )
        }
    }
}

@Composable
fun GlassPanel(
    blurRadius: Dp = GlassTokens.BlurMedium,
    tint: Color = Color.White.copy(alpha = GlassTokens.TintLight),
    rimColor: Color = Color.White.copy(alpha = 0.35f),
    rimWidth: Dp = 1.dp,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .background(tint, shape)
            .border(rimWidth, rimColor, shape)
            .clip(shape),
        content = content,
    )
}
