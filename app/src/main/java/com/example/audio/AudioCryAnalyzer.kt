package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

class AudioCryAnalyzer(
    private val onSoundLevelChanged: (dbLevel: Float, isCryOrAbnormalSound: Boolean) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private var listeningJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

    @SuppressLint("MissingPermission")
    fun startListening(sensitivityThresholdDb: Float) {
        if (isListening) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioCryAnalyzer", "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            isListening = true

            listeningJob = scope.launch {
                val buffer = ShortArray(bufferSize)
                var lastTriggerTime = 0L

                while (isActive && isListening) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / readSize)
                        val db = if (rms > 0) 20 * log10(rms) else 0.0
                        // Normalize dB roughly to 0..100 scale
                        val dbNormalized = (db * 1.2).coerceIn(0.0, 100.0).toFloat()

                        val now = System.currentTimeMillis()
                        val isAbnormal = dbNormalized >= sensitivityThresholdDb

                        if (isAbnormal && (now - lastTriggerTime > 3000)) {
                            lastTriggerTime = now
                            onSoundLevelChanged(dbNormalized, true)
                        } else {
                            onSoundLevelChanged(dbNormalized, false)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioCryAnalyzer", "Error starting audio analyzer", e)
        }
    }

    fun stopListening() {
        isListening = false
        listeningJob?.cancel()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
    }
}
