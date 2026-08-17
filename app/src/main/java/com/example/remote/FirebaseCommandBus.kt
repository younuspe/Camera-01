package com.example.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject

/**
 * Shared configuration for pairing the control and client devices. Both devices
 * must use the same [deviceId] so they rendezvous on the same Realtime Database
 * node. Persist the pairing id on device so it survives restarts.
 */
data class DevicePairing(
    val deviceId: String,
    val firebaseApiKey: String? = null,
    val firebaseDatabaseUrl: String? = null,
    val firebaseAppId: String? = null,
    val firebaseProjectId: String? = null
) {
    fun isConfigured(): Boolean =
        !firebaseDatabaseUrl.isNullOrBlank() &&
            !firebaseApiKey.isNullOrBlank() &&
            !firebaseAppId.isNullOrBlank() &&
            !firebaseProjectId.isNullOrBlank()
}

/**
 * Status the client (camera) mobile publishes back to the control mobile so the
 * viewer sees live lens/flash/arm/record state.
 */
data class ClientStatus(
    val lensFacing: Int,
    val flashMode: Int,
    val isArmed: Boolean,
    val isMonitoringActive: Boolean,
    val isRecording: Boolean,
    val currentDecibels: Float,
    val isCryDetected: Boolean,
    val isMotionDetected: Boolean,
    val lastCommandText: String?,
    val online: Boolean = true
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("lensFacing", lensFacing)
        json.put("flashMode", flashMode)
        json.put("isArmed", isArmed)
        json.put("isMonitoringActive", isMonitoringActive)
        json.put("isRecording", isRecording)
        json.put("currentDecibels", currentDecibels)
        json.put("isCryDetected", isCryDetected)
        json.put("isMotionDetected", isMotionDetected)
        json.put("lastCommandText", lastCommandText ?: "")
        json.put("online", online)
        json.put("timestamp", System.currentTimeMillis())
        return json.toString()
    }

    companion object {
        fun fromJson(raw: String): ClientStatus? = try {
            val j = JSONObject(raw)
            ClientStatus(
                lensFacing = j.optInt("lensFacing"),
                flashMode = j.optInt("flashMode"),
                isArmed = j.optBoolean("isArmed"),
                isMonitoringActive = j.optBoolean("isMonitoringActive"),
                isRecording = j.optBoolean("isRecording"),
                currentDecibels = j.optDouble("currentDecibels", 0.0).toFloat(),
                isCryDetected = j.optBoolean("isCryDetected"),
                isMotionDetected = j.optBoolean("isMotionDetected"),
                lastCommandText = j.optString("lastCommandText").ifBlank { null },
                online = j.optBoolean("online", true)
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Thin wrapper over Firebase Realtime Database used as the command/status bus
 * between the control mobile and the client (camera) mobile.
 *
 * If Firebase is not configured (no google-services.json and no manual pairing),
 * every operation degrades to a safe no-op and [isAvailable] returns false so
 * callers can fall back to local-only behavior. This keeps the app fully
 * functional offline and the build green without credentials.
 */
object FirebaseCommandBus {

    private const val TAG = "FirebaseCommandBus"
    private const val APP_NAME = "camguard-remote"
    private const val PREFS = "camguard_remote_prefs"
    private const val PREF_DEVICE_ID = "device_id"
    private const val PREF_API_KEY = "fb_api_key"
    private const val PREF_DB_URL = "fb_db_url"
    private const val PREF_APP_ID = "fb_app_id"
    private const val PREF_PROJECT_ID = "fb_project_id"

    @Volatile
    private var database: FirebaseDatabase? = null

    @Volatile
    private var pairing: DevicePairing = DevicePairing(deviceId = "")

    val isAvailable: Boolean get() = database != null
    val deviceId: String get() = pairing.deviceId

    /**
     * Initialize from a google-services.json baked into the APK (the default
     * Firebase app). Because the google-services Gradle plugin is NOT applied,
     * this only succeeds when a config was provided some other way (e.g. a
     * manually-placed google-services.json processed by a custom build step).
     * Safe to call even when no config is present — returns false and leaves
     * the bus inactive.
     */
    fun initFromDefaultApp(context: Context): Boolean {
        return try {
            val defaultApp = FirebaseApp.initializeApp(context)
            val db = defaultApp?.let { FirebaseDatabase.getInstance(it) }
            val savedId = loadSavedDeviceId(context)
            val id = savedId.ifBlank { "camguard-" + randomSuffix() }
            if (savedId.isBlank()) saveDeviceId(context, id)
            pairing = DevicePairing(deviceId = id)
            database = db?.apply { setPersistenceEnabled(true) }
            Log.i(TAG, "Default Firebase app init. deviceId=$id available=${db != null}")
            db != null
        } catch (e: Exception) {
            Log.w(TAG, "Default Firebase app not configured: ${e.message}")
            database = null
            false
        }
    }

    /**
     * Initialize from a manually-provided pairing (e.g. entered in Settings).
     * Useful when you don't want to ship google-services.json.
     */
    fun initManual(context: Context, config: DevicePairing): Boolean {
        if (!config.isConfigured()) {
            Log.w(TAG, "Manual config incomplete; ignoring.")
            database = null
            pairing = config
            return false
        }
        return try {
            val options = FirebaseOptions.Builder()
                .setApiKey(config.firebaseApiKey)
                .setDatabaseUrl(config.firebaseDatabaseUrl)
                .setApplicationId(config.firebaseAppId)
                .setProjectId(config.firebaseProjectId)
                .build()
            val app = FirebaseApp.initializeApp(context, options, APP_NAME)
            val db = FirebaseDatabase.getInstance(app).apply { setPersistenceEnabled(true) }
            pairing = config
            database = db
            savePairing(context, config)
            Log.i(TAG, "Manual Firebase init OK. deviceId=${config.deviceId}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Manual Firebase init failed", e)
            database = null
            false
        }
    }

    /** Restore a previously saved manual pairing on startup. */
    fun restoreSavedPairing(context: Context): DevicePairing {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getString(PREF_DEVICE_ID, null) ?: ("camguard-" + randomSuffix()).also {
            prefs.edit().putString(PREF_DEVICE_ID, it).apply()
        }
        val config = DevicePairing(
            deviceId = id,
            firebaseApiKey = prefs.getString(PREF_API_KEY, null),
            firebaseDatabaseUrl = prefs.getString(PREF_DB_URL, null),
            firebaseAppId = prefs.getString(PREF_APP_ID, null),
            firebaseProjectId = prefs.getString(PREF_PROJECT_ID, null)
        )
        pairing = config
        return config
    }

    fun savePairing(context: Context, config: DevicePairing) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            putString(PREF_DEVICE_ID, config.deviceId)
            putString(PREF_API_KEY, config.firebaseApiKey)
            putString(PREF_DB_URL, config.firebaseDatabaseUrl)
            putString(PREF_APP_ID, config.firebaseAppId)
            putString(PREF_PROJECT_ID, config.firebaseProjectId)
            apply()
        }
        pairing = config
    }

    fun setDeviceId(context: Context, deviceId: String) {
        val updated = pairing.copy(deviceId = deviceId)
        savePairing(context, updated)
    }

    private fun loadSavedDeviceId(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(PREF_DEVICE_ID, null) ?: ""

    private fun saveDeviceId(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(PREF_DEVICE_ID, id).apply()
    }

    private fun randomSuffix(): String =
        (1..6).map { ('A'..'Z').random() }.joinToString("")

    private fun commandsRef(): DatabaseReference? =
        database?.reference?.child("devices")?.child(pairing.deviceId)?.child("commands")

    private fun statusRef(): DatabaseReference? =
        database?.reference?.child("devices")?.child(pairing.deviceId)?.child("status")

    /** Control mobile -> push a command for the client to execute. */
    fun sendCommand(command: RemoteCommand): Boolean {
        val ref = commandsRef()?.push() ?: return false
        ref.setValue(command.toJson())
        return true
    }

    /**
     * Client mobile -> observe incoming commands. Emits each new command as it
     * arrives under /devices/{deviceId}/commands.
     */
    fun observeCommands(): Flow<RemoteCommand> = callbackFlow {
        val ref = commandsRef()
        if (ref == null) {
            close()
            return@callbackFlow
        }
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val raw = snapshot.getValue(String::class.java) ?: return
                RemoteCommand.fromJson(raw)?.let { trySend(it) }
                // Consume the command so it isn't replayed on every restart.
                snapshot.ref.removeValue()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Commands observer cancelled: ${error.message}")
                close(error.toException())
            }
        }
        ref.addChildEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Client mobile -> publish its current status for the control mobile. */
    fun publishStatus(status: ClientStatus): Boolean {
        val ref = statusRef() ?: return false
        ref.setValue(status.toJson())
        return true
    }

    /** Client mobile -> mark itself online/offline. */
    fun setOnline(online: Boolean): Boolean {
        val ref = statusRef() ?: return false
        val updates = mapOf("online" to online, "timestamp" to System.currentTimeMillis())
        ref.updateChildren(updates)
        return true
    }

    /**
     * Control mobile -> observe the client device status. Emits the latest
     * status every time it changes.
     */
    fun observeStatus(): Flow<ClientStatus> = callbackFlow {
        val ref = statusRef()
        if (ref == null) {
            close()
            return@callbackFlow
        }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val raw = snapshot.getValue(String::class.java) ?: return
                ClientStatus.fromJson(raw)?.let { trySend(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Status observer cancelled: ${error.message}")
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
