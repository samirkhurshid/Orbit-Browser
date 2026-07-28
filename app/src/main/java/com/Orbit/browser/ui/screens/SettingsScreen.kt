package com.orbit.browser.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import com.orbit.browser.ui.glass.frostedGlass
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.components.FrostedBackButton
import com.orbit.browser.ui.theme.LocalOBTheme
import com.orbit.browser.ui.theme.OBThemePreset

@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
) {
    val ui           by viewModel.ui.collectAsState()
    val theme        = LocalOBTheme.current
    val g            = theme.glass
    val isDark       = theme.isDark
    val a1           = theme.effectiveA1
    val a2           = theme.effectiveA2
    val context      = LocalContext.current
    var showAboutModal by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FrostedBackButton(
                    onClick = { viewModel.closeSettings() },
                    isDark  = isDark,
                )

                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    color = g.txColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                // Right-side Circular FrostedGlass Search Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .frostedGlass(isDark = isDark, shape = CircleShape, blurRadius = 24.dp)
                        .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.65f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = LocalIndication.current,
                            onClick           = { viewModel.openSearch() }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = g.txColor,
                            modifier = Modifier.size(20.dp)
                        )
                }
            }

            // Scrollable Content
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Profile Row (App.tsx line 1973)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(a1.copy(alpha = 0.15f), a2.copy(alpha = 0.10f))))
                            .border(1.dp, a1.copy(alpha = 0.30f), RoundedCornerShape(20.dp))
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Orbit User", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = g.txColor)
                                Text("orbit@example.com", fontSize = 11.sp, color = g.tx2Color, modifier = Modifier.padding(top = 2.dp))
                            }

                            Text("›", fontSize = 20.sp, color = g.tx3Color, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Section: General
                item {
                    SettingsGroupCard(title = "General", isDark = isDark) {
                        var openLinksInBg by remember { mutableStateOf(false) }

                        val engines = remember { listOf("Google", "Bing", "DuckDuckGo", "Ecosia") }
                        val currentEngine = ui.defaultSearchEngine

                        SettingsToggleRow(
                            label = "Default Browser", sub = "Set Orbit as default app", checked = true,
                            onCheckedChange = {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    viewModel.showToast("Open Android Settings to set default browser")
                                }
                            }
                        )
                        SettingsSelectRow(
                            label = "Search Engine", value = currentEngine,
                            onClick = {
                                val nextIndex = (engines.indexOf(currentEngine) + 1) % engines.size
                                viewModel.setSearchEngine(engines[nextIndex])
                            }
                        )
                        SettingsToggleRow(
                            label = "Default Desktop Mode", sub = "Request desktop sites by default",
                            checked = ui.isDesktopSite,
                            onCheckedChange = { viewModel.toggleDesktopSite() }
                        )
                        SettingsToggleRow(
                            label = "Open Links in Background", sub = "Open in new background tab",
                            checked = openLinksInBg,
                            onCheckedChange = { openLinksInBg = it }
                        )
                        SettingsSelectRow(label = "Language", value = "English (US)")
                    }
                }

                // Section: Privacy & Security
                item {
                    SettingsGroupCard(title = "Privacy & Security", isDark = isDark) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openPasswords() }
                                .padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Passwords", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = g.txColor)
                                Text("AES-256 encrypted, biometric-locked", fontSize = 10.5.sp, color = g.tx2Color, modifier = Modifier.padding(top = 1.dp))
                            }
                            Text("›", fontSize = 18.sp, color = g.tx3Color)
                        }
                        SettingsToggleRow(
                            label = "Block Trackers & Ads", sub = "Blocks 3rd-party trackers and ads",
                            checked = ui.showPrivacyDash,
                            onCheckedChange = { viewModel.setShowPrivacyDash(it) }
                        )
                        SettingsToggleRow(
                            label = "HTTPS-Only Mode", sub = "Warn before loading HTTP sites",
                            checked = ui.httpsOnly,
                            onCheckedChange = { viewModel.setHttpsOnly(it) }
                        )
                        SettingsToggleRow(
                            label = "Block 3rd-Party Cookies", sub = "Block tracking cookies across sites",
                            checked = ui.blockCookies,
                            onCheckedChange = { viewModel.setBlockCookies(it) }
                        )
                        SettingsToggleRow(
                            label = "DNS-over-HTTPS (DoH)", sub = "Secure encrypted DNS queries",
                            checked = ui.dohEnabled,
                            onCheckedChange = { viewModel.setDohEnabled(it) }
                        )
                        SettingsToggleRow(
                            label = "Clear Data on Exit", sub = "Automatically clear history when closed",
                            checked = ui.clearOnExit,
                            onCheckedChange = { viewModel.setClearOnExit(it) }
                        )
                        
                        // Clear data action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.clearBrowsingData() }
                                .padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Clear Browsing Data", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF4D6D))
                                Text("Cookies, cache, history", fontSize = 10.5.sp, color = g.tx2Color, modifier = Modifier.padding(top = 1.dp))
                            }
                            Text("›", fontSize = 18.sp, color = g.tx3Color)
                        }
                    }
                }

                // Section: Appearance & Customisation
                item {
                    SettingsGroupCard(title = "Appearance", isDark = isDark) {
                        var showFavicons by remember { mutableStateOf(true) }
                        val isDynamic = ui.theme == OBThemePreset.Dynamic

                        val darkModeLabels = remember { listOf("System Default", "Always Dark", "Always Light") }
                        val currentDarkLabel = when (ui.isDarkMode) {
                            true -> "Always Dark"
                            false -> "Always Light"
                            null -> "System Default"
                        }

                        SettingsToggleRow(
                            label = "Dynamic Theme", sub = "Adapts to time & weather",
                            checked = isDynamic,
                            onCheckedChange = {
                                viewModel.setTheme(if (it) OBThemePreset.Dynamic else OBThemePreset.PurpleAurora)
                            }
                        )
                        SettingsSelectRow(
                            label = "Dark Mode Preference", value = currentDarkLabel,
                            onClick = {
                                when (ui.isDarkMode) {
                                    null -> viewModel.setDarkMode(true)
                                    true -> viewModel.setDarkMode(false)
                                    false -> viewModel.setDarkMode(null)
                                }
                            }
                        )
                        val fontSizes = remember { listOf(80, 100, 120, 150) }
                        val currentTextSize = ui.textSizePercent
                        SettingsSelectRow(
                            label = "Text Size", value = "$currentTextSize%",
                            onClick = {
                                val nextIndex = (fontSizes.indexOf(currentTextSize).coerceAtLeast(0) + 1) % fontSizes.size
                                viewModel.setTextSize(fontSizes[nextIndex])
                            }
                        )
                        SettingsToggleRow(
                            label = "Show Quick Access", sub = "Quick shortcuts on Home Screen",
                            checked = ui.showQuickAccess,
                            onCheckedChange = { viewModel.setShowQuickAccess(it) }
                        )
                        SettingsToggleRow(
                            label = "Show Frequently Visited", sub = "Top sites on Home Screen",
                            checked = ui.showFreqVisited,
                            onCheckedChange = { viewModel.setShowFreqVisited(it) }
                        )
                        SettingsToggleRow(
                            label = "Show News Feed", sub = "Discover news on Home Screen",
                            checked = ui.showNewsFeed,
                            onCheckedChange = { viewModel.setShowNewsFeed(it) }
                        )
                        SettingsToggleRow(label = "Show Favicons", sub = "In tabs and bookmarks", checked = showFavicons, onCheckedChange = { showFavicons = it })
                    }
                }

                // Section: Advanced & Developer
                item {
                    SettingsGroupCard(title = "Advanced", isDark = isDark) {
                        var javaScript by remember { mutableStateOf(true) }
                        Text(
                            text = "v1.0.0 Public Beta",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = a1
                        )
                    }
                }

                // Technology & Developer Attribution Card
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutModal = true }
                            .padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("About Orbit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = g.txColor)
                            Text("Glassconn Tech · Built by Samir Khurshid", fontSize = 10.5.sp, color = g.tx2Color, modifier = Modifier.padding(top = 1.dp))
                        }
                        Text("›", fontSize = 18.sp, color = g.tx3Color)
                    }
                }
            }
        }

        if (showAboutModal) {
            AboutOrbitModal(
                theme = theme,
                onDismiss = { showAboutModal = false },
                onReRunSetup = {
                    showAboutModal = false
                    viewModel.reopenOnboarding()
                }
            )
        }
    }
}

@Composable
private fun AboutOrbitModal(
    theme: com.orbit.browser.ui.theme.OBThemeConfig,
    onDismiss: () -> Unit,
    onReRunSetup: () -> Unit
) {
    val isDark = theme.isDark
    val g = theme.glass
    val a1 = theme.effectiveA1
    val context = LocalContext.current

    val appIconBitmap: androidx.compose.ui.graphics.ImageBitmap? = remember(context) {
        try {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val w = drawable.intrinsicWidth.coerceAtLeast(128)
            val h = drawable.intrinsicHeight.coerceAtLeast(128)
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .frostedGlass(isDark = isDark, shape = RoundedCornerShape(28.dp), blurRadius = 36.dp)
                .background(
                    if (isDark) Color(0xFF0B0F1D).copy(alpha = 0.94f)
                    else Color(0xFFF8FAFC).copy(alpha = 0.96f)
                )
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header app icon badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(a1.copy(alpha = 0.15f))
                        .border(1.dp, a1.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap,
                            contentDescription = "Orbit App Icon",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        com.orbit.browser.ui.components.OrbitAppIcon(size = 52.dp, showGlassBackground = false)
                    }
                }

                // App Title & Version Tag
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Orbit Browser",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(a1.copy(alpha = 0.18f))
                            .border(1.dp, a1.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "v1.0.0 Public Beta",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = a1
                        )
                    }
                }

                HorizontalDivider(color = g.glassBorder2.copy(alpha = 0.4f))

                // Technology & Developer Attribution Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f))
                        .border(1.dp, g.glassBorder2.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Glassconn Tech",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = a1,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = "Engineered & Designed by Samir Khurshid",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Orbit Browser is built under Glassconn Tech, independently engineered by Samir Khurshid. Architected with dynamic ambient design language, hardware-backed biometric security, and zero telemetry tracking.",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = g.tx2Color,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Re-run Onboarding button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                        .clickable { onReRunSetup() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✨ Re-Run First-Time Setup",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = a1
                    )
                }

                // Copyright footer
                Text(
                    text = "© 2026 Glassconn Tech. Designed & Engineered by Samir Khurshid. All rights reserved.",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = g.tx3Color,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    isDark: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.3).sp,
            color = if (isDark) Color.White else Color.Black,
            modifier = Modifier.padding(top = 10.dp, bottom = 8.dp, start = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.6f))
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    sub: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val theme  = LocalOBTheme.current
    val g      = theme.glass
    val a1     = theme.effectiveA1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = g.txColor)
            Text(sub, fontSize = 10.5.sp, color = g.tx2Color, modifier = Modifier.padding(top = 1.dp))
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = a1,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = g.glassBg2
            )
        )
    }
}

@Composable
private fun SettingsSelectRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    val theme = LocalOBTheme.current
    val g     = theme.glass
    val a1    = theme.effectiveA1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = g.txColor, modifier = Modifier.weight(1f))

        Text(value, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = a1)
        Spacer(Modifier.width(4.dp))
        Text("›", fontSize = 18.sp, color = g.tx3Color)
    }
}
