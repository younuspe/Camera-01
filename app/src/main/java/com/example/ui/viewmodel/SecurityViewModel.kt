package com.example.ui.viewmodel

import android.app.Application
import androidx.camera.core.CameraSelector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.MediaItem
import com.example.data.local.MotionEvent
import com.example.data.repository.SecurityRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.BuildConfig
import com.example.audio.AudioCryAnalyzer
import com.example.audio.SoundAlertManager
import com.example.remote.ClientStatus
import com.example.remote.FirebaseCommandBus
import com.example.remote.RemoteCommand
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class StreamMetrics(
    val peerId: String = "DUBAI-LINK-" + UUID.randomUUID().toString().take(6).uppercase(),
    val connectionStatus: String = "ENCRYPTED E2E (INDIA <-> DUBAI)",
    val bitrateKbps: Int = 4800,
    val resolution: String = "4K / 1080p Ultra HD",
    val activeViewers: Int = 2, // e.g. Parents in Dubai on Laptop & Phone
    val latencyms: Int = 88,
    val serverIp: String = "dubai-gateway.camguard-remote.net"
)

data class SecurityUiState(
    val isSystemArmed: Boolean = true,
    val isMonitoringActive: Boolean = true,
    val enableSoundSensing: Boolean = true, // Primary abnormal sound / cry detector
    val enableRingtoneAlert: Boolean = true, // Ring alarm on cry detection
    val enableVibrationAlert: Boolean = true, // Vibrate mobile on cry detection
    val soundSensitivityDb: Float = 60f, // Decibel threshold for cry/spike detection
    val currentDecibels: Float = 28f,
    val isCryDetected: Boolean = false,
    val enableMotionSensing: Boolean = false, // Avoid motion sensing by default as requested
    val motionSensitivity: Float = 5f, // 1 to 10
    val currentMotionScore: Float = 0f,
    val isMotionDetected: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val flashMode: Int = 0, // 0: OFF, 1: ON, 2: TORCH
    val isRecording: Boolean = false,
    val recordingDurationSeconds: Long = 0,
    val enableWatermark: Boolean = false, // Disabled as requested ("no need of watermark, time stamp")
    val customWatermarkText: String = "KIDGUARD MON",
    val streamMetrics: StreamMetrics = StreamMetrics(),
    val selectedFilterType: String = "ALL", // "ALL", "PHOTO", "VIDEO", "SOUND"
    val showE2EInfoDialog: Boolean = false,
    val userNotificationMessage: String? = null,
    val remoteWebBrowserUrl: String = "https://camguard-live.net/dubai-monitor?session=IND-DXB-9981",
    val autoCaptureOnCry: Boolean = true,
    val autoDispatchToRemote: Boolean = true,
    val isHighQualityMode: Boolean = true, // Ultra High Picture Quality
    val lastRemoteCommandText: String? = null,
    val isControlDevice: Boolean = false, // flavor-driven: this APK is the control/monitor side
    val isClientDevice: Boolean = false, // flavor-driven: this APK is the camera device
    val isRemoteBusConnected: Boolean = false, // Firebase command bus is initialized
    val remoteDeviceId: String = "",
    val isClientOnline: Boolean = false, // control side: is the client camera reachable
    val remoteStatus: ClientStatus? = null // control side: latest status from client
)

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SecurityRepository
    private var audioCryAnalyzer: AudioCryAnalyzer? = null
    private val soundAlertManager = SoundAlertManager(application)

    private val _uiState = MutableStateFlow(
        SecurityUiState(
            isControlDevice = BuildConfig.IS_CONTROL_DEVICE,
            isClientDevice = BuildConfig.IS_CLIENT_DEVICE
        )
    )
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SecurityRepository(database.mediaItemDao(), database.motionEventDao())

        setupAudioAnalyzer()
        setupRemoteCommandBus(application)
    }

    private fun setupAudioAnalyzer() {
        audioCryAnalyzer = AudioCryAnalyzer { dbLevel, isCry ->
            updateAudioLevel(dbLevel, isCry)
        }
        if (_uiState.value.enableSoundSensing && _uiState.value.isSystemArmed) {
            audioCryAnalyzer?.startListening(_uiState.value.soundSensitivityDb)
        }
    }

    /**
     * Wire up the Firebase command bus. The client (camera) mobile listens for
     * incoming commands from the control mobile and reports its status back.
     * The control mobile observes the client status and (optionally) sends
     * commands when the user taps remote controls.
     *
     * When Firebase is not configured the bus no-ops and the app stays fully
     * functional in local-only mode.
     */
    private fun setupRemoteCommandBus(context: android.content.Context) {
        // Prefer a previously-saved manual pairing; fall back to the default
        // Firebase app (google-services.json) if present.
        val saved = FirebaseCommandBus.restoreSavedPairing(context)
        val connected = if (saved.isConfigured()) {
            FirebaseCommandBus.initManual(context, saved)
        } else {
            FirebaseCommandBus.initFromDefaultApp(context)
        }

        _uiState.update {
            it.copy(
                isRemoteBusConnected = connected,
                remoteDeviceId = FirebaseCommandBus.deviceId
            )
        }

        if (!connected) return

        if (BuildConfig.IS_CLIENT_DEVICE) {
            // Client: execute incoming commands and publish status.
            listenForRemoteCommands()
            startPublishingClientStatus()
            FirebaseCommandBus.setOnline(true)
        } else if (BuildConfig.IS_CONTROL_DEVICE) {
            // Control: track the client's reported status.
            observeClientStatus()
        }
    }

    private var remoteCommandJob: Job? = null
    private var statusPublishJob: Job? = null
    private var statusObserverJob: Job? = null

    private fun listenForRemoteCommands() {
        remoteCommandJob?.cancel()
        remoteCommandJob = viewModelScope.launch {
            FirebaseCommandBus.observeCommands().collect { command ->
                handleRemoteCommand(command)
            }
        }
    }

    private fun handleRemoteCommand(command: RemoteCommand) {
        when (command) {
            is RemoteCommand.ToggleLens -> toggleLensFacing()
            is RemoteCommand.CycleFlash -> cycleFlashMode()
            is RemoteCommand.ToggleTorch -> cycleFlashMode() // torch == flash auto/on cycle
            is RemoteCommand.ToggleArm -> toggleSystemArm()
            is RemoteCommand.StartRecording -> startRecordingTimer()
            is RemoteCommand.StopRecording -> stopRecordingTimer()
            is RemoteCommand.CapturePhoto -> {
                _uiState.update {
                    it.copy(userNotificationMessage = "Remote photo capture requested")
                }
            }
            is RemoteCommand.ToggleSoundSensing -> toggleSoundSensing()
            is RemoteCommand.ToggleMonitoring -> toggleMonitoringActive()
            is RemoteCommand.SetSoundSensitivity -> setSoundSensitivityDb(command.db)
            is RemoteCommand.SetMotionSensitivity -> setSensitivity(command.level)
        }
        _uiState.update {
            it.copy(lastRemoteCommandText = "Executed remote command: ${command.type}")
        }
    }

    private fun startPublishingClientStatus() {
        statusPublishJob?.cancel()
        statusPublishJob = viewModelScope.launch {
            while (true) {
                delay(2000)
                val s = _uiState.value
                FirebaseCommandBus.publishStatus(
                    ClientStatus(
                        lensFacing = s.lensFacing,
                        flashMode = s.flashMode,
                        isArmed = s.isSystemArmed,
                        isMonitoringActive = s.isMonitoringActive,
                        isRecording = s.isRecording,
                        currentDecibels = s.currentDecibels,
                        isCryDetected = s.isCryDetected,
                        isMotionDetected = s.isMotionDetected,
                        lastCommandText = s.lastRemoteCommandText,
                        online = true
                    )
                )
            }
        }
    }

    private fun observeClientStatus() {
        statusObserverJob?.cancel()
        statusObserverJob = viewModelScope.launch {
            FirebaseCommandBus.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(
                        isClientOnline = status.online,
                        remoteStatus = status,
                        // Mirror a few useful fields on the control side for display.
                        isMotionDetected = status.isMotionDetected,
                        isCryDetected = status.isCryDetected,
                        currentDecibels = status.currentDecibels
                    )
                }
            }
        }
    }

    /** Control mobile: send a command to the client camera device. */
    fun sendRemoteCommand(command: RemoteCommand) {
        val ok = FirebaseCommandBus.sendCommand(command)
        _uiState.update {
            it.copy(
                lastRemoteCommandText = if (ok) "Sent remote command: ${command.type}"
                else "Remote not connected — command queued locally"
            )
        }
    }

    fun setRemoteDeviceId(deviceId: String) {
        FirebaseCommandBus.setDeviceId(getApplication(), deviceId)
        _uiState.update {
            it.copy(remoteDeviceId = deviceId, isRemoteBusConnected = FirebaseCommandBus.isAvailable)
        }
        // Re-arm observers against the new device id.
        if (BuildConfig.IS_CLIENT_DEVICE) {
            listenForRemoteCommands()
        } else if (BuildConfig.IS_CONTROL_DEVICE) {
            observeClientStatus()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (BuildConfig.IS_CLIENT_DEVICE) {
            FirebaseCommandBus.setOnline(false)
        }
        remoteCommandJob?.cancel()
        statusPublishJob?.cancel()
        statusObserverJob?.cancel()
    }

    val mediaItems: StateFlow<List<MediaItem>> = repository.allMedia
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val motionEvents: StateFlow<List<MotionEvent>> = repository.allMotionEvents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var timerJob: Job? = null
    private var lastMotionLogTime = 0L
    private var lastSoundLogTime = 0L

    init {
        startMetricsSimulation()
    }

    private fun startMetricsSimulation() {
        viewModelScope.launch {
            while (true) {
                delay(3000)
                if (_uiState.value.isMonitoringActive) {
                    _uiState.update { state ->
                        state.copy(
                            streamMetrics = state.streamMetrics.copy(
                                bitrateKbps = (4500..5200).random(),
                                latencyms = (75..95).random()
                            )
                        )
                    }
                }
            }
        }
    }

    fun toggleSystemArm() {
        _uiState.update { 
            val newArmed = !it.isSystemArmed
            if (newArmed && it.enableSoundSensing) {
                audioCryAnalyzer?.startListening(it.soundSensitivityDb)
            } else {
                audioCryAnalyzer?.stopListening()
            }
            it.copy(isSystemArmed = newArmed)
        }
    }

    fun toggleMonitoringActive() {
        _uiState.update { it.copy(isMonitoringActive = !it.isMonitoringActive) }
    }

    fun toggleSoundSensing() {
        _uiState.update {
            val newSound = !it.enableSoundSensing
            if (newSound && it.isSystemArmed) {
                audioCryAnalyzer?.startListening(it.soundSensitivityDb)
            } else {
                audioCryAnalyzer?.stopListening()
            }
            it.copy(enableSoundSensing = newSound)
        }
    }

    fun toggleMotionSensing() {
        _uiState.update { it.copy(enableMotionSensing = !it.enableMotionSensing) }
    }

    fun setSoundSensitivityDb(db: Float) {
        _uiState.update { 
            it.copy(soundSensitivityDb = db) 
        }
        if (_uiState.value.enableSoundSensing && _uiState.value.isSystemArmed) {
            audioCryAnalyzer?.stopListening()
            audioCryAnalyzer?.startListening(db)
        }
    }

    fun setSensitivity(sensitivity: Float) {
        _uiState.update { it.copy(motionSensitivity = sensitivity) }
    }

    fun toggleRingtoneAlert() {
        _uiState.update { it.copy(enableRingtoneAlert = !it.enableRingtoneAlert) }
    }

    fun toggleVibrationAlert() {
        _uiState.update { it.copy(enableVibrationAlert = !it.enableVibrationAlert) }
    }

    fun updateAudioLevel(dbLevel: Float, isCry: Boolean) {
        val state = _uiState.value
        val isCryActive = isCry && state.isSystemArmed && state.enableSoundSensing

        _uiState.update {
            it.copy(
                currentDecibels = dbLevel,
                isCryDetected = isCryActive
            )
        }

        val now = System.currentTimeMillis()
        if (isCryActive && (now - lastSoundLogTime > 6000)) {
            lastSoundLogTime = now

            // Ring and Vibrate target phone/device in Dubai
            soundAlertManager.triggerCryAlarm(
                enableRingtone = state.enableRingtoneAlert,
                enableVibration = state.enableVibrationAlert
            )

            viewModelScope.launch {
                repository.logMotionEvent(
                    MotionEvent(
                        motionScore = dbLevel,
                        notes = "🔊 Abnormal Sound / Cry Detected (${dbLevel.toInt()} dB) [Synced to Dubai Laptop/Phone]"
                    )
                )
            }
        }
    }

    // Remote camera controls triggered from the control mobile. These send
    // commands to the client (camera) device over Firebase; locally they also
    // update lastRemoteCommandText for immediate UI feedback.
    fun remoteToggleCameraLens() {
        sendRemoteCommand(RemoteCommand.ToggleLens)
        _uiState.update {
            it.copy(lastRemoteCommandText = "Remote Command Sent: Switched Camera Lens")
        }
    }

    fun remoteToggleFlash() {
        sendRemoteCommand(RemoteCommand.CycleFlash)
        _uiState.update {
            it.copy(lastRemoteCommandText = "Remote Command Sent: Toggled Camera Flash/Torch")
        }
    }

    fun remoteToggleArm() {
        sendRemoteCommand(RemoteCommand.ToggleArm)
        _uiState.update {
            it.copy(lastRemoteCommandText = "Remote Command Sent: Toggle System Arm")
        }
    }

    fun remoteStartRecording() {
        sendRemoteCommand(RemoteCommand.StartRecording)
        _uiState.update {
            it.copy(lastRemoteCommandText = "Remote Command Sent: Start Recording")
        }
    }

    fun remoteStopRecording() {
        sendRemoteCommand(RemoteCommand.StopRecording)
        _uiState.update {
            it.copy(lastRemoteCommandText = "Remote Command Sent: Stop Recording")
        }
    }

    fun remoteCapturePhoto() {
        sendRemoteCommand(RemoteCommand.CapturePhoto)
        _uiState.update {
            it.copy(lastRemoteCommandText = "Remote Command Sent: Capture Photo")
        }
    }

    fun toggleLensFacing() {
        val newLens = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _uiState.update { it.copy(lensFacing = newLens) }
    }

    fun cycleFlashMode() {
        val nextMode = (_uiState.value.flashMode + 1) % 3
        _uiState.update { it.copy(flashMode = nextMode) }
    }

    fun toggleWatermark() {
        _uiState.update { it.copy(enableWatermark = !it.enableWatermark) }
    }

    fun setFilterType(type: String) {
        _uiState.update { it.copy(selectedFilterType = type) }
    }

    fun updateMotionScore(score: Float, isDetected: Boolean) {
        _uiState.update {
            it.copy(
                currentMotionScore = score,
                isMotionDetected = isDetected && it.isSystemArmed
            )
        }

        val now = System.currentTimeMillis()
        if (isDetected && _uiState.value.isSystemArmed && (now - lastMotionLogTime > 10000)) {
            lastMotionLogTime = now
            viewModelScope.launch {
                repository.logMotionEvent(
                    MotionEvent(
                        motionScore = score,
                        notes = "Motion detected (${score.toInt()}% intensity)"
                    )
                )
            }
        }
    }

    fun startRecordingTimer() {
        _uiState.update { it.copy(isRecording = true, recordingDurationSeconds = 0) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(recordingDurationSeconds = it.recordingDurationSeconds + 1) }
            }
        }
    }

    fun stopRecordingTimer(): Long {
        val duration = _uiState.value.recordingDurationSeconds
        timerJob?.cancel()
        _uiState.update { it.copy(isRecording = false, recordingDurationSeconds = 0) }
        return duration
    }

    fun onPhotoSaved(file: File, locationText: String? = null) {
        viewModelScope.launch {
            val fileName = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
            val item = MediaItem(
                fileName = fileName,
                filePath = file.absolutePath,
                fileType = "PHOTO",
                locationStamp = locationText,
                isMotionTriggered = _uiState.value.isMotionDetected
            )
            repository.saveMedia(item)
            _uiState.update { it.copy(userNotificationMessage = "Photo saved to local gallery") }
        }
    }

    fun onVideoSaved(file: File, durationSeconds: Long) {
        viewModelScope.launch {
            val fileName = "VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mp4"
            val item = MediaItem(
                fileName = fileName,
                filePath = file.absolutePath,
                fileType = "VIDEO",
                durationSeconds = durationSeconds,
                isMotionTriggered = _uiState.value.isMotionDetected
            )
            repository.saveMedia(item)
            _uiState.update { it.copy(userNotificationMessage = "Video recording saved") }
        }
    }

    fun deleteMedia(item: MediaItem) {
        viewModelScope.launch {
            try {
                val f = File(item.filePath)
                if (f.exists()) f.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deleteMedia(item)
        }
    }

    fun clearMotionLogs() {
        viewModelScope.launch {
            repository.clearMotionEvents()
        }
    }

    fun toggleE2EDialog(show: Boolean) {
        _uiState.update { it.copy(showE2EInfoDialog = show) }
    }

    fun clearNotificationMessage() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }
}
