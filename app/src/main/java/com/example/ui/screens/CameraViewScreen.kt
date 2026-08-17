package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.Recorder
import androidx.camera.core.VideoCapture
import androidx.camera.core.VideoRecordEvent
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.camera.MotionAnalyzer
import com.example.camera.WatermarkUtil
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.SecurityUiState
import com.example.ui.viewmodel.SecurityViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.delay

private const val SNAPSHOT_INTERVAL_MS = 3000L

@Composable
fun CameraViewScreen(
    viewModel: SecurityViewModel,
    uiState: SecurityUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    // Handle to the active recording so we can stop it; null when not recording.
    val activeRecording = remember { mutableStateOf<androidx.camera.core.Recording?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    // Duration (seconds) captured at stop time, read when Finalize fires.
    val pendingDurationSec = remember { mutableStateOf(0L) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Pulsing recording effect
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Re-bind camera when lens or monitoring state changes
    LaunchedEffect(uiState.lensFacing, uiState.flashMode, uiState.isMonitoringActive) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                if (uiState.isMonitoringActive) {
                    val preview = Preview.Builder().build()
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(uiState.lensFacing)
                        .build()

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY) // Ultra High Picture Quality
                        .setFlashMode(
                            when (uiState.flashMode) {
                                1 -> ImageCapture.FLASH_MODE_ON
                                2 -> ImageCapture.FLASH_MODE_ON
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                        )
                        .build()

                    // Real video recording: CameraX Recorder -> VideoCapture use case.
                    val recorder = Recorder.Builder()
                        .setQualitySelector(androidx.camera.core.QualitySelector.from(androidx.camera.core.Quality.HD))
                        .build()
                    val vCapture = VideoCapture.withOutput(recorder)

                    val useCases = mutableListOf<androidx.camera.core.UseCase>(preview, capture, vCapture)

                    if (uiState.enableMotionSensing) {
                        val analysis = androidx.camera.core.ImageAnalysis.Builder()
                            .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        analysis.setAnalyzer(
                            cameraExecutor,
                            MotionAnalyzer(uiState.motionSensitivity) { score, isDetected ->
                                viewModel.updateMotionScore(score, isDetected)
                            }
                        )
                        useCases.add(analysis)
                    }

                    previewView?.let { pView ->
                        preview.setSurfaceProvider(pView.surfaceProvider)
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            *useCases.toTypedArray()
                        )
                        imageCapture = capture
                        videoCapture = vCapture
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraViewScreen", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // React to recording state changes (local button OR remote Firebase command):
    // start/stop the real CameraX recording to match uiState.isRecording.
    LaunchedEffect(uiState.isRecording) {
        val vCap = videoCapture
        if (uiState.isRecording) {
            // Only start if not already recording (avoid double-start).
            if (activeRecording.value == null && vCap != null) {
                val name = "VID_${System.currentTimeMillis()}.mp4"
                val outFile = File(context.filesDir, name)
                val recorder = vCap.output
                val fileOutput = androidx.camera.core.FileOutputOptions.builder(outFile).build()
                val pending = recorder
                    .prepareRecording(context, fileOutput)
                    .withAudioEnabled()
                val rec = pending.start(cameraExecutor) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        if (!event.hasError()) {
                            viewModel.onVideoSaved(outFile, pendingDurationSec.value)
                        } else {
                            Log.e("CameraViewScreen", "Recording error: ${event.error}")
                        }
                    }
                }
                activeRecording.value = rec
                recordedFile = outFile
                viewModel.startRecordingTimer()
            }
        } else {
            // Stop any active recording.
            if (activeRecording.value != null) {
                pendingDurationSec.value = viewModel.stopRecordingTimer()
                activeRecording.value?.stop()
                activeRecording.value = null
            }
        }
    }

    // Client flavor: periodically capture a low-res JPEG and publish it to
    // Firebase so the control phone can display a live view of this camera.
    // Runs only when monitoring is active and a camera use case is bound.
    LaunchedEffect(uiState.isMonitoringActive, uiState.snapshotPublishingEnabled) {
        if (!uiState.isMonitoringActive || !uiState.snapshotPublishingEnabled) return@LaunchedEffect
        while (true) {
            delay(SNAPSHOT_INTERVAL_MS)
            val capture = imageCapture ?: continue
            try {
                val out = ByteArrayOutputStream()
                // In-memory capture (no disk file) — small JPEG for the live view.
                val opts = ImageCapture.OutputFileOptions.Builder(out).build()
                capture.takePicture(
                    opts,
                    cameraExecutor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val jpeg = out.toByteArray()
                            val b64 = android.util.Base64.encodeToString(
                                jpeg, android.util.Base64.NO_WRAP
                            )
                            viewModel.publishClientSnapshot(b64)
                        }

                        override fun onError(exc: ImageCaptureException) {
                            Log.w("CameraViewScreen", "Snapshot capture failed: ${exc.message}")
                        }
                    }
                )
            } catch (e: Exception) {
                Log.w("CameraViewScreen", "Snapshot loop error: ${e.message}")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDark)
    ) {
        // Viewfinder
        if (uiState.isMonitoringActive) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewView = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camera_preview_view")
            )
        } else {
            // Disabled Camera Standby Screen
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Standby",
                        modifier = Modifier.size(72.dp),
                        tint = TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CAMERA MONITOR IN STANDBY",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextMuted,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap Active Monitoring to enable live viewfinder",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        // HUD Scanner Overlay (Corner crosshairs & target box)
        if (uiState.isMonitoringActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val boxWidth = w * 0.7f
                val boxHeight = h * 0.45f
                val left = (w - boxWidth) / 2
                val top = (h - boxHeight) / 2
                val right = left + boxWidth
                val bottom = top + boxHeight

                val lineLen = 40f
                val strokeW = 4f
                val hudColor = if (uiState.isMotionDetected) LiveRed else CyanAccent

                // Corners
                // Top-Left
                drawLine(hudColor, Offset(left, top), Offset(left + lineLen, top), strokeW)
                drawLine(hudColor, Offset(left, top), Offset(left, top + lineLen), strokeW)
                // Top-Right
                drawLine(hudColor, Offset(right, top), Offset(right - lineLen, top), strokeW)
                drawLine(hudColor, Offset(right, top), Offset(right, top + lineLen), strokeW)
                // Bottom-Left
                drawLine(hudColor, Offset(left, bottom), Offset(left + lineLen, bottom), strokeW)
                drawLine(hudColor, Offset(left, bottom), Offset(left, bottom - lineLen), strokeW)
                // Bottom-Right
                drawLine(hudColor, Offset(right, bottom), Offset(right - lineLen, bottom), strokeW)
                drawLine(hudColor, Offset(right, bottom), Offset(right, bottom - lineLen), strokeW)
            }
        }

        // Top Status Header - Security Compliance Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SlateCard.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isMonitoringActive) ActiveGreen else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isMonitoringActive) "LIVE MONITORING" else "STANDBY",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AES-256",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Abnormal Sound / Cry Alert Warning Banner
            AnimatedVisibility(
                visible = uiState.isCryDetected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = LiveRed.copy(alpha = 0.95f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔊 ABNORMAL SOUND / CRY DETECTED (${uiState.currentDecibels.toInt()} dB)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Audio Decibel Sound Meter
            if (uiState.isMonitoringActive && uiState.enableSoundSensing) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateCard.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "AUDIO MIC (${uiState.currentDecibels.toInt()} dB)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { (uiState.currentDecibels / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (uiState.isCryDetected) LiveRed else ActiveGreen,
                        trackColor = SlateDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SYNCED TO DUBAI",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, SlateDark.copy(alpha = 0.95f), SlateDark)
                    )
                )
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timestamp watermark badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SlateCard.copy(alpha = 0.8f))
                    .clickable { viewModel.toggleWatermark() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Subtitles,
                    contentDescription = "Watermark",
                    tint = if (uiState.enableWatermark) CyanAccent else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (uiState.enableWatermark) "WATERMARK: ON" else "WATERMARK: OFF",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.enableWatermark) TextPrimary else TextMuted
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Camera Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash Cycle Button
                IconButton(
                    onClick = { viewModel.cycleFlashMode() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(SlateCard, CircleShape)
                        .testTag("flash_button")
                ) {
                    Icon(
                        imageVector = when (uiState.flashMode) {
                            1 -> Icons.Default.FlashOn
                            2 -> Icons.Default.FlashAuto
                            else -> Icons.Default.FlashOff
                        },
                        contentDescription = "Flash Mode",
                        tint = if (uiState.flashMode > 0) WarningAmber else TextPrimary
                    )
                }

                // Snap Photo Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CyanAccent)
                        .clickable {
                            if (uiState.isMonitoringActive && imageCapture != null) {
                                takePhoto(
                                    context = context,
                                    imageCapture = imageCapture!!,
                                    enableWatermark = uiState.enableWatermark,
                                    customLabel = uiState.customWatermarkText,
                                    onPhotoSaved = { file ->
                                        viewModel.onPhotoSaved(file)
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Turn on active monitoring first", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(4.dp)
                        .testTag("capture_photo_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(3.dp, SlateDark, CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Snap Photo",
                            tint = SlateDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Record Video Toggle — flips isRecording; the LaunchedEffect
                // above starts/stops the real CameraX recording to match state.
                // This makes both the local button and remote Firebase commands work.
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isRecording) LiveRed else SlateCard)
                        .clickable {
                            if (videoCapture == null) {
                                Toast.makeText(context, "Camera not ready", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.toggleRecording()
                            }
                        }
                        .testTag("record_video_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.RadioButtonChecked,
                        contentDescription = "Record Video",
                        tint = if (uiState.isRecording) Color.White else LiveRed,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Camera Lens Switch
                IconButton(
                    onClick = { viewModel.toggleLensFacing() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(SlateCard, CircleShape)
                        .testTag("flip_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Flip Camera",
                        tint = TextPrimary
                    )
                }
            }

            // Recording Timer text
            if (uiState.isRecording) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(LiveRed.copy(alpha = alphaPulse))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "REC ${formatSeconds(uiState.recordingDurationSeconds)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = LiveRed,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    enableWatermark: Boolean,
    customLabel: String,
    onPhotoSaved: (File) -> Unit
) {
    val photoFile = File(
        context.filesDir,
        "CAMGUARD_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val finalFile = if (enableWatermark) {
                    WatermarkUtil.applyTimestampWatermark(
                        imageFile = photoFile,
                        customLabel = customLabel
                    )
                } else {
                    photoFile
                }
                onPhotoSaved(finalFile)
                Toast.makeText(context, "Snapshot captured!", Toast.LENGTH_SHORT).show()
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraViewScreen", "Photo capture failed: ${exc.message}", exc)
                Toast.makeText(context, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

private fun formatSeconds(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
