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

            val location = IntArray(2)
            wv.getLocationInWindow(location)
            val topInset = location[1].coerceAtLeast(0)

            val cardAspectRatio = 1.40f
            val thumbW = 600
            val thumbH = (thumbW * cardAspectRatio).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val scale = thumbW.toFloat() / width.toFloat()

            canvas.scale(scale, scale)
            canvas.translate(0f, -topInset.toFloat())
            wv.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
