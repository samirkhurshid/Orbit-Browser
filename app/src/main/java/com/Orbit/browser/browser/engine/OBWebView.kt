package com.orbit.browser.browser.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.util.AttributeSet
import android.view.View
import android.webkit.*
import com.orbit.browser.browser.tabs.SecurityState
import com.orbit.browser.browser.tabs.TabManager
import com.orbit.browser.security.adblock.AdBlocker
import kotlinx.coroutines.*
import java.util.Locale

data class OBContextMenuElement(
    val linkUrl: String? = null,
    val imageUrl: String? = null,
    val title: String? = null
)

@SuppressLint("SetJavaScriptEnabled")
class OBWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : WebView(context, attrs) {

    var tabId: String = ""
    @Volatile var currentMainUrl: String = ""
    var tabManager: TabManager? = null
    var adBlocker: AdBlocker? = null
    var onProgressChanged: ((Int) -> Unit)? = null
    var onPageTitleChanged: ((String) -> Unit)? = null
    var onFaviconChanged: ((Bitmap?) -> Unit)? = null
    var onSecurityStateChanged: ((SecurityState, String) -> Unit)? = null
    var onUrlChanged: ((String) -> Unit)? = null
    var onPageFinished: (() -> Unit)? = null
    var onCreateNewTab: ((String) -> Unit)? = null
    var onDownloadRequested: ((url: String, userAgent: String, contentDisposition: String, mimeType: String, contentLength: Long) -> Unit)? = null
    var onShowCustomView: ((view: View, callback: WebChromeClient.CustomViewCallback) -> Unit)? = null
    var onHideCustomView: (() -> Unit)? = null
    var onContextMenuRequested: ((OBContextMenuElement) -> Unit)? = null
    var shouldClearHistoryOnNextLoad: Boolean = false
    var passwordAutofillBridge: com.orbit.browser.browser.autofill.PasswordAutofillBridge? = null
        set(value) {
            field = value
            if (value != null) {
                try {
                    addJavascriptInterface(value, com.orbit.browser.browser.autofill.PasswordAutofillBridge.JS_INTERFACE_NAME)
                } catch (_: Exception) {}
            }
        }

    val previewProvider: com.orbit.browser.browser.preview.PreviewProvider by lazy {
        com.orbit.browser.browser.preview.WebViewPreviewProvider(this)
    }

    var previewScheduler: com.orbit.browser.browser.preview.PreviewScheduler? = null
    var previewManager: com.orbit.browser.browser.preview.PreviewManager? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        setupSettings()
        setupWebViewClient()
        setupWebChromeClient()
        setupDownloadListener()
        setupContextMenuListener()
    }

    private fun setupContextMenuListener() {
        setOnLongClickListener {
            val result = hitTestResult
            val type = result.type
            val extra = result.extra

            when (type) {
                HitTestResult.SRC_ANCHOR_TYPE -> {
                    if (!extra.isNullOrBlank()) {
                        onContextMenuRequested?.invoke(
                            OBContextMenuElement(linkUrl = extra, title = extra)
                        )
                        true
                    } else false
                }
                HitTestResult.IMAGE_TYPE -> {
                    if (!extra.isNullOrBlank()) {
                        onContextMenuRequested?.invoke(
                            OBContextMenuElement(imageUrl = extra, title = extra.substringAfterLast('/'))
                        )
                        true
                    } else false
                }
                HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    val handler = ContextNodeHandler { linkUrl, imageUrl, title ->
                        val finalImg = if (imageUrl.isNullOrBlank()) extra else imageUrl
                        val finalLink = if (linkUrl.isNullOrBlank()) extra else linkUrl
                        if (!finalLink.isNullOrBlank() || !finalImg.isNullOrBlank()) {
                            onContextMenuRequested?.invoke(
                                OBContextMenuElement(
                                    linkUrl = finalLink,
                                    imageUrl = finalImg,
                                    title = title ?: finalLink
                                )
                            )
                        }
                    }
                    val msg = handler.obtainMessage()
                    requestFocusNodeHref(msg)
                    true
                }
                else -> false
            }
        }
    }

    private class ContextNodeHandler(
        private val onResult: (linkUrl: String?, imageUrl: String?, title: String?) -> Unit
    ) : android.os.Handler(android.os.Looper.getMainLooper()) {
        override fun handleMessage(msg: android.os.Message) {
            val linkUrl = msg.data.getString("url")
            val imageUrl = msg.data.getString("src")
            val title = msg.data.getString("title")
            onResult(linkUrl, imageUrl, title)
        }
    }

    private fun setupDownloadListener() {
        setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            if (!url.isNullOrBlank()) {
                onDownloadRequested?.invoke(url, userAgent ?: "", contentDisposition ?: "", mimeType ?: "", contentLength)
            }
        }
    }

    private fun setupSettings() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled   = true

            loadWithOverviewMode = true
            useWideViewPort      = true
            setSupportZoom(true)
            builtInZoomControls  = true
            displayZoomControls  = false

            cacheMode = WebSettings.LOAD_DEFAULT

            setGeolocationEnabled(false)
            allowFileAccess     = false
            allowContentAccess  = false
            allowFileAccessFromFileURLs      = false
            allowUniversalAccessFromFileURLs = false

            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

            userAgentString = getOrInitMobileUserAgent()
            setSafeBrowsingEnabled(true)

            @Suppress("DEPRECATION")
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            setOffscreenPreRaster(true)
        }

        try {
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        } catch (_: Exception) {}

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                settings.forceDark = WebSettings.FORCE_DARK_ON
            } catch (_: Exception) {}
        }

        WebView.setWebContentsDebuggingEnabled(false)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    private var savedMobileUserAgent: String? = null

    private fun getOrInitMobileUserAgent(): String {
        val existing = savedMobileUserAgent
        if (existing != null) return existing
        val base = settings.userAgentString ?: ""
        val cleaned = base.replace("wv", "")
            .replace("Version/\\S+".toRegex(), "")
            .trimEnd() + " OrbitBrowser/1.0"
        savedMobileUserAgent = cleaned
        return cleaned
    }

    var httpsOnlyEnabled: Boolean = false
    var onFindMatchCount: ((Int, Int) -> Unit)? = null

    init {
        setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            onFindMatchCount?.invoke(activeMatchOrdinal + 1, numberOfMatches)
        }
    }

    fun updateDarkMode(isDark: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                settings.forceDark = if (isDark) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
            } catch (_: Exception) {}
        }
    }

    fun updateBlockCookies(block: Boolean) {
        try {
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, !block)
        } catch (_: Exception) {}
    }

    fun findInPage(query: String) {
        if (query.isBlank()) {
            clearMatches()
            onFindMatchCount?.invoke(0, 0)
        } else {
            findAllAsync(query)
        }
    }

    private var lastScrollCaptureTime = 0L

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        previewManager?.requestPreview(
            tabId = tabId,
            provider = previewProvider,
            policy = com.orbit.browser.browser.preview.SchedulePolicy.Debounced(com.orbit.browser.browser.preview.PreviewTimingDefaults.SCROLL_SETTLE_DELAY_MS)
        )
    }

    fun captureCurrentStateThumbnail() {
        try {
            val width = this@OBWebView.width
            val height = this@OBWebView.height
            if (width > 0 && height > 0) {
                val location = IntArray(2)
                getLocationInWindow(location)
                val topInset = location[1].coerceAtLeast(0)

                val cardAspectRatio = 1.40f
                val thumbW = 600
                val thumbH = (thumbW * cardAspectRatio).toInt().coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                val scale = thumbW.toFloat() / width.toFloat()

                canvas.scale(scale, scale)
                canvas.translate(0f, -topInset.toFloat())
                this@OBWebView.draw(canvas)
                tabManager?.updateTab(tabId) { it.copy(thumbnail = bitmap) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupWebViewClient() {
        webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?,
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null

                if (adBlocker?.shouldBlock(url, currentMainUrl) == true) {
                    tabManager?.updateTab(tabId) { tab ->
                        tab.copy(trackersBlocked = tab.trackersBlocked + 1)
                    }
                    return WebResourceResponse("text/plain", "utf-8", null)
                }
                return null
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val urlString = request?.url?.toString() ?: return false
                val scheme = request.url?.scheme?.lowercase(Locale.ROOT) ?: ""

                // Standard HTTP / HTTPS links stay inside WebView
                if (scheme == "http" || scheme == "https") {
                    return false
                }

                // Handle external apps, custom intents, mailto, tel, whatsapp, maps, market, etc.
                try {
                    val intent = if (urlString.startsWith("intent://")) {
                        android.content.Intent.parseUri(urlString, android.content.Intent.URI_INTENT_SCHEME)
                    } else {
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(urlString))
                    }

                    if (intent != null) {
                        intent.addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                        intent.component = null
                        intent.selector = null
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

                        val packageManager = context.packageManager
                        val resolveInfo = packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                        if (resolveInfo != null) {
                            context.startActivity(intent)
                            return true
                        } else {
                            // Fallback URL if app isn't installed
                            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                            if (!fallbackUrl.isNullOrBlank()) {
                                view?.loadUrl(fallbackUrl)
                                return true
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (tabId.isNotBlank()) {
                    previewScheduler?.cancelScheduledRequest(tabId)
                }
                if (url == null) return

                if (httpsOnlyEnabled && url.startsWith("http://") && !url.startsWith("http://localhost") && !url.startsWith("http://127.0.0.1")) {
                    val upgradedUrl = url.replace("http://", "https://")
                    post { loadUrl(upgradedUrl) }
                    return
                }

                currentMainUrl = url

                onUrlChanged?.invoke(url)
                val extractedQuery = extractSearchQueryFromUrl(url)
                val canBack = if (shouldClearHistoryOnNextLoad) false else (view?.canGoBack() ?: false)
                tabManager?.updateTab(tabId) { tab ->
                    val query = extractedQuery ?: tab.searchQuery
                    tab.copy(
                        url          = url,
                        searchQuery  = query,
                        displayUrl   = formatDisplayUrl(url, query),
                        isLoading    = true,
                        loadProgress = 0f,
                        canGoBack    = canBack,
                        canGoForward = view?.canGoForward() ?: false,
                    )
                }

                val security = when {
                    url.startsWith("https://") -> SecurityState.Secure
                    url.startsWith("http://")  -> SecurityState.Insecure
                    else                       -> SecurityState.Unknown
                }
                onSecurityStateChanged?.invoke(security, url)
                tabManager?.updateTab(tabId) { it.copy(securityState = security) }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null) {
                    currentMainUrl = url
                }
                val isHistoryClearing = shouldClearHistoryOnNextLoad
                if (shouldClearHistoryOnNextLoad) {
                    shouldClearHistoryOnNextLoad = false
                    try {
                        view?.clearHistory()
                    } catch (_: Exception) {}
                }
                onPageFinished?.invoke()
                passwordAutofillBridge?.injectFormListener(this@OBWebView)
                val targetUrl = url ?: currentMainUrl
                val extractedQuery = extractSearchQueryFromUrl(targetUrl)
                val canBack = if (isHistoryClearing) false else (view?.canGoBack() ?: false)
                tabManager?.updateTab(tabId) { tab ->
                    val query = extractedQuery ?: tab.searchQuery
                    tab.copy(
                        isLoading    = false,
                        loadProgress = 1f,
                        canGoBack    = canBack,
                        canGoForward = view?.canGoForward() ?: false,
                        url          = targetUrl,
                        searchQuery  = query,
                        displayUrl   = formatDisplayUrl(targetUrl, query),
                    )
                }
                previewManager?.requestPreview(
                    tabId = tabId,
                    provider = previewProvider,
                    policy = com.orbit.browser.browser.preview.SchedulePolicy.Immediate
                )
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                val canBack = if (shouldClearHistoryOnNextLoad) false else (view?.canGoBack() ?: false)
                val canForward = view?.canGoForward() ?: false
                tabManager?.updateTab(tabId) { tab ->
                    tab.copy(
                        canGoBack    = canBack,
                        canGoForward = canForward,
                    )
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?,
            ) {
                handler?.cancel()
                tabManager?.updateTab(tabId) { it.copy(securityState = SecurityState.Warning) }
                onSecurityStateChanged?.invoke(SecurityState.Warning, view?.url ?: "")
            }
        }
    }

    private fun setupWebChromeClient() {
        webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressChanged?.invoke(newProgress)
                tabManager?.updateTab(tabId) { tab ->
                    tab.copy(loadProgress = newProgress / 100f)
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (title.isNullOrBlank()) return
                onPageTitleChanged?.invoke(title)
                tabManager?.updateTab(tabId) { it.copy(title = title) }
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                onFaviconChanged?.invoke(icon)
                tabManager?.updateTab(tabId) { it.copy(favicon = icon) }
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?,
            ): Boolean {
                val targetContext = view?.context ?: context
                val dummyWebView = WebView(targetContext)
                dummyWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val targetUrl = request?.url?.toString()
                        if (!targetUrl.isNullOrBlank()) {
                            post { onCreateNewTab?.invoke(targetUrl) }
                            try { view?.destroy() } catch (_: Exception) {}
                        }
                        return true
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        if (!url.isNullOrBlank() && url != "about:blank") {
                            post { onCreateNewTab?.invoke(url) }
                            try { view?.destroy() } catch (_: Exception) {}
                        }
                    }
                }
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = dummyWebView
                resultMsg?.sendToTarget()
                return true
            }

            override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
                if (view != null && callback != null) {
                    onShowCustomView?.invoke(view, callback)
                }
            }

            override fun onHideCustomView() {
                onHideCustomView?.invoke()
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?,
            ) {
                callback?.invoke(origin, false, false)
            }

            override fun onJsAlert(
                view: WebView?, url: String?, message: String?,
                result: JsResult?
            ): Boolean {
                result?.cancel()
                return true
            }
        }
    }

    fun clearWebPage() {
        currentMainUrl = "orbit://home"
        try {
            super.loadUrl("about:blank")
            clearHistory()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun loadUrl(url: String) {
        if (url == "orbit://home" || url.startsWith("orbit://")) return
        super.loadUrl(url)
    }

    override fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>) {
        if (url == "orbit://home" || url.startsWith("orbit://")) return
        super.loadUrl(url, additionalHttpHeaders)
    }

    fun loadSmart(query: String) {
        loadUrl(resolveUrl(query))
    }

    fun goBackSafely(): Boolean = if (canGoBack()) { goBack(); true } else false
    fun goForwardSafely(): Boolean = if (canGoForward()) { goForward(); true } else false

    private var isDesktopModeApplied: Boolean? = null

    fun applyDesktopMode(enabled: Boolean) {
        if (isDesktopModeApplied == enabled) return
        isDesktopModeApplied = enabled
        if (enabled) {
            settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        } else {
            settings.userAgentString = getOrInitMobileUserAgent()
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        }
        if (!url.isNullOrBlank() && url != "about:blank") {
            reload()
        }
    }

    fun findInPageAsync(query: String) {
        if (query.isNotBlank()) {
            findAllAsync(query)
        } else {
            clearMatches()
        }
    }

    fun findNextMatch(forward: Boolean) {
        findNext(forward)
    }

    fun clearPageMatches() {
        clearMatches()
    }

    fun setTextZoomPercent(zoom: Int) {
        settings.textZoom = zoom.coerceIn(50, 200)
    }

    fun extractReaderContent(onExtracted: (title: String, byline: String, contentHtml: String) -> Unit) {
        val currentUrl = url ?: currentMainUrl
        scope.launch(Dispatchers.IO) {
            try {
                val doc = org.jsoup.Jsoup.connect(currentUrl).userAgent(settings.userAgentString).get()
                val title = doc.title().ifBlank { "Article" }
                val articleText = doc.select("article, main, .post-content, .entry-content, #content").html().ifBlank {
                    doc.select("p").outerHtml()
                }
                withContext(Dispatchers.Main) {
                    onExtracted(title, currentUrl, articleText)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onExtracted("Reader Mode", currentUrl, "<p>Unable to extract article text.</p>")
                }
            }
        }
    }

    companion object {
        fun resolveUrl(input: String): String {
            val trimmed = input.trim()
            return when {
                trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                !trimmed.contains(" ") && trimmed.contains(".") && trimmed.length > 3 -> "https://$trimmed"
                trimmed.startsWith("localhost") || trimmed.matches(Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}.*")) ->
                    "http://$trimmed"
                else -> "https://www.google.com/search?q=${Uri.encode(trimmed)}"
            }
        }

        fun extractSearchQueryFromUrl(url: String): String? {
            if (url.isBlank()) return null
            return try {
                val uri = Uri.parse(url)
                val host = uri.host?.lowercase(Locale.ROOT) ?: ""
                if (host.contains("google.") || host.contains("bing.") || host.contains("duckduckgo.") || host.contains("yahoo.")) {
                    val q = uri.getQueryParameter("q") ?: uri.getQueryParameter("p")
                    if (!q.isNullOrBlank()) Uri.decode(q) else null
                } else null
            } catch (e: Exception) { null }
        }

        fun formatDisplayUrl(url: String, searchQuery: String = ""): String {
            if (searchQuery.isNotBlank()) return searchQuery
            val extracted = extractSearchQueryFromUrl(url)
            if (!extracted.isNullOrBlank()) return extracted

            return try {
                val uri = Uri.parse(url)
                val host = uri.host?.removePrefix("www.") ?: url
                if (host.isBlank()) url else host
            } catch (e: Exception) { url }
        }
    }

    override fun onDetachedFromWindow() {
        if (tabId.isNotBlank()) {
            previewManager?.cancelAndEvict(tabId)
        }
        scope.cancel()
        super.onDetachedFromWindow()
    }

    override fun destroy() {
        if (tabId.isNotBlank()) {
            previewManager?.cancelAndEvict(tabId)
        }
        super.destroy()
    }
}
