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

    // --- Device Owner / kiosk commands (client flavor only) ---
    object SetKioskMode : RemoteCommand() { override val type = "SET_KIOSK" }
    object UnsetKioskMode : RemoteCommand() { override val type = "UNSET_KIOSK" }
    object LockDevice : RemoteCommand() { override val type = "LOCK_DEVICE" }
    object DisableCamera : RemoteCommand() { override val type = "DISABLE_CAMERA" }
    object EnableCamera : RemoteCommand() { override val type = "ENABLE_CAMERA" }

    data class UninstallPackage(val packageName: String) : RemoteCommand() {
        override val type = "UNINSTALL_PACKAGE"
    }

    data class WipeDevice(val wipeStorage: Boolean = false) : RemoteCommand() {
        override val type = "WIPE_DEVICE"
    }

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
            is UninstallPackage -> json.put("packageName", packageName)
            is WipeDevice -> json.put("wipeStorage", wipeStorage)
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
                "SET_KIOSK" -> SetKioskMode
                "UNSET_KIOSK" -> UnsetKioskMode
                "LOCK_DEVICE" -> LockDevice
                "DISABLE_CAMERA" -> DisableCamera
                "ENABLE_CAMERA" -> EnableCamera
                "UNINSTALL_PACKAGE" -> UninstallPackage(json.optString("packageName"))
                "WIPE_DEVICE" -> WipeDevice(json.optBoolean("wipeStorage", false))
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
