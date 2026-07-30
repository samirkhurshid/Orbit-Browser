package com.orbit.browser.browser.tabs

import android.graphics.Bitmap
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class OBTab(
    val id: String        = UUID.randomUUID().toString(),
    val url: String       = "",
    val displayUrl: String = "",
    val searchQuery: String = "",
    val title: String     = "New Tab",
    val favicon: Bitmap?  = null,
    val thumbnail: Bitmap? = null,
    val isPrivate: Boolean = false,
    val isLoading: Boolean = false,
    val loadProgress: Float = 0f,
    val canGoBack: Boolean  = false,
    val canGoForward: Boolean = false,
    val securityState: SecurityState = SecurityState.Unknown,
    val trackersBlocked: Int = 0,
    val groupName: String? = null,
    val groupColor: String? = null,
    val lastVisitedUrl: String = "",
    val createdAt: Long  = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis(),
)

enum class SecurityState {
    Secure, Insecure, Warning, Unknown,
}

@Singleton
class TabManager @Inject constructor() {

    private val managerScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob())

    private val _tabs = MutableStateFlow<List<OBTab>>(listOf(OBTab()))
    val tabs: StateFlow<List<OBTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String>(_tabs.value.first().id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    val activeTab: StateFlow<OBTab?> = combine(_tabs, _activeTabId) { tabs, id ->
        tabs.find { it.id == id }
    }.stateIn(
        scope        = managerScope,
        started      = SharingStarted.WhileSubscribed(5000),
        initialValue = _tabs.value.firstOrNull(),
    )

    val normalTabCount: StateFlow<Int> = _tabs.map { list ->
        list.count { !it.isPrivate }
    }.stateIn(
        scope        = managerScope,
        started      = SharingStarted.WhileSubscribed(5000),
        initialValue = 1,
    )

    val privateTabCount: StateFlow<Int> = _tabs.map { list ->
        list.count { it.isPrivate }
    }.stateIn(
        scope        = managerScope,
        started      = SharingStarted.WhileSubscribed(5000),
        initialValue = 0,
    )

    fun openNewTab(
        url: String       = "",
        isPrivate: Boolean = false,
        background: Boolean = false,
    ): String {
        val tab = OBTab(url = url, isPrivate = isPrivate)
        _tabs.update { current -> current + tab }
        if (!background) {
            _activeTabId.value = tab.id
        }
        return tab.id
    }

    fun closeTab(tabId: String) {
        val current = _tabs.value
        val idx     = current.indexOfFirst { it.id == tabId }
        if (idx < 0) return

        val updated = current.filterNot { it.id == tabId }

        if (tabId == _activeTabId.value) {
            if (updated.isEmpty()) {
                val fresh = OBTab()
                _tabs.value        = listOf(fresh)
                _activeTabId.value = fresh.id
                return
            }
            val newActive = if (idx < updated.size) updated[idx] else updated.last()
            _activeTabId.value = newActive.id
        }
        _tabs.value = updated
    }

    fun closeAllTabs(privateOnly: Boolean = false) {
        if (privateOnly) {
            _tabs.update { list -> list.filter { !it.isPrivate } }
            if (_tabs.value.none { it.id == _activeTabId.value }) {
                _activeTabId.value = _tabs.value.firstOrNull()?.id ?: run {
                    val fresh = OBTab()
                    _tabs.update { it + fresh }
                    fresh.id
                }
            }
        } else {
            val fresh = OBTab()
            _tabs.value        = listOf(fresh)
            _activeTabId.value = fresh.id
        }
    }

    fun switchToTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
            updateTab(tabId) { it.copy(lastAccessedAt = System.currentTimeMillis()) }
        }
    }

    fun updateTab(tabId: String, transform: (OBTab) -> OBTab) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) transform(it) else it }
        }
    }

    fun updateActiveTab(transform: (OBTab) -> OBTab) {
        updateTab(_activeTabId.value, transform)
    }

    fun reorderTabs(fromIndex: Int, toIndex: Int) {
        val current = _tabs.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _tabs.value = current
    }

    fun duplicateTab(tabId: String): String {
        val source = _tabs.value.find { it.id == tabId } ?: return ""
        val copy   = source.copy(
            id             = UUID.randomUUID().toString(),
            thumbnail      = null,
            createdAt      = System.currentTimeMillis(),
            lastAccessedAt = System.currentTimeMillis(),
        )
        _tabs.update { list ->
            val idx = list.indexOfFirst { it.id == tabId }
            list.toMutableList().also { it.add(idx + 1, copy) }
        }
        return copy.id
    }

    fun restoreTabs(savedTabs: List<OBTab>, savedActiveId: String?) {
        if (savedTabs.isEmpty()) return
        _tabs.value = savedTabs
        if (savedActiveId != null && savedTabs.any { it.id == savedActiveId }) {
            _activeTabId.value = savedActiveId
        } else {
            _activeTabId.value = savedTabs.first().id
        }
    }

    fun assignTabToGroup(tabId: String, groupName: String?, groupColor: String? = null) {
        updateTab(tabId) { t ->
            t.copy(groupName = groupName?.ifBlank { null }, groupColor = groupColor ?: t.groupColor)
        }
    }

    fun createGroup(groupName: String, colorHex: String, tabIds: List<String>) {
        if (groupName.isBlank()) return
        _tabs.update { list ->
            list.map { tab ->
                if (tab.id in tabIds) {
                    tab.copy(groupName = groupName, groupColor = colorHex)
                } else tab
            }
        }
    }

    fun renameGroup(oldName: String, newName: String, newColor: String? = null) {
        if (oldName.isBlank()) return
        _tabs.update { list ->
            list.map { tab ->
                if (tab.groupName == oldName) {
                    tab.copy(groupName = newName.ifBlank { null }, groupColor = newColor ?: tab.groupColor)
                } else tab
            }
        }
    }

    fun closeGroup(groupName: String) {
        val groupTabs = _tabs.value.filter { it.groupName == groupName }
        groupTabs.forEach { closeTab(it.id) }
    }
}
