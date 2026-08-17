package com.example.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Device admin receiver for the client (camera) flavor. Required for
 * Device Owner provisioning via:
 *
 *   adb shell dpm set-device-owner com.example.client/.admin.ClientDeviceAdminReceiver
 *
 * When provisioned as Device Owner the app can silently install/uninstall
 * packages, lock itself to kiosk (lock-task) mode, disable the camera, wipe
 * the device, etc. — all over Firebase remote commands. Without Device Owner
 * the receiver is inert and every elevated call no-ops.
 */
class ClientDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Log.i(TAG, "Device admin enabled")
        postStatusNotification(context, "Cam Guard client is now a device admin")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.i(TAG, "Device admin disabled")
        postStatusNotification(context, "Cam Guard client device admin removed")
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        Log.i(TAG, "Lock task mode entering for $pkg")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        Log.i(TAG, "Lock task mode exiting")
    }

    private fun postStatusNotification(context: Context, text: String) {
        try {
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Cam Guard")
                .setContentText(text)
                .setOngoing(false)
                .build()
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
        } catch (e: Exception) {
            Log.w(TAG, "Could not post device-admin notification: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "ClientDeviceAdmin"
        private const val CHANNEL_ID = "camguard_admin"
        private const val NOTIF_ID = 4242
    }
}
