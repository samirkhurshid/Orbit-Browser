package com.orbit.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.browser.ui.glass.frostedGlass
import com.orbit.browser.ui.theme.LocalOBTheme
import com.orbit.browser.ui.theme.OBThemePreset

@Composable
fun OnboardingScreen(
    onComplete: (themePreset: OBThemePreset, modePref: String, weatherEffectsEnabled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = LocalOBTheme.current
    val systemIsDark = theme.isDark
    val haptics = LocalHapticFeedback.current

    var currentStep by remember { mutableStateOf(0) }
    var selectedTheme by remember { mutableStateOf(OBThemePreset.Dynamic) }
    var selectedMode by remember { mutableStateOf("SYSTEM") }
    var weatherEffectsEnabled by remember { mutableStateOf(true) }
    var showLocationDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Auto-prompt location explanation as soon as user visits last setup page (step 3)
    LaunchedEffect(currentStep) {
        if (currentStep == 3) {
            showLocationDialog = true
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            weatherEffectsEnabled = true
            // Check if Location / GPS provider is enabled on device
            try {
                val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                val gpsEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true
                val netEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
                if (!gpsEnabled && !netEnabled) {
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            } catch (_: Exception) {}
        } else {
            weatherEffectsEnabled = false
        }
    }

    val isEffectiveDark = when (selectedMode) {
        "ALWAYS_DARK" -> true
        "ALWAYS_LIGHT" -> false
        else -> systemIsDark
    }

    // Gradual animated background gradient matching selected theme & mode
    val targetBgColors = remember(selectedTheme, isEffectiveDark) {
        when (selectedTheme) {
            OBThemePreset.Dynamic -> if (isEffectiveDark) listOf(Color(0xFF090D1A), Color(0xFF11172A), Color(0xFF070A14)) else listOf(Color(0xFFEBF3FF), Color(0xFFF8FAFF), Color(0xFFE2EDFF))
            OBThemePreset.BlueFrost -> if (isEffectiveDark) listOf(Color(0xFF070E26), Color(0xFF0F1B3E), Color(0xFF060918)) else listOf(Color(0xFFE6F0FF), Color(0xFFF0F5FF), Color(0xFFDDE9FF))
            OBThemePreset.PurpleAurora -> if (isEffectiveDark) listOf(Color(0xFF160626), Color(0xFF240B3B), Color(0xFF0D0317)) else listOf(Color(0xFFF6E8FF), Color(0xFFFAF0FF), Color(0xFFEDD8FF))
            OBThemePreset.OceanGlass -> if (isEffectiveDark) listOf(Color(0xFF031926), Color(0xFF072B3E), Color(0xFF021019)) else listOf(Color(0xFFE0F7FA), Color(0xFFF0FBFD), Color(0xFFCDEDF6))
            OBThemePreset.EmeraldCrystal -> if (isEffectiveDark) listOf(Color(0xFF031A14), Color(0xFF072C22), Color(0xFF02110D)) else listOf(Color(0xFFE6F9F3), Color(0xFFF2FBF7), Color(0xFFD3F4E9))
            OBThemePreset.SunsetGlow -> if (isEffectiveDark) listOf(Color(0xFF260D07), Color(0xFF3E170F), Color(0xFF170603)) else listOf(Color(0xFFFFF0E6), Color(0xFFFFF7F2), Color(0xFFFFE3D4))
        }
    }

    val animBg1 by animateColorAsState(targetValue = targetBgColors[0], animationSpec = tween(800, easing = FastOutSlowInEasing), label = "bg1")
    val animBg2 by animateColorAsState(targetValue = targetBgColors[1], animationSpec = tween(800, easing = FastOutSlowInEasing), label = "bg2")
    val animBg3 by animateColorAsState(targetValue = targetBgColors[2], animationSpec = tween(800, easing = FastOutSlowInEasing), label = "bg3")

    val bgBrush = Brush.verticalGradient(listOf(animBg1, animBg2, animBg3))

    // Gradual animated ambient orb glow matching selected theme
    val (targetOrb1, targetOrb2) = remember(selectedTheme) {
        when (selectedTheme) {
            OBThemePreset.Dynamic -> Pair(Color(0xFF3B82F6), Color(0xFF8B5CF6))
            OBThemePreset.BlueFrost -> Pair(Color(0xFF1A6FFF), Color(0xFF7C3AED))
            OBThemePreset.PurpleAurora -> Pair(Color(0xFF9333EA), Color(0xFFC026D3))
            OBThemePreset.OceanGlass -> Pair(Color(0xFF0891B2), Color(0xFF0E7490))
            OBThemePreset.EmeraldCrystal -> Pair(Color(0xFF059669), Color(0xFF047857))
            OBThemePreset.SunsetGlow -> Pair(Color(0xFFF97316), Color(0xFFDC2626))
        }
    }

    val animOrb1 by animateColorAsState(targetValue = targetOrb1, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "orb1")
    val animOrb2 by animateColorAsState(targetValue = targetOrb2, animationSpec = tween(800, easing = FastOutSlowInEasing), label = "orb2")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        // Floating ambient glowing background circles
        AmbientMotionBackground(orb1 = animOrb1, orb2 = animOrb2)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Bar — Skip / Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Step Indicator Pills
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { idx ->
                        val active = idx == currentStep
                        val width by animateDpAsState(
                            targetValue = if (active) 28.dp else 10.dp,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                            label = "dot_w"
                        )
                        val color = if (active) Color(0xFF3B82F6) else if (isEffectiveDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f)

                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                if (currentStep < 3) {
                    Text(
                        text = "Skip",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEffectiveDark) Color.White.copy(alpha = 0.7f) else Color(0xFF0F172A),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentStep = 3
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.weight(0.5f))

            // Main Graphic Motion Content Area
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) + slideInHorizontally { width -> width / 2 } togetherWith
                            fadeOut(animationSpec = tween(300)) + slideOutHorizontally { width -> -width / 2 }
                },
                label = "onboard_slide"
            ) { step ->
                when (step) {
                    0 -> OnboardingSlide1(isDark = isEffectiveDark)
                    1 -> OnboardingSlide2(isDark = isEffectiveDark)
                    2 -> OnboardingSlide3(isDark = isEffectiveDark)
                    3 -> SetupStepSlide(
                        selectedTheme = selectedTheme,
                        onThemeSelected = { selectedTheme = it },
                        selectedMode = selectedMode,
                        onModeSelected = { selectedMode = it },
                        weatherEffectsEnabled = weatherEffectsEnabled,
                        onWeatherEffectsToggled = { enabled ->
                            weatherEffectsEnabled = enabled
                            if (enabled) {
                                showLocationDialog = true
                            }
                        },
                        isDark = isEffectiveDark,
                    )
                }
            }

            Spacer(Modifier.weight(0.8f))

            // Bottom Navigation Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFEC4899))
                        )
                    )
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (currentStep < 3) {
                            currentStep++
                        } else {
                            onComplete(selectedTheme, selectedMode, weatherEffectsEnabled)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (currentStep == 3) "Launch Orbit Browser" else "Continue",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.3.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // High-Blur Frosted Glass Location Permission Card Dialog
        if (showLocationDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = {
                    showLocationDialog = false
                    weatherEffectsEnabled = false
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .frostedGlass(isDark = isEffectiveDark, shape = RoundedCornerShape(28.dp), blurRadius = 36.dp)
                        .background(
                            if (isEffectiveDark) Color(0xFF0D1226).copy(alpha = 0.92f)
                            else Color(0xFFF1F5F9).copy(alpha = 0.94f)
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Motion graphic high quality SVG glassmorphic location icon
                        GlassLocationIconMotion(isDark = isEffectiveDark)

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Enable Live Weather Effects",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isEffectiveDark) Color.White else Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Orbit uses location access to fetch real-time temperature, rain, and atmospheric conditions so your ambient glass theme adapts dynamically.",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isEffectiveDark) Color.White.copy(alpha = 0.78f) else Color(0xFF334155),
                                textAlign = TextAlign.Center,
                                lineHeight = 21.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel button -> automatically turns off Live Weather toggle
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isEffectiveDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                                    .clickable {
                                        showLocationDialog = false
                                        weatherEffectsEnabled = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEffectiveDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569)
                                )
                            }

                            // Agree button -> triggers native permission request & location check
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                                        )
                                    )
                                    .clickable {
                                        showLocationDialog = false
                                        permissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Agree & Allow",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Glassmorphic Motion Graphic Location Pin Icon ─────────────────────────────
@Composable
private fun GlassLocationIconMotion(isDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "location_pin")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Restart),
        label = "pulse"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Restart),
        label = "alpha"
    )

    val floatY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "float"
    )

    Box(
        modifier = Modifier
            .size(90.dp)
            .offset(y = floatY.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerPt = Offset(size.width / 2f, size.height / 2f + 16f)

            // Radar pulse ring 1
            drawCircle(
                color = Color(0xFF3B82F6).copy(alpha = pulseAlpha),
                radius = 36f * pulseScale,
                center = centerPt
            )

            // Ground reflection shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.25f),
                topLeft = Offset(size.width / 2f - 18f, size.height / 2f + 18f),
                size = androidx.compose.ui.geometry.Size(36f, 10f)
            )

            // Glass Pin Body Path
            val pinPath = androidx.compose.ui.graphics.Path().apply {
                val cx = size.width / 2f
                val topY = size.height / 2f - 24f
                val r = 20f
                val tipY = size.height / 2f + 18f

                moveTo(cx, tipY)
                cubicTo(
                    cx - r * 1.5f, topY + r * 1.5f,
                    cx - r, topY,
                    cx, topY
                )
                cubicTo(
                    cx + r, topY,
                    cx + r * 1.5f, topY + r * 1.5f,
                    cx, tipY
                )
                close()
            }

            // Glass Fill
            drawPath(
                path = pinPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF60A5FA).copy(alpha = 0.75f),
                        Color(0xFF3B82F6).copy(alpha = 0.45f),
                        Color(0xFF1D4ED8).copy(alpha = 0.85f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
            )

            // Glass Specular Highlight Border
            drawPath(
                path = pinPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        Color.White.copy(alpha = 0.2f),
                        Color(0xFF93C5FD).copy(alpha = 0.8f)
                    )
                ),
                style = Stroke(width = 2.5f)
            )

            // Inner Core Lens
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = 7.5f,
                center = Offset(size.width / 2f, size.height / 2f - 12f)
            )
        }
    }
}

// ── Motion Slide 1: Welcome to Orbit ─────────────────────────────────────────
@Composable
private fun OnboardingSlide1(isDark: Boolean) {
    val infinite = rememberInfiniteTransition(label = "orb_anim")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "rotation"
    )
    val pulseScale by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Graphic motion glowing 3D Orb
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().scale(pulseScale)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.35f), Color.Transparent),
                        center = center,
                        radius = size.width / 1.4f
                    )
                )
            }

            Canvas(modifier = Modifier.fillMaxSize().rotate(rotation)) {
                drawCircle(
                    color = Color(0xFF60A5FA).copy(alpha = 0.4f),
                    style = Stroke(width = 3f),
                    radius = size.width / 2.3f
                )
                drawCircle(
                    color = Color(0xFF8B5CF6),
                    radius = 8f,
                    center = Offset(size.width * 0.15f, size.height * 0.3f)
                )
                drawCircle(
                    color = Color(0xFFEC4899),
                    radius = 6f,
                    center = Offset(size.width * 0.85f, size.height * 0.7f)
                )
            }

            com.orbit.browser.ui.components.OrbitAppIcon(
                size = 110.dp,
                showGlassBackground = true
            )
        }

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .frostedGlass(isDark = isDark, shape = RoundedCornerShape(50.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "DYNAMIC GLASS ENGINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Welcome to Orbit",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = if (isDark) Color.White else Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Experience mobile browsing reimagined with dynamic ambient frosted glass, privacy guards, and real-time weather themes.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF1E293B),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

// ── Motion Slide 2: Ultra Private & Shielded ────────────────────────────────
@Composable
private fun OnboardingSlide2(isDark: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF10B981).copy(alpha = 0.30f), Color.Transparent),
                        center = center,
                        radius = size.width / 1.4f
                    )
                )
            }

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .frostedGlass(isDark = isDark, shape = CircleShape, blurRadius = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(60.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .frostedGlass(isDark = isDark, shape = RoundedCornerShape(50.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "HARDWARE PRIVACY SHIELD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Zero Trackers. Pure Speed.",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = if (isDark) Color.White else Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Orbit automatically blocks aggressive ads, anti-fingerprinting scripts, and telemetry trackers before pages load.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF1E293B),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

// ── Motion Slide 3: Dynamic Island Nav Bar ───────────────────────────────────
@Composable
private fun OnboardingSlide3(isDark: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.30f), Color.Transparent),
                        center = center,
                        radius = size.width / 1.4f
                    )
                )
            }

            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .frostedGlass(isDark = isDark, shape = RoundedCornerShape(30.dp), blurRadius = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF0F172A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Search or type URL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF334155)
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .frostedGlass(isDark = isDark, shape = RoundedCornerShape(50.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ViewCarousel,
                    contentDescription = null,
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "DYNAMIC ISLAND NAVBAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9),
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Fluid Controls at Fingertips",
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = if (isDark) Color.White else Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Our floating dynamic island navigation bar seamlessly expands and transforms, giving you instant access to search, tabs, and reader mode.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF1E293B),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

// ── Setup Step Slide: Theme & Appearance Selection ───────────────────────────
private data class ThemeOption(val preset: OBThemePreset, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private data class ModeOption(val key: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun SetupStepSlide(
    selectedTheme: OBThemePreset,
    onThemeSelected: (OBThemePreset) -> Unit,
    selectedMode: String,
    onModeSelected: (String) -> Unit,
    weatherEffectsEnabled: Boolean,
    onWeatherEffectsToggled: (Boolean) -> Unit,
    isDark: Boolean,
) {
    val themes = remember {
        listOf(
            ThemeOption(OBThemePreset.Dynamic, "Dynamic Ambient", Icons.Rounded.AutoAwesome),
            ThemeOption(OBThemePreset.BlueFrost, "Cosmic Glass", Icons.Rounded.WaterDrop),
            ThemeOption(OBThemePreset.PurpleAurora, "Purple Aurora", Icons.Rounded.AutoAwesome),
            ThemeOption(OBThemePreset.OceanGlass, "Ocean Glass", Icons.Rounded.Waves),
            ThemeOption(OBThemePreset.EmeraldCrystal, "Emerald Crystal", Icons.Rounded.Park),
            ThemeOption(OBThemePreset.SunsetGlow, "Sunset Glow", Icons.Rounded.WbSunny),
        )
    }

    val modes = remember {
        listOf(
            ModeOption("SYSTEM", "System Default", Icons.Rounded.SettingsBrightness),
            ModeOption("ALWAYS_DARK", "Dark Mode", Icons.Rounded.DarkMode),
            ModeOption("ALWAYS_LIGHT", "Light Mode", Icons.Rounded.LightMode),
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Personalize Your Orbit",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = if (isDark) Color.White else Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Select your preferred frosted glass theme and atmospheric options.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF1E293B),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        // Live Weather Effects Toggle Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (weatherEffectsEnabled) Color(0xFF3B82F6).copy(alpha = 0.18f) else if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.60f))
                .border(
                    width = if (weatherEffectsEnabled) 1.5.dp else 0.5.dp,
                    color = if (weatherEffectsEnabled) Color(0xFF3B82F6) else if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { onWeatherEffectsToggled(!weatherEffectsEnabled) }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WbSunny,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Live Weather Effects",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "Animates real-time rain, snow, clouds & sun reflection directly over glass elements using local GPS coordinates.",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF475569),
                            lineHeight = 15.sp
                        )
                    }
                }

                androidx.compose.material3.Switch(
                    checked = weatherEffectsEnabled,
                    onCheckedChange = { onWeatherEffectsToggled(it) },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF3B82F6)
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Theme Preset Selector Header
        Text(
            text = "CHOOSING THEME PRESET",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(8.dp))

        // Theme presets list
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            themes.forEach { item ->
                val isSelected = selectedTheme == item.preset
                val isDynamic = item.preset == OBThemePreset.Dynamic

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isSelected) (if (isDynamic) Color(0xFF3B82F6).copy(alpha = 0.28f) else Color(0xFF3B82F6).copy(alpha = 0.22f))
                            else if (isDark) Color.White.copy(alpha = 0.05f)
                            else Color.White.copy(alpha = 0.60f)
                        )
                        .border(
                            width = if (isSelected) (if (isDynamic) 2.dp else 1.5.dp) else 0.5.dp,
                            color = if (isSelected) Color(0xFF3B82F6) else if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onThemeSelected(item.preset) }
                        .padding(horizontal = 16.dp, vertical = if (isDynamic) 12.dp else 10.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF2563EB) else if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF1E293B),
                                    modifier = Modifier.size(if (isDynamic) 22.dp else 18.dp)
                                )
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = item.label,
                                            fontSize = if (isDynamic) 15.sp else 14.sp,
                                            fontWeight = if (isSelected || isDynamic) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isDark) Color.White else Color(0xFF0F172A)
                                        )
                                        if (isDynamic) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF2563EB).copy(alpha = 0.20f))
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "RECOMMENDED",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(if (isDynamic) 22.dp else 18.dp)
                                )
                            }
                        }

                        // Brief explanation for Dynamic Ambient
                        if (isDynamic) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Adaptively shifts glass colors & mesh background in real-time based on your local live weather and time of day.",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF334155),
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(start = 32.dp, end = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Appearance Mode Selector Header
        Text(
            text = "APPEARANCE MODE",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDark) Color(0xFFA78BFA) else Color(0xFF6D28D9),
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            modes.forEach { item ->
                val isSelected = selectedMode == item.key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.28f)
                            else if (isDark) Color.White.copy(alpha = 0.05f)
                            else Color.White.copy(alpha = 0.60f)
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) Color(0xFF7C3AED) else if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onModeSelected(item.key) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF7C3AED) else if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF1E293B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = item.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ── Background Particle Glow Animation ───────────────────────────────────────
@Composable
private fun AmbientMotionBackground(orb1: Color, orb2: Color) {
    val infinite = rememberInfiniteTransition(label = "bg_glow")
    val alpha1 by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "glow_a1"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(orb1.copy(alpha = alpha1), Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.25f),
                radius = size.width * 0.85f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(orb2.copy(alpha = alpha1 * 0.85f), Color.Transparent),
                center = Offset(size.width * 0.8f, size.height * 0.7f),
                radius = size.width * 0.85f
            )
        )
    }
}
