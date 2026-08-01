package com.orbit.browser.browser.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import com.orbit.browser.browser.engine.OBWebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

/**
 * PreviewProvider implementation for capturing bitmap snapshots from an [OBWebView].
 * Holds a WeakReference to OBWebView to guarantee memory safety.
 */
class WebViewPreviewProvider(
    webView: OBWebView
) : PreviewProvider {

    private val webViewRef = WeakReference(webView)

    override val sourceName: String = "WebView"

    override suspend fun capturePreview(): Bitmap? = withContext(Dispatchers.Main) {
        val wv = webViewRef.get() ?: return@withContext null
        try {
            val width = wv.width
            val height = wv.height
            if (width <= 0 || height <= 0) return@withContext null

            val scale = (600f / width).coerceAtMost(1.0f)
            val thumbW = (width * scale).toInt().coerceAtLeast(1)
            val thumbH = (height * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            wv.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
