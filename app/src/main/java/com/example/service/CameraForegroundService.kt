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

    override fun onCreate() {
        super.onCreate()
        createChannel()
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

    override fun onBind(intent: Intent?): IBinder? = null

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
