package com.orbit.browser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orbit.browser.ui.theme.OBColors
import com.orbit.browser.ui.theme.LocalOBThemeLegacy
import com.orbit.browser.ui.theme.LocalOBTheme
import com.orbit.browser.ui.glass.frostedGlass
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp   = 24.dp,
    borderAlpha: Float = 0.09f,
    fillAlpha: Float   = 0.055f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = OBColors.Border.copy(alpha = borderAlpha)
    val fillColor   = Color.White.copy(alpha = fillAlpha)

    val baseModifier = modifier
        .clip(shape)
        .background(fillColor)
        .border(1.dp, borderColor, shape)

    if (onClick != null) {
        Box(
            modifier = baseModifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = androidx.compose.material.ripple.rememberRipple(color = Color.White.copy(alpha = 0.08f)),
                onClick           = onClick,
            ),
            content = content,
        )
    } else {
        Box(modifier = baseModifier, content = content)
    }
}

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    fillAlpha: Float   = 0.09f,
    borderAlpha: Float = 0.14f,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = fillAlpha))
            .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun OBToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalOBThemeLegacy.current
    val trackColor by animateColorAsState(
        targetValue   = if (checked) theme.accent1 else Color.White.copy(alpha = 0.12f),
        animationSpec = tween(durationMillis = 280),
        label         = "toggle_track",
    )
    val thumbOffset by animateDpAsState(
        targetValue   = if (checked) 20.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label         = "toggle_thumb",
    )

    Box(
        modifier = modifier
            .size(width = 46.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
            ) { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .offset(x = thumbOffset, y = 3.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White),
        )
    }
}

@Composable
fun SecurityBadge(
    isSecure: Boolean,
    modifier: Modifier = Modifier,
) {
    val color  = if (isSecure) OBColors.Green else OBColors.Red
    val bg     = color.copy(alpha = 0.12f)
    val border = color.copy(alpha = 0.25f)
    val icon   = if (isSecure) "🔒" else "🔓"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(icon, style = MaterialTheme.typography.labelSmall)
        Text(
            text  = if (isSecure) "Secure" else "Not Secure",
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
fun PrivacyStatChip(
    number: String,
    label: String,
    color: Color = OBColors.Green,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier     = modifier,
        cornerRadius = 14.dp,
        fillAlpha    = 0.09f,
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = number, style = MaterialTheme.typography.titleLarge, color = color)
            Spacer(Modifier.height(2.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = OBColors.TextSecondary)
        }
    }
}

@Composable
fun FrostedCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp   = 28.dp,
    theme: com.orbit.browser.ui.theme.OBThemeConfig,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = theme.isDark
    val shape = RoundedCornerShape(cornerRadius)

    val baseModifier = modifier
        .frostedGlass(isDark = isDark, shape = shape, blurRadius = 32.dp)

    if (onClick != null) {
        Box(
            modifier = baseModifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = androidx.compose.material.ripple.rememberRipple(color = Color.White.copy(alpha = 0.08f)),
                onClick           = onClick,
            ),
            content = content,
        )
    } else {
        Box(modifier = baseModifier, content = content)
    }
}

fun Modifier.glowShadow(color: Color, radius: Dp = 24.dp): Modifier = this.drawBehind {
    val shadowColor = color.copy(alpha = 0.35f)
    drawCircle(
        color     = shadowColor,
        radius    = size.minDimension / 2 + radius.toPx(),
        center    = center,
        blendMode = BlendMode.Screen,
    )
}

@Composable
fun ChevronLeftIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(22.dp)) {
        val sw = 2.6f * density
        val p = Path().apply {
            moveTo(15f / 24f * size.width, 18f / 24f * size.height)
            lineTo(9f / 24f * size.width,  12f / 24f * size.height)
            lineTo(15f / 24f * size.width,  6f / 24f * size.height)
        }
        drawPath(
            path  = p,
            color = color,
            style = Stroke(
                width = sw,
                cap   = androidx.compose.ui.graphics.StrokeCap.Round,
                join  = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}

@Composable
fun FrostedBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    iconColor: Color = Color.Unspecified,
) {
    val theme = LocalOBTheme.current
    val color = if (iconColor != Color.Unspecified) iconColor else theme.glass.txColor

    Box(
        modifier = modifier
            .size(44.dp)
            .frostedGlass(isDark = isDark, shape = CircleShape, blurRadius = 24.dp)
            .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = LocalIndication.current,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        ChevronLeftIcon(color = color)
    }
}

@Composable
fun WeatherDetailModal(
    weather: com.orbit.browser.ui.WeatherState,
    theme: com.orbit.browser.ui.theme.OBThemeConfig,
    onDismiss: () -> Unit
) {
    val isDark = theme.isDark
    val g = theme.glass
    val a1 = theme.effectiveA1

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .frostedGlass(isDark = isDark, shape = RoundedCornerShape(32.dp), blurRadius = 36.dp)
                .background(
                    if (isDark) Color(0xFF0C1022).copy(alpha = 0.94f)
                    else Color(0xFFF1F5F9).copy(alpha = 0.96f)
                )
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header location & close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = weather.cityName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = g.txColor
                        )
                        Text(
                            text = "Live Weather Update",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = g.tx2Color
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(g.glassBg2)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = g.tx2Color)
                    }
                }

                // Weather Condition Big Display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    com.orbit.browser.ui.components.WeatherConditionIcon(
                        kind = weather.weatherKind,
                        isDark = isDark,
                        size = 56.dp
                    )

                    Column {
                        Text(
                            text = "${weather.temperature.toInt()}°C",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = a1,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "${weather.weatherKind.label} · Feels like ${weather.feelsLike}°C",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = g.tx2Color
                        )
                    }
                }

                HorizontalDivider(color = g.glassBorder2.copy(alpha = 0.5f))

                // Weather Details Grid Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WeatherInfoChip(
                        modifier = Modifier.weight(1f),
                        label = "Humidity",
                        value = "${weather.humidity}%",
                        isDark = isDark,
                        g = g
                    )
                    WeatherInfoChip(
                        modifier = Modifier.weight(1f),
                        label = "Wind",
                        value = "${weather.windSpeed.toInt()} km/h",
                        isDark = isDark,
                        g = g
                    )
                    WeatherInfoChip(
                        modifier = Modifier.weight(1f),
                        label = "High / Low",
                        value = "${weather.maxTemp}° / ${weather.minTemp}°",
                        isDark = isDark,
                        g = g
                    )
                }

                // Hourly forecast list if present
                if (weather.hourlyForecast.isNotEmpty()) {
                    Text(
                        text = "HOURLY FORECAST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = a1,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        weather.hourlyForecast.forEach { hour ->
                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.7f))
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(hour.time, fontSize = 11.sp, color = g.tx2Color, fontWeight = FontWeight.Bold)
                                    Text("${hour.temp}°", fontSize = 14.sp, color = g.txColor, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherInfoChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    isDark: Boolean,
    g: com.orbit.browser.ui.theme.OBGlassTokens
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.65f))
            .border(1.dp, g.glassBorder2.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.5.sp, color = g.tx2Color, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, color = g.txColor, fontWeight = FontWeight.Bold)
        }
    }
}


