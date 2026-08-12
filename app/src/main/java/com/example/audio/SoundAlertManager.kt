package com.example.audio

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class SoundAlertManager(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            Log.e("SoundAlertManager", "ToneGenerator init failed", e)
        }
    }

    fun triggerCryAlarm(enableRingtone: Boolean, enableVibration: Boolean) {
        if (enableVibration) {
            vibrateDevice()
        }

        if (enableRingtone) {
            playAlarmTone()
        }
    }

    private fun vibrateDevice() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                val pattern = VibrationEffect.createWaveform(longArrayOf(0, 400, 150, 400, 150, 600), -1)
                vibrator.vibrate(pattern)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = VibrationEffect.createWaveform(longArrayOf(0, 400, 150, 400, 150, 600), -1)
                    vibrator.vibrate(pattern)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 400, 150, 400, 150, 600), -1)
                }
            }
        } catch (e: Exception) {
            Log.e("SoundAlertManager", "Vibration error", e)
        }
    }

    private fun playAlarmTone() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 800)
            
            // Also play standard notification/alarm ringtone if available
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e("SoundAlertManager", "Ringtone error", e)
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        toneGenerator = null
    }
}
