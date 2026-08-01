package com.orbit.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.browser.data.db.FrequentSite
import com.orbit.browser.data.db.QuickAccessSite
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.WeatherKind
import com.orbit.browser.ui.WeatherState
import com.orbit.browser.ui.theme.LocalOBTheme
import com.orbit.browser.ui.glass.frostedGlass
import com.orbit.browser.ui.theme.GREET_EMOJI
import com.orbit.browser.ui.theme.GREET_TEXT
import com.orbit.browser.ui.theme.WEATHER_ICON
import com.orbit.browser.ui.theme.WEATHER_LABEL
import com.orbit.browser.ui.theme.OBThemeConfig
import com.orbit.browser.ui.theme.getTimeSlot
import com.orbit.browser.ui.components.WeatherDetailModal
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.util.Calendar

// ═══════════════════════════════════════════════════════════════════════════
// HOME SCREEN  — exact port of App.tsx home section
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun HomeScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    val ui            by viewModel.ui.collectAsState()
    val quickAccess   by viewModel.quickAccessSites.collectAsState()
    val frequentSites by viewModel.frequentSites.collectAsState()
    val theme         = LocalOBTheme.current
    var showAddDialog by remember { mutableStateOf(false) }

    // ── Live clock (updates every 15s, same as App.tsx useClock) ─────────
    var clockStr by remember { mutableStateOf(currentClockString()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            clockStr = currentClockString()
        }
    }

    // ── Entrance fade (matches App.tsx visible animation) ─────────────────
    var visible by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        visible = true
        viewModel.fetchLiveWeather(context)
        viewModel.checkDefaultBrowserPrompt(context)
    }
    LaunchedEffect(ui.showWeatherEffects) {
        if (ui.showWeatherEffects) {
            viewModel.fetchLiveWeather(context)
        }
    }

    val localView = androidx.compose.ui.platform.LocalView.current
    val activeTabId by viewModel.tabManager.activeTabId.collectAsState()

    LaunchedEffect(activeTabId, ui.theme, ui.weatherState, ui.manualWeather, quickAccess) {
        if (activeTabId.isNotBlank() && localView.width > 0 && localView.height > 0) {
            val provider = com.orbit.browser.browser.preview.ComposePreviewProvider(localView, "HomeScreen")
            viewModel.previewManager.requestPreview(
                tabId = activeTabId,
                provider = provider,
                policy = com.orbit.browser.browser.preview.SchedulePolicy.Debounced(300L)
            )
        }
    }

    val entranceAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label         = "home_entrance",
    )

    val shrinkTopPadding = (14.dp - (scrollState.value / 4f).dp).coerceAtLeast(0.dp)
    val shrinkTopSpacer  = (14.dp - (scrollState.value / 4f).dp).coerceAtLeast(0.dp)

    Box(modifier = modifier.fillMaxSize()) {

        // ── Scrollable content ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(entranceAlpha)
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(top = shrinkTopPadding, bottom = 110.dp),
        ) {
            // ── Greeting row + title (App.tsx lines 655–698) ──────────────
            Spacer(Modifier.height(shrinkTopSpacer))
            GreetingSection(
                weather          = ui.weatherState,
                theme            = theme,
                onCustomizeClick = { viewModel.openCustom() },
                onWeatherClick   = { viewModel.setWeatherDetailOpen(true) },
            )
            Spacer(Modifier.height(22.dp))

            // ── Search bar (App.tsx lines 719–746) ────────────────────────
            HomeSearchBar(theme = theme, onClick = { viewModel.openSearch() })
            Spacer(Modifier.height(18.dp))

            // ── Quick Access card (App.tsx lines 749–787) ─────────────────
            if (ui.showQuickAccess) {
                QuickAccessCard(
                    theme       = theme,
                    sites       = quickAccess,
                    onSiteClick = { url -> viewModel.navigate(url) },
                    onAddClick  = { showAddDialog = true },
                )
                Spacer(Modifier.height(14.dp))
            }

            // ── Privacy Shield card (App.tsx lines 790–827) ───────────────
            if (ui.showPrivacyDash) {
                PrivacyShieldCard(
                    theme           = theme,
                    trackersBlocked = ui.trackersBlocked,
                )
                Spacer(Modifier.height(14.dp))
            }

            // ── Frequently Visited card (App.tsx lines 830–864) ───────────
            if (ui.showFreqVisited) {
                FrequentlyVisitedCard(
                    theme       = theme,
                    sites       = frequentSites,
                    onSiteClick = { url -> viewModel.navigate(url) },
                )
                Spacer(Modifier.height(14.dp))
            }

            // ── Top 4 News Articles + News Hub Link ─────────────────────
            val newsArticles by viewModel.newsArticles.collectAsState()

            if (ui.showNewsFeed && newsArticles.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text          = "📡 Top Stories",
                        fontSize      = 14.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp,
                        color         = theme.glass.txColor,
                    )
                    Text(
                        text = "Explore All ➔",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.effectiveA1,
                        modifier = Modifier.clickable { viewModel.openNewsHub() }
                    )
                }
                Spacer(Modifier.height(12.dp))
                newsArticles.take(4).forEachIndexed { index, news ->
                    NewsCard(
                        item    = news,
                        index   = index,
                        theme   = theme,
                        onClick = { viewModel.onNewsArticleClick(news) },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Full News Hub Action Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .frostedGlass(isDark = theme.isDark, shape = RoundedCornerShape(22.dp))
                        .background(if (theme.isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.42f))
                        .border(1.dp, theme.glass.glassBorder, RoundedCornerShape(22.dp))
                        .clickable { viewModel.openNewsHub() }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "📰 Explore Full News Hub & Global Sources",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            color = theme.effectiveA1
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = theme.effectiveA1,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (ui.weatherState.isDetailOpen) {
            WeatherDetailModal(
                weather = ui.weatherState,
                theme = theme,
                onDismiss = { viewModel.setWeatherDetailOpen(false) }
            )
        }

        if (showAddDialog) {
            AddQuickAccessDialog(
                theme = theme,
                frequentSites = frequentSites,
                onDismiss = { showAddDialog = false },
                onConfirm = { title, url ->
                    viewModel.addQuickAccessSite(title, url)
                    showAddDialog = false
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GREETING SECTION  (App.tsx lines 654–698 + customize button)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GreetingSection(
    weather: WeatherState,
    theme: OBThemeConfig,
    onCustomizeClick: () -> Unit,
    onWeatherClick: () -> Unit,
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val slot = remember(hour) { getTimeSlot(hour) }

    val greetText = when (slot) {
        com.orbit.browser.ui.theme.TimeSlot.Dawn -> "Good Dawn"
        com.orbit.browser.ui.theme.TimeSlot.Morning -> "Good Morning"
        com.orbit.browser.ui.theme.TimeSlot.Noon -> "Good Noon"
        com.orbit.browser.ui.theme.TimeSlot.Afternoon -> "Good Afternoon"
        com.orbit.browser.ui.theme.TimeSlot.Evening -> "Good Evening"
        com.orbit.browser.ui.theme.TimeSlot.Night -> "Good Night"
        else -> "Good Day"
    }

    val g = theme.glass
    val a1 = theme.effectiveA1
    val a2 = theme.effectiveA2
    val isDark = theme.isDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)) {
            Spacer(Modifier.height(10.dp))

            // ── Single Unified Greeting Bar ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .frostedGlass(isDark = isDark, shape = RoundedCornerShape(50.dp), blurRadius = 32.dp)
                    .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.65f))
                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.75f), RoundedCornerShape(50.dp))
                    .padding(start = 14.dp, top = 5.dp, end = 6.dp, bottom = 5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 1. Greeting text
                    Text(
                        text = greetText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                        color = g.txColor,
                    )

                    // Separator dot
                    Text("•", fontSize = 12.sp, color = g.tx2Color.copy(alpha = 0.5f))

                    // 2. Temperature in degree
                    Text(
                        text = "${weather.temperature.toInt()}°C",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = a1,
                        modifier = Modifier.clickable { onWeatherClick() }
                    )

                    Spacer(Modifier.width(2.dp))

                    // 3. Inner Frosted Glass Layer for Day icon + Weather icon + Weather condition text
                    if (!weather.loading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .frostedGlass(isDark = isDark, shape = RoundedCornerShape(50.dp), blurRadius = 24.dp)
                                .background(if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.75f))
                                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.85f), RoundedCornerShape(50.dp))
                                .clip(RoundedCornerShape(50.dp))
                                .clickable { onWeatherClick() }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            // Single SVG icon: TimeOfDayIcon if Clear, WeatherConditionIcon otherwise
                            if (weather.weatherKind == WeatherKind.Clear) {
                                com.orbit.browser.ui.components.TimeOfDayIcon(slot = slot, isDark = isDark, size = 15.dp)
                            } else {
                                com.orbit.browser.ui.components.WeatherConditionIcon(kind = weather.weatherKind, isDark = isDark, size = 15.dp)
                            }

                            // Condition text
                            Text(
                                text = weather.weatherKind.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = g.txColor
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── "Orbit Browser" minimal 2-stop gradient title ─────────────
            Text(
                text = "Orbit Browser",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            a1,
                            if (isDark) Color.White.copy(alpha = 0.95f) else a2
                        )
                    )
                ),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                lineHeight = (32 * 1.1).sp,
            )
        }

        // ── Customise button ───────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp)
                .size(40.dp)
                .frostedGlass(isDark = isDark, shape = RoundedCornerShape(50.dp), blurRadius = 32.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCustomizeClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Customize",
                tint = g.txColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HOME SEARCH BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeSearchBar(theme: OBThemeConfig, onClick: () -> Unit) {
    val g = theme.glass
    val a1 = theme.effectiveA1
    val a2 = theme.effectiveA2
    val isDark = theme.isDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(54.dp)
            .frostedGlass(isDark = isDark, shape = RoundedCornerShape(50.dp), blurRadius = 32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
    ) {
        Row(
            modifier              = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Search vector icon in accent color
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = a1,
                modifier = Modifier.size(20.dp)
            )

            // Placeholder text
            Text(
                text       = "Search or enter URL…",
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = g.tx2Color,
                modifier   = Modifier.weight(1f),
            )

            // ORBIT gradient pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(a1, a2)))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(
                    text          = "ORBIT",
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color         = Color.White,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FROSTED CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FrostedCard(
    theme: OBThemeConfig,
    a1: Color = theme.effectiveA1,
    a2: Color = theme.effectiveA2,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = theme.isDark
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedGlass(isDark = isDark, shape = RoundedCornerShape(28.dp), blurRadius = 32.dp),
    ) {
        // Inner shimmer gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(a1.copy(alpha = 0.15f), Color.Transparent),
                        radius = 800f,
                    )
                ),
        )
        Column(content = content)
    }
}



// ─────────────────────────────────────────────────────────────────────────────
// GLASS TILE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GlassTile(
    url: String,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    size: Int = 52,
    theme: OBThemeConfig,
    isDashed: Boolean = false,
    onClick: () -> Unit = {},
) {
    val g = theme.glass
    val isDark = theme.isDark
    val a1 = theme.effectiveA1

    val cleanDomain = remember(url) {
        if (url.isBlank()) ""
        else try {
            val uri = android.net.Uri.parse(url)
            uri.host?.removePrefix("www.") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    val faviconUrl = remember(cleanDomain) {
        if (cleanDomain.isNotBlank()) "https://www.google.com/s2/favicons?domain=$cleanDomain&sz=128" else null
    }

    val greyBgColor = if (isDashed) {
        Color.Transparent
    } else if (isDark) {
        Color(0xFF1E2234).copy(alpha = 0.48f)
    } else {
        Color(0xFFE2E8F0).copy(alpha = 0.65f)
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .frostedGlass(isDark = isDark, shape = androidx.compose.foundation.shape.CircleShape, blurRadius = 24.dp)
            .background(greyBgColor, shape = androidx.compose.foundation.shape.CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = LocalIndication.current,
                onClick           = onClick,
            )
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isDashed) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = theme.effectiveA1,
                modifier = Modifier.size(22.dp)
            )
        } else if (faviconUrl == null) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = g.txColor,
                modifier = Modifier.size(24.dp)
            )
        } else {
            AsyncImage(
                model              = faviconUrl,
                contentDescription = null,
                contentScale       = androidx.compose.ui.layout.ContentScale.Fit,
                modifier           = Modifier
                    .size(28.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QUICK ACCESS CARD  (App.tsx lines 749–787)
// ─────────────────────────────────────────────────────────────────────────────

private data class QASite(val label: String, val url: String)

private val DEFAULT_QA = listOf(
    QASite("Google",    "https://www.google.com"),
    QASite("YouTube",   "https://www.youtube.com"),
    QASite("Wikipedia", "https://www.wikipedia.org"),
    QASite("Reddit",    "https://www.reddit.com"),
    QASite("Twitter",   "https://www.x.com"),
    QASite("GitHub",    "https://www.github.com"),
    QASite("Amazon",    "https://www.amazon.com"),
    QASite("Add",       ""),
)

@Composable
private fun QuickAccessCard(
    theme: OBThemeConfig,
    sites: List<QuickAccessSite>,
    onSiteClick: (String) -> Unit,
    onAddClick: () -> Unit = {},
) {
    val a1 = theme.effectiveA1
    val g  = theme.glass

    FrostedCard(theme = theme) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text       = "Quick Access",
                fontSize   = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.2).sp,
                color      = if (theme.isDark) Color.White else Color(0xFF12122A),
            )
            // "Add" pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(a1.copy(alpha = 0.13f))
                    .border(1.dp, a1.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    .clickable { onAddClick() }
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            ) {
                Text(
                    text       = "＋ Add",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = a1,
                )
            }
        }

        // 4-column icon grid - vertical scrollable when multiple sites are added
        val displaySites: List<QASite> = if (sites.isEmpty()) {
            DEFAULT_QA
        } else {
            sites.map { QASite(it.title, it.url) } + QASite("Add", "")
        }

        val rows = displaySites.chunked(4)
        Column(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, bottom = 14.dp)
                .run {
                    if (rows.size > 2) {
                        this.heightIn(max = 210.dp).verticalScroll(rememberScrollState())
                    } else this
                }
        ) {
            rows.forEachIndexed { ri, row ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { site ->
                        val handleTap = {
                            if (site.url.isBlank()) onAddClick()
                            else onSiteClick(site.url)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = LocalIndication.current,
                                    onClick           = handleTap,
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            GlassTile(
                                url      = site.url,
                                size     = 52,
                                theme    = theme,
                                isDashed = site.url.isBlank(),
                                onClick  = handleTap,
                            )
                            Text(
                                text          = site.label,
                                fontSize      = 11.5.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = g.txColor,
                                maxLines      = 1,
                                overflow      = TextOverflow.Ellipsis,
                                textAlign     = TextAlign.Center,
                            )
                        }
                    }
                    // Fill empty columns in last row
                    repeat(4 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                if (ri < rows.size - 1) Spacer(Modifier.height(14.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRIVACY SHIELD CARD  (App.tsx lines 790–827)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PrivacyShieldCard(theme: OBThemeConfig, trackersBlocked: Int) {
    val green     = theme.glass.green
    val tealGreen = Color(0xFF00DDA0)
    val emerald   = Color(0xFF059669)
    val a1        = theme.effectiveA1
    val amber     = theme.glass.amber
    val g         = theme.glass
    val isDark    = theme.isDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .frostedGlass(
                isDark = isDark,
                shape = RoundedCornerShape(28.dp),
                borderWidth = 1.dp,
                accentColor = tealGreen,
            )
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to tealGreen.copy(alpha = if (isDark) 0.18f else 0.22f),
                        0.5f to emerald.copy(alpha = if (isDark) 0.10f else 0.12f),
                        1.0f to Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text       = "Privacy Shield",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (isDark) Color.White else Color(0xFF12122A),
                )
                // A+ badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(green.copy(alpha = 0.18f))
                        .border(1.dp, green.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                ) {
                    Text(
                        text       = "A+",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Black,
                        color      = green,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // 3-col stat chips (App.tsx lines 801–816)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrivacyStatChip(
                    number = "$trackersBlocked",
                    label  = "Trackers Blocked",
                    accentColor = green,
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                )
                PrivacyStatChip(
                    number = "47",
                    label  = "Sites Protected",
                    accentColor = a1,
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                )
                PrivacyStatChip(
                    number = "3",
                    label  = "Ads Removed",
                    accentColor = amber,
                    isDark = isDark,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))

            // Privacy progress bar (App.tsx lines 818–825)
            val progress = 0.92f
            val animatedProgress by animateFloatAsState(
                targetValue   = progress,
                animationSpec = tween(800, easing = FastOutSlowInEasing),
                label         = "privacy_bar",
            )
            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text       = "Privacy Level",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = g.tx2Color,
                )
                Text(
                    text       = "${(progress * 100).toInt()}%",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = green,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Brush.linearGradient(listOf(a1, green))),
                )
            }
        }
    }
}

@Composable
private fun PrivacyStatChip(
    number: String,
    label: String,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = number,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color      = accentColor,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text       = label,
                fontSize   = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center,
                lineHeight = 12.sp,
                color      = if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF12122A).copy(alpha = 0.55f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FREQUENTLY VISITED CARD  (App.tsx lines 830–864)
// ─────────────────────────────────────────────────────────────────────────────

private val FALLBACK_FREQUENT = listOf(
    FrequentSite(url = "https://github.com",         title = "GitHub"),
    FrequentSite(url = "https://youtube.com",        title = "YouTube"),
    FrequentSite(url = "https://maps.google.com",    title = "Maps"),
    FrequentSite(url = "https://amazon.com",         title = "Amazon"),
    FrequentSite(url = "https://instagram.com",      title = "Instagram"),
    FrequentSite(url = "https://facebook.com",       title = "Facebook"),
)

private fun emojiForUrl(url: String) = when {
    url.contains("github")    -> "🐙"
    url.contains("youtube")   -> "▶️"
    url.contains("maps")      -> "🗺️"
    url.contains("amazon")    -> "🛒"
    url.contains("instagram") -> "📸"
    url.contains("facebook")  -> "📘"
    url.contains("gmail")     -> "📧"
    url.contains("reddit")    -> "📰"
    else                      -> "🌐"
}

@Composable
private fun FrequentlyVisitedCard(
    theme: OBThemeConfig,
    sites: List<FrequentSite>,
    onSiteClick: (String) -> Unit,
) {
    val g      = theme.glass
    val isDark = theme.isDark
    val displaySites = (sites.takeIf { it.isNotEmpty() } ?: FALLBACK_FREQUENT)

    FrostedCard(theme = theme) {
        // Header without "See All" button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text       = "Frequently Visited",
                fontSize   = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = if (isDark) Color.White else Color(0xFF12122A),
            )
        }

        // Horizontal scroll row (icon size = 52, matching Quick Access!)
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.padding(bottom = 14.dp),
        ) {
            itemsIndexed(displaySites) { _, site ->
                val handleTap = { onSiteClick(site.url) }
                Column(
                    modifier = Modifier
                        .width(72.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = LocalIndication.current,
                            onClick           = handleTap,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    GlassTile(
                        url     = site.url,
                        emoji   = emojiForUrl(site.url),
                        size    = 52,
                        theme   = theme,
                        onClick = handleTap,
                    )
                    Text(
                        text       = site.title.take(10),
                        fontSize   = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color      = g.txColor,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        textAlign  = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NEWS CARD  (App.tsx lines 867–895)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NewsCard(
    item: com.orbit.browser.data.news.RealNewsArticle,
    index: Int,
    theme: OBThemeConfig,
    onClick: () -> Unit,
) {
    val g      = theme.glass
    val isDark = theme.isDark

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(index) { delay(index * 60L); visible = true }
    val alpha   by animateFloatAsState(if (visible) 1f else 0f, tween(300), label = "news_a_$index")
    val offsetY by animateDpAsState(if (visible) 0.dp else 16.dp, spring(0.75f, 400f), label = "news_y_$index")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .alpha(alpha)
            .offset(y = offsetY)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.40f))
            .border(1.dp, g.glassBorder, RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
    ) {
        Column {
            // Hero area with gradient + emoji / image + recommendation badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                item.gradientStart.copy(alpha = 0.22f),
                                item.gradientEnd.copy(alpha = 0.16f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(item.emoji, fontSize = 46.sp)
                }

                if (item.isRecommended && !item.recommendationReason.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(item.gradientStart.copy(alpha = 0.90f))
                            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "✨ ${item.recommendationReason}",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            // Text block
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text          = item.category.uppercase(),
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color         = item.gradientStart,
                    )
                    Text(
                        text     = item.source,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color    = g.tx2Color,
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text       = item.title,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp,
                    color      = g.txColor,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                if (item.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = item.description,
                        fontSize = 11.sp,
                        color    = g.tx2Color,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

private fun currentClockString(): String {
    val c = Calendar.getInstance()
    val h = c.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val m = c.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$h:$m"
}

@Composable
private fun AddQuickAccessDialog(
    theme: OBThemeConfig,
    frequentSites: List<FrequentSite>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, url: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val isDark = theme.isDark
    val g = theme.glass
    val a1 = theme.effectiveA1
    val a2 = theme.effectiveA2

    // Suggest ONLY sites the user visits most!
    val quickSuggestions = remember(frequentSites) {
        val visited = frequentSites.map { site ->
            Pair(site.title.take(16), site.url)
        }.distinctBy { it.second }.take(8)

        visited.ifEmpty {
            listOf(
                Pair("Google", "https://google.com"),
                Pair("YouTube", "https://youtube.com"),
                Pair("GitHub", "https://github.com"),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 340.dp)
                .frostedGlass(
                    isDark = isDark,
                    shape = RoundedCornerShape(32.dp),
                    blurRadius = 36.dp,
                    borderWidth = 1.dp
                )
                .background(
                    if (isDark) Color(0xFF0C0F1D).copy(alpha = 0.88f)
                    else Color(0xFFF8FAFC).copy(alpha = 0.90f),
                    shape = RoundedCornerShape(32.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { }
                .padding(24.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header with glowing icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(a1.copy(alpha = 0.15f))
                        .border(1.dp, a1.copy(alpha = 0.35f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLink,
                        contentDescription = null,
                        tint = a1,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Add Shortcut",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.3).sp,
                    color = g.txColor
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Save your favorite site for 1-tap quick access",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = g.tx2Color,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // Quick suggestions chips
                Text(
                    text = "QUICK SUGGESTIONS",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp,
                    color = g.tx2Color.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickSuggestions.forEach { (sName, sUrl) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                                .border(0.5.dp, g.glassBorder, RoundedCornerShape(20.dp))
                                .clickable {
                                    title = sName
                                    url = sUrl
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = sName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = g.txColor
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Glass Title Input Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Title (e.g. YouTube)", color = g.tx2Color.copy(alpha = 0.5f), fontSize = 13.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = g.txColor,
                            unfocusedTextColor = g.txColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Glass URL Input Field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    TextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = { Text("URL (e.g. youtube.com)", color = g.tx2Color.copy(alpha = 0.5f), fontSize = 13.sp) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = g.txColor,
                            unfocusedTextColor = g.txColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(22.dp))

                // Action buttons: Cancel & Add
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Cancel
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                            .border(1.dp, g.glassBorder, RoundedCornerShape(50.dp))
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Cancel", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = g.tx2Color)
                    }

                    // Add
                    val canSave = url.isNotBlank()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .alpha(if (canSave) 1f else 0.4f)
                            .clip(RoundedCornerShape(50.dp))
                            .background(Brush.linearGradient(listOf(a1, a2)))
                            .clickable(enabled = canSave) {
                                if (canSave) {
                                    val finalTitle = title.ifBlank { url.substringAfter("://").substringAfter("www.").substringBefore("/") }
                                    onConfirm(finalTitle, url)
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Add Shortcut", fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }
    }
}
