package com.orbit.browser.ui.screens

import android.view.ViewGroup
import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.orbit.browser.browser.engine.OBWebView
import com.orbit.browser.browser.tabs.SecurityState
import com.orbit.browser.ui.glass.frostedGlass
import com.orbit.browser.ui.BrowserCommand
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.theme.LocalOBTheme

// ═══════════════════════════════════════════════════════════════════════════
// BROWSER SCREEN — exact port of App.tsx "BROWSER VIEW" (lines 898–1003)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier:  Modifier = Modifier,
) {
    val ui        by viewModel.ui.collectAsState()
    val activeTab by viewModel.tabManager.activeTab.collectAsState()
    val theme     = LocalOBTheme.current
    val g         = theme.glass
    val isDark    = theme.isDark

    var webViewRef by remember { mutableStateOf<OBWebView?>(null) }
    var currentWvUrl by remember { mutableStateOf("") }

    LaunchedEffect(ui.screen, ui.tabsOpen) {
        if (ui.screen == com.orbit.browser.ui.BrowserScreen.TabSwitcher || ui.tabsOpen) {
            val wv = webViewRef
            if (wv != null) {
                wv.captureCurrentStateThumbnail()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        // ── WebView: scale+fade out when tabs panel open ─────────────────
        val secOpen    = ui.secPanelOpen

        // When security panel is open, page scales down (App.tsx line 967–971)
        val contentScale by animateFloatAsState(
            targetValue   = if (secOpen) 0.84f else 1f,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f),
            label         = "content_scale",
        )
        val contentAlpha by animateFloatAsState(
            targetValue   = if (secOpen) 0.6f else 1f,
            animationSpec = tween(400),
            label         = "content_alpha",
        )
        val contentBlur by animateDpAsState(
            targetValue   = if (secOpen) 2.dp else 0.dp,
            animationSpec = tween(400),
            label         = "content_blur",
        )
        val contentCorner by animateDpAsState(
            targetValue   = if (secOpen) 22.dp else 0.dp,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f),
            label         = "content_corner",
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(contentCorner))
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                    alpha  = contentAlpha
                },
        ) {
            // ── Address Bar + progress (App.tsx lines 906–927) ───────────
            val addressBarAlpha by animateFloatAsState(
                targetValue   = if (ui.searchOpen) 0f else 1f,
                animationSpec = tween(180),
                label         = "address_bar_alpha",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = addressBarAlpha }
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp)
                    .statusBarsPadding(),
            ) {
                AddressBar(
                    viewModel     = viewModel,
                    url           = activeTab?.displayUrl ?: "",
                    isSecure      = activeTab?.securityState == SecurityState.Secure,
                    isLoading     = activeTab?.isLoading == true,
                    progress      = activeTab?.loadProgress ?: 0f,
                    onReaderClick = { viewModel.extractAndOpenReaderMode(webViewRef) },
                )
            }

            // Space occupied by WebView below address bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                // Beautiful loading placeholder (fades in to cover previous page content while new page loads)
                val showPlaceholder = activeTab?.isLoading == true
                androidx.compose.animation.AnimatedVisibility(
                    visible = showPlaceholder,
                    enter   = fadeIn(tween(180)),
                    exit    = fadeOut(tween(250)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(theme.glass.phoneBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color       = theme.effectiveA1,
                                strokeWidth = 3.dp,
                                modifier    = Modifier.size(36.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text       = "Loading...",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color      = g.tx2Color,
                            )
                        }
                    }
                }
            }
        }

        // ── Security panel (App.tsx lines 929–960) ────────────────────────
        AnimatedVisibility(
            visible = secOpen,
            enter   = slideInVertically(
                initialOffsetY = { -20 },
                animationSpec  = tween(380, easing = com.orbit.browser.ui.animations.OBEasing.FigmaSpring),
            ) + scaleIn(
                initialScale  = 0.96f,
                animationSpec = tween(380, easing = com.orbit.browser.ui.animations.OBEasing.FigmaSpring),
            ) + fadeIn(tween(300, easing = com.orbit.browser.ui.animations.OBEasing.FigmaEase)),
            exit    = slideOutVertically(
                targetOffsetY = { -20 },
                animationSpec = tween(240, easing = com.orbit.browser.ui.animations.OBEasing.FigmaEase),
            ) + scaleOut(
                targetScale   = 0.96f,
                animationSpec = tween(240, easing = com.orbit.browser.ui.animations.OBEasing.FigmaEase),
            ) + fadeOut(tween(240, easing = com.orbit.browser.ui.animations.OBEasing.FigmaEase)),
        ) {
            SecurityPanel(
                tab      = activeTab,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 66.dp, start = 12.dp, end = 12.dp),
            )
        }

        // ── Share sheet (App.tsx lines 1153–1183) ─────────────────────────
        AnimatedVisibility(
            visible  = ui.shareOpen,
            enter    = com.orbit.browser.ui.animations.OBMotion.sheetEnterFromBottom,
            exit     = com.orbit.browser.ui.animations.OBMotion.sheetExitToBottom,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ShareSheet(
                url       = activeTab?.url ?: "",
                onDismiss = { viewModel.closeShare() },
            )
        }

        // ── Context Menu Sheet (Long Press Image / Link) ───────────────────
        val contextMenuElement = ui.activeContextMenuElement
        AnimatedVisibility(
            visible  = contextMenuElement != null,
            enter    = com.orbit.browser.ui.animations.OBMotion.sheetEnterFromBottom,
            exit     = com.orbit.browser.ui.animations.OBMotion.sheetExitToBottom,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            contextMenuElement?.let { element ->
                ContextMenuSheet(
                    element   = element,
                    viewModel = viewModel,
                    onDismiss = { viewModel.dismissContextMenu() },
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PERSISTENT WEB VIEW STACK — Always mounted to preserve page state & media
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun PersistentWebViewStack(
    viewModel: BrowserViewModel,
    modifier:  Modifier = Modifier,
) {
    val ui by viewModel.ui.collectAsState()
    val isDark = LocalOBTheme.current.isDark
    val tabs by viewModel.tabManager.tabs.collectAsState()
    val activeTabId by viewModel.tabManager.activeTabId.collectAsState()
    val webViewMap = remember { mutableStateMapOf<String, OBWebView>() }

    val isBrowserActive = ui.screen == com.orbit.browser.ui.BrowserScreen.Browser && !ui.tabsOpen

    // Clean up WebViews for closed tabs
    DisposableEffect(tabs) {
        val openTabIds = tabs.map { it.id }.toSet()
        val closedIds = webViewMap.keys.filter { it !in openTabIds }
        closedIds.forEach { closedId ->
            webViewMap.remove(closedId)?.apply {
                stopLoading()
                clearHistory()
                destroy()
            }
        }
        onDispose { }
    }

    var webViewResetKey by remember { mutableStateOf(0) }

    LaunchedEffect(viewModel) {
        viewModel.commands.collect { command ->
            val activeWv = webViewMap[activeTabId]
            when (command) {
                is BrowserCommand.GoBack    -> {
                    if (activeWv?.canGoBack() == true) {
                        activeWv.goBack()
                    } else {
                        viewModel.goHome()
                    }
                }
                is BrowserCommand.GoForward -> activeWv?.goForward()
                is BrowserCommand.Refresh   -> activeWv?.reload()
                is BrowserCommand.LoadUrl   -> {
                    if (command.clearHistory) {
                        activeWv?.shouldClearHistoryOnNextLoad = true
                        activeWv?.clearHistory()
                    }
                    activeWv?.loadUrl(command.url)
                }
            }
        }
    }

    val activeCardBounds by viewModel.activeTabCardBounds.collectAsState()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        tabs.forEach { tab ->
            val isCurrentTabActive = tab.id == activeTabId
            val isHomeScreen = ui.screen == com.orbit.browser.ui.BrowserScreen.Home
            val isTabSwitcherActive = ui.screen == com.orbit.browser.ui.BrowserScreen.TabSwitcher || ui.tabsOpen

            key(tab.id) {
                val scaleRatio = if (isCurrentTabActive && isTabSwitcherActive && activeCardBounds != null && activeCardBounds!!.width > 0 && screenWidthPx > 0) {
                    (activeCardBounds!!.width / screenWidthPx).coerceIn(0.2f, 1.0f)
                } else if (isTabSwitcherActive) 0.46f else 1.0f

                val calcTargetX = if (isCurrentTabActive && isTabSwitcherActive && activeCardBounds != null && activeCardBounds!!.width > 0) {
                    activeCardBounds!!.x
                } else 0f

                val calcTargetY = if (isCurrentTabActive && isTabSwitcherActive && activeCardBounds != null && activeCardBounds!!.height > 0) {
                    activeCardBounds!!.y
                } else 0f

                val offXInPx = with(density) { 1000.dp.toPx() }

                val tabScale by animateFloatAsState(
                    targetValue = if (isCurrentTabActive) scaleRatio else 0.85f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f),
                    label = "tab_scale"
                )
                val tabCornerRadius by animateDpAsState(
                    targetValue = if (isCurrentTabActive && isTabSwitcherActive) 14.dp else 0.dp,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f),
                    label = "tab_corner"
                )
                val tabOffsetY by animateFloatAsState(
                    targetValue = when {
                        !isCurrentTabActive -> 0f
                        isTabSwitcherActive -> calcTargetY
                        else -> 0f
                    },
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f),
                    label = "tab_offset_y"
                )
                val tabOffsetX by animateFloatAsState(
                    targetValue = when {
                        !isCurrentTabActive -> -offXInPx
                        isHomeScreen -> offXInPx
                        isTabSwitcherActive -> calcTargetX
                        else -> 0f
                    },
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
                    label = "tab_offset_x"
                )
                val tabAlpha by animateFloatAsState(
                    targetValue = when {
                        isCurrentTabActive -> 1.0f
                        else -> 0.0f
                    },
                    animationSpec = tween(220, easing = com.orbit.browser.ui.animations.OBEasing.IosCurve),
                    label = "tab_alpha"
                )

                val activeCardHeightDp = if (isCurrentTabActive && isTabSwitcherActive && activeCardBounds != null && activeCardBounds!!.height > 0) {
                    with(density) { (activeCardBounds!!.height / scaleRatio).toDp() }
                } else androidx.compose.ui.unit.Dp.Unspecified

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (isCurrentTabActive && isTabSwitcherActive) 100f else 0f)
                        .then(if (!isTabSwitcherActive) Modifier.padding(top = 56.dp).statusBarsPadding() else Modifier)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0f)
                            translationX    = tabOffsetX
                            translationY    = tabOffsetY
                            scaleX          = tabScale
                            scaleY          = tabScale
                            alpha           = tabAlpha
                            shadowElevation = if (isTabSwitcherActive) 16f else 0f
                        }
                        .run {
                            if (activeCardHeightDp != androidx.compose.ui.unit.Dp.Unspecified) {
                                this.height(activeCardHeightDp)
                            } else this
                        }
                        .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp, topStart = 14.dp, topEnd = 14.dp))
                        .run {
                            if (!isCurrentTabActive && tabAlpha == 0f) {
                                this.offset(x = (-9999).dp)
                            } else this
                        }
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            OBWebView(context).also { wv ->
                                wv.layoutParams   = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                wv.tabId          = tab.id
                                wv.tabManager     = viewModel.tabManager
                                wv.adBlocker      = viewModel.adBlocker
                                wv.onCreateNewTab = { url -> viewModel.newTab(url) }
                                wv.onUrlChanged   = { newUrl ->
                                    if (tab.id == activeTabId && newUrl.isNotBlank()) {
                                        wv.currentMainUrl = newUrl
                                        viewModel.tabManager.updateTab(tab.id) { t -> t.copy(url = newUrl, displayUrl = newUrl) }
                                    }
                                }
                                wv.httpsOnlyEnabled = ui.httpsOnly
                                wv.onFindMatchCount = { current, total -> viewModel.updateFindMatchCount(current, total) }
                                wv.onDownloadRequested = { url, userAgent, contentDisposition, mimeType, contentLength ->
                                    viewModel.startDownload(context, url, userAgent, contentDisposition, mimeType, contentLength)
                                }
                                wv.onContextMenuRequested = { element ->
                                    viewModel.showContextMenu(element)
                                }
                                wv.updateDarkMode(isDark)
                                wv.updateBlockCookies(ui.blockCookies)
                                 if (tab.url.isNotBlank() && tab.url != "orbit://home" && !tab.url.startsWith("orbit://")) {
                                    wv.currentMainUrl = tab.url
                                    wv.loadUrl(tab.url)
                                }
                                webViewMap[tab.id] = wv
                            }
                        },
                        update = { wv ->
                            wv.httpsOnlyEnabled = ui.httpsOnly
                            wv.updateDarkMode(isDark)
                            wv.updateBlockCookies(ui.blockCookies)
                            if (ui.findInPageOpen) {
                                wv.findInPage(ui.findInPageQuery)
                            }
                            if (tab.url.isNotBlank() && !tab.url.startsWith("orbit://") && (wv.url.isNullOrBlank() || wv.url == "about:blank" || wv.currentMainUrl != tab.url)) {
                                wv.currentMainUrl = tab.url
                                wv.loadUrl(tab.url)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AddressBar(
    viewModel:     BrowserViewModel,
    url:           String,
    isSecure:      Boolean,
    isLoading:     Boolean,
    progress:      Float,
    onReaderClick: () -> Unit = {},
    modifier:      Modifier = Modifier,
) {
    val theme  = LocalOBTheme.current
    val g      = theme.glass
    val isDark = theme.isDark
    val a1     = theme.effectiveA1

    val secColor = if (isSecure) Color(0xFF00DDA0) else Color(0xFFFF5555)

    Column(modifier = modifier) {
        // ── Main pill — sleek thin 44dp height ───────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .frostedGlass(isDark = isDark, shape = RoundedCornerShape(50.dp), blurRadius = 28.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) { viewModel.openSearch() },
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Lock/LockOpen icon — opens security panel (Sleek 32dp container, 18dp icon)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                        ) { viewModel.toggleSecPanel() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isSecure) "Connection Secure" else "Not Secure",
                        tint = secColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clean single-line site name (e.g. YouTube)
                Text(
                    text       = extractCleanDomain(url),
                    fontSize   = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    color      = g.txColor,
                    modifier   = Modifier.weight(1f),
                )

                // Single action button: Reload / Refresh (or loading spinner)
                if (isLoading) {
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            color       = a1,
                            strokeWidth = 2.dp,
                        )
                    }
                } else {
                    AddressActionBtn(icon = Icons.Default.Refresh, contentDescription = "Refresh", tx2 = g.txColor) { viewModel.refresh() }
                }
            }
        }

        // ── Progress bar under address bar ──────────────────────────────
        if (isLoading) {
            Spacer(Modifier.height(2.dp))
            val animatedProgress by animateFloatAsState(
                targetValue   = progress,
                animationSpec = tween(200),
                label         = "page_progress",
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color      = a1,
                trackColor = Color.Transparent,
            )
        }
    }
}

@Composable
private fun AddressActionBtn(
    icon:               androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tx2:                Color,
    onClick:            () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tx2,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SECURITY PANEL — Frosted Glassmorphism Card with Backdrop Blur
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SecurityPanel(
    tab:      com.orbit.browser.browser.tabs.OBTab?,
    modifier: Modifier = Modifier,
) {
    val theme    = LocalOBTheme.current
    val g        = theme.glass
    val isDark   = theme.isDark
    val isSecure = tab?.securityState == SecurityState.Secure
    val green    = Color(0xFF00DDA0)

    val domain   = extractCleanDomain(tab?.displayUrl ?: "")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .frostedGlass(isDark = isDark, shape = RoundedCornerShape(32.dp), blurRadius = 32.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(green.copy(alpha = 0.16f))
                        .border(1.dp, green.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (isSecure) green else Color(0xFFFF5555),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = domain,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = g.txColor,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    Text(
                        text       = if (isSecure) "● Connection Secure" else "● Not Secure",
                        fontSize   = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isSecure) green else Color(0xFFFF5555),
                    )
                }
                Text(
                    text       = if (isSecure) "A+" else "C",
                    fontSize   = 30.sp,
                    fontWeight = FontWeight.Black,
                    color      = if (isSecure) green else Color(0xFFFF5555),
                )
            }

            Spacer(Modifier.height(18.dp))

            // Security rows with frosted glass backdrop blur tiles
            val trackers = tab?.trackersBlocked ?: 7
            val secItems = listOf(
                Triple(Icons.Default.VpnKey, "Encryption",       if (isSecure) "TLS 1.3" else "None"),
                Triple(Icons.Default.VerifiedUser, "Certificate",       if (isSecure) "Valid · Let's Encrypt" else "Invalid"),
                Triple(Icons.Default.Language, "DNS-over-HTTPS",    "Active"),
                Triple(Icons.Default.Shield, "Trackers Blocked",  "$trackers blocked"),
            )
            secItems.forEachIndexed { i, (icon, label, value) ->
                SecurityRow(
                    iconVector = icon,
                    label      = label,
                    value      = value,
                    txColor     = g.txColor,
                    green       = green,
                    isDark      = isDark,
                )
                if (i < 3) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SecurityRow(
    iconVector:  androidx.compose.ui.graphics.vector.ImageVector,
    label:       String,
    value:       String,
    txColor:     Color,
    green:       Color,
    isDark:      Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlass(isDark = isDark, shape = RoundedCornerShape(20.dp), blurRadius = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = label,
                tint = txColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text       = label,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = txColor,
                modifier   = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(green.copy(alpha = 0.16f))
                    .border(1.dp, green.copy(alpha = 0.35f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 11.dp, vertical = 3.dp),
            ) {
                Text(
                    text       = value,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color      = green,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SHARE SHEET — App.tsx lines 1153–1183
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ShareSheet(
    url:       String,
    onDismiss: () -> Unit,
) {
    val theme  = LocalOBTheme.current
    val g      = theme.glass
    val isDark = theme.isDark

    val sheetBg = if (isDark) Color(0xFF0C0E1A).copy(alpha = 0.98f) else Color(0xFFF5F7FF).copy(alpha = 0.98f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isDark) Color(0xFF040510).copy(alpha = 0.65f)
                else Color(0xFFC8D2F0).copy(alpha = 0.65f)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass(isDark = isDark, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), blurRadius = 32.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) { /* consume */ }
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Column {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(g.glassBorder2)
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(18.dp))

                // Title
                Text(
                    text       = "Share: ${extractDomain(url)}",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = g.tx2Color,
                )
                Spacer(Modifier.height(16.dp))

                // 5-item share grid
                val options = listOf(
                    Icons.Default.ContentCopy to "Copy",
                    Icons.AutoMirrored.Filled.Message to "Message",
                    Icons.Default.Email to "Email",
                    Icons.Default.CameraAlt to "Insta",
                    Icons.AutoMirrored.Filled.Send to "Twitter"
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    options.forEach { (icon, label) ->
                        Column(
                            modifier            = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null,
                                ) { onDismiss() },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(g.glassBg2)
                                    .border(1.dp, g.glassBorder2, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = g.txColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text       = label,
                                fontSize   = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color      = g.tx2Color,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = g.glassBorder2, thickness = 1.dp)
                Spacer(Modifier.height(10.dp))

                // Share via Android
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(g.glassBg)
                        .border(1.dp, g.glassBorder, RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = LocalIndication.current,
                        ) { onDismiss() }
                        .padding(12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = g.txColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text       = "Share via Android…",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = g.txColor,
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════════════════════

private fun extractCleanDomain(url: String): String = try {
    val host = android.net.Uri.parse(url).host?.removePrefix("www.") ?: url
    if (host.contains("youtube.com")) "YouTube"
    else if (host.contains("google.com")) "Google"
    else if (host.contains("github.com")) "GitHub"
    else if (host.contains("reddit.com")) "Reddit"
    else if (host.contains("wikipedia.org")) "Wikipedia"
    else if (host.contains("amazon.com")) "Amazon"
    else host.ifBlank { "Home" }
} catch (_: Exception) { url }

private fun extractDomain(url: String): String = extractCleanDomain(url)

private fun extractPath(url: String): String = try {
    val uri  = android.net.Uri.parse(url)
    val path = uri.path ?: ""
    val q    = uri.query?.let { "?$it" } ?: ""
    "${path}${q}".take(60)
} catch (_: Exception) { "" }

@Composable
private fun ContextMenuSheet(
    element: com.orbit.browser.browser.engine.OBContextMenuElement,
    viewModel: BrowserViewModel,
    onDismiss: () -> Unit,
) {
    val theme  = LocalOBTheme.current
    val g      = theme.glass
    val isDark = theme.isDark
    val context = androidx.compose.ui.platform.LocalContext.current

    val link = element.linkUrl ?: ""
    val img  = element.imageUrl ?: ""
    val title = element.title ?: ""

    val targetUrl = if (link.isNotBlank()) link else img

    val wallpaperGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF1E1B4B),
                Color(0xFF090A15)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFEEF2FF),
                Color(0xFFE2E8F0)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(wallpaperGradient)
                .border(
                    width = 1.5.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                ) { /* consume */ }
                .padding(20.dp),
        ) {
            Column {

                val titleText = if (title.isNotBlank()) title else targetUrl
                Text(
                    text       = titleText,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = g.txColor,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                )
                if (targetUrl.isNotBlank() && targetUrl != titleText) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text       = targetUrl,
                        fontSize   = 11.5.sp,
                        color      = g.tx2Color,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = g.glassBorder2, thickness = 1.dp)
                Spacer(Modifier.height(8.dp))

                if (link.isNotBlank()) {
                    ContextMenuItem(icon = Icons.AutoMirrored.Filled.OpenInNew, label = "Open in New Tab") {
                        viewModel.newTab(link)
                        onDismiss()
                    }
                    ContextMenuItem(icon = Icons.Default.Security, label = "Open in Incognito Tab") {
                        viewModel.newTab(link, isPrivate = true)
                        onDismiss()
                    }
                    ContextMenuItem(icon = Icons.Default.ContentCopy, label = "Copy Link Address") {
                        viewModel.copyToClipboard(context, "Link", link)
                        onDismiss()
                    }
                }

                if (img.isNotBlank()) {
                    ContextMenuItem(icon = Icons.Default.FileDownload, label = "Save Image / Download Media") {
                        viewModel.startDownload(context, img, "", "", "", 0L)
                        onDismiss()
                    }
                } else if (link.isNotBlank()) {
                    ContextMenuItem(icon = Icons.Default.FileDownload, label = "Download Link Target") {
                        viewModel.startDownload(context, link, "", "", "", 0L)
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val theme = LocalOBTheme.current
    val g = theme.glass
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = g.txColor, modifier = Modifier.size(20.dp))
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = g.txColor)
    }
}
