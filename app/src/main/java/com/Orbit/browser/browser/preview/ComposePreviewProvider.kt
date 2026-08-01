package com.orbit.browser.browser.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

/**
 * PreviewProvider implementation for capturing bitmap snapshots from Jetpack Compose UI Views.
 * Holds a WeakReference to the root View hierarchy to guarantee memory safety.
 */
class ComposePreviewProvider(
    view: View,
    override val sourceName: String = "ComposeScreen"
) : PreviewProvider {

    private val viewRef = WeakReference(view)

    override suspend fun capturePreview(): Bitmap? = withContext(Dispatchers.Main) {
        val targetView = viewRef.get() ?: return@withContext null
        try {
            val width = targetView.width
            val height = targetView.height
            if (width <= 0 || height <= 0) return@withContext null

            val scale = (600f / width).coerceAtMost(1.0f)
            val thumbW = (width * scale).toInt().coerceAtLeast(1)
            val thumbH = (height * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.scale(scale, scale)
            targetView.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
