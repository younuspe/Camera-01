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
    val online: Boolean = true,
    val isDeviceOwner: Boolean = false
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
        json.put("isDeviceOwner", isDeviceOwner)
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
                online = j.optBoolean("online", true),
                isDeviceOwner = j.optBoolean("isDeviceOwner", false)
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * A media entry (photo/video) the client uploaded to Cloud Storage and listed
 * in the Realtime Database under /devices/{deviceId}/media. The control phone
 * observes this list to browse/download the client's recordings.
 */
data class RemoteMediaEntry(
    val fileName: String,
    val fileType: String,   // "PHOTO" or "VIDEO"
    val url: String,        // Firebase Storage download URL
    val durationSeconds: Long = 0L,
    val sizeBytes: Long = 0L,
    val uploadedAt: Long = 0L
)

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
                .setApiKey(config.firebaseApiKey!!)
                .setDatabaseUrl(config.firebaseDatabaseUrl!!)
                .setApplicationId(config.firebaseAppId!!)
                .setProjectId(config.firebaseProjectId!!)
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

    private fun snapshotRef(): DatabaseReference? =
        database?.reference?.child("devices")?.child(pairing.deviceId)?.child("snapshot")

    private fun mediaRef(): DatabaseReference? =
        database?.reference?.child("devices")?.child(pairing.deviceId)?.child("media")

    /**
     * Storage path for a media file uploaded by the client:
     *   media/{deviceId}/{fileName}
     */
    private fun storageRef(fileName: String): com.google.firebase.storage.StorageReference? {
        if (database == null) return null
        return try {
            com.google.firebase.storage.FirebaseStorage.getInstance().reference
                .child("media")
                .child(pairing.deviceId)
                .child(fileName)
        } catch (e: Exception) {
            Log.w(TAG, "Storage unavailable: ${e.message}")
            null
        }
    }

    /**
     * Client -> upload a media file (photo/video) to Firebase Cloud Storage and
     * record its download URL + metadata under /devices/{deviceId}/media so the
     * control phone can browse and fetch it. Returns the download URL on success.
     */
    fun uploadMedia(file: java.io.File, fileType: String, durationSeconds: Long = 0L,
                    onDone: (String?) -> Unit) {
        val ref = storageRef(file.name)
        if (ref == null) { onDone(null); return }
        ref.putFile(android.net.Uri.fromFile(file))
            .continueWithTask { it.result?.storage?.downloadUrl }
            .addOnSuccessListener { uri ->
                val entry = org.json.JSONObject()
                entry.put("fileName", file.name)
                entry.put("fileType", fileType)
                entry.put("url", uri.toString())
                entry.put("durationSeconds", durationSeconds)
                entry.put("sizeBytes", file.length())
                entry.put("uploadedAt", System.currentTimeMillis())
                mediaRef()?.push()?.setValue(entry.toString())
                Log.i(TAG, "Media uploaded: ${file.name}")
                onDone(uri.toString())
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Media upload failed: ${e.message}")
                onDone(null)
            }
    }

    /**
     * Control -> observe the client's uploaded media list. Emits the full list
     * each time the client adds an entry.
     */
    fun observeMediaList(): Flow<List<RemoteMediaEntry>> = callbackFlow {
        val ref = mediaRef()
        if (ref == null) { close(); return@callbackFlow }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<RemoteMediaEntry>()
                snapshot.children.forEach { child ->
                    val raw = child.getValue(String::class.java) ?: return@forEach
                    try {
                        val j = org.json.JSONObject(raw)
                        items.add(
                            RemoteMediaEntry(
                                fileName = j.optString("fileName"),
                                fileType = j.optString("fileType"),
                                url = j.optString("url"),
                                durationSeconds = j.optLong("durationSeconds", 0L),
                                sizeBytes = j.optLong("sizeBytes", 0L),
                                uploadedAt = j.optLong("uploadedAt", 0L)
                            )
                        )
                    } catch (_: Exception) {}
                }
                trySend(items.sortedByDescending { it.uploadedAt })
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

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

    /**
     * Client mobile -> publish a low-res JPEG snapshot (base64) so the control
     * mobile can see what the client camera sees. Overwrites the previous value
     * each time, so only the latest frame is kept (small RTDB footprint).
     */
    fun publishSnapshot(base64Jpeg: String): Boolean {
        val ref = snapshotRef() ?: return false
        ref.setValue(base64Jpeg)
        return true
    }

    /**
     * Control mobile -> observe the latest client snapshot (base64 JPEG).
     * Emits every time the client publishes a new frame.
     */
    fun observeSnapshot(): Flow<String> = callbackFlow {
        val ref = snapshotRef()
        if (ref == null) {
            close()
            return@callbackFlow
        }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val raw = snapshot.getValue(String::class.java) ?: return
                trySend(raw)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Snapshot observer cancelled: ${error.message}")
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
