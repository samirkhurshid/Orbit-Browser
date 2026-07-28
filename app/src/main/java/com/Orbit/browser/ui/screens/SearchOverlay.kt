package com.orbit.browser.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.theme.LocalOBTheme
import com.orbit.browser.ui.glass.frostedGlass

// ═══════════════════════════════════════════════════════════════════════════
// SEARCH OVERLAY — 100% exact port of App.tsx "SEARCH OVERLAY" (lines 1145–1280)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun SearchOverlay(viewModel: BrowserViewModel, visible: Boolean) {
    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current
    val ui             by viewModel.ui.collectAsState()
    val history        by viewModel.recentHistory.collectAsState()
    val theme          = LocalOBTheme.current
    val g              = theme.glass
    val isDark         = theme.isDark
    val border         = g.glassBorder2
    val a1             = theme.effectiveA1
    val a2             = theme.effectiveA2

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(150)
            focusRequester.requestFocus()
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    val overlayBg = if (isDark) Color(0xFF060712).copy(alpha = 0.55f) else Color(0xFFDCECFF).copy(alpha = 0.45f)

    val figmaSpring = com.orbit.browser.ui.animations.OBEasing.FigmaSpring
    val figmaEase   = com.orbit.browser.ui.animations.OBEasing.FigmaEase

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(220, easing = figmaEase)),
        exit    = fadeOut(tween(180, easing = figmaEase)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayBg)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) { viewModel.closeSearch() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) {}
                    .statusBarsPadding()
                    .padding(top = 8.dp),
            ) {
                // Connected active search bar morphing into view
                AnimatedVisibility(
                    visible = visible,
                    enter   = slideInVertically(initialOffsetY = { -28 }, animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f)) + fadeIn(tween(200)),
                    exit    = slideOutVertically(targetOffsetY = { -28 }, animationSpec = tween(220, easing = figmaEase)) + fadeOut(tween(160)),
                ) {
                    SearchBarTop(
                        viewModel      = viewModel,
                        query          = ui.searchQuery,
                        onQueryChange  = { viewModel.onSearchQueryChanged(it) },
                        onSubmit       = { viewModel.submitSearch(it) },
                        onBack         = { viewModel.closeSearch() },
                        focusRequester = focusRequester,
                        theme          = theme,
                        modifier       = Modifier.padding(horizontal = 14.dp),
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Search menu results list card popping & expanding down from directly under the bar
                AnimatedVisibility(
                    visible = visible,
                    enter   = expandVertically(expandFrom = Alignment.Top, animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f)) +
                              slideInVertically(initialOffsetY = { -30 }, animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f)) +
                              fadeIn(tween(220)),
                    exit    = shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = tween(200)) +
                              slideOutVertically(targetOffsetY = { -30 }, animationSpec = tween(180)) +
                              fadeOut(tween(160)),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 14.dp)
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .frostedGlass(isDark = isDark, shape = RoundedCornerShape(28.dp), blurRadius = 32.dp),
                    ) {
                        LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)) {
                            if (ui.historyResults.isNotEmpty()) {
                                item { SectionLabel("Recent", a1) }
                                itemsIndexed(ui.historyResults.take(5)) { index, entry ->
                                    val formatted = formatHistoryEntry(entry.url, entry.title)
                                    SearchResultRow(
                                        isWeb    = true,
                                        title    = formatted.title,
                                        subtitle = formatted.subtitle,
                                        index    = index,
                                        theme    = theme,
                                        onClick  = { viewModel.submitSearch(formatted.targetQuery) },
                                    )
                                }
                            } else if (ui.searchQuery.isBlank()) {
                                item { SectionLabel("Recent", a1) }
                                itemsIndexed(history.take(5)) { index, entry ->
                                    val formatted = formatHistoryEntry(entry.url, entry.title)
                                    SearchResultRow(
                                        isWeb    = true,
                                        title    = formatted.title,
                                        subtitle = formatted.subtitle,
                                        index    = index,
                                        theme    = theme,
                                        onClick  = { viewModel.submitSearch(formatted.targetQuery) },
                                    )
                                }
                            }

                            if (ui.searchSuggestions.isNotEmpty()) {
                                item { Spacer(Modifier.height(8.dp)) }
                                item { SectionLabel("Suggestions", a1) }
                                itemsIndexed(ui.searchSuggestions) { index, suggestion ->
                                    SearchResultRow(
                                        isWeb    = false,
                                        title    = suggestion,
                                        subtitle = "Search Google",
                                        index    = index,
                                        theme    = theme,
                                        onClick  = { viewModel.submitSearch(suggestion) },
                                    )
                                }
                            }

                            if (ui.searchQuery.contains(".") && !ui.searchQuery.contains(" ")) {
                                item { Spacer(Modifier.height(8.dp)) }
                                item { SectionLabel("Navigate", a1) }
                                item {
                                    SearchResultRow(
                                        isWeb    = true,
                                        title    = ui.searchQuery,
                                        subtitle = "Go to website",
                                        index    = 0,
                                        theme    = theme,
                                        onClick  = { viewModel.submitSearch(ui.searchQuery) },
                                    )
                                }
                            }

                            if (ui.searchQuery.isNotBlank()) {
                                item { Spacer(Modifier.height(8.dp)) }
                                item {
                                    SearchResultRow(
                                        isWeb    = false,
                                        title    = "Search for \"${ui.searchQuery}\"",
                                        subtitle = "Google Search",
                                        index    = 0,
                                        theme    = theme,
                                        onClick  = { viewModel.submitSearch(ui.searchQuery) },
                                    )
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
private fun SearchBarTop(
    viewModel:      BrowserViewModel,
    query:          String,
    onQueryChange:  (String) -> Unit,
    onSubmit:       (String) -> Unit,
    onBack:         () -> Unit,
    focusRequester: FocusRequester,
    theme:          com.orbit.browser.ui.theme.OBThemeConfig,
    modifier:       Modifier = Modifier,
) {
    val g      = theme.glass
    val isDark = theme.isDark
    val a1     = theme.effectiveA1
    val border = g.glassBorder2

    // Search input & back button backgrounds (App.tsx lines 1168 & 1183)
    val btnBg   = if (isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.65f)
    val inputBg = if (isDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.72f)
    val inputBorder = a1.copy(alpha = if (isDark) 0.45f else 0.55f)

    Row(
        modifier          = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Back button — 40x40 circular frosted glass
        Box(
            modifier = Modifier
                .size(40.dp)
                .frostedGlass(isDark = isDark, shape = CircleShape, blurRadius = 20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = onBack,
                ),
            contentAlignment = Alignment.Center,
        ) {
            ChevronLeftIcon(color = g.txColor)
        }

        // Search input pill — 46dp frosted glass pill
        Box(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .frostedGlass(isDark = isDark, shape = RoundedCornerShape(50.dp), blurRadius = 24.dp, isFocused = true)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SearchIcon(color = a1)

                BasicTextField(
                    value         = query,
                    onValueChange = onQueryChange,
                    modifier      = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle     = TextStyle(color = g.txColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    singleLine    = true,
                    cursorBrush   = SolidColor(a1),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onSubmit(query) }),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    text       = "Search or enter URL…",
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = g.tx3Color,
                                )
                            }
                            inner()
                        }
                    },
                )

                // Full URL action buttons: Copy, Share, Clear
                AnimatedVisibility(visible = query.isNotEmpty()) {
                    val context = LocalContext.current
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Copy Full URL
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f))
                                .clickable {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("URL", query)
                                    clipboard.setPrimaryClip(clip)
                                    viewModel.showToast("URL copied to clipboard")
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy URL",
                                tint = g.txColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Share Full URL
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f))
                                .clickable {
                                    viewModel.openShare()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share URL",
                                tint = g.txColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Clear Button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.10f))
                                .clickable { onQueryChange("") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = g.txColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, accent: Color) {
    Text(
        text          = text.uppercase(),
        fontSize      = 11.sp,
        color         = accent.copy(alpha = 0.8f),
        fontWeight    = FontWeight.ExtraBold,
        letterSpacing = 0.9.sp,
        modifier      = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp),
    )
}

@Composable
private fun SearchResultRow(
    isWeb:    Boolean = false,
    title:    String,
    subtitle: String,
    index:    Int,
    theme:    com.orbit.browser.ui.theme.OBThemeConfig,
    onClick:  () -> Unit,
) {
    val g      = theme.glass
    val isDark = theme.isDark
    val a1     = theme.effectiveA1
    val a2     = theme.effectiveA2

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        kotlinx.coroutines.delay(index * 35L)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(200),
        label         = "row_alpha_$index",
    )

    val rowBg     = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.55f)
    val rowBorder = if (isDark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.8f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .alpha(alpha)
            .clip(RoundedCornerShape(18.dp))
            .background(rowBg)
            .border(1.dp, rowBorder, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = LocalIndication.current,
                onClick           = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon chip
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(a1.copy(alpha = 0.22f), a2.copy(alpha = 0.16f))
                    )
                )
                .border(1.dp, a1.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isWeb) Icons.Default.Public else Icons.Default.Search,
                contentDescription = null,
                tint = g.txColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // Title + Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = g.txColor,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text     = subtitle,
                fontSize = 10.sp,
                color    = g.tx2Color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Chevron Right SVG icon
        ChevronRightIcon(color = g.tx3Color)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SVG ICONS — exact path renditions from App.tsx
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ChevronLeftIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
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
private fun ChevronRightIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(14.dp)) {
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
private fun SearchIcon(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
        val sw = 2.2f * density
        val sx = size.width  / 24f
        val sy = size.height / 24f
        drawCircle(
            color  = color,
            radius = 7f * sx,
            center = androidx.compose.ui.geometry.Offset(11 * sx, 11 * sy),
            style  = Stroke(width = sw),
        )
        drawLine(
            color       = color,
            start       = androidx.compose.ui.geometry.Offset(16.5f * sx, 16.5f * sy),
            end         = androidx.compose.ui.geometry.Offset(22f * sx, 22f * sy),
            strokeWidth = sw,
            cap         = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

private data class FormattedSearchResult(
    val icon: String,
    val title: String,
    val subtitle: String,
    val targetQuery: String
)

private fun formatHistoryEntry(url: String, title: String): FormattedSearchResult {
    if (url.contains("google.com/search") || url.contains("google.co.in/search")) {
        val queryParam = try {
            val uri = android.net.Uri.parse(url)
            uri.getQueryParameter("q")
        } catch (e: Exception) {
            null
        }
        val cleanQuery = queryParam?.takeIf { it.isNotBlank() }
            ?: title.replace(" - Google Search", "").replace("- Google Search", "").trim()
        return FormattedSearchResult(
            icon        = "🔍",
            title       = cleanQuery.ifBlank { "Search Query" },
            subtitle    = "Google Search",
            targetQuery = cleanQuery,
        )
    }
    val cleanHost = try {
        val uri = android.net.Uri.parse(url)
        uri.host?.removePrefix("www.") ?: url
    } catch (e: Exception) {
        url
    }
    return FormattedSearchResult(
        icon        = "🕒",
        title       = title.ifBlank { cleanHost },
        subtitle    = cleanHost,
        targetQuery = url,
    )
}
