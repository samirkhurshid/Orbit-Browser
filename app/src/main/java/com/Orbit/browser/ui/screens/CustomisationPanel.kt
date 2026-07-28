package com.orbit.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.WeatherKind
import com.orbit.browser.ui.WeatherState
import com.orbit.browser.ui.components.OBToggle
import com.orbit.browser.ui.theme.LocalOBTheme
import com.orbit.browser.ui.theme.OBThemePreset
import com.orbit.browser.ui.glass.frostedGlass
import java.util.Calendar

// ═══════════════════════════════════════════════════════════════════════════
// CUSTOMISATION PANEL — exact port of App.tsx lines 1450–1528
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun CustomisationPanel(viewModel: BrowserViewModel, visible: Boolean) {
    val theme = LocalOBTheme.current
    val g     = theme.glass
    val isDark = theme.isDark

    val panelBg = if (isDark) Color(0xFF0C0E1A).copy(alpha = 0.98f) else Color(0xFFF5F7FF).copy(alpha = 0.98f)
    val border  = g.glassBorder2

    val figmaSpring = com.orbit.browser.ui.animations.OBEasing.FigmaSpring
    val figmaEase   = com.orbit.browser.ui.animations.OBEasing.FigmaEase

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(260, easing = figmaEase)) + slideInVertically(initialOffsetY = { -24 }, animationSpec = tween(340, easing = figmaSpring)),
        exit    = fadeOut(tween(220, easing = figmaEase)) + slideOutVertically(targetOffsetY = { -24 }, animationSpec = tween(260, easing = figmaEase)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) { viewModel.closeCustom() },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .frostedGlass(isDark = isDark, shape = RoundedCornerShape(28.dp), blurRadius = 36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) {},
            ) {
                Column {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text       = "✦ Customise Orbit",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = g.txColor,
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { viewModel.closeCustom() }) {
                            Icon(
                                imageVector        = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint               = g.tx2Color,
                            )
                        }
                    }
                    HorizontalDivider(color = border, thickness = 1.dp)

                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        item { ThemeSection(viewModel = viewModel) }
                        item { CurrentConditionsSection(viewModel = viewModel) }
                        item { VisibilitySection(viewModel = viewModel) }
                        item { CardManagerSection(viewModel = viewModel) }
                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSection(viewModel: BrowserViewModel) {
    val ui by viewModel.ui.collectAsState()
    val theme = LocalOBTheme.current
    val g = theme.glass
    val context = androidx.compose.ui.platform.LocalContext.current

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                          permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                viewModel.fetchLiveWeather(context)
            }
        }
    )

    Column {
        SectionHeader("Theme")
        Spacer(Modifier.height(12.dp))
        OBThemePreset.entries.chunked(3).forEach { row ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { preset ->
                    ThemeSwatch(
                        preset   = preset,
                        selected = ui.theme == preset,
                        onClick  = { viewModel.setTheme(preset) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(g.glassBg2)
                .border(1.dp, g.glassBorder2, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🌤️", fontSize = 16.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Weather Effects",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = g.txColor
                )
                Text(
                    text = "Live condition particles & dynamic theme",
                    fontSize = 10.5.sp,
                    color = g.tx2Color
                )
            }
            OBToggle(
                checked = ui.showWeatherEffects,
                onCheckedChange = { enabled ->
                    viewModel.toggleWeatherEffects(context, enabled)
                    if (enabled) {
                        launcher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ThemeSwatch(
    preset:   OBThemePreset,
    selected: Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue   = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
        label         = "swatch_scale_${preset.name}",
    )
    val borderColor by animateColorAsState(
        targetValue   = if (selected) Color.White.copy(alpha = 0.7f) else Color.Transparent,
        animationSpec = tween(200),
        label         = "swatch_border_${preset.name}",
    )

    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(preset.staticA1, preset.staticA2)))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            )
            .scale(scale),
        contentAlignment = Alignment.BottomCenter,
    ) {
        if (selected) {
            Text(
                text       = "✓",
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 18.sp,
                modifier   = Modifier.align(Alignment.Center),
            )
        }
        Text(
            text       = preset.displayName,
            fontSize   = 9.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White.copy(alpha = 0.8f),
            modifier   = Modifier.padding(bottom = 5.dp),
        )
    }
}

@Composable
private fun VisibilitySection(viewModel: BrowserViewModel) {
    val ui    by viewModel.ui.collectAsState()
    val theme = LocalOBTheme.current
    val g     = theme.glass
    val border = g.glassBorder2

    Column {
        SectionHeader("Home Screen Sections")
        val items = listOf(
            Triple("⚡", "Quick Access", ui.showQuickAccess),
            Triple("🛡️", "Privacy Dashboard", ui.showPrivacyDash),
            Triple("🔄", "Frequently Visited", ui.showFreqVisited),
            Triple("📡", "News Feed", ui.showNewsFeed),
        )
        items.forEachIndexed { i, (emoji, label, value) ->
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(emoji, fontSize = 20.sp, modifier = Modifier.width(26.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = label,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = g.txColor,
                    )
                    Text(
                        text = when (i) {
                            0 -> "Favourite site shortcuts"
                            1 -> "Tracker and ad stats"
                            2 -> "Most-visited sites"
                            else -> "Always at bottom • infinite scroll"
                        },
                        fontSize = 11.sp,
                        color    = g.tx2Color,
                    )
                }
                OBToggle(
                    checked         = value,
                    onCheckedChange = { v ->
                        when (i) {
                            0 -> viewModel.setShowQuickAccess(v)
                            1 -> viewModel.setShowPrivacyDash(v)
                            2 -> viewModel.setShowFreqVisited(v)
                            3 -> viewModel.setShowNewsFeed(v)
                        }
                    },
                )
            }
            if (i < items.size - 1) {
                HorizontalDivider(
                    modifier  = Modifier.padding(horizontal = 18.dp),
                    color     = border,
                    thickness = 0.5.dp,
                )
            }
        }
    }
}

data class CardInfo(val title: String, val emoji: String, val visible: Boolean)

@Composable
private fun CardManagerSection(viewModel: BrowserViewModel) {
    val ui    by viewModel.ui.collectAsState()
    val theme = LocalOBTheme.current
    val g     = theme.glass

    Column {
        SectionHeader("Card Order & Size")
        val visibleCards = listOf(
            CardInfo("Quick Access", "⚡", ui.showQuickAccess),
            CardInfo("Privacy Dashboard", "🛡️", ui.showPrivacyDash),
            CardInfo("Frequently Visited", "🔄", ui.showFreqVisited),
        ).filter { it.visible }

        if (visibleCards.isEmpty()) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text     = "Enable sections above to manage cards",
                    fontSize = 12.sp,
                    color    = g.tx3Color,
                )
            }
            return@Column
        }
        visibleCards.forEach { card ->
            CardManagerRow(
                card     = card,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun CardManagerRow(card: CardInfo, modifier: Modifier = Modifier) {
    val theme  = LocalOBTheme.current
    val g      = theme.glass
    val isDark = theme.isDark
    val a1     = theme.effectiveA1
    val border = g.glassBorder2

    var state by remember { mutableStateOf("Full") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(g.glassBg)
            .border(1.dp, border, RoundedCornerShape(18.dp)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("⠿", color = g.tx3Color, fontSize = 18.sp)
                Text(card.emoji, fontSize = 18.sp)
                Text(
                    text     = card.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color    = g.txColor,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Full", "Compact").forEach { s ->
                        val active = state == s
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (active) a1.copy(alpha = 0.18f)
                                    else g.glassBg2
                                )
                                .border(
                                    1.dp,
                                    if (active) a1.copy(alpha = 0.35f)
                                    else border,
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null,
                                ) { state = s }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text       = s,
                                fontSize   = 11.sp,
                                color      = if (active) a1 else g.txColor,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            MiniCardPreview(card = card, g = g, border = border)
        }
    }
}

@Composable
private fun MiniCardPreview(card: CardInfo, g: com.orbit.browser.ui.theme.OBGlassTokens, border: Color) {
    when (card.title) {
        "Quick Access" -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("🔍", "🐙", "▶️", "📧").forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(g.glassBg2)
                        .border(0.5.dp, border, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, fontSize = 14.sp)
                }
            }
        }
        "Privacy Dashboard" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("284" to "Blocked", "47" to "Sites", "99%" to "Privacy").forEach { (num, lbl) ->
                Column(
                    modifier            = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(g.glassBg2)
                        .border(0.5.dp, border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(num, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00DDA0))
                    Text(lbl, fontSize = 8.sp, color = g.tx2Color)
                }
            }
        }
        "Frequently Visited" -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("🐙", "▶️", "🗺️", "🛒").forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(g.glassBg2)
                        .border(0.5.dp, border, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun CurrentConditionsSection(viewModel: BrowserViewModel) {
    val ui      by viewModel.ui.collectAsState()
    val weather = ui.weatherState
    val theme   = LocalOBTheme.current
    val g       = theme.glass
    val border  = g.glassBorder2

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val timeSlot = when (hour) {
        in 5..6   -> "dawn"
        in 7..11  -> "morning"
        in 12..13 -> "noon"
        in 14..17 -> "afternoon"
        in 18..20 -> "evening"
        else      -> "night"
    }
    val greetText = when (timeSlot) {
        "dawn"      -> "Good dawn"
        "morning"   -> "Good morning"
        "noon"      -> "Good noon"
        "afternoon" -> "Good afternoon"
        "evening"   -> "Good evening"
        else        -> "Good night"
    }
    val greetEmoji = when (timeSlot) {
        "dawn"      -> "🌅"
        "morning"   -> "☀️"
        "noon"      -> "🌞"
        "afternoon" -> "⛅"
        "evening"   -> "🌇"
        else        -> "🌙"
    }

    val weatherIcon = when (weather.kind) {
        WeatherKind.Clear        -> "☀️"
        WeatherKind.Cloudy       -> "☁️"
        WeatherKind.Fog          -> "🌫️"
        WeatherKind.Drizzle      -> "🌦️"
        WeatherKind.Rain         -> "🌧️"
        WeatherKind.Thunderstorm -> "⛈️"
        WeatherKind.Snow         -> "❄️"
    }
    val weatherLabel = when (weather.kind) {
        WeatherKind.Clear        -> "Clear"
        WeatherKind.Cloudy       -> "Cloudy"
        WeatherKind.Fog          -> "Foggy"
        WeatherKind.Drizzle      -> "Drizzle"
        WeatherKind.Rain         -> "Rainy"
        WeatherKind.Thunderstorm -> "Thunderstorm"
        WeatherKind.Snow         -> "Snowing"
    }

    Column {
        SectionHeader("Current Conditions")
        Spacer(Modifier.height(12.dp))
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(g.glassBg)
                    .border(1.dp, border, RoundedCornerShape(16.dp)),
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(greetEmoji, fontSize = 24.sp)
                    Column {
                        Text(
                            text       = timeSlot.replaceFirstChar { it.uppercase() },
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = g.txColor,
                        )
                        Text(
                            text     = greetText,
                            fontSize = 10.sp,
                            color    = g.tx2Color,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(62.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(g.glassBg)
                    .border(1.dp, border, RoundedCornerShape(16.dp)),
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(weatherIcon, fontSize = 24.sp)
                    Column {
                        Text(
                            text       = weatherLabel,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = g.txColor,
                        )
                        Text(
                            text     = if (weather.temp != null) "${weather.temp}°C" else "—",
                            fontSize = 10.sp,
                            color    = g.tx2Color,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val theme = LocalOBTheme.current
    val g     = theme.glass
    val border = g.glassBorder2

    Column {
        HorizontalDivider(color = border, thickness = 0.5.dp)
        Text(
            text          = title.uppercase(),
            fontSize      = 10.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = g.tx2Color,
            letterSpacing = 0.8.sp,
            modifier      = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}
