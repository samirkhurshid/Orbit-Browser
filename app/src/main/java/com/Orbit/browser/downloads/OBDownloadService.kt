package com.orbit.browser.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.orbit.browser.R
import com.orbit.browser.data.db.DownloadDao
import com.orbit.browser.data.db.DownloadStatus
import com.orbit.browser.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class OBDownloadService : Service() {

    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var downloadDao: DownloadDao

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeDownloadJobs = ConcurrentHashMap<Long, Job>()
    private val pausedDownloadIds = ConcurrentHashMap.newKeySet<Long>()

    companion object {
        const val CHANNEL_ID = "orbit_downloads_channel"
        const val CHANNEL_NAME = "Orbit Downloads"
        const val NOTIF_ID_BASE = 1000

        const val ACTION_START_DOWNLOAD = "com.orbit.browser.action.START_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "com.orbit.browser.action.PAUSE_DOWNLOAD"
        const val ACTION_RESUME_DOWNLOAD = "com.orbit.browser.action.RESUME_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.orbit.browser.action.CANCEL_DOWNLOAD"

        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FILENAME = "extra_filename"
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_MIME_TYPE = "extra_mime_type"
        const val EXTRA_SIZE_BYTES = "extra_size_bytes"

        fun startDownload(
            context: Context,
            downloadId: Long,
            url: String,
            filename: String,
            filePath: String,
            mimeType: String,
            sizeBytes: Long
        ) {
            val intent = Intent(context, OBDownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_FILENAME, filename)
                putExtra(EXTRA_FILE_PATH, filePath)
                putExtra(EXTRA_MIME_TYPE, mimeType)
                putExtra(EXTRA_SIZE_BYTES, sizeBytes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, OBDownloadService::class.java).apply {
                action = ACTION_PAUSE_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }

        fun resumeDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, OBDownloadService::class.java).apply {
                action = ACTION_RESUME_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancelDownload(context: Context, downloadId: Long) {
            val intent = Intent(context, OBDownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId <= 0) return START_NOT_STICKY

        when (intent.action) {
            ACTION_START_DOWNLOAD, ACTION_RESUME_DOWNLOAD -> {
                var url = intent.getStringExtra(EXTRA_URL) ?: ""
                var filename = intent.getStringExtra(EXTRA_FILENAME) ?: ""
                var filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: ""
                var sizeBytes = intent.getLongExtra(EXTRA_SIZE_BYTES, 0L)

                if (url.isBlank() || filePath.isBlank()) {
                    runBlocking {
                        val dbDl = downloadDao.getDownloadById(downloadId)
                        if (dbDl != null) {
                            url = dbDl.url
                            filename = dbDl.filename
                            filePath = dbDl.filePath
                            sizeBytes = dbDl.sizeBytes
                        }
                    }
                }

                pausedDownloadIds.remove(downloadId)
                if (url.isNotBlank()) {
                    startDownloadInternal(downloadId, url, filename, filePath, sizeBytes)
                }
            }
            ACTION_PAUSE_DOWNLOAD -> {
                pausedDownloadIds.add(downloadId)
                activeDownloadJobs[downloadId]?.cancel()
                activeDownloadJobs.remove(downloadId)
                val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notif = createPausedNotification(downloadId, "Download")
                notifManager.notify(NOTIF_ID_BASE + downloadId.toInt(), notif)
                serviceScope.launch {
                    val dbDl = downloadDao.getDownloadById(downloadId)
                    downloadDao.updateProgress(downloadId, DownloadStatus.Paused, dbDl?.downloadedBytes ?: 0L)
                }
            }
            ACTION_CANCEL_DOWNLOAD -> {
                pausedDownloadIds.remove(downloadId)
                activeDownloadJobs[downloadId]?.cancel()
                activeDownloadJobs.remove(downloadId)
                val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notifManager.cancel(NOTIF_ID_BASE + downloadId.toInt())
                serviceScope.launch {
                    val dbDl = downloadDao.getDownloadById(downloadId)
                    if (dbDl != null && dbDl.filePath.isNotBlank()) {
                        try {
                            val f = File(dbDl.filePath)
                            if (f.exists()) f.delete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    downloadDao.updateProgress(downloadId, DownloadStatus.Cancelled, 0L)
                }
                if (activeDownloadJobs.isEmpty()) {
                    stopForeground(true)
                }
            }
        }

        return START_STICKY
    }

    private fun startDownloadInternal(
        downloadId: Long,
        url: String,
        filename: String,
        filePath: String,
        sizeBytes: Long
    ) {
        if (activeDownloadJobs.containsKey(downloadId)) return

        val job = serviceScope.launch {
            val file = File(filePath)
            val downloadedBytes = if (file.exists()) file.length() else 0L

            val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val initialNotif = createProgressNotification(downloadId, filename, downloadedBytes, sizeBytes, 0, 0)
            startForeground(NOTIF_ID_BASE + downloadId.toInt(), initialNotif)

            if (url.startsWith("data:")) {
                try {
                    val base64Str = url.substringAfter(",")
                    val imageBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                    file.writeBytes(imageBytes)
                    downloadDao.updateProgress(
                        id = downloadId,
                        status = DownloadStatus.Completed,
                        bytes = imageBytes.size.toLong(),
                        etaSec = 0L,
                        speedBytesSec = 0L,
                        completedAt = System.currentTimeMillis()
                    )
                    showCompletionNotification(downloadId, filename, "Download Completed", true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    downloadDao.updateProgress(downloadId, DownloadStatus.Failed, 0L)
                    showCompletionNotification(downloadId, filename, "Download Failed", false)
                } finally {
                    activeDownloadJobs.remove(downloadId)
                    if (activeDownloadJobs.isEmpty()) {
                        stopForeground(false)
                    }
                }
                return@launch
            }

            try {
                val defaultUserAgent = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 OrbitBrowser/1.0"
                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", defaultUserAgent)
                    .header("Accept", "image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")

                val host = try { android.net.Uri.parse(url).host } catch (_: Exception) { null }
                if (!host.isNullOrBlank()) {
                    requestBuilder.header("Referer", "https://$host/")
                }

                val cookie = try { android.webkit.CookieManager.getInstance().getCookie(url) } catch (_: Exception) { null }
                if (!cookie.isNullOrBlank()) {
                    requestBuilder.header("Cookie", cookie)
                }

                if (downloadedBytes > 0) {
                    requestBuilder.addHeader("Range", "bytes=$downloadedBytes-")
                }

                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    downloadDao.updateProgress(downloadId, DownloadStatus.Failed, downloadedBytes)
                    showCompletionNotification(downloadId, filename, "Download Failed", false)
                    return@launch
                }

                val isRangeResponse = response.code == 206
                val totalBytes = if (isRangeResponse) {
                    downloadedBytes + body.contentLength()
                } else if (sizeBytes > 0) {
                    sizeBytes
                } else {
                    body.contentLength()
                }

                var currentBytes = if (isRangeResponse) downloadedBytes else 0L
                val outputStream = FileOutputStream(file, isRangeResponse)
                val inputStream: InputStream = body.byteStream()
                val buffer = ByteArray(8192)
                var read: Int

                var lastNotifTime = System.currentTimeMillis()
                var lastSpeedCalcTime = System.currentTimeMillis()
                var bytesSinceLastCalc = 0L

                downloadDao.updateProgress(downloadId, DownloadStatus.Downloading, currentBytes)

                while (inputStream.read(buffer).also { read = it } != -1) {
                    if (pausedDownloadIds.contains(downloadId) || !isActive) {
                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                        return@launch
                    }

                    outputStream.write(buffer, 0, read)
                    currentBytes += read
                    bytesSinceLastCalc += read

                    val now = System.currentTimeMillis()
                    if (now - lastNotifTime >= 600L) {
                        val timeDiffSec = (now - lastSpeedCalcTime) / 1000f
                        val speedBytesSec = if (timeDiffSec > 0) (bytesSinceLastCalc / timeDiffSec).toLong() else 0L
                        val etaSec = if (speedBytesSec > 0 && totalBytes > currentBytes) (totalBytes - currentBytes) / speedBytesSec else 0L

                        lastSpeedCalcTime = now
                        bytesSinceLastCalc = 0L
                        lastNotifTime = now

                        val percent = if (totalBytes > 0) ((currentBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

                        val notif = createProgressNotification(downloadId, filename, currentBytes, totalBytes, percent, etaSec)
                        notifManager.notify(NOTIF_ID_BASE + downloadId.toInt(), notif)

                        downloadDao.updateProgress(
                            id = downloadId,
                            status = DownloadStatus.Downloading,
                            bytes = currentBytes,
                            etaSec = etaSec,
                            speedBytesSec = speedBytesSec
                        )
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                downloadDao.updateProgress(downloadId, DownloadStatus.Completed, currentBytes)
                showCompletionNotification(downloadId, filename, "Download Completed", true)

            } catch (e: Exception) {
                if (!pausedDownloadIds.contains(downloadId)) {
                    e.printStackTrace()
                    downloadDao.updateProgress(downloadId, DownloadStatus.Failed, 0L)
                    showCompletionNotification(downloadId, filename, "Download Failed: ${e.localizedMessage ?: "Error"}", false)
                }
            } finally {
                activeDownloadJobs.remove(downloadId)
                if (activeDownloadJobs.isEmpty()) {
                    stopForeground(false)
                }
            }
        }

        activeDownloadJobs[downloadId] = job
    }

    private fun createProgressNotification(
        downloadId: Long,
        filename: String,
        downloadedBytes: Long,
        totalBytes: Long,
        percent: Int,
        etaSec: Long
    ): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, downloadId.toInt(), openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(this, OBDownloadService::class.java).apply {
            action = ACTION_PAUSE_DOWNLOAD
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val pausePendingIntent = PendingIntent.getService(
            this, (downloadId * 10 + 1).toInt(), pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, OBDownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, (downloadId * 10 + 2).toInt(), cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val etaStr = if (etaSec > 0) formatEta(etaSec) else "Downloading…"
        val percentStr = "$percent%"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle("Orbit Browser · $percentStr")
            .setContentText("$filename · $etaStr")
            .setSubText("Orbit Browser")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent, totalBytes <= 0)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Pause", pausePendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Cancel", cancelPendingIntent)
            .build()
    }

    private fun createPausedNotification(downloadId: Long, filename: String): android.app.Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, downloadId.toInt(), openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val resumeIntent = Intent(this, OBDownloadService::class.java).apply {
            action = ACTION_RESUME_DOWNLOAD
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val resumePendingIntent = PendingIntent.getService(
            this, (downloadId * 10 + 3).toInt(), resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, OBDownloadService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, (downloadId * 10 + 2).toInt(), cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle("Orbit Browser")
            .setContentText("Download Paused")
            .setSubText("Orbit Browser")
            .setOngoing(false)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Resume", resumePendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Cancel", cancelPendingIntent)
            .build()
    }

    private fun showCompletionNotification(downloadId: Long, filename: String, message: String, isSuccess: Boolean) {
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, downloadId.toInt(), openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle("Orbit Browser")
            .setContentText("$filename · $message")
            .setSubText("Orbit Browser")
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        notifManager.notify(NOTIF_ID_BASE + downloadId.toInt(), notif)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active download progress with Orbit Browser controls"
                setShowBadge(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun formatEta(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return when {
            mins >= 60 -> String.format("%d hrs %d mins left", mins / 60, mins % 60)
            mins > 0 -> String.format("%d mins %d secs left", mins, secs)
            else -> String.format("%d secs left", secs)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
