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
                "SET_SOUND_SENSITIVITY" -> SetSoundSensitivity(json.optDouble("db", 60.0).toFloat())
                "SET_MOTION_SENSITIVITY" -> SetMotionSensitivity(json.optDouble("level", 5.0).toFloat())
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
