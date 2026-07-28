package com.orbit.browser.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.orbit.browser.ui.BrowserViewModel
import com.orbit.browser.ui.TabMode

@Composable
fun IslandNavBar(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
) {
    val ui           by viewModel.ui.collectAsState()
    val activeTab    by viewModel.tabManager.activeTab.collectAsState()
    val normalCount  by viewModel.tabManager.normalTabCount.collectAsState()
    val privateCount by viewModel.tabManager.privateTabCount.collectAsState()

    val tabCount = if (ui.tabMode == TabMode.Private) privateCount else normalCount

    val islandState = when {
        ui.screen == com.orbit.browser.ui.BrowserScreen.TabSwitcher || ui.tabsOpen -> IslandState.TabsOpen
        ui.searchOpen -> IslandState.Address
        else          -> IslandState.Default
    }

    OBIslandNavBar(
        uiState       = ui,
        activeTab     = activeTab,
        tabCount      = tabCount,
        islandState   = islandState,
        tabMode       = ui.tabMode,
        onTabModeChanged = { viewModel.setTabMode(it) },
        onIslandClick = { if (!ui.tabsOpen) viewModel.openSearch() },
        onBack        = { viewModel.goBack() },
        onForward     = { viewModel.goForward() },
        onHome        = { viewModel.goHome() },
        onTabs        = { viewModel.openTabs() },
        onMenu        = { viewModel.toggleMenu() },
        modifier      = modifier,
    )
}

