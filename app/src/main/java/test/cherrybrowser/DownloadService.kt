package test.cherrybrowser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Minimal foreground download service using HttpURLConnection.
 * - Avoids system DownloadManager inconsistencies.
 * - Writes into app's external files dir (no storage permission required).
 */
class DownloadService : Service() {
    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_FILENAME = "filename"
        const val CHANNEL_ID = "cherry_downloads"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
        val filename = intent.getStringExtra(EXTRA_FILENAME) ?: url.substringAfterLast('/', "download-${System.currentTimeMillis()}")
        startForeground(1, buildNotification("Downloading $filename"))
        thread {
            download(url, filename)
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cherry Browser")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun download(urlString: String, filename: String) {
        try {
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }
            conn.connect()
            val outFile = java.io.File(getExternalFilesDir(null), filename)
            conn.inputStream.use { input ->
                FileOutputStream(outFile).use { out ->
                    val buf = ByteArray(8 * 1024)
                    var len: Int
                    while (input.read(buf).also { len = it } > 0) {
                        out.write(buf, 0, len)
                    }
                    out.flush()
                }
            }
            // Optionally: send a broadcast or tray notification with open action
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify((System.currentTimeMillis() % 10000).toInt(),
                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Download finished")
                    .setContentText(filename)
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentIntent(null)
                    .build()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
