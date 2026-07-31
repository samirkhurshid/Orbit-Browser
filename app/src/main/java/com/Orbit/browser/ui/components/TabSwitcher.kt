package com.orbit.browser.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.orbit.browser.browser.tabs.OBTab
import com.orbit.browser.ui.TabMode
import com.orbit.browser.ui.theme.LocalOBTheme
import com.orbit.browser.ui.glass.frostedGlass
import com.orbit.browser.ui.glass.LocalHazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

// ═══════════════════════════════════════════════════════════════════════════
// TAB SWITCHER OVERLAY — Taller Cards & Tab Grouping System
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun TabSwitcherScreen(
    viewModel: com.orbit.browser.ui.BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.ui.collectAsState()
    val tabs by viewModel.tabManager.tabs.collectAsState()
    val activeTabId by viewModel.tabManager.activeTabId.collectAsState()

    TabSwitcherContent(
        tabs             = tabs,
        activeTabId      = activeTabId,
        tabMode          = ui.tabMode,
        onClose          = { viewModel.closeTabs() },
        onSelectTab      = { viewModel.switchTab(it) },
        onCloseTab       = { viewModel.closeTab(it) },
        onCloseAll       = { viewModel.closeAllTabs() },
        onNewTab         = { viewModel.newTab() },
        onNewPrivateTab  = { viewModel.newTab(isPrivate = true) },
        onTabModeChanged = { viewModel.setTabMode(it) },
        onCreateGroup    = { name, color, ids -> viewModel.createTabGroup(name, color, ids) },
        onAssignGroup    = { tabId, groupName -> viewModel.assignTabToGroup(tabId, groupName) },
        onCloseGroup     = { groupName -> viewModel.closeTabGroup(groupName) },
        modifier         = modifier,
    )
}

@Composable
private fun TabSwitcherContent(
    tabs: List<OBTab>,
    activeTabId: String,
    tabMode: TabMode,
    onClose: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCloseAll: () -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onTabModeChanged: (TabMode) -> Unit,
    onCreateGroup: (name: String, colorHex: String, tabIds: List<String>) -> Unit,
    onAssignGroup: (tabId: String, groupName: String?) -> Unit,
    onCloseGroup: (groupName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme   = LocalOBTheme.current
    val g       = theme.glass
    val isDark  = theme.isDark
    val a1      = theme.effectiveA1
    val context = LocalContext.current

    val normalTabs  = remember(tabs) { tabs.filter { !it.isPrivate } }
    val privateTabs = remember(tabs) { tabs.filter { it.isPrivate } }

    var selectedGroupFilter by remember { mutableStateOf<String?>("All") }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var tabToAssignGroup by remember { mutableStateOf<OBTab?>(null) }

    // Distinct groups in normal tabs
    val activeGroups = remember(normalTabs) {
        normalTabs.mapNotNull { it.groupName }.distinct()
    }

    val screenBg = if (isDark) Color(0xFF060814) else Color(0xFFEAF0FB)
    val hazeState = LocalHazeState.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(state = hazeState) {
                        blurRadius = 40.dp
                        backgroundColor = screenBg
                        tints = listOf(HazeTint(screenBg.copy(alpha = 0.72f)))
                        noiseFactor = 0f
                    }
                } else {
                    Modifier.background(screenBg)
                }
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // Header (Matches exact screenshot layout: Title "Tabs", subtitle "X open · Y incognito", top-right circular buttons ✕ and ⋯)
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 14.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text          = "Tabs",
                        fontSize      = 28.sp,
                        fontWeight    = FontWeight.Black,
                        letterSpacing = (-0.7).sp,
                        color         = g.txColor,
                    )
                    Text(
                        text       = "${normalTabs.size} open · ${privateTabs.size} incognito",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color      = g.tx2Color,
                        modifier   = Modifier.padding(top = 3.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Close button ✕
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .frostedGlass(isDark = isDark, shape = CircleShape, blurRadius = 20.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = LocalIndication.current,
                                onClick           = { onClose() },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", fontSize = 15.sp, color = g.txColor, fontWeight = FontWeight.Bold)
                    }

                    // More options button ⋯ with dropdown menu
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .frostedGlass(isDark = isDark, shape = CircleShape, blurRadius = 20.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = LocalIndication.current,
                                    onClick           = { menuExpanded = true },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("⋯", fontSize = 15.sp, color = g.txColor, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded         = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier         = Modifier
                                .background(if (isDark) Color(0xFF0F1122).copy(alpha = 0.95f) else Color(0xFFF5F7FF).copy(alpha = 0.95f))
                                .border(0.5.dp, g.glassBorder2, RoundedCornerShape(12.dp)),
                        ) {
                            DropdownMenuItem(
                                text = { Text("📁 New Tab Group", color = a1, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    menuExpanded = false
                                    showCreateGroupDialog = true
                                },
                            )
                            HorizontalDivider(color = g.glassBorder2.copy(alpha = 0.4f))
                            DropdownMenuItem(
                                text = { Text("Add new tab", color = g.txColor, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    menuExpanded = false
                                    onNewTab()
                                },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = g.txColor, modifier = Modifier.size(18.dp)) },
                            )
                            DropdownMenuItem(
                                text = { Text("Add new incognito tab", color = Color(0xFFC084FC), fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    menuExpanded = false
                                    onNewPrivateTab()
                                },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(18.dp)) },
                            )
                            DropdownMenuItem(
                                text = { Text("Close all tabs", color = Color(0xFFFF4D6D), fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    menuExpanded = false
                                    onCloseAll()
                                    Toast.makeText(context, "All tabs closed", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = { Text("✕", color = Color(0xFFFF4D6D)) },
                            )
                        }
                    }
                }
            }

            // Tab Group Filter Bar (Visible in Normal Mode when groups exist or to create a group)
            if (tabMode == TabMode.Normal && (activeGroups.isNotEmpty() || normalTabs.size >= 2)) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item {
                        val isSelected = selectedGroupFilter == "All"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) Brush.linearGradient(listOf(a1, theme.effectiveA2)) else androidx.compose.ui.graphics.SolidColor(g.glassBg2))
                                .border(1.dp, if (isSelected) Color.Transparent else g.glassBorder, RoundedCornerShape(50))
                                .clickable { selectedGroupFilter = "All" }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All (${normalTabs.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else g.txColor
                            )
                        }
                    }

                    items(activeGroups) { groupName ->
                        val groupTabs = normalTabs.filter { it.groupName == groupName }
                        val isSelected = selectedGroupFilter == groupName
                        val colorHex = groupTabs.firstOrNull()?.groupColor ?: "#3B82F6"
                        val groupColor = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { a1 }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) groupColor else groupColor.copy(alpha = 0.15f))
                                .border(1.dp, groupColor.copy(alpha = 0.4f), RoundedCornerShape(50))
                                .clickable { selectedGroupFilter = groupName }
                                .padding(horizontal = 13.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else groupColor)
                                )
                                Text(
                                    text = "$groupName (${groupTabs.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else g.txColor
                                )
                            }
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(g.glassBg2)
                                .border(1.dp, g.glassBorder, RoundedCornerShape(50))
                                .clickable { showCreateGroupDialog = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = a1)
                                Text("New Group", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = a1)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // Grid sliding transition
            val tabGridEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
            AnimatedContent(
                targetState = tabMode,
                transitionSpec = {
                    if (targetState == TabMode.Private) {
                        (slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(280, easing = tabGridEasing)) + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(280, easing = tabGridEasing)) + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(280, easing = tabGridEasing)) + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(280, easing = tabGridEasing)) + fadeOut(tween(200)))
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                label    = "tab_grid_transition",
            ) { mode ->
                val baseTabs = if (mode == TabMode.Private) privateTabs else normalTabs
                val gridTabs = remember(baseTabs, selectedGroupFilter, mode) {
                    if (mode == TabMode.Normal && selectedGroupFilter != null && selectedGroupFilter != "All") {
                        baseTabs.filter { it.groupName == selectedGroupFilter }
                    } else baseTabs
                }

                LazyVerticalGrid(
                    columns               = GridCells.Fixed(2),
                    modifier              = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalArrangement   = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding        = PaddingValues(top = 8.dp, bottom = 120.dp),
                ) {
                    itemsIndexed(gridTabs, key = { _, tab -> tab.id }) { index, tab ->
                        TabCard(
                            tab      = tab,
                            index    = index,
                            isActive = tab.id == activeTabId,
                            onClick  = { onSelectTab(tab.id) },
                            onClose  = { onCloseTab(tab.id) },
                            onLongClick = { tabToAssignGroup = tab },
                        )
                    }
                    if (mode == TabMode.Private) {
                        if (privateTabs.isEmpty()) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    ShieldIcon(color = Color(0xFFC084FC).copy(alpha = 0.5f), size = 36.dp)
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text       = "No incognito tabs",
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = Color(0xFFC084FC).copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }
                        item {
                            NewPrivateTabCard(onClick = onNewPrivateTab)
                        }
                    } else {
                        item {
                            NewTabCard(onClick = onNewTab)
                        }
                    }
                }
            }
        }
    }

    // Modal: Create Tab Group
    if (showCreateGroupDialog) {
        CreateTabGroupModal(
            normalTabs = normalTabs,
            onDismiss  = { showCreateGroupDialog = false },
            onConfirm  = { name, colorHex, selectedIds ->
                onCreateGroup(name, colorHex, selectedIds)
                showCreateGroupDialog = false
            }
        )
    }

    // Modal: Assign Tab to Group Options
    if (tabToAssignGroup != null) {
        val currentTab = tabToAssignGroup!!
        AssignGroupSheet(
            tab          = currentTab,
            activeGroups = activeGroups,
            onDismiss    = { tabToAssignGroup = null },
            onAssign     = { groupName ->
                onAssignGroup(currentTab.id, groupName)
                tabToAssignGroup = null
            },
            onCreateNew  = {
                tabToAssignGroup = null
                showCreateGroupDialog = true
            }
        )
    }
}

@Composable
private fun HomeSvgIcon(color: Color, size: androidx.compose.ui.unit.Dp = 18.dp) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val p = Path().apply {
            val sx = this@Canvas.size.width / 24f
            val sy = this@Canvas.size.height / 24f
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
            quadraticTo(12 * sx, 2.2f * sy, 9.5f * sx, 4.4f * sy)
            lineTo(4 * sx, 9.2f * sy)
            cubicTo(3.4f * sx, 9.8f * sy, 3 * sx, 10.6f * sy, 3 * sx, 11.5f * sy)
            close()
        }
        drawPath(path = p, color = color, style = androidx.compose.ui.graphics.drawscope.Fill)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TALLER TAB CARD (Matching Reference Image Design: 225dp height)
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabCard(
    tab:         OBTab,
    index:       Int = 0,
    isActive:    Boolean,
    onClick:     () -> Unit,
    onClose:     () -> Unit,
    onLongClick: () -> Unit,
) {
    val theme  = LocalOBTheme.current
    val g      = theme.glass
    val isDark = theme.isDark
    val a1     = theme.effectiveA1

    val isHomeTab = tab.url.isBlank() || tab.url == "about:blank" || tab.title == "Home" || tab.displayUrl.isBlank()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(tab.id) {
        kotlinx.coroutines.delay(com.orbit.browser.ui.animations.staggerDelay(index, baseDelay = 20, maxDelay = 120).toLong())
        visible = true
    }

    val cardScale by animateFloatAsState(
        targetValue   = if (visible) 1f else 0.88f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 320f),
        label         = "tabcard_scale",
    )
    val cardEntryAlpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(220, easing = com.orbit.browser.ui.animations.OBEasing.IosCurve),
        label         = "tabcard_alpha",
    )

    val groupColor = remember(tab.groupColor) {
        if (!tab.groupColor.isNullOrBlank()) {
            try { Color(android.graphics.Color.parseColor(tab.groupColor)) } catch (e: Exception) { a1 }
        } else null
    }

    val cardBackground = if (isActive) {
        Brush.linearGradient(listOf(a1, theme.effectiveA2))
    } else {
        androidx.compose.ui.graphics.SolidColor(if (isDark) Color(0xFF101322) else Color(0xFFE2E8F5))
    }

    val scope = rememberCoroutineScope()
    var offsetX by remember { mutableStateOf(0f) }
    var isDismissed by remember { mutableStateOf(false) }

    val animatedOffsetX by animateFloatAsState(
        targetValue   = if (isDismissed) (if (offsetX >= 0) 1000f else -1000f) else offsetX,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label         = "swipe_offset",
        finishedListener = {
            if (isDismissed) {
                onClose()
            }
        }
    )

    val cardAlpha = (cardEntryAlpha * (1f - (kotlin.math.abs(animatedOffsetX) / 450f))).coerceIn(0f, 1f)

    val activeBezelBorder = if (isActive) {
        Modifier
    } else {
        Modifier.border(
            width = 1.dp,
            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
            shape = RoundedCornerShape(22.dp)
        )
    }

    Box(
        modifier = Modifier
            .scale(cardScale)
            .fillMaxWidth()
            .height(260.dp) // Taller height matching screenshot ratio
            .graphicsLayer {
                translationX = animatedOffsetX
                alpha        = cardAlpha
            }
            .pointerInput(tab.id) {
                detectHorizontalDragGestures(
                    onDragCancel = { offsetX = 0f },
                    onDragEnd = {
                        if (kotlin.math.abs(offsetX) > 110f) {
                            isDismissed = true
                        } else {
                            offsetX = 0f
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            }
            .clip(RoundedCornerShape(22.dp))
            .background(cardBackground)
            .then(activeBezelBorder)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = {
                    if (kotlin.math.abs(offsetX) < 15f) {
                        onClick()
                    }
                },
            ),
    ) {
        Column(modifier = Modifier.padding(top = 6.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)) {
            // Card Header Bar (Favicon + Site Title + Circular Grey Close Button)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
            ) {
                // Favicon / Home Icon
                if (isHomeTab) {
                    HomeSvgIcon(
                        color = if (isActive) Color.White else (if (tab.isPrivate) Color(0xFFC084FC) else (groupColor ?: a1)),
                        size  = 16.dp,
                    )
                } else if (tab.favicon != null) {
                    Image(
                        bitmap             = tab.favicon.asImageBitmap(),
                        contentDescription = null,
                        modifier           = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(5.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background((if (isActive) Color.White else if (tab.isPrivate) Color(0xFFC084FC) else Color.Gray).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (tab.isPrivate) Icons.Default.Security else Icons.Default.Public,
                            contentDescription = null,
                            tint = if (isActive) Color.White else (if (tab.isPrivate) Color(0xFFC084FC) else g.txColor),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Title
                Text(
                    text       = if (isHomeTab) (if (tab.isPrivate) "Incognito Home" else "Home") else tab.title.ifBlank { "New Tab" },
                    fontSize   = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    color      = if (isActive) Color.White else (if (tab.isPrivate) Color(0xFFC084FC) else g.txColor),
                    modifier   = Modifier.weight(1f),
                )

                Spacer(Modifier.width(4.dp))

                // Circular grey close button ✕ (Matching reference screenshot)
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onClose,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", fontSize = 10.sp, color = g.txColor, fontWeight = FontWeight.Bold)
                }
            }

            // Taller Inner Preview Area (Reduced left, right & bottom bezels for maximum preview size)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp, topStart = 14.dp, topEnd = 14.dp))
                    .background(
                        if (tab.isPrivate) Color(0xFF1E152A)
                        else if (isHomeTab) (if (isDark) Color(0xFF090B18) else Color.White)
                        else Color.White
                    )
                    .border(
                        0.5.dp,
                        if (tab.isPrivate) Color(0xFFC084FC).copy(alpha = 0.2f)
                        else if (isDark) Color.White.copy(alpha = 0.12f)
                        else Color.Black.copy(alpha = 0.08f),
                        RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp, topStart = 14.dp, topEnd = 14.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isHomeTab) {
                    // Home Page Preview State (Black in Dark Mode, White in Light Mode)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background((if (tab.isPrivate) Color(0xFFC084FC) else if (isDark) Color.White else (groupColor ?: a1)).copy(alpha = 0.14f))
                                .border(1.dp, (if (tab.isPrivate) Color(0xFFC084FC) else if (isDark) Color.White else (groupColor ?: a1)).copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            HomeSvgIcon(
                                color = if (tab.isPrivate) Color(0xFFC084FC) else if (isDark) Color.White else (groupColor ?: a1),
                                size  = 24.dp,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text       = if (tab.isPrivate) "Incognito" else "Orbit Home",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = if (tab.isPrivate) Color(0xFFC084FC).copy(alpha = 0.8f) else if (isDark) Color.White.copy(alpha = 0.9f) else g.tx2Color,
                        )
                    }
                } else if (tab.thumbnail != null) {
                    Image(
                        bitmap             = tab.thumbnail.asImageBitmap(),
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        alignment          = Alignment.TopCenter,
                        modifier           = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (tab.isPrivate) Icons.Default.Security else Icons.Default.Public,
                            contentDescription = null,
                            tint = (if (tab.isPrivate) Color(0xFFC084FC) else g.txColor).copy(alpha = 0.3f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Group Badge Overlay on card
                if (!tab.groupName.isNullOrBlank() && groupColor != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(20))
                            .background(groupColor.copy(alpha = 0.90f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = tab.groupName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                        )
                    }
                }
            }
        }

        // Active indicator badge
        if (isActive) {
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.BottomEnd)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(groupColor ?: Color(0xFF00DDA0)),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TALLER NEW TAB CARDS (Matching Reference Screenshot Dashed Style)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun NewTabCard(onClick: () -> Unit) {
    val theme  = LocalOBTheme.current
    val g      = theme.glass
    val isDark = theme.isDark

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.45f))
            .border(
                width = 1.5.dp,
                color = if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f),
                shape = RoundedCornerShape(22.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", fontSize = 24.sp, color = g.txColor, fontWeight = FontWeight.Normal)
            }
            Spacer(Modifier.height(10.dp))
            Text("New Tab", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = g.txColor)
        }
    }
}

@Composable
private fun NewPrivateTabCard(onClick: () -> Unit) {
    val violet = Color(0xFFC084FC)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF501478).copy(alpha = 0.18f))
            .border(1.5.dp, violet.copy(alpha = 0.30f), RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7828B4).copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", fontSize = 24.sp, color = violet, fontWeight = FontWeight.Normal)
            }
            Spacer(Modifier.height(10.dp))
            Text("New Incognito", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = violet)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MODALS: Create Tab Group & Assign Sheet
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun CreateTabGroupModal(
    normalTabs: List<OBTab>,
    onDismiss: () -> Unit,
    onConfirm: (groupName: String, colorHex: String, selectedIds: List<String>) -> Unit,
) {
    val theme  = LocalOBTheme.current
    val g      = theme.glass
    val isDark = theme.isDark

    var groupName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#3B82F6") }
    val selectedTabIds = remember { mutableStateListOf<String>() }

    val presetColors = listOf("#3B82F6", "#8B5CF6", "#10B981", "#F59E0B", "#EC4899", "#06B6D4")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(26.dp))
                .border(1.dp, g.glassBorder2, RoundedCornerShape(26.dp)),
            color = if (isDark) Color(0xFF0D0F1D) else Color(0xFFFFFFFF),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "📁 Create Tab Group",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = g.txColor
                )

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name (e.g. Work, Research)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                // Color picker options
                Text("Group Color", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = g.tx2Color, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    presetColors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(if (isSelected) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Select tabs to include
                Text("Select Tabs to Include", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = g.tx2Color, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    normalTabs.forEach { tab ->
                        val isChecked = tab.id in selectedTabIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedTabIds.remove(tab.id)
                                    else selectedTabIds.add(tab.id)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) selectedTabIds.add(tab.id)
                                    else selectedTabIds.remove(tab.id)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = tab.title.ifBlank { "New Tab" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = g.txColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(g.glassBg2)
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = g.tx2Color)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.linearGradient(listOf(theme.effectiveA1, theme.effectiveA2)))
                            .clickable {
                                if (groupName.isNotBlank()) {
                                    onConfirm(groupName, selectedColor, selectedTabIds.toList())
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Create", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignGroupSheet(
    tab: OBTab,
    activeGroups: List<String>,
    onDismiss: () -> Unit,
    onAssign: (groupName: String?) -> Unit,
    onCreateNew: () -> Unit,
) {
    val theme  = LocalOBTheme.current
    val g      = theme.glass
    val isDark = theme.isDark

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, g.glassBorder2, RoundedCornerShape(24.dp)),
            color = if (isDark) Color(0xFF0F1122) else Color(0xFFFFFFFF),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "🏷️ Assign to Group",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = g.txColor
                )
                Text(
                    text = tab.title,
                    fontSize = 12.sp,
                    color = g.tx2Color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(Modifier.height(14.dp))

                if (tab.groupName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAssign(null) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🚫 Remove from Group", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF4D6D))
                    }
                    HorizontalDivider(color = g.glassBorder2.copy(alpha = 0.4f))
                }

                activeGroups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAssign(group) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("📁 $group", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = g.txColor)
                    }
                }

                HorizontalDivider(color = g.glassBorder2.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCreateNew() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✨ Create New Group", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = theme.effectiveA1)
                }
            }
        }
    }
}

@Composable
private fun ShieldIcon(color: Color, size: androidx.compose.ui.unit.Dp) {
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
