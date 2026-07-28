package com.orbit.browser.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orbit.browser.ui.glass.frostedGlass
import com.orbit.browser.ui.theme.LocalOBTheme

/**
 * Minimal Glassmorphic Orbit App Icon Composable.
 * Features:
 * - Squircle Glassmorphic background container with specular border
 * - Clean minimal "O" letter ring matching the launcher app icon
 * - Proportional smaller "O" radius for refined minimal aesthetics
 */
@Composable
fun OrbitAppIcon(
    size: Dp = 64.dp,
    modifier: Modifier = Modifier,
    overrideA1: Color? = null,
    overrideA2: Color? = null,
    showGlassBackground: Boolean = true,
) {
    val theme = LocalOBTheme.current
    val isDark = theme.isDark
    val a1 = overrideA1 ?: theme.effectiveA1
    val a2 = overrideA2 ?: theme.effectiveA2

    val glassBgGradient = remember(a1, a2, isDark) {
        if (isDark) {
            Brush.linearGradient(
                listOf(
                    a1.copy(alpha = 0.22f),
                    a2.copy(alpha = 0.14f),
                    Color(0x20070A18)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.88f),
                    a1.copy(alpha = 0.15f),
                    a2.copy(alpha = 0.10f)
                )
            )
        }
    }

    val squircleShape = RoundedCornerShape(size * 0.28f)

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showGlassBackground) {
                    Modifier
                        .frostedGlass(isDark = isDark, shape = squircleShape, blurRadius = 24.dp)
                        .background(brush = glassBgGradient, shape = squircleShape)
                        .border(
                            width = 0.75.dp,
                            brush = Brush.verticalGradient(
                                if (isDark) listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))
                                else listOf(Color.White.copy(alpha = 0.90f), Color.White.copy(alpha = 0.25f))
                            ),
                            shape = squircleShape
                        )
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size * 0.72f)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            // Smaller "O" radius and refined stroke width
            val oRadius = w * 0.22f
            val oStrokeWidth = w * 0.10f

            val iconColor = if (isDark) Color.White.copy(alpha = 0.95f) else Color(0xFF0F172A)

            // CLEAN MINIMAL "O" CIRCLE (Matching launcher app icon)
            drawCircle(
                color = iconColor,
                radius = oRadius,
                center = Offset(cx, cy),
                style = Stroke(width = oStrokeWidth)
            )
        }
    }
}
