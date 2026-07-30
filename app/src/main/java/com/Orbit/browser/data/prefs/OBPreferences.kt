package com.orbit.browser.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.orbit.browser.ui.theme.OBThemePreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "orbit_prefs")

@Singleton
class OBPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.dataStore
    private val sharedPrefs = context.getSharedPreferences("orbit_fast_prefs", Context.MODE_PRIVATE)

    fun getSyncDarkModePref(): String {
        return sharedPrefs.getString("dark_mode_pref", "SYSTEM") ?: "SYSTEM"
    }

    fun getSyncSavedTabsJson(): String? {
        return sharedPrefs.getString("saved_tabs_json", null)
    }

    fun getSyncActiveTabId(): String? {
        return sharedPrefs.getString("active_tab_id", null)
    }

    companion object Keys {
        val THEME             = stringPreferencesKey("theme")
        val SHOW_QUICK_ACCESS = booleanPreferencesKey("show_quick_access")
        val SHOW_PRIVACY_DASH = booleanPreferencesKey("show_privacy_dash")
        val SHOW_FREQ_VISITED = booleanPreferencesKey("show_freq_visited")
        val SHOW_NEWS_FEED    = booleanPreferencesKey("show_news_feed")
        val SEARCH_ENGINE     = stringPreferencesKey("search_engine")
        val ADBLOCK_ENABLED   = booleanPreferencesKey("adblock_enabled")
        val DOH_ENABLED       = booleanPreferencesKey("doh_enabled")
        val HTTPS_ONLY        = booleanPreferencesKey("https_only")
        val ANTI_FP_ENABLED   = booleanPreferencesKey("anti_fp")
        val CLEAR_ON_EXIT     = booleanPreferencesKey("clear_on_exit")
        val BOTTOM_TOOLBAR    = booleanPreferencesKey("bottom_toolbar")
        val SAVE_PASSWORDS    = booleanPreferencesKey("save_passwords")
        val BIOMETRIC_LOCK    = booleanPreferencesKey("biometric_lock")
        val CARD_ORDER        = stringPreferencesKey("card_order")
        val FORCE_DARK        = booleanPreferencesKey("force_dark")
        val DESKTOP_SITE      = booleanPreferencesKey("desktop_site")
        val DARK_MODE_PREF    = stringPreferencesKey("dark_mode_pref")
        val SAVED_TABS_JSON   = stringPreferencesKey("saved_tabs_json")
        val ACTIVE_TAB_ID     = stringPreferencesKey("active_tab_id")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SHOW_WEATHER_EFFECTS = booleanPreferencesKey("show_weather_effects")
        val SAVED_LATITUDE       = floatPreferencesKey("saved_latitude")
        val SAVED_LONGITUDE      = floatPreferencesKey("saved_longitude")
        val SAVED_TEMP           = floatPreferencesKey("saved_temp")
        val SAVED_WEATHER_CODE   = intPreferencesKey("saved_weather_code")
        val APP_LAUNCH_COUNT     = intPreferencesKey("app_launch_count")
        val HAS_DISMISSED_DEFAULT_BROWSER_PROMPT = booleanPreferencesKey("dismissed_default_browser")
        val LAST_CLOSED_SCREEN   = stringPreferencesKey("last_closed_screen")
    }

    val lastClosedScreen: Flow<String?> = store.data.map { it[LAST_CLOSED_SCREEN] ?: sharedPrefs.getString("last_closed_screen", null) }

    val theme: Flow<OBThemePreset> = store.data.map { prefs ->
        try { OBThemePreset.valueOf(prefs[THEME] ?: OBThemePreset.BlueFrost.name) }
        catch (_: Exception) { OBThemePreset.BlueFrost }
    }
    val showQuickAccess: Flow<Boolean> = store.data.map { it[SHOW_QUICK_ACCESS] ?: true }
    val showPrivacyDash: Flow<Boolean> = store.data.map { it[SHOW_PRIVACY_DASH] ?: true }
    val showFreqVisited: Flow<Boolean> = store.data.map { it[SHOW_FREQ_VISITED] ?: true }
    val showNewsFeed:    Flow<Boolean> = store.data.map { it[SHOW_NEWS_FEED] ?: true }
    val searchEngine:    Flow<String>  = store.data.map { it[SEARCH_ENGINE] ?: "Google" }
    val adBlockEnabled:  Flow<Boolean> = store.data.map { it[ADBLOCK_ENABLED] ?: true }
    val dohEnabled:      Flow<Boolean> = store.data.map { it[DOH_ENABLED] ?: true }
    val httpsOnly:       Flow<Boolean> = store.data.map { it[HTTPS_ONLY] ?: true }
    val antiFpEnabled:   Flow<Boolean> = store.data.map { it[ANTI_FP_ENABLED] ?: true }
    val clearOnExit:     Flow<Boolean> = store.data.map { it[CLEAR_ON_EXIT] ?: false }
    val bottomToolbar:   Flow<Boolean> = store.data.map { it[BOTTOM_TOOLBAR] ?: false }
    val savePasswords:   Flow<Boolean> = store.data.map { it[SAVE_PASSWORDS] ?: true }
    val biometricLock:   Flow<Boolean> = store.data.map { it[BIOMETRIC_LOCK] ?: false }
    val forceDark:       Flow<Boolean> = store.data.map { it[FORCE_DARK] ?: true }
    val desktopSite:     Flow<Boolean> = store.data.map { it[DESKTOP_SITE] ?: false }
    val darkModePref:    Flow<String>  = store.data.map { it[DARK_MODE_PREF] ?: getSyncDarkModePref() }
    val savedTabsJson:   Flow<String?> = store.data.map { it[SAVED_TABS_JSON] ?: getSyncSavedTabsJson() }
    val activeTabIdPref: Flow<String?> = store.data.map { it[ACTIVE_TAB_ID] ?: getSyncActiveTabId() }
    val onboardingCompleted: Flow<Boolean> = store.data.map { it[ONBOARDING_COMPLETED] ?: false }
    val showWeatherEffects: Flow<Boolean>  = store.data.map { it[SHOW_WEATHER_EFFECTS] ?: false }
    val savedLatitude:       Flow<Float?>  = store.data.map { it[SAVED_LATITUDE] }
    val savedLongitude:      Flow<Float?>  = store.data.map { it[SAVED_LONGITUDE] }
    val savedTemp:           Flow<Float?>  = store.data.map { it[SAVED_TEMP] }
    val savedWeatherCode:    Flow<Int?>    = store.data.map { it[SAVED_WEATHER_CODE] }
    val appLaunchCount:      Flow<Int>     = store.data.map { it[APP_LAUNCH_COUNT] ?: 0 }
    val dismissedDefaultBrowserPrompt: Flow<Boolean> = store.data.map { it[HAS_DISMISSED_DEFAULT_BROWSER_PROMPT] ?: false }

    suspend fun incrementLaunchCount(): Int {
        var newCount = 1
        store.edit { prefs ->
            val count = (prefs[APP_LAUNCH_COUNT] ?: 0) + 1
            prefs[APP_LAUNCH_COUNT] = count
            newCount = count
        }
        return newCount
    }

    fun getSyncLastClosedScreen(): String? {
        return sharedPrefs.getString("last_closed_screen", null)
    }

    suspend fun setDismissedDefaultBrowserPrompt(v: Boolean) = set(HAS_DISMISSED_DEFAULT_BROWSER_PROMPT, v)

    suspend fun setTheme(theme: OBThemePreset) = set(THEME, theme.name)
    suspend fun setOnboardingCompleted(v: Boolean) = set(ONBOARDING_COMPLETED, v)
    suspend fun setShowWeatherEffects(v: Boolean)   = set(SHOW_WEATHER_EFFECTS, v)
    suspend fun saveLocation(lat: Double, lon: Double) {
        store.edit {
            it[SAVED_LATITUDE]  = lat.toFloat()
            it[SAVED_LONGITUDE] = lon.toFloat()
        }
    }
    suspend fun saveWeatherData(temp: Float, code: Int) {
        store.edit {
            it[SAVED_TEMP]         = temp
            it[SAVED_WEATHER_CODE] = code
        }
    }
    suspend fun setShowQuickAccess(v: Boolean)  = set(SHOW_QUICK_ACCESS, v)
    suspend fun setShowPrivacyDash(v: Boolean)  = set(SHOW_PRIVACY_DASH, v)
    suspend fun setShowFreqVisited(v: Boolean)  = set(SHOW_FREQ_VISITED, v)
    suspend fun setShowNewsFeed(v: Boolean)     = set(SHOW_NEWS_FEED, v)
    suspend fun setSearchEngine(v: String)      = set(SEARCH_ENGINE, v)
    suspend fun setAdBlockEnabled(v: Boolean)   = set(ADBLOCK_ENABLED, v)
    suspend fun setDohEnabled(v: Boolean)       = set(DOH_ENABLED, v)
    suspend fun setHttpsOnly(v: Boolean)        = set(HTTPS_ONLY, v)
    suspend fun setAntiFp(v: Boolean)           = set(ANTI_FP_ENABLED, v)
    suspend fun setClearOnExit(v: Boolean)      = set(CLEAR_ON_EXIT, v)
    suspend fun setBottomToolbar(v: Boolean)    = set(BOTTOM_TOOLBAR, v)
    suspend fun setSavePasswords(v: Boolean)    = set(SAVE_PASSWORDS, v)
    suspend fun setBiometricLock(v: Boolean)    = set(BIOMETRIC_LOCK, v)
    suspend fun setForceDark(v: Boolean)        = set(FORCE_DARK, v)
    suspend fun setDesktopSite(v: Boolean)      = set(DESKTOP_SITE, v)
    suspend fun setDarkModePref(v: String) {
        sharedPrefs.edit().putString("dark_mode_pref", v).commit()
        set(DARK_MODE_PREF, v)
    }
    suspend fun saveTabsState(tabsJson: String, activeId: String) {
        sharedPrefs.edit()
            .putString("saved_tabs_json", tabsJson)
            .putString("active_tab_id", activeId)
            .commit()
        store.edit {
            it[SAVED_TABS_JSON] = tabsJson
            it[ACTIVE_TAB_ID]   = activeId
        }
    }

    suspend fun saveLastClosedScreen(screenName: String) {
        sharedPrefs.edit().putString("last_closed_screen", screenName).commit()
        set(LAST_CLOSED_SCREEN, screenName)
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        store.edit { prefs -> prefs[key] = value }
    }
}
