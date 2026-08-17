package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

/**
 * Keeps the Cam Guard client (camera) process alive in the foreground so the
 * camera and microphone keep streaming/recording while the app is backgrounded
 * or the screen is off. Android 11+ requires a foreground service of type
 * camera (and/or microphone) for background camera/mic access; without it the
 * system kills the camera session within seconds of backgrounding.
 *
 * The persistent notification is REQUIRED by the OS and cannot be hidden.
 */
class CameraForegroundService : Service() {

    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIF_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                )
            } else {
                startForeground(NOTIF_ID, notification)
            }
            Log.i(TAG, "Foreground camera service started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Holds a PARTIAL_WAKE_LOCK so the CPU and camera pipeline keep running
     * while the screen is off — WITHOUT turning the screen on. This is the
     * key to background camera operation on Android 13+ where the OS otherwise
     * suspends camera access shortly after the display sleeps.
     */
    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = pm.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK,
                "CamGuard::CameraWake"
            ).apply { acquire(/* no timeout — held for the service lifetime */) }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Could not release wake lock: ${e.message}")
        }
    }

    private fun buildNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Cam Guard")
            .setContentText("Camera monitoring is active")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cam Guard Camera",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the camera running while Cam Guard is in the background."
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "CameraFgService"
        private const val CHANNEL_ID = "camguard_camera"
        private const val NOTIF_ID = 9001

        fun start(context: Context) {
            val intent = Intent(context, CameraForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not start foreground service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, CameraForegroundService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Could not stop foreground service: ${e.message}")
            }
        }
    }
}
