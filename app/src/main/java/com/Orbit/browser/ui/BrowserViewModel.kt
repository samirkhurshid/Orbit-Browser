package com.orbit.browser.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.browser.browser.engine.OBWebView
import com.orbit.browser.browser.tabs.TabManager
import com.orbit.browser.data.db.*
import com.orbit.browser.data.prefs.OBPreferences
import com.orbit.browser.security.adblock.AdBlocker
import com.orbit.browser.security.dns.SecureDnsResolver
import com.orbit.browser.security.vault.PasswordVaultRepository
import com.orbit.browser.security.vault.SavedLoginMeta
import com.orbit.browser.security.vault.VaultResult
import com.orbit.browser.ui.theme.OBThemePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import okhttp3.OkHttpClient
import com.orbit.browser.data.news.*

sealed interface BrowserCommand {
    object GoBack : BrowserCommand
    object GoForward : BrowserCommand
    object Refresh : BrowserCommand
    data class LoadUrl(val url: String, val clearHistory: Boolean = false) : BrowserCommand
}

enum class WeatherKind(val label: String) {
    Clear("Clear"),
    Cloudy("Cloudy"),
    Fog("Foggy"),
    Drizzle("Drizzle"),
    Rain("Rainy"),
    Thunderstorm("Thunderstorm"),
    Snow("Snowing");

    companion object {
        val Foggy = Fog
        val Rainy = Rain
        val Snowing = Snow
    }
}

data class WeatherState(
    val kind: WeatherKind = WeatherKind.Clear,
    val temp: Int? = 25,
    val feelsLike: Int = 25,
    val humidity: Int = 55,
    val windSpeed: Float = 12f,
    val maxTemp: Int = 28,
    val minTemp: Int = 21,
    val cityName: String = "Local Area",
    val hourlyForecast: List<com.orbit.browser.data.weather.HourlyForecast> = emptyList(),
    val loading: Boolean = false,
    val isDetailOpen: Boolean = false
) {
    val weatherKind: WeatherKind get() = kind
    val temperature: Float get() = (temp ?: 25).toFloat()
}

enum class TabMode { Normal, Private }

data class BrowserUiState(
    val screen: BrowserScreen         = BrowserScreen.Home,
    val previousScreen: BrowserScreen = BrowserScreen.Home,
    val searchOpen: Boolean           = false,
    val searchQuery: String       = "",
    val tabsOpen: Boolean         = false,
    val menuOpen: Boolean         = false,
    val secPanelOpen: Boolean     = false,
    val shareOpen: Boolean        = false,
    val customPanelOpen: Boolean  = false,
    val settingsOpen: Boolean     = false,
    val bookmarksOpen: Boolean    = false,
    val historyOpen: Boolean      = false,
    val downloadsOpen: Boolean    = false,
    val readerOpen: Boolean       = false,
    val readerTitle: String       = "",
    val readerByline: String      = "",
    val readerContent: String     = "",
    val findInPageOpen: Boolean   = false,
    val findInPageQuery: String   = "",
    val qrModalOpen: Boolean      = false,
    val isDesktopSite: Boolean    = false,
    val defaultSearchEngine: String = "Google",
    val textSizePercent: Int      = 100,
    val theme: OBThemePreset      = OBThemePreset.Dynamic,
    val isDarkMode: Boolean?      = null,    // null = follow system
    val showQuickAccess: Boolean  = true,
    val showPrivacyDash: Boolean  = true,
    val showFreqVisited: Boolean  = true,
    val showNewsFeed: Boolean     = true,
    val httpsOnly: Boolean        = true,
    val blockCookies: Boolean     = false,
    val savePasswords: Boolean    = true,
    val clearOnExit: Boolean      = false,
    val dohEnabled: Boolean       = true,
    val devMode: Boolean          = false,
    val findMatchCurrent: Int     = 0,
    val findMatchTotal: Int       = 0,
    val trackersBlocked: Int      = 0,
    val adsBlocked: Int           = 0,
    val searchSuggestions: List<String> = emptyList(),
    val historyResults: List<HistoryEntry> = emptyList(),
    val weatherState: WeatherState = WeatherState(),
    val tabMode: TabMode           = TabMode.Normal,
    val toastMessage: String?      = null,
    val manualTimeSlot: com.orbit.browser.ui.theme.TimeSlot? = null,
    val manualWeather: WeatherKind? = null,
    val showOnboarding: Boolean = false,
    val customVideoView: android.view.View? = null,
    val activeContextMenuElement: com.orbit.browser.browser.engine.OBContextMenuElement? = null,
    val previewPageUrl: String? = null,
    val previewImageUrl: String? = null,
    val showWeatherEffects: Boolean = false,
    val showDefaultBrowserPrompt: Boolean = false,
)

enum class BrowserScreen { Home, Browser, TabSwitcher, Bookmarks, History, Downloads, Settings, Passwords, NewsHub }

@HiltViewModel
class BrowserViewModel @Inject constructor(
    val tabManager: TabManager,
    val adBlocker: AdBlocker,
    private val prefs: OBPreferences,
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao,
    private val quickAccessDao: QuickAccessDao,
    private val frequentSiteDao: FrequentSiteDao,
    private val downloadDao: DownloadDao,
    private val searchSuggestionDao: SearchSuggestionDao,
    private val secureDnsResolver: SecureDnsResolver,
    private val okHttpClient: OkHttpClient,
    private val newsRepository: NewsRepository,
    private val newsPatternLearner: NewsPatternLearner,
    private val weatherRepository: com.orbit.browser.data.weather.WeatherRepository,
    private val passwordVaultRepository: PasswordVaultRepository,
) : ViewModel() {

    val savedLogins: StateFlow<List<SavedLoginMeta>> = passwordVaultRepository.savedLogins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Save a new credential. Caller (the vault screen) must already have a fresh biometric success. */
    suspend fun addPassword(site: String, siteDisplayUrl: String, username: String, password: String): VaultResult<Unit> =
        passwordVaultRepository.addLogin(site, siteDisplayUrl, username, password)

    fun deletePassword(id: Long) {
        viewModelScope.launch { passwordVaultRepository.deleteLogin(id) }
    }

    /** Decrypt a single password. Caller must already have a fresh biometric success for this action. */
    suspend fun revealPassword(id: Long): VaultResult<String> =
        passwordVaultRepository.revealPassword(id)

    suspend fun isSiteAlreadySaved(site: String, username: String): Boolean =
        passwordVaultRepository.existsForSite(site, username)

    private val _newsArticles = MutableStateFlow<List<RealNewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<RealNewsArticle>> = _newsArticles.asStateFlow()

    private var currentNewsPage = 1
    private val _isNewsLoadingMore = MutableStateFlow(false)
    val isNewsLoadingMore: StateFlow<Boolean> = _isNewsLoadingMore.asStateFlow()

    fun loadRealNews() {
        viewModelScope.launch {
            currentNewsPage = 1
            _newsArticles.value = newsRepository.getRealNews(page = 1)
        }
    }

    fun loadMoreNews() {
        if (_isNewsLoadingMore.value) return
        viewModelScope.launch {
            _isNewsLoadingMore.value = true
            val nextPage = currentNewsPage + 1
            val more = newsRepository.getRealNews(page = nextPage)
            if (more.isNotEmpty()) {
                currentNewsPage = nextPage
                val combined = (_newsArticles.value + more).distinctBy { it.title }
                _newsArticles.value = combined
            }
            _isNewsLoadingMore.value = false
        }
    }

    fun checkDefaultBrowserPrompt(context: Context) {
        viewModelScope.launch {
            val count = prefs.incrementLaunchCount()
            val dismissed = prefs.dismissedDefaultBrowserPrompt.first()
            val isDefault = isOrbitDefaultBrowser(context)

            if ((count == 2 || count == 3) && !dismissed && !isDefault) {
                // Mark as asked so whether user sets default or cancels, app NEVER asks again
                prefs.setDismissedDefaultBrowserPrompt(true)

                // Directly launch Android's native default browser system picker
                try {
                    val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
                        if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_BROWSER) && !roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_BROWSER)) {
                            roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_BROWSER)
                        } else {
                            Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                        }
                    } else {
                        Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun dismissDefaultBrowserPrompt(dontAskAgain: Boolean = true) {
        viewModelScope.launch {
            if (dontAskAgain) {
                prefs.setDismissedDefaultBrowserPrompt(true)
            }
            _ui.update { it.copy(showDefaultBrowserPrompt = false) }
        }
    }

    private fun isOrbitDefaultBrowser(context: Context): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
            val resolveInfo = context.packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName == context.packageName
        } catch (e: Exception) {
            false
        }
    }

    fun onNewsArticleClick(article: RealNewsArticle) {
        newsPatternLearner.recordArticleClick(NewsCategory.fromTag(article.category))
        navigate(article.url)
        loadRealNews()
    }

    private val _commands = MutableSharedFlow<BrowserCommand>(extraBufferCapacity = 64)
    val commands = _commands.asSharedFlow()

    fun goBack() {
        if (_ui.value.screen == BrowserScreen.Home) {
            return
        }
        _commands.tryEmit(BrowserCommand.GoBack)
    }

    fun goForward() {
        if (_ui.value.screen == BrowserScreen.Home) {
            val activeTab = tabManager.activeTab.value
            val activeUrl = activeTab?.url?.takeIf { it.isNotBlank() && it != "orbit://home" }
                ?: activeTab?.lastVisitedUrl?.takeIf { it.isNotBlank() && it != "orbit://home" }
            if (activeUrl != null) {
                _ui.update { it.copy(screen = BrowserScreen.Browser) }
                return
            }
        }
        _commands.tryEmit(BrowserCommand.GoForward)
    }

    fun refresh() {
        _commands.tryEmit(BrowserCommand.Refresh)
    }

    private val _ui = MutableStateFlow(
        BrowserUiState(
            isDarkMode = when (prefs.getSyncDarkModePref()) {
                "ALWAYS_DARK" -> true
                "ALWAYS_LIGHT" -> false
                else -> null
            }
        )
    )
    val ui: StateFlow<BrowserUiState> = _ui.asStateFlow()

    val bookmarks: StateFlow<List<Bookmark>> = bookmarkDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val quickAccessSites: StateFlow<List<QuickAccessSite>> = quickAccessDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val frequentSites: StateFlow<List<FrequentSite>> = frequentSiteDao.getTop()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentHistory: StateFlow<List<HistoryEntry>> = historyDao.getRecent(100)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val bookmarkList: StateFlow<List<Bookmark>> = bookmarkDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val downloadsList: StateFlow<List<Download>> = downloadDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        restoreTabsAndScreenFromPrefs()
        loadPreferences()
        observeTabsForPersistence()
        observeScreenForPersistence()
        initAdBlocker()
        seedDefaultQuickAccess()
        fetchWeather()
        loadRealNews()
    }

    private fun observeScreenForPersistence() = viewModelScope.launch {
        _ui.map { it.screen }.distinctUntilChanged().drop(1).collect { screen ->
            prefs.saveLastClosedScreen(screen.name)
        }
    }

    fun saveAppStateOnExit(context: android.content.Context) {
        viewModelScope.launch {
            val currentScreen = _ui.value.screen
            prefs.saveLastClosedScreen(currentScreen.name)

            val normalTabs = tabManager.tabs.value.filter { !it.isPrivate }
            if (normalTabs.isNotEmpty()) {
                val jsonArray = org.json.JSONArray()
                for (tab in normalTabs) {
                    val obj = org.json.JSONObject().apply {
                        put("id", tab.id)
                        put("url", tab.url)
                        put("displayUrl", tab.displayUrl)
                        put("title", tab.title)
                        put("searchQuery", tab.searchQuery)
                    }
                    jsonArray.put(obj)
                }
                prefs.saveTabsState(jsonArray.toString(), tabManager.activeTabId.value)
            }
        }
    }

    private fun observeTabsForPersistence() = viewModelScope.launch {
        combine(tabManager.tabs, tabManager.activeTabId) { tabs, activeId ->
            tabs to activeId
        }.drop(1).collectLatest { (tabs, activeId) ->
            val normalTabs = tabs.filter { !it.isPrivate }
            if (normalTabs.isNotEmpty()) {
                val jsonArray = org.json.JSONArray()
                for (tab in normalTabs) {
                    val obj = org.json.JSONObject().apply {
                        put("id", tab.id)
                        put("url", tab.url)
                        put("displayUrl", tab.displayUrl)
                        put("title", tab.title)
                        put("searchQuery", tab.searchQuery)
                    }
                    jsonArray.put(obj)
                }
                prefs.saveTabsState(jsonArray.toString(), activeId)
            }
        }
    }

    private fun restoreTabsAndScreenFromPrefs() {
        val jsonStr = prefs.getSyncSavedTabsJson()
        val activeId = prefs.getSyncActiveTabId()
        if (!jsonStr.isNullOrBlank()) {
            try {
                val jsonArray = org.json.JSONArray(jsonStr)
                val list = mutableListOf<com.orbit.browser.browser.tabs.OBTab>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                    val url = obj.optString("url", "")
                    val displayUrl = obj.optString("displayUrl", url)
                    val title = obj.optString("title", "New Tab")
                    val searchQuery = obj.optString("searchQuery", "")
                    if (url.isNotBlank()) {
                        list.add(
                            com.orbit.browser.browser.tabs.OBTab(
                                id = id,
                                url = url,
                                displayUrl = displayUrl,
                                title = title,
                                searchQuery = searchQuery,
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) {
                    tabManager.restoreTabs(list, activeId)
                }
            } catch (_: Exception) {}
        }

        val lastScreenStr = prefs.getSyncLastClosedScreen()
        val savedScreen = try {
            if (!lastScreenStr.isNullOrBlank()) BrowserScreen.valueOf(lastScreenStr) else null
        } catch (_: Exception) { null }

        val activeTabObj = tabManager.activeTab.value
        val hasActiveSite = activeTabObj != null && activeTabObj.url.isNotBlank() && activeTabObj.url != "orbit://home"

        val targetScreen = when {
            savedScreen == BrowserScreen.Browser && hasActiveSite -> BrowserScreen.Browser
            savedScreen == BrowserScreen.Home -> BrowserScreen.Home
            savedScreen != null -> savedScreen
            hasActiveSite -> BrowserScreen.Browser
            else -> BrowserScreen.Home
        }

        _ui.update { it.copy(screen = targetScreen) }
    }

    private fun loadPreferences() = viewModelScope.launch {

        combine(
            prefs.theme,
            prefs.showQuickAccess,
            prefs.showPrivacyDash,
            prefs.showFreqVisited,
            prefs.showNewsFeed,
            prefs.searchEngine,
            prefs.httpsOnly,
            prefs.clearOnExit,
            prefs.savePasswords,
            prefs.dohEnabled,
            prefs.darkModePref,
            prefs.onboardingCompleted,
            prefs.showWeatherEffects,
        ) { args ->
            val theme = args[0] as OBThemePreset
            val qa = args[1] as Boolean
            val priv = args[2] as Boolean
            val freq = args[3] as Boolean
            val news = args[4] as Boolean
            val se = args[5] as String
            val ho = args[6] as Boolean
            val coe = args[7] as Boolean
            val sp = args[8] as Boolean
            val doh = args[9] as Boolean
            val darkPref = args[10] as String
            val onboardDone = args[11] as Boolean
            val weatherFx = args[12] as Boolean

            val isDark = when (darkPref) {
                "ALWAYS_DARK" -> true
                "ALWAYS_LIGHT" -> false
                else -> null
            }

            _ui.update {
                it.copy(
                    theme               = theme,
                    showQuickAccess     = qa,
                    showPrivacyDash     = priv,
                    showFreqVisited     = freq,
                    showNewsFeed        = news,
                    defaultSearchEngine = se,
                    httpsOnly           = ho,
                    clearOnExit         = coe,
                    savePasswords       = sp,
                    dohEnabled          = doh,
                    isDarkMode          = isDark,
                    showOnboarding      = !onboardDone,
                    showWeatherEffects  = weatherFx,
                )
            }
        }.collect()
    }

    fun completeOnboarding(themePreset: OBThemePreset, modePref: String, weatherEffectsEnabled: Boolean = true) {
        viewModelScope.launch {
            prefs.setTheme(themePreset)
            prefs.setDarkModePref(modePref)
            prefs.setShowWeatherEffects(weatherEffectsEnabled)
            prefs.setOnboardingCompleted(true)
            val isDark = when (modePref) {
                "ALWAYS_DARK" -> true
                "ALWAYS_LIGHT" -> false
                else -> null
            }
            _ui.update {
                it.copy(
                    showOnboarding     = false,
                    theme              = themePreset,
                    isDarkMode         = isDark,
                    showWeatherEffects = weatherEffectsEnabled,
                )
            }
        }
    }

    fun reopenOnboarding() {
        _ui.update { it.copy(showOnboarding = true, customPanelOpen = false, settingsOpen = false) }
    }

    private fun initAdBlocker() = viewModelScope.launch { adBlocker.initialize() }

    private fun seedDefaultQuickAccess() = viewModelScope.launch {
        if (quickAccessDao.getCount() == 0) {
            val defaults = listOf(
                QuickAccessSite(url = "https://google.com",     title = "Google",  emoji = "🔍", sortOrder = 0, isDefault = true),
                QuickAccessSite(url = "https://github.com",     title = "GitHub",  emoji = "🐙", sortOrder = 1, isDefault = true),
                QuickAccessSite(url = "https://youtube.com",    title = "YouTube", emoji = "▶️", sortOrder = 2, isDefault = true),
                QuickAccessSite(url = "https://gmail.com",      title = "Gmail",   emoji = "📧", sortOrder = 3, isDefault = true),
                QuickAccessSite(url = "https://maps.google.com",title = "Maps",    emoji = "🗺️", sortOrder = 4, isDefault = true),
                QuickAccessSite(url = "https://reddit.com",     title = "Reddit",  emoji = "📰", sortOrder = 5, isDefault = true),
                QuickAccessSite(url = "https://amazon.com",     title = "Amazon",  emoji = "🛒", sortOrder = 6, isDefault = true),
            )
            defaults.forEach { quickAccessDao.insert(it) }
        }
    }

    fun navigate(url: String, clearHistory: Boolean = false) {
        val fromHome = _ui.value.screen == BrowserScreen.Home
        val shouldClearHistory = clearHistory || fromHome
        val resolvedUrl = OBWebView.resolveUrl(url)
        val extractedQuery = OBWebView.extractSearchQueryFromUrl(resolvedUrl) ?: if (!url.startsWith("http://") && !url.startsWith("https://") && (url.contains(" ") || !url.contains("."))) url else ""

        _ui.update { it.copy(screen = BrowserScreen.Browser, searchOpen = false) }
        tabManager.updateActiveTab { tab ->
            tab.copy(
                url            = resolvedUrl,
                lastVisitedUrl = resolvedUrl,
                searchQuery    = extractedQuery.ifBlank { tab.searchQuery },
                displayUrl     = OBWebView.formatDisplayUrl(resolvedUrl, extractedQuery.ifBlank { tab.searchQuery }),
                isLoading      = true,
                loadProgress   = 0.05f,
                canGoBack      = if (shouldClearHistory) false else tab.canGoBack,
                canGoForward   = if (shouldClearHistory) false else tab.canGoForward,
            )
        }
        _commands.tryEmit(BrowserCommand.LoadUrl(resolvedUrl, clearHistory = shouldClearHistory))

        val isPrivate = tabManager.activeTab.value?.isPrivate == true
        if (!isPrivate) {
            viewModelScope.launch {
                val display = OBWebView.formatDisplayUrl(resolvedUrl, extractedQuery)
                val existing = historyDao.findByUrl(resolvedUrl)
                if (existing != null) historyDao.incrementVisit(resolvedUrl)
                else historyDao.insert(HistoryEntry(url = resolvedUrl, title = display))
                frequentSiteDao.insertIfAbsent(FrequentSite(url = resolvedUrl, title = display))
                frequentSiteDao.incrementVisit(resolvedUrl)
                newsPatternLearner.recordUrlVisit(resolvedUrl, extractedQuery ?: resolvedUrl)
            }
        }
    }

    fun goHome() {
        closeAll()
        _ui.update { it.copy(screen = BrowserScreen.Home) }
    }

    fun openSearch() {
        val fromHome = _ui.value.screen == BrowserScreen.Home
        val active   = tabManager.activeTab.value
        val fullUrl  = if (fromHome) "" else (
            active?.url?.takeIf { it.isNotBlank() && it != "orbit://home" && it != "about:blank" }
                ?: active?.searchQuery?.ifBlank { OBWebView.extractSearchQueryFromUrl(active.url) ?: "" }
                ?: ""
        )
        _ui.update { it.copy(searchOpen = true, searchQuery = fullUrl) }
        if (fullUrl.isNotBlank()) {
            onSearchQueryChanged(fullUrl)
        } else {
            onSearchQueryChanged("")
        }
    }
    fun closeSearch() = _ui.update { it.copy(searchOpen = false, searchQuery = "") }

    private fun openFullScreenDestination(target: BrowserScreen) {
        _ui.update { current ->
            val prev = if (current.screen in listOf(BrowserScreen.Bookmarks, BrowserScreen.History, BrowserScreen.Downloads, BrowserScreen.Settings)) {
                current.previousScreen
            } else {
                current.screen
            }
            current.copy(screen = target, previousScreen = prev, menuOpen = false, searchOpen = false, tabsOpen = false)
        }
    }

    fun goBackFromFullScreenDestination() {
        _ui.update { current ->
            val active = tabManager.activeTab.value
            val target = current.previousScreen
            val resolvedTarget = if (target == BrowserScreen.Browser && active?.url.isNullOrBlank()) {
                BrowserScreen.Home
            } else {
                target
            }
            current.copy(screen = resolvedTarget)
        }
    }

    fun openBookmarks()  = openFullScreenDestination(BrowserScreen.Bookmarks)
    fun closeBookmarks() = goBackFromFullScreenDestination()

    fun openHistory()  = openFullScreenDestination(BrowserScreen.History)
    fun closeHistory() = goBackFromFullScreenDestination()

    fun openDownloads()  = openFullScreenDestination(BrowserScreen.Downloads)
    fun closeDownloads() = goBackFromFullScreenDestination()

    fun openSettings()  = openFullScreenDestination(BrowserScreen.Settings)
    fun closeSettings() = goBackFromFullScreenDestination()

    fun openPasswords()  = openFullScreenDestination(BrowserScreen.Passwords)
    fun closePasswords() = goBackFromFullScreenDestination()

    fun openNewsHub()  = openFullScreenDestination(BrowserScreen.NewsHub)
    fun closeNewsHub() = goBackFromFullScreenDestination()



    fun toggleDesktopSite() {
        val next = !_ui.value.isDesktopSite
        _ui.update { it.copy(isDesktopSite = next, menuOpen = false) }
        showToast(if (next) "Desktop site requested" else "Mobile site requested")
    }

    fun openReaderMode(title: String, byline: String, contentHtml: String) {
        _ui.update { it.copy(
            readerOpen    = true,
            readerTitle   = title,
            readerByline  = byline,
            readerContent = contentHtml,
            menuOpen      = false,
        ) }
    }
    fun closeReaderMode() = _ui.update { it.copy(readerOpen = false) }

    fun openFindInPage()  = _ui.update { it.copy(findInPageOpen = true, findInPageQuery = "", menuOpen = false) }
    fun closeFindInPage() = _ui.update { it.copy(findInPageOpen = false, findInPageQuery = "") }
    fun onFindQueryChanged(query: String) = _ui.update { it.copy(findInPageQuery = query) }

    fun openQrModal()  = _ui.update { it.copy(qrModalOpen = true, shareOpen = false, menuOpen = false) }
    fun closeQrModal() = _ui.update { it.copy(qrModalOpen = false) }

    fun openNewTab(url: String) {
        if (url.isBlank()) return
        tabManager.openNewTab(url = url)
        _ui.update { it.copy(screen = BrowserScreen.Browser, menuOpen = false, searchOpen = false, tabsOpen = false) }
        _commands.tryEmit(BrowserCommand.LoadUrl(url, clearHistory = true))
    }

    fun assignTabToGroup(tabId: String, groupName: String?, groupColor: String? = null) {
        tabManager.assignTabToGroup(tabId, groupName, groupColor)
    }

    fun createTabGroup(groupName: String, colorHex: String, tabIds: List<String>) {
        tabManager.createGroup(groupName, colorHex, tabIds)
        showToast("Group '$groupName' created")
    }

    fun renameTabGroup(oldName: String, newName: String, newColor: String? = null) {
        tabManager.renameGroup(oldName, newName, newColor)
        showToast("Group updated")
    }

    fun closeTabGroup(groupName: String) {
        tabManager.closeGroup(groupName)
        showToast("Group '$groupName' closed")
    }

    fun showContextMenu(element: com.orbit.browser.browser.engine.OBContextMenuElement) {
        _ui.update { it.copy(activeContextMenuElement = element) }
    }

    fun dismissContextMenu() {
        _ui.update { it.copy(activeContextMenuElement = null) }
    }

    fun openPagePreview(url: String) {
        _ui.update { it.copy(activeContextMenuElement = null, previewPageUrl = url) }
    }

    fun closePagePreview() {
        _ui.update { it.copy(previewPageUrl = null) }
    }

    fun openImagePreview(url: String) {
        _ui.update { it.copy(activeContextMenuElement = null, previewImageUrl = url) }
    }

    fun closeImagePreview() {
        _ui.update { it.copy(previewImageUrl = null) }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            showToast("$label copied")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareText(context: Context, text: String, title: String = "Share") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerNativeShare(context: Context) {
        val currentUrl = tabManager.activeTab.value?.url ?: ""
        if (currentUrl.isNotBlank() && currentUrl != "orbit://home") {
            shareText(context, currentUrl, "Share Page")
        } else {
            showToast("No active page to share")
        }
        _ui.update { it.copy(shareOpen = false, menuOpen = false) }
    }

    fun setWeatherDetailOpen(open: Boolean) {
        _ui.update { it.copy(weatherState = it.weatherState.copy(isDetailOpen = open)) }
    }

    fun fetchLiveWeather(context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _ui.update { it.copy(weatherState = it.weatherState.copy(loading = true)) }
            try {
                var lat: Double? = null
                var lon: Double? = null

                val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                    val gpsLoc = try { locationManager?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
                    val netLoc = try { locationManager?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
                    val loc = gpsLoc ?: netLoc

                    if (loc != null) {
                        lat = loc.latitude
                        lon = loc.longitude
                        prefs.saveLocation(lat, lon)
                    }
                }

                if (lat == null || lon == null) {
                    val savedLat = prefs.savedLatitude.firstOrNull()
                    val savedLon = prefs.savedLongitude.firstOrNull()
                    if (savedLat != null && savedLon != null) {
                        lat = savedLat.toDouble()
                        lon = savedLon.toDouble()
                    }
                }

                var resolvedCity = "Current Location"
                if (lat != null && lon != null) {
                    try {
                        if (android.location.Geocoder.isPresent()) {
                            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(lat, lon, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val addr = addresses[0]
                                val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.countryName
                                if (!city.isNullOrBlank()) {
                                    resolvedCity = city
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    val result = weatherRepository.getWeather(lat, lon)
                    if (result != null) {
                        prefs.saveWeatherData(result.temperature, result.weatherCode)
                        _ui.update {
                            it.copy(
                                weatherState = WeatherState(
                                    kind = result.weatherKind,
                                    temp = result.temperature.toInt(),
                                    cityName = resolvedCity,
                                    loading = false
                                )
                            )
                        }
                        return@launch
                    }
                }

                val cachedTemp = prefs.savedTemp.firstOrNull() ?: 25f
                val cachedCode = prefs.savedWeatherCode.firstOrNull() ?: 0
                val cachedKind = weatherRepository.mapWmoCodeToKind(cachedCode)
                _ui.update {
                    it.copy(
                        weatherState = WeatherState(
                            kind = cachedKind,
                            temp = cachedTemp.toInt(),
                            cityName = resolvedCity,
                            loading = false
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _ui.update { it.copy(weatherState = it.weatherState.copy(loading = false)) }
            }
        }
    }

    fun toggleWeatherEffects(context: Context, enabled: Boolean) {
        _ui.update { it.copy(showWeatherEffects = enabled) }
        viewModelScope.launch {
            prefs.setShowWeatherEffects(enabled)
            if (enabled) {
                fetchLiveWeather(context)
            }
        }
    }

    fun setSearchEngine(engine: String) {
        _ui.update { it.copy(defaultSearchEngine = engine) }
        showToast("Search engine set to $engine")
    }

    fun setTextSize(percent: Int) {
        _ui.update { it.copy(textSizePercent = percent) }
    }

    fun clearBrowsingData() {
        viewModelScope.launch {
            historyDao.clearAll()
            frequentSiteDao.clearAll()
            showToast("Browsing data cleared")
        }
    }

    fun onSearchQueryChanged(query: String) {
        _ui.update { it.copy(searchQuery = query) }
        if (query.isNotBlank()) {
            viewModelScope.launch {
                val history = historyDao.search("%$query%")
                val suggestions = searchSuggestionDao.getSuggestions("%$query%")
                _ui.update {
                    it.copy(
                        historyResults    = history,
                        searchSuggestions = suggestions.map { s -> s.query },
                    )
                }
            }
        } else {
            _ui.update { it.copy(historyResults = emptyList(), searchSuggestions = emptyList()) }
        }
    }

    fun submitSearch(query: String) {
        val isPrivate = tabManager.activeTab.value?.isPrivate == true
        if (!isPrivate) {
            viewModelScope.launch {
                searchSuggestionDao.insertIfAbsent(SearchSuggestion(query = query))
                searchSuggestionDao.incrementUse(query)
            }
        }
        navigate(query)
    }

    fun openTabs()  = _ui.update { it.copy(tabsOpen = true, screen = BrowserScreen.TabSwitcher, menuOpen = false) }
    fun closeTabs() = _ui.update {
        val active = tabManager.activeTab.value
        it.copy(
            tabsOpen = false,
            screen = if (active?.url.isNullOrBlank()) BrowserScreen.Home else BrowserScreen.Browser
        )
    }

    fun newTab(url: String = "", isPrivate: Boolean = false) {
        tabManager.openNewTab(url, isPrivate)
        _ui.update { it.copy(
            tabsOpen = false,
            screen = if (url.isBlank()) BrowserScreen.Home else BrowserScreen.Browser,
            tabMode = if (isPrivate) TabMode.Private else TabMode.Normal
        ) }
        if (url.isNotBlank()) navigate(url)
    }

    fun closeTab(tabId: String) {
        tabManager.closeTab(tabId)
        val active = tabManager.activeTab.value
        val remainingCount = tabManager.tabs.value.size
        _ui.update { current ->
            if (current.screen == BrowserScreen.TabSwitcher && remainingCount > 0) {
                current.copy(
                    tabMode = if (active?.isPrivate == true) TabMode.Private else TabMode.Normal
                )
            } else {
                current.copy(
                    tabsOpen = false,
                    screen   = if (active?.url.isNullOrBlank()) BrowserScreen.Home else BrowserScreen.Browser,
                    tabMode  = if (active?.isPrivate == true) TabMode.Private else TabMode.Normal
                )
            }
        }
    }

    fun closeAllTabs() {
        tabManager.closeAllTabs()
        _ui.update { it.copy(
            tabsOpen = false,
            screen   = BrowserScreen.Home,
            tabMode  = TabMode.Normal
        ) }
    }

    fun switchTab(tabId: String) {
        val tab = tabManager.tabs.value.find { it.id == tabId }
        tabManager.switchToTab(tabId)
        _ui.update { it.copy(
            tabsOpen = false,
            screen   = if (tab?.url.isNullOrBlank()) BrowserScreen.Home else BrowserScreen.Browser,
            tabMode  = if (tab?.isPrivate == true) TabMode.Private else TabMode.Normal
        ) }
    }

    fun setTabMode(mode: TabMode) {
        _ui.update { it.copy(tabMode = mode) }
    }

    fun toggleMenu()     = _ui.update { it.copy(menuOpen = !it.menuOpen, secPanelOpen = false) }
    fun closeMenu()      = _ui.update { it.copy(menuOpen = false) }
    fun toggleSecPanel() = _ui.update { it.copy(secPanelOpen = !it.secPanelOpen, menuOpen = false) }
    fun closeSecPanel()  = _ui.update { it.copy(secPanelOpen = false) }
    fun openShare()      = _ui.update { it.copy(shareOpen = true, menuOpen = false) }
    fun closeShare()     = _ui.update { it.copy(shareOpen = false) }
    fun openCustom()     = _ui.update { it.copy(customPanelOpen = true, menuOpen = false) }
    fun closeCustom()    = _ui.update { it.copy(customPanelOpen = false) }

    fun showToast(msg: String) {
        _ui.update { it.copy(toastMessage = msg) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(2200L)
            _ui.update { if (it.toastMessage == msg) it.copy(toastMessage = null) else it }
        }
    }

    fun setManualTimeSlot(slot: com.orbit.browser.ui.theme.TimeSlot?) {
        _ui.update { it.copy(manualTimeSlot = if (it.manualTimeSlot == slot) null else slot) }
    }

    fun setManualWeather(kind: WeatherKind?) {
        _ui.update { it.copy(manualWeather = if (it.manualWeather == kind) null else kind) }
    }

    fun resetDevOverrides() {
        _ui.update { it.copy(manualTimeSlot = null, manualWeather = null, isDarkMode = null) }
        showToast("Dev overrides cleared")
    }

    fun closeAll() {
        _ui.update {
            it.copy(
                searchOpen      = false,
                tabsOpen        = false,
                menuOpen        = false,
                secPanelOpen    = false,
                shareOpen       = false,
                customPanelOpen = false,
                settingsOpen    = false,
            )
        }
    }

    fun addBookmark(url: String, title: String) = viewModelScope.launch {
        bookmarkDao.insert(Bookmark(url = url, title = title))
    }
    fun removeBookmark(url: String) = viewModelScope.launch { bookmarkDao.deleteByUrl(url) }
    suspend fun isBookmarked(url: String) = bookmarkDao.isBookmarked(url)

    fun setTheme(theme: OBThemePreset)   = viewModelScope.launch { prefs.setTheme(theme) }
    fun setShowQuickAccess(v: Boolean)   = viewModelScope.launch { prefs.setShowQuickAccess(v) }
    fun setShowPrivacyDash(v: Boolean)   = viewModelScope.launch { prefs.setShowPrivacyDash(v) }
    fun setShowFreqVisited(v: Boolean)   = viewModelScope.launch { prefs.setShowFreqVisited(v) }
    fun setShowNewsFeed(v: Boolean)      = viewModelScope.launch { prefs.setShowNewsFeed(v) }
    fun setHttpsOnly(v: Boolean)         = viewModelScope.launch { prefs.setHttpsOnly(v) }
    fun setClearOnExit(v: Boolean)       = viewModelScope.launch { prefs.setClearOnExit(v) }
    fun setSavePasswords(v: Boolean)     = viewModelScope.launch { prefs.setSavePasswords(v) }
    fun setDohEnabled(v: Boolean)        = viewModelScope.launch {
        prefs.setDohEnabled(v)
        secureDnsResolver.isDohEnabled = v
    }
    fun setBlockCookies(v: Boolean)      = _ui.update { it.copy(blockCookies = v) }
    fun setDevMode(v: Boolean)           = _ui.update { it.copy(devMode = v) }

    fun addQuickAccessSite(title: String, url: String) {
        if (url.isBlank()) return
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        viewModelScope.launch {
            quickAccessDao.insert(QuickAccessSite(title = title.ifBlank { formattedUrl }, url = formattedUrl, emoji = "🌐"))
            showToast("Shortcut added")
        }
    }

    fun deleteQuickAccessSite(site: QuickAccessSite) {
        viewModelScope.launch {
            quickAccessDao.delete(site)
            showToast("Shortcut removed")
        }
    }

    fun updateFindMatchCount(current: Int, total: Int) {
        _ui.update { it.copy(findMatchCurrent = current, findMatchTotal = total) }
    }

    fun extractAndOpenReaderMode(wv: com.orbit.browser.browser.engine.OBWebView?) {
        if (wv == null) return
        wv.evaluateJavascript(
            "(function(){ return { title: document.title || '', text: document.body ? document.body.innerText : '' }; })();"
        ) { result ->
            try {
                if (result != null && result != "null") {
                    val json = org.json.JSONObject(result)
                    val title = json.optString("title")
                    val text = json.optString("text")
                    openReaderMode(title = title, byline = "Reader Mode", contentHtml = text)
                }
            } catch (e: Exception) {
                openReaderMode(title = wv.title ?: "Article", byline = "Reader Mode", contentHtml = "Page content formatted for reading.")
            }
        }
    }

    /** Toggle between dark/light, overriding system preference. null = follow system. */
    fun toggleDarkMode() {
        val next = !(_ui.value.isDarkMode ?: true)
        setDarkMode(next)
    }

    fun setDarkMode(dark: Boolean?) {
        _ui.update { it.copy(isDarkMode = dark) }
        viewModelScope.launch {
            prefs.setDarkModePref(
                when (dark) {
                    true  -> "ALWAYS_DARK"
                    false -> "ALWAYS_LIGHT"
                    else  -> "SYSTEM"
                }
            )
        }
    }

    fun recordHistory(url: String, title: String) {
        val isPrivate = tabManager.activeTab.value?.isPrivate == true
        if (isPrivate || url.isBlank() || url == "about:blank" || url.startsWith("chrome://")) return
        viewModelScope.launch {
            historyDao.insert(HistoryEntry(url = url, title = title.ifBlank { url }, visitedAt = System.currentTimeMillis()))
            frequentSiteDao.incrementVisit(url)
        }
    }

    fun addCurrentTabBookmark(folder: String = "Default") {
        val active = tabManager.activeTab.value ?: return
        if (active.url.isBlank() || active.url == "about:blank") {
            showToast("Open a webpage first to bookmark")
            return
        }
        viewModelScope.launch {
            bookmarkDao.insert(Bookmark(url = active.url, title = active.title.ifBlank { active.url }, folder = folder))
            showToast("Bookmark added!")
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            bookmarkDao.delete(bookmark)
            showToast("Bookmark removed")
        }
    }

    fun deleteHistoryEntry(entry: HistoryEntry) {
        viewModelScope.launch {
            historyDao.delete(entry)
        }
    }

    fun deleteDownload(download: Download) {
        viewModelScope.launch {
            downloadDao.delete(download)
            showToast("Download item removed")
        }
    }

    fun clearHistory() = viewModelScope.launch {
        historyDao.clearAll()
        frequentSiteDao.clearAll()
        showToast("History cleared")
    }

    fun fetchWeather() = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        _ui.update { it.copy(weatherState = it.weatherState.copy(loading = true)) }
        try {
            val geoRequest = okhttp3.Request.Builder()
                .url("https://ipapi.co/json/")
                .build()
            val geoResponse = okHttpClient.newCall(geoRequest).execute()
            if (!geoResponse.isSuccessful) throw Exception("Failed geo lookup")
            val geoBody = geoResponse.body?.string() ?: throw Exception("Empty geo body")
            val geoJson = org.json.JSONObject(geoBody)
            val lat = geoJson.getDouble("latitude")
            val lon = geoJson.getDouble("longitude")

            val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=weather_code,temperature_2m&timezone=auto"
            val weatherRequest = okhttp3.Request.Builder()
                .url(weatherUrl)
                .build()
            val weatherResponse = okHttpClient.newCall(weatherRequest).execute()
            if (!weatherResponse.isSuccessful) throw Exception("Failed weather lookup")
            val weatherBody = weatherResponse.body?.string() ?: throw Exception("Empty weather body")
            val weatherJson = org.json.JSONObject(weatherBody)
            val current = weatherJson.getJSONObject("current")
            val code = current.getInt("weather_code")
            val temp = current.getDouble("temperature_2m").toInt()

            val kind = wmoToWeather(code)
            _ui.update {
                it.copy(
                    weatherState = WeatherState(
                        kind = kind,
                        temp = temp,
                        loading = false
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _ui.update {
                it.copy(
                    weatherState = WeatherState(
                        kind = WeatherKind.Clear,
                        temp = 24,
                        loading = false
                    )
                )
            }
        }
    }

    private fun wmoToWeather(code: Int): WeatherKind {
        return when (code) {
            0, 1 -> WeatherKind.Clear
            2, 3 -> WeatherKind.Cloudy
            45, 48 -> WeatherKind.Fog
            in 51..57 -> WeatherKind.Drizzle
            in 61..67, in 80..82 -> WeatherKind.Rain
            in 95..99 -> WeatherKind.Thunderstorm
            in 71..77, 85, 86 -> WeatherKind.Snow
            else -> WeatherKind.Clear
        }
    }

    // Fullscreen Video View Management
    private var customViewCallback: android.webkit.WebChromeClient.CustomViewCallback? = null

    fun showCustomVideoView(view: android.view.View, callback: android.webkit.WebChromeClient.CustomViewCallback) {
        customViewCallback = callback
        _ui.update { it.copy(customVideoView = view) }
    }

    fun hideCustomVideoView() {
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        _ui.update { it.copy(customVideoView = null) }
    }

    // Android DownloadManager Integration
    fun startDownload(
        context: android.content.Context,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ) {
        viewModelScope.launch {
            try {
                val (fileName, resolvedMime) = smartGuessFileName(url, contentDisposition, mimeType)
                val request = android.app.DownloadManager.Request(android.net.Uri.parse(url)).apply {
                    if (resolvedMime.isNotBlank()) setMimeType(resolvedMime)
                    if (userAgent.isNotBlank()) addRequestHeader("User-Agent", userAgent)
                    val cookie = android.webkit.CookieManager.getInstance().getCookie(url)
                    if (!cookie.isNullOrBlank()) addRequestHeader("Cookie", cookie)
                    setDescription("Downloading $fileName")
                    setTitle(fileName)
                    setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                }

                val dbId = System.currentTimeMillis()
                val targetFile = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath + "/" + fileName

                downloadDao.insert(
                    Download(
                        id = dbId,
                        filename = fileName,
                        url = url,
                        mimeType = resolvedMime,
                        filePath = targetFile,
                        sizeBytes = if (contentLength > 0) contentLength else 0L,
                        downloadedBytes = 0L,
                        status = DownloadStatus.Downloading,
                        startedAt = System.currentTimeMillis()
                    )
                )
                showToast("Downloading $fileName")

                com.orbit.browser.downloads.OBDownloadService.startDownload(
                    context = context,
                    downloadId = dbId,
                    url = url,
                    filename = fileName,
                    filePath = targetFile,
                    mimeType = resolvedMime,
                    sizeBytes = if (contentLength > 0) contentLength else 0L
                )
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to start download: ${e.localizedMessage ?: "Error"}")
            }
        }
    }

    fun cancelDownload(context: android.content.Context, download: Download) {
        viewModelScope.launch {
            try {
                com.orbit.browser.downloads.OBDownloadService.cancelDownload(context, download.id)
                downloadDao.updateProgress(
                    id = download.id,
                    status = DownloadStatus.Cancelled,
                    bytes = download.downloadedBytes
                )
                showToast("Download cancelled")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pauseDownload(context: android.content.Context, download: Download) {
        viewModelScope.launch {
            try {
                com.orbit.browser.downloads.OBDownloadService.pauseDownload(context, download.id)
                downloadDao.updateProgress(
                    id = download.id,
                    status = DownloadStatus.Paused,
                    bytes = download.downloadedBytes
                )
                showToast("Download paused")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resumeDownload(context: android.content.Context, download: Download) {
        viewModelScope.launch {
            try {
                com.orbit.browser.downloads.OBDownloadService.resumeDownload(context, download.id)
                downloadDao.updateProgress(
                    id = download.id,
                    status = DownloadStatus.Downloading,
                    bytes = download.downloadedBytes
                )
                showToast("Resuming download…")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun monitorDownload(context: android.content.Context, downloadId: Long) {
        if (downloadId <= 0) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val dm = context.applicationContext.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as? android.app.DownloadManager
                ?: return@launch

            var downloading = true
            while (downloading) {
                try {
                    val query = android.app.DownloadManager.Query().setFilterById(downloadId)
                    val cursor = dm.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val bytesDownloadedIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val statusIdx = cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS)

                        val bytesSoFar = if (bytesDownloadedIdx >= 0) cursor.getLong(bytesDownloadedIdx) else 0L
                        val statusInt = if (statusIdx >= 0) cursor.getInt(statusIdx) else 0

                        val newStatus = when (statusInt) {
                            android.app.DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.Completed
                            android.app.DownloadManager.STATUS_FAILED -> DownloadStatus.Failed
                            android.app.DownloadManager.STATUS_RUNNING, android.app.DownloadManager.STATUS_PENDING -> DownloadStatus.Downloading
                            else -> DownloadStatus.Cancelled
                        }

                        downloadDao.updateProgress(
                            id = downloadId,
                            status = newStatus,
                            bytes = bytesSoFar,
                            completedAt = if (newStatus == DownloadStatus.Completed) System.currentTimeMillis() else null
                        )

                        if (newStatus != DownloadStatus.Downloading) {
                            downloading = false
                        }
                        cursor.close()
                    } else {
                        downloading = false
                        cursor?.close()
                    }
                } catch (e: Exception) {
                    downloading = false
                }
                if (downloading) {
                    kotlinx.coroutines.delay(500L)
                }
            }
        }
    }

    private fun smartGuessFileName(url: String, contentDisposition: String?, mimeType: String?): Pair<String, String> {
        if (url.startsWith("data:")) {
            val typeStr = url.substringAfter("data:").substringBefore(";")
            val ext = when {
                typeStr.contains("png") -> "png"
                typeStr.contains("jpeg") || typeStr.contains("jpg") -> "jpg"
                typeStr.contains("webp") -> "webp"
                typeStr.contains("gif") -> "gif"
                typeStr.contains("svg") -> "svg"
                else -> "png"
            }
            val name = "image_${System.currentTimeMillis()}.$ext"
            val mime = if (typeStr.isNotBlank()) typeStr else "image/$ext"
            return Pair(name, mime)
        }

        val rawGuessed = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val urlExt = cleanUrl.substringAfterLast('.', "").lowercase()

        var dispositionName: String? = null
        if (!contentDisposition.isNullOrBlank()) {
            val match = Regex("""filename\*?=['"]?(?:UTF-8'')?([^;'"\r\n]+)['"]?""", RegexOption.IGNORE_CASE).find(contentDisposition)
            if (match != null) {
                dispositionName = android.net.Uri.decode(match.groupValues[1])
            }
        }

        val baseName = dispositionName ?: if (rawGuessed.endsWith(".bin") && urlExt.isNotBlank() && urlExt.length in 2..5) {
            val fileBase = cleanUrl.substringAfterLast('/').substringBeforeLast('.')
            if (fileBase.isNotBlank()) "$fileBase.$urlExt" else rawGuessed
        } else {
            rawGuessed
        }

        val currentExt = baseName.substringAfterLast('.', "").lowercase()
        val imageExt = when {
            mimeType?.contains("jpeg") == true || mimeType?.contains("jpg") == true -> "jpg"
            mimeType?.contains("png") == true -> "png"
            mimeType?.contains("webp") == true -> "webp"
            mimeType?.contains("gif") == true -> "gif"
            mimeType?.contains("svg") == true -> "svg"
            mimeType?.startsWith("image/") == true -> "jpg"
            else -> null
        }

        val knownExt = if (currentExt == "bin" || currentExt.isBlank()) (imageExt ?: urlExt) else currentExt

        val extToMime = mapOf(
            "mkv" to "video/x-matroska",
            "mp4" to "video/mp4",
            "webm" to "video/webm",
            "avi" to "video/x-msvideo",
            "mov" to "video/quicktime",
            "mp3" to "audio/mpeg",
            "wav" to "audio/wav",
            "flac" to "audio/flac",
            "pdf" to "application/pdf",
            "apk" to "application/vnd.android.package-archive",
            "zip" to "application/zip",
            "rar" to "application/x-rar-compressed",
            "7z" to "application/x-7z-compressed",
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "webp" to "image/webp",
            "gif" to "image/gif"
        )

        val resolvedMime = if (mimeType.isNullOrBlank() || mimeType == "application/octet-stream" || mimeType == "image/*") {
            extToMime[knownExt] ?: android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(knownExt) ?: "image/jpeg"
        } else mimeType

        var resolvedName = if (baseName.endsWith(".bin") && knownExt != "bin" && knownExt.isNotBlank()) {
            baseName.removeSuffix(".bin") + ".$knownExt"
        } else baseName

        if (!resolvedName.contains(".") && knownExt.isNotBlank()) {
            resolvedName += ".$knownExt"
        }

        return Pair(resolvedName, resolvedMime)
    }

    fun deleteDownload(download: Download, deleteFileFromDisk: Boolean = true) {
        viewModelScope.launch {
            try {
                if (deleteFileFromDisk && download.filePath.isNotBlank()) {
                    val file = java.io.File(download.filePath)
                    if (file.exists()) file.delete()
                }
                downloadDao.delete(download)
                showToast(if (deleteFileFromDisk) "File deleted from storage" else "Removed from history")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun renameDownload(download: Download, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                val oldFile = java.io.File(download.filePath)
                val parentDir = oldFile.parentFile
                val newFile = java.io.File(parentDir, newName)

                if (oldFile.exists()) {
                    oldFile.renameTo(newFile)
                }

                downloadDao.updateFileNameAndPath(
                    id = download.id,
                    newFilename = newName,
                    newPath = newFile.absolutePath
                )
                showToast("Renamed to $newName")
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to rename file")
            }
        }
    }
}
