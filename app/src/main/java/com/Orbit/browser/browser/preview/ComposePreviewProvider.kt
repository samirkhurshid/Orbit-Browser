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

            val location = IntArray(2)
            targetView.getLocationInWindow(location)
            val topInset = location[1].coerceAtLeast(0)

            val cardAspectRatio = 1.40f
            val thumbW = 600
            val thumbH = (thumbW * cardAspectRatio).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(thumbW, thumbH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val scale = thumbW.toFloat() / width.toFloat()

            canvas.scale(scale, scale)
            canvas.translate(0f, -topInset.toFloat())
            targetView.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
