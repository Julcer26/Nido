package com.example

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.io.File

object OtaUpdateManager {
    
    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersionCode: Int,
        val downloadUrl: String
    )

    suspend fun checkForUpdates(currentVersionCode: Int): UpdateInfo {
        return try {
            val db = FirebaseDatabase.getInstance().reference.child("config")
            val snapshot = db.get().await()
            val latestVersion = snapshot.child("latest_version_code").getValue(Int::class.java) ?: 0
            val downloadUrl = snapshot.child("latest_apk_url").getValue(String::class.java) ?: ""
            
            UpdateInfo(
                isUpdateAvailable = latestVersion > currentVersionCode,
                latestVersionCode = latestVersion,
                downloadUrl = downloadUrl
            )
        } catch (e: Exception) {
            UpdateInfo(false, 0, "")
        }
    }

    fun downloadAndInstallUpdate(context: Context, url: String) {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("Actualización de Nido")
            setDescription("Descargando la nueva versión...")
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "app-update.apk")
            setMimeType("application/vnd.android.package-archive")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }

        // Delete previous update file if it exists so it doesn't get renamed to app-update-1.apk
        val oldFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "app-update.apk")
        if (oldFile.exists()) {
            oldFile.delete()
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(context)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(context: Context) {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "app-update.apk")
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
