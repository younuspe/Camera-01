package com.example.camera

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.abs

class MotionAnalyzer(
    private val sensitivity: Float, // 1.0 (low) to 10.0 (high)
    private val onMotionChanged: (score: Float, isDetected: Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private var previousBuffer: ByteArray? = null
    private var lastAnalysisTimestamp = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        // Throttle analysis to roughly 10 fps to save battery/cpu
        if (currentTimestamp - lastAnalysisTimestamp < 100) {
            image.close()
            return
        }
        lastAnalysisTimestamp = currentTimestamp

        val planes = image.planes
        if (planes.isEmpty()) {
            image.close()
            return
        }

        val yBuffer: ByteBuffer = planes[0].buffer
        val ySize = yBuffer.remaining()

        // Downsample for performance (e.g., sample every 16th byte)
        val step = 16
        val sampleCount = ySize / step
        val currentSample = ByteArray(sampleCount)

        yBuffer.rewind()
        var idx = 0
        var i = 0
        while (i < ySize && idx < sampleCount) {
            currentSample[idx] = yBuffer.get(i)
            idx++
            i += step
        }

        val prev = previousBuffer
        if (prev != null && prev.size == currentSample.size) {
            var totalDiff = 0L
            val thresholdPixel = (30 - (sensitivity * 2)).coerceAtLeast(5f).toInt()

            for (j in currentSample.indices) {
                val diff = abs((currentSample[j].toInt() and 0xFF) - (prev[j].toInt() and 0xFF))
                if (diff > thresholdPixel) {
                    totalDiff += diff
                }
            }

            val maxDiff = sampleCount * 255L
            val normalizedScore = ((totalDiff.toDouble() / maxDiff) * 1000.0).toFloat().coerceIn(0f, 100f)
            val isMotionTriggered = normalizedScore > (15f / sensitivity).coerceAtLeast(2f)

            onMotionChanged(normalizedScore, isMotionTriggered)
        }

        previousBuffer = currentSample
        image.close()
    }
}
