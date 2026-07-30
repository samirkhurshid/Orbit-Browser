package com.orbit.browser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.browser.browser.tabs.OBTab
import com.orbit.browser.ui.BrowserScreen
import com.orbit.browser.ui.BrowserUiState
import com.orbit.browser.ui.theme.LocalOBTheme
import com.orbit.browser.ui.glass.frostedGlass
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

import com.orbit.browser.ui.TabMode

// ─── Island states ───────────────────────────────────────────────────────────
enum class IslandState { Default, Address, TabsOpen }

// ═══════════════════════════════════════════════════════════════════════════
// ISLAND NAV BAR  — exact port of App.tsx lines 1530–1624
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun OBIslandNavBar(
    uiState:       BrowserUiState,
    activeTab:     OBTab?,
    tabCount:      Int,
    islandState:   IslandState,
    tabMode:       TabMode,
    onTabModeChanged: (TabMode) -> Unit,
    onIslandClick: () -> Unit,
    onBack:        () -> Unit,
    onForward:     () -> Unit,
    onHome:        () -> Unit,
    onTabs:        () -> Unit,
    onMenu:        () -> Unit,
    modifier:      Modifier = Modifier,
) {
    val theme   = LocalOBTheme.current
    val g       = theme.glass
    val haptics = LocalHapticFeedback.current

    val isDark  = theme.isDark
    val a1      = theme.effectiveA1
    val a2      = theme.effectiveA2

    var isCollapsed by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val backArrowRotation by animateFloatAsState(
        targetValue   = if (isCollapsed) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
        label         = "back_arrow_rotation"
    )

    // ── Animated island dimensions ─────────────────────────────────────────
    val targetWidth = if (isCollapsed) 52.dp else when (islandState) {
        IslandState.TabsOpen -> 300.dp
        else                 -> 326.dp
    }
    val targetHeight = if (isCollapsed) 52.dp else when (islandState) {
        IslandState.TabsOpen -> 52.dp
        else                 -> 60.dp
    }
    val targetRadius = if (isCollapsed) 50.dp else 36.dp

    val iosSpringDp = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.85f, stiffness = 300f)

    val animWidth  by animateDpAsState(targetWidth,  iosSpringDp, label = "island_w")
    val animHeight by animateDpAsState(targetHeight, iosSpringDp, label = "island_h")
    val animRadius by animateDpAsState(targetRadius, iosSpringDp, label = "island_r")

    // ── Background: islandBg token ──────────────────────────────────────────
    val islandBg = g.islandBg   // dark: rgba(8,10,22,0.55)  light: rgba(255,255,255,0.42)
    val border   = g.glassBorder2

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(isCollapsed) {
                detectDragGestures { change, dragAmount ->
                    if (isCollapsed) {
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    } else if (dragAmount.y > 15f) {
                        isCollapsed = true
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // ── Main island pill ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .width(animWidth)
                .height(animHeight)
                .frostedGlass(isDark = isDark, shape = RoundedCornerShape(animRadius), blurRadius = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (isCollapsed) {
                        isCollapsed = false
                        offsetX = 0f
                        offsetY = 0f
                    } else {
                        onIslandClick()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isCollapsed) {
                // Collapsed state: Shows only the upward-pointing rotated back arrow!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = backArrowRotation),
                    contentAlignment = Alignment.Center
                ) {
                    ChevronLeftIcon(color = g.txColor)
                }
            } else {
                when (islandState) {
                    // ──────────────────────────────────────────────────────────
                    // TABS OPEN  → segmented normal/incognito switcher
                    // App.tsx lines 1549–1589
                    // ──────────────────────────────────────────────────────────
                    IslandState.TabsOpen -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                        ) {
                            // Sliding indicator pill
                            val indicatorOffset by animateDpAsState(
                                targetValue   = if (tabMode == TabMode.Private) (animWidth / 2 - 8.dp) else 0.dp,
                                animationSpec = tween(280, easing = com.orbit.browser.ui.animations.OBEasing.IosCurve),
                                label         = "tab_indicator",
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.5f)
                                    .offset(x = indicatorOffset)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(
                                        when {
                                            tabMode == TabMode.Private ->
                                                Color(0xFFC084FC).copy(alpha = 0.22f)
                                            isDark ->
                                                Color.White.copy(alpha = 0.14f)
                                            else ->
                                                Color.White.copy(alpha = 0.88f)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        when {
                                            tabMode == TabMode.Private -> Color(0xFFC084FC).copy(alpha = 0.30f)
                                            isDark -> Color.White.copy(alpha = 0.18f)
                                            else   -> Color.White.copy(alpha = 0.95f)
                                        },
                                        RoundedCornerShape(50.dp),
                                    ),
                            )

                            // Normal / Incognito buttons
                            Row(modifier = Modifier.fillMaxSize()) {
                                listOf(TabMode.Normal, TabMode.Private).forEach { mode ->
                                    val isActive = tabMode == mode
                                    val textColor by animateColorAsState(
                                        targetValue = when {
                                            isActive && mode == TabMode.Private -> Color(0xFFC084FC)
                                            isActive -> g.txColor
                                            else     -> g.tx3Color
                                        },
                                        label = "tab_color_${mode.name}",
                                    )
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication        = null,
                                                onClick           = { onTabModeChanged(mode) },
                                            ),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        if (mode == TabMode.Normal) {
                                            Box(
                                                modifier = Modifier
                                                    .size(17.dp)
                                                    .border(2.dp, textColor, RoundedCornerShape(5.dp)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text       = "$tabCount",
                                                    fontSize   = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color      = textColor,
                                                )
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text       = "Tabs",
                                                fontSize   = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = (-0.2).sp,
                                                color      = textColor,
                                            )
                                        } else {
                                            // Shield SVG for incognito
                                            ShieldIcon(color = textColor, size = 14.dp)
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text       = "Incognito",
                                                fontSize   = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = (-0.2).sp,
                                                color      = textColor,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ──────────────────────────────────────────────────────────
                    // DEFAULT & ADDRESS  → 5 nav buttons
                    // App.tsx lines 1591–1622
                    // ──────────────────────────────────────────────────────────
                    else -> {
                        val isHome        = uiState.screen == BrowserScreen.Home
                        val hasActivePage = activeTab?.url?.isNotBlank() == true && activeTab?.url != "orbit://home"
                        val canGoBack     = if (isHome) false else true
                        val canGoForward  = if (isHome) hasActivePage else (activeTab?.canGoForward == true)

                        Row(
                            modifier              = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            // Back — chevron left SVG
                            NavButton(
                                color    = if (canGoBack) g.txColor else g.txColor.copy(alpha = 0.32f),
                                modifier = Modifier.weight(1f),
                                onClick  = onBack,
                            ) {
                                ChevronLeftIcon(color = if (canGoBack) g.txColor else g.txColor.copy(alpha = 0.32f))
                            }

                            // Forward — chevron right SVG
                            NavButton(
                                color    = if (canGoForward) g.txColor else g.txColor.copy(alpha = 0.32f),
                                modifier = Modifier.weight(1f),
                                onClick  = onForward,
                            ) {
                                ChevronRightIcon(color = if (canGoForward) g.txColor else g.txColor.copy(alpha = 0.32f))
                            }

                            // Home — solid house SVG path (App.tsx line 1603–1605)
                            NavButton(
                                color    = if (isHome) a1 else g.txColor,
                                modifier = Modifier.weight(1f),
                                onClick  = onHome,
                            ) {
                                HomeIcon(color = if (isHome) a1 else g.txColor)
                            }

                            // Tabs — rounded rect + count text (App.tsx line 1609–1612)
                            NavButton(
                                color    = g.txColor,
                                modifier = Modifier.weight(1f),
                                onClick  = onTabs,
                            ) {
                                TabsIcon(tabCount = tabCount, color = g.txColor)
                            }

                            // Menu — 3 dots (App.tsx line 1615–1621)
                            NavButton(
                                color    = g.txColor,
                                modifier = Modifier.weight(1f),
                                onClick  = onMenu,
                            ) {
                                DotsMenuIcon(color = g.txColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// NAV BUTTON  — 44dp hit target, spring press scale, NavBtn in App.tsx
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun NavButton(
    color:    Color,
    modifier: Modifier = Modifier,
    onClick:  () -> Unit,
    content:  @Composable () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.78f else 1f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 500f),
        label         = "nav_scale",
    )
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .height(44.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SVG ICONS  — exact paths from App.tsx
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ChevronLeftIcon(color: Color) {
    // App.tsx line 1594: <polyline points="15 18 9 12 15 6"/>  w=22
    androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
        val sw = 2.2f * density
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
private fun ChevronRightIcon(color: Color) {
    // App.tsx line 1598: <polyline points="9 18 15 12 9 6"/>
    androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
        val sw = 2.2f * density
        val p = Path().apply {
            moveTo(9f / 24f * size.width,  18f / 24f * size.height)
            lineTo(15f / 24f * size.width, 12f / 24f * size.height)
            lineTo(9f / 24f * size.width,   6f / 24f * size.height)
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
private fun HomeIcon(color: Color) {
    // App.tsx line 1604: solid house path M3 20C3 21.1 3.9 22 5 22H9V17C9 15.3...
    // 26×26 canvas
    androidx.compose.foundation.Canvas(modifier = Modifier.size(26.dp)) {
        val p = Path().apply {
            // Scaled from 24px viewBox to size.width/height
            val sx = size.width  / 24f
            val sy = size.height / 24f
            // M3 20C3 21.1 3.9 22 5 22H9V17C9 15.3 10.3 14 12 14C13.7 14 15 15.3 15 17V22H19C20.1 22 21 21.1 21 20V11.5C21 10.6 20.6 9.8 20 9.2L14.5 4.4Q12 2.2 9.5 4.4L4 9.2C3.4 9.8 3 10.6 3 11.5Z
            moveTo(3 * sx, 20 * sy)
            cubicTo(3 * sx, 21.1f * sy, 3.9f * sx, 22 * sy, 5 * sx, 22 * sy)
            lineTo(9 * sx, 22 * sy)
            lineTo(9 * sx, 17 * sy)
            cubicTo(9 * sx, 15.3f * sy, 10.3f * sx, 14 * sy, 12 * sx, 14 * sy)
            cubicTo(13.7f * sx, 14 * sy, 15 * sx, 15.3f * sy, 15 * sx, 17 * sy)
            lineTo(15 * sx, 22 * sy)
            lineTo(19 * sx, 22 * sy)
            cubicTo(20.1f * sx, 22 * sy, 21 * sx, 21.1f * sy, 21 * sx, 20 * sy)
            lineTo(21 * sx, 11.5f * sy)
            cubicTo(21 * sx, 10.6f * sy, 20.6f * sx, 9.8f * sy, 20 * sx, 9.2f * sy)
            lineTo(14.5f * sx, 4.4f * sy)
            // Q bezier for peak: Q12 2.2 9.5 4.4
            quadraticTo(12 * sx, 2.2f * sy, 9.5f * sx, 4.4f * sy)
            lineTo(4 * sx, 9.2f * sy)
            cubicTo(3.4f * sx, 9.8f * sy, 3 * sx, 10.6f * sy, 3 * sx, 11.5f * sy)
            close()
        }
        drawPath(path = p, color = color, style = Fill)
    }
}

@Composable
private fun TabsIcon(tabCount: Int, color: Color) {
    // App.tsx line 1609–1612: rounded rect 3,3,18,18 rx4 + text count in center
    Box(
        modifier = Modifier.size(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val sw = 2.2f * density
            val sx = size.width  / 24f
            val sy = size.height / 24f
            drawRoundRect(
                color        = color,
                topLeft      = androidx.compose.ui.geometry.Offset(3 * sx, 3 * sy),
                size         = androidx.compose.ui.geometry.Size(18 * sx, 18 * sy),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4 * sx),
                style        = Stroke(width = sw),
            )
        }
        Text(
            text       = "$tabCount",
            fontSize   = 8.sp,
            fontWeight = FontWeight.Black,
            color      = color,
        )
    }
}

@Composable
private fun DotsMenuIcon(color: Color) {
    // App.tsx line 1616–1620: 3 filled circles at cx=12, cy=5,12,19 r=1.5
    androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
        val r  = 1.5f * density
        val cx = size.width / 2f
        listOf(5f, 12f, 19f).forEach { cy ->
            drawCircle(
                color  = color,
                radius = r,
                center = androidx.compose.ui.geometry.Offset(cx, size.height * cy / 24f),
            )
        }
    }
}

@Composable
private fun ShieldIcon(color: Color, size: androidx.compose.ui.unit.Dp) {
    // App.tsx line 1582–1584: <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val sw = 2.2f * density
        val sx = this.size.width  / 24f
        val sy = this.size.height / 24f
        val p = Path().apply {
            moveTo(12 * sx, 22 * sy)
            cubicTo(12 * sx, 22 * sy, 20 * sx, 18 * sy, 20 * sx, 12 * sy)
            lineTo(20 * sx, 5 * sy)
            lineTo(12 * sx, 2 * sy)
            lineTo(4 * sx,  5 * sy)
            lineTo(4 * sx,  12 * sy)
            cubicTo(4 * sx, 18 * sy, 12 * sx, 22 * sy, 12 * sx, 22 * sy)
            close()
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
