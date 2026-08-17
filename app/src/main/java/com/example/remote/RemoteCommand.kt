package com.example.remote

import org.json.JSONObject

/**
 * Remote commands sent from the control mobile to the client (camera) mobile.
 *
 * These travel over Firebase Realtime Database under
 *   /devices/{deviceId}/commands
 * where the client device listens for new entries and the control device
 * pushes them. The client executes the command and reports status back under
 *   /devices/{deviceId}/status
 */
sealed class RemoteCommand {
    abstract val type: String

    object ToggleLens : RemoteCommand() { override val type = "TOGGLE_LENS" }
    object CycleFlash : RemoteCommand() { override val type = "CYCLE_FLASH" }
    object ToggleTorch : RemoteCommand() { override val type = "TOGGLE_TORCH" }
    object ToggleArm : RemoteCommand() { override val type = "TOGGLE_ARM" }
    object StartRecording : RemoteCommand() { override val type = "START_RECORDING" }
    object StopRecording : RemoteCommand() { override val type = "STOP_RECORDING" }
    object CapturePhoto : RemoteCommand() { override val type = "CAPTURE_PHOTO" }
    object ToggleSoundSensing : RemoteCommand() { override val type = "TOGGLE_SOUND_SENSING" }
    object ToggleMonitoring : RemoteCommand() { override val type = "TOGGLE_MONITORING" }
    object StartLiveView : RemoteCommand() { override val type = "START_LIVE_VIEW" }
    object StopLiveView : RemoteCommand() { override val type = "STOP_LIVE_VIEW" }

    // --- App-icon visibility (no Device Owner needed; uses PackageManager) ---
    object HideAppIcon : RemoteCommand() { override val type = "HIDE_APP_ICON" }
    object ShowAppIcon : RemoteCommand() { override val type = "SHOW_APP_ICON" }

    // NOTE: Device Owner commands (remote lock / disable camera / uninstall /
    // wipe / true kiosk) require Device Owner provisioning, which itself
    // requires either a factory reset (QR provisioning) or a USB connection
    // (adb dpm). The user cannot factory-reset or enable USB debugging, so
    // these are NOT supported here and have been removed to avoid confusion.

    data class SetSoundSensitivity(val db: Float) : RemoteCommand() {
        override val type = "SET_SOUND_SENSITIVITY"
    }

    data class SetMotionSensitivity(val level: Float) : RemoteCommand() {
        override val type = "SET_MOTION_SENSITIVITY"
    }

    fun toJson(): String {
        val json = JSONObject()
        json.put("type", type)
        json.put("timestamp", System.currentTimeMillis())
        when (this) {
            is SetSoundSensitivity -> json.put("db", db)
            is SetMotionSensitivity -> json.put("level", level)
            else -> {}
        }
        return json.toString()
    }

    companion object {
        fun fromJson(raw: String): RemoteCommand? = try {
            val json = JSONObject(raw)
            when (json.optString("type")) {
                "TOGGLE_LENS" -> ToggleLens
                "CYCLE_FLASH" -> CycleFlash
                "TOGGLE_TORCH" -> ToggleTorch
                "TOGGLE_ARM" -> ToggleArm
                "START_RECORDING" -> StartRecording
                "STOP_RECORDING" -> StopRecording
                "CAPTURE_PHOTO" -> CapturePhoto
                "TOGGLE_SOUND_SENSING" -> ToggleSoundSensing
                "TOGGLE_MONITORING" -> ToggleMonitoring
                "START_LIVE_VIEW" -> StartLiveView
                "STOP_LIVE_VIEW" -> StopLiveView
                "HIDE_APP_ICON" -> HideAppIcon
                "SHOW_APP_ICON" -> ShowAppIcon
                "SET_SOUND_SENSITIVITY" -> SetSoundSensitivity(json.optDouble("db", 60.0).toFloat())
                "SET_MOTION_SENSITIVITY" -> SetMotionSensitivity(json.optDouble("level", 5.0).toFloat())
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
