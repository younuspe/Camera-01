package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.CameraForegroundService

/**
 * Relaunches the client's camera foreground service after the device reboots,
 * so monitoring resumes automatically without anyone opening the app.
 *
 * Note: Android will only allow this receiver to run after boot if the user
 * has opened the app at least once after install (standard security rule for
 * BOOT_COMPLETED receivers in apps installed via APK). It will then persist
 * across subsequent reboots.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Only the client (camera) flavor needs the foreground camera service.
        if (BuildConfig.IS_CLIENT_DEVICE) {
            CameraForegroundService.start(context)
        }
    }
}
