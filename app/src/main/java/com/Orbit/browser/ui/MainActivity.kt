package com.orbit.browser.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.orbit.browser.ui.components.IslandNavBar
import com.orbit.browser.ui.components.TabSwitcherScreen
import com.orbit.browser.ui.components.FindInPageBar
import com.orbit.browser.ui.components.QrCodeModal
import com.orbit.browser.ui.glass.frostedGlass
import com.orbit.browser.ui.glass.LocalHazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.HazeState
import com.orbit.browser.ui.screens.*
import com.orbit.browser.ui.theme.LocalOBTheme
import com.orbit.browser.ui.theme.OBColors
import com.orbit.browser.ui.theme.OBTheme
import com.orbit.browser.ui.theme.OBThemePreset
import com.orbit.browser.ui.theme.getTimeSlot
import com.orbit.browser.ui.components.WeatherOverlayLayer
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar


@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        unlockHighRefreshRate()
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContent {
            val ui by viewModel.ui.collectAsState()
            val activeTab by viewModel.tabManager.activeTab.collectAsState()
            val isIncognito = activeTab?.isPrivate == true

            val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
            val timeSlot = ui.manualTimeSlot ?: remember(hour) { getTimeSlot(hour) }
            val weatherKind = ui.manualWeather ?: ui.weatherState.kind

            OBTheme(
                preset         = if (isIncognito) OBThemePreset.PurpleAurora else ui.theme,
                isDarkOverride  = if (isIncognito) true else ui.isDarkMode,
                weatherKind    = weatherKind,
                timeSlot       = timeSlot,
            ) {
                OrbitBrowserApp(viewModel = viewModel)
            }
        }
    }

    private fun unlockHighRefreshRate() {
        try {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
            val dm = getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
            val display = dm?.displays?.firstOrNull()
            val modes = display?.supportedModes ?: emptyArray()
            val maxMode = modes.maxByOrNull { it.refreshRate }

            val lp = window.attributes
            if (maxMode != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    lp.preferredDisplayModeId = maxMode.modeId
                    @Suppress("DEPRECATION")
                    lp.preferredRefreshRate = maxMode.refreshRate
                }
            }
            try {
                val field = android.view.WindowManager.LayoutParams::class.java.getField("preferredFrameRateCategory")
                field.setInt(lp, 3) // 3 = FRAME_RATE_CATEGORY_HIGH
            } catch (_: Exception) {}
            window.attributes = lp
        } catch (_: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveAppStateOnExit(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveAppStateOnExit(this)
    }

    override fun onDestroy() {
        if (viewModel.ui.value.clearOnExit) {
            viewModel.clearHistory()
        }
        super.onDestroy()
    }
}

@Composable
fun OrbitBrowserApp(viewModel: BrowserViewModel) {
    val ui        by viewModel.ui.collectAsState()
    val theme     = LocalOBTheme.current
    val activeTab by viewModel.tabManager.activeTab.collectAsState()

    val isFullScreenDestination = ui.screen in listOf(
        BrowserScreen.Bookmarks,
        BrowserScreen.History,
        BrowserScreen.Downloads,
        BrowserScreen.Settings
    )

    val hasActivePage = activeTab?.url?.isNotBlank() == true && activeTab?.url != "orbit://home"
    BackHandler(
        enabled = ui.customVideoView != null || isFullScreenDestination || ui.menuOpen || ui.customPanelOpen || ui.searchOpen || ui.tabsOpen || ui.screen == BrowserScreen.TabSwitcher || ui.screen == BrowserScreen.Browser
    ) {
        when {
            ui.customVideoView != null -> viewModel.hideCustomVideoView()
            isFullScreenDestination -> viewModel.goBackFromFullScreenDestination()
            ui.menuOpen        -> viewModel.closeMenu()
            ui.customPanelOpen -> viewModel.closeCustom()
            ui.searchOpen      -> viewModel.closeSearch()
            ui.screen == BrowserScreen.TabSwitcher || ui.tabsOpen -> viewModel.closeTabs()
            ui.screen == BrowserScreen.Browser -> {
                viewModel.goBack()
            }
        }
    }

    val hazeState = remember { HazeState() }

    val homeScrollState      = rememberScrollState()
    val settingsListState    = rememberLazyListState()
    val bookmarksListState   = rememberLazyListState()
    val historyListState     = rememberLazyListState()
    val downloadsListState   = rememberLazyListState()

    val figmaSpring = com.orbit.browser.ui.animations.OBEasing.IosCurve
    val figmaEase   = com.orbit.browser.ui.animations.OBEasing.IosCurve

    CompositionLocalProvider(LocalHazeState provides hazeState) {
    Box(modifier = Modifier.fillMaxSize().background(theme.glass.phoneBg)) {

        // Everything in this inner Box is the "backdrop" that frostedGlass
        // surfaces (menus, sheets, nav bar, search overlay, tab switcher cards,
        // etc.) will show blurred behind them via Haze.
        Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {

            if (ui.screen != BrowserScreen.TabSwitcher) {
                MeshBackground()

                // Weather overlay: tint + fog + particles + frost
                // Dynamic theme only — static themes show clean backgrounds
                if (ui.theme == OBThemePreset.Dynamic) {
                    WeatherOverlayLayer()
                }
            }

            // Persistent WebView stack (ALWAYS mounted so YouTube & tab states are NEVER lost)
            PersistentWebViewStack(viewModel = viewModel)

            AnimatedContent(
                targetState   = ui.screen,
                transitionSpec = {
                    val isOpeningFullScreen = targetState in listOf(BrowserScreen.Bookmarks, BrowserScreen.History, BrowserScreen.Downloads, BrowserScreen.Settings, BrowserScreen.Passwords, BrowserScreen.NewsHub)
                    val isClosingFullScreen = initialState in listOf(BrowserScreen.Bookmarks, BrowserScreen.History, BrowserScreen.Downloads, BrowserScreen.Settings, BrowserScreen.Passwords, BrowserScreen.NewsHub)
                    val iosCurve  = com.orbit.browser.ui.animations.OBEasing.IosCurve
                    val iosSpringInt = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.85f, stiffness = 300f)

                    if (isOpeningFullScreen) {
                        (slideInVertically(initialOffsetY = { it }, animationSpec = tween(280, easing = iosCurve)) + fadeIn(tween(180, easing = iosCurve))) togetherWith
                        fadeOut(tween(160, easing = iosCurve))
                    } else if (isClosingFullScreen) {
                        fadeIn(tween(180, easing = iosCurve)) togetherWith
                        (slideOutVertically(targetOffsetY = { it }, animationSpec = tween(260, easing = iosCurve)) + fadeOut(tween(160, easing = iosCurve)))
                    } else if (targetState == BrowserScreen.TabSwitcher) {
                        // Open Tab Manager: Physics shrink-to-grid transition (380ms, dampingRatio 0.82, stiffness 320)
                        val openSpringFloat = com.orbit.browser.ui.animations.OBSpring.TabManagerOpen
                        (scaleIn(initialScale = 0.90f, animationSpec = openSpringFloat) + fadeIn(tween(220, easing = iosCurve))) togetherWith
                        (scaleOut(targetScale = 0.92f, animationSpec = openSpringFloat) + fadeOut(tween(200, easing = iosCurve)))
                    } else if (initialState == BrowserScreen.TabSwitcher) {
                        // Select Tab: Physics expand-from-grid transition
                        val openSpringFloat = com.orbit.browser.ui.animations.OBSpring.TabManagerOpen
                        (scaleIn(initialScale = 0.92f, animationSpec = openSpringFloat) + fadeIn(tween(220, easing = iosCurve))) togetherWith
                        (scaleOut(targetScale = 0.90f, animationSpec = openSpringFloat) + fadeOut(tween(200, easing = iosCurve)))
                    } else if (targetState == BrowserScreen.Browser) {
                        // Open new site or click Next button: Slide in Right to Left (iOS style steady & gradual)
                        (slideInHorizontally(initialOffsetX = { it }, animationSpec = iosSpringInt) + fadeIn(tween(180, easing = iosCurve))) togetherWith
                        (slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = iosSpringInt) + fadeOut(tween(180, easing = iosCurve)))
                    } else if (initialState == BrowserScreen.Browser && targetState == BrowserScreen.Home) {
                        // Perform Back gesture/button to Home: Slide out Right until disappear (iOS style steady & gradual)
                        (slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = iosSpringInt) + fadeIn(tween(180, easing = iosCurve))) togetherWith
                        (slideOutHorizontally(targetOffsetX = { it }, animationSpec = iosSpringInt) + fadeOut(tween(180, easing = iosCurve)))
                    } else {
                        fadeIn(tween(200, easing = iosCurve)) togetherWith
                        (slideOutVertically(targetOffsetY = { it }, animationSpec = tween(240, easing = iosCurve)) + fadeOut(tween(160, easing = iosCurve)))
                    }
                },
                label = "screen_transition",
            ) { screen ->
                when (screen) {
                    BrowserScreen.Home    -> {
                        if (activeTab?.isPrivate == true) {
                            IncognitoHomeScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                        } else {
                            HomeScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize(), scrollState = homeScrollState)
                        }
                    }
                    BrowserScreen.Browser -> BrowserScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    BrowserScreen.TabSwitcher -> TabSwitcherScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    BrowserScreen.Bookmarks -> BookmarksScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize(), lazyListState = bookmarksListState)
                    BrowserScreen.History   -> HistoryScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize(), lazyListState = historyListState)
                    BrowserScreen.Downloads -> DownloadsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize(), lazyListState = downloadsListState)
                    BrowserScreen.Settings  -> SettingsScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize(), lazyListState = settingsListState)
                    BrowserScreen.Passwords -> PasswordVaultScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                    BrowserScreen.NewsHub   -> NewsHubScreen(viewModel = viewModel, modifier = Modifier.fillMaxSize())
                }
            }
        }

        SearchOverlay(viewModel = viewModel, visible = ui.searchOpen)
        CustomisationPanel(viewModel = viewModel, visible = ui.customPanelOpen)
        FindInPageBar(viewModel = viewModel, visible = ui.findInPageOpen, onFindNext = { forward -> })
        QrCodeModal(viewModel = viewModel, visible = ui.qrModalOpen)
        ReaderModeView(
            viewModel   = viewModel,
            visible     = ui.readerOpen,
            title       = ui.readerTitle,
            byline      = ui.readerByline,
            contentHtml = ui.readerContent,
        )

        AnimatedVisibility(
            visible  = ui.menuOpen,
            enter    = com.orbit.browser.ui.animations.OBMotion.menuEnter,
            exit     = com.orbit.browser.ui.animations.OBMotion.menuExit,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 80.dp, end = 16.dp),
        ) {
            ThreeDotMenu(viewModel = viewModel)
        }

        AnimatedVisibility(
            visible  = !isFullScreenDestination,
            enter    = slideInVertically(initialOffsetY = { it }, animationSpec = tween(260, easing = figmaSpring)) + fadeIn(tween(180, easing = figmaEase)),
            exit     = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220, easing = figmaEase)) + fadeOut(tween(160, easing = figmaEase)),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 14.dp),
        ) {
            IslandNavBar(viewModel = viewModel)
        }

        // ── TOAST (App.tsx lines 1777–1789) ──────────────────────────────────
        val toastMsg = ui.toastMessage
        AnimatedVisibility(
            visible  = toastMsg != null,
            enter    = slideInVertically(initialOffsetY = { 16 }, animationSpec = tween(220, easing = figmaSpring)) + fadeIn(tween(160, easing = figmaEase)),
            exit     = slideOutVertically(targetOffsetY = { 16 }, animationSpec = tween(160, easing = figmaEase)) + fadeOut(tween(140, easing = figmaEase)),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 108.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (theme.isDark) Color(0xFF121422).copy(alpha = 0.97f) else Color(0xFFF0F4FF).copy(alpha = 0.97f))
                    .border(1.dp, theme.glass.glassBorder2, RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text(
                    text       = toastMsg ?: "",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color      = theme.glass.tx2Color,
                )
            }
        }

        // ── ONBOARDING OVERLAY ───────────────────────────────────────────────
        AnimatedVisibility(
            visible  = ui.showOnboarding,
            enter    = fadeIn(animationSpec = tween(350)),
            exit     = fadeOut(animationSpec = tween(300)) + slideOutVertically(targetOffsetY = { -it / 4 }),
            modifier = Modifier.fillMaxSize(),
        ) {
            com.orbit.browser.ui.screens.OnboardingScreen(
                onComplete = { preset, mode, weatherFx ->
                    viewModel.completeOnboarding(preset, mode, weatherFx)
                }
            )
        }

        // ── FULLSCREEN VIDEO OVERLAY ───────────────────────────────────────
        val customVideoView = ui.customVideoView
        if (customVideoView != null) {
            val context = LocalContext.current
            val activity = context as? Activity
            DisposableEffect(customVideoView) {
                val window = activity?.window
                val origOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

                if (window != null) {
                    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
                onDispose {
                    activity?.requestedOrientation = origOrientation
                    if (window != null) {
                        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                        insetsController.show(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.widget.FrameLayout(ctx).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            (customVideoView.parent as? android.view.ViewGroup)?.removeView(customVideoView)
                            addView(
                                customVideoView,
                                android.widget.FrameLayout.LayoutParams(
                                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
}

@Composable
private fun MeshBackground() {
    val theme = LocalOBTheme.current
    val isDark = theme.isDark

    // Solid base: pure white for light mode (visible on most of screen), pure black for dark mode
    val baseBg  = if (isDark) Color(0xFF05060C) else Color(0xFFFFFFFF)
    val orb1Clr = theme.effectiveA1
    val orb2Clr = theme.effectiveA2

    Box(modifier = Modifier.fillMaxSize().background(baseBg)) {
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer()) {
            val w = size.width
            val h = size.height

            // 1. Top-Left Corner Gradient (Main Color - Equal Strength, shifted 10% down)
            // Radiates smoothly starting 10% down from top-left corner
            val r1 = w * 1.15f
            val maxAlpha = if (isDark) 0.38f else 0.45f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to orb1Clr.copy(alpha = maxAlpha),
                        0.35f to orb1Clr.copy(alpha = maxAlpha * 0.55f),
                        0.70f to orb1Clr.copy(alpha = maxAlpha * 0.18f),
                        1.0f to Color.Transparent,
                    ),
                    center = androidx.compose.ui.geometry.Offset(0f, h * 0.10f),
                    radius = r1,
                ),
                radius = r1,
                center = androidx.compose.ui.geometry.Offset(0f, h * 0.10f),
            )

            // 2. Bottom-Right Corner Gradient (Secondary Color - Exact Same Strength as Top-Left!)
            // Radiates smoothly from bottom-right (w, h) across lower right section
            val r2 = w * 1.15f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to orb2Clr.copy(alpha = maxAlpha),
                        0.35f to orb2Clr.copy(alpha = maxAlpha * 0.55f),
                        0.70f to orb2Clr.copy(alpha = maxAlpha * 0.18f),
                        1.0f to Color.Transparent,
                    ),
                    center = androidx.compose.ui.geometry.Offset(w, h),
                    radius = r2,
                ),
                radius = r2,
                center = androidx.compose.ui.geometry.Offset(w, h),
            )
        }
    }
}

@Composable
private fun ThreeDotMenu(viewModel: BrowserViewModel) {
    val ui      by viewModel.ui.collectAsState()
    val theme   = LocalOBTheme.current
    val g       = theme.glass
    val isDark  = theme.isDark
    val border  = g.glassBorder2
    val context = LocalContext.current

    val menuBg = if (isDark) Color(0xFF080A14).copy(alpha = 0.88f) else Color(0xFFFFFFFF).copy(alpha = 0.85f)

    Box(
        modifier = Modifier
            .width(252.dp)
            .frostedGlass(isDark = isDark, shape = RoundedCornerShape(28.dp), blurRadius = 32.dp)
            .padding(vertical = 4.dp),
    ) {
        Column {
            // Dark / Light Mode toggle row (App.tsx lines 1478–1547)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = { viewModel.toggleDarkMode() },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val chipBg = if (isDark)
                        Brush.linearGradient(listOf(Color(0x336478FF), Color(0x1F3C50C8)))
                    else
                        Brush.linearGradient(listOf(Color(0x40FFC832), Color(0x26FFA014)))
                    val chipBorder = if (isDark) Color(0x33788CFF) else Color(0x4DFFB41E)

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(chipBg)
                            .border(1.dp, chipBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = g.txColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text       = if (isDark) "Dark Mode" else "Light Mode",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = g.txColor,
                        )
                        Text(
                            text     = if (isDark) "Switch to light" else "Switch to dark",
                            fontSize = 10.sp,
                            color    = g.tx2Color,
                        )
                    }
                }

                // Animated Toggle Switch (50x28dp)
                val trackGradient = if (isDark)
                    Brush.horizontalGradient(listOf(Color(0xFF1A6FFF), Color(0xFF7C3AED)))
                else
                    Brush.horizontalGradient(listOf(Color(0xFFF5A623), Color(0xFFFFD200)))

                val knobOffset by animateDpAsState(
                    targetValue   = if (isDark) 25.dp else 3.dp,
                    animationSpec = spring(dampingRatio = 0.68f, stiffness = 400f),
                    label         = "mode_knob_offset",
                )

                Box(
                    modifier = Modifier
                        .size(width = 50.dp, height = 28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(trackGradient),
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = knobOffset, y = 3.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF1A6FFF) else Color(0xFFF5A623),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = border, thickness = 1.dp)

            // Menu Items List
            data class MenuItem(val icon: androidx.compose.ui.graphics.vector.ImageVector?, val label: String, val action: () -> Unit)
            val items = listOf(
                MenuItem(Icons.Default.Add, "New Tab", { viewModel.newTab() }),
                MenuItem(Icons.Default.Security, "New Incognito", { viewModel.newTab(isPrivate = true) }),
                MenuItem(null, "", {}),
                MenuItem(Icons.Default.BookmarkBorder, "Bookmarks", { viewModel.openBookmarks() }),
                MenuItem(Icons.Default.History, "History", { viewModel.openHistory() }),
                MenuItem(Icons.Default.FileDownload, "Downloads", { viewModel.openDownloads() }),
                MenuItem(Icons.Default.Search, "Find in Page", { viewModel.openFindInPage() }),
                MenuItem(Icons.Default.QrCodeScanner, "QR Code", { viewModel.openQrModal() }),
                MenuItem(if (ui.isDesktopSite) Icons.Default.Smartphone else Icons.Default.Computer, if (ui.isDesktopSite) "Mobile Site" else "Desktop Site", { viewModel.toggleDesktopSite() }),
                MenuItem(null, "", {}),
                MenuItem(Icons.Default.Share, "Share", { viewModel.openShare() }),
                MenuItem(Icons.Default.Settings, "Settings", { viewModel.openSettings() }),
                MenuItem(Icons.Default.Palette, "Customise Orbit", { viewModel.openCustom() }),
            )

            items.forEach { item ->
                if (item.icon == null) {
                    HorizontalDivider(
                        modifier  = Modifier.padding(vertical = 2.dp),
                        color     = border,
                        thickness = 0.5.dp,
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = LocalIndication.current,
                                onClick           = {
                                    viewModel.closeMenu()
                                    item.action()
                                },
                            )
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = g.txColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text       = item.label,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = g.txColor,
                        )
                    }
                }
            }
        }
    }
}
