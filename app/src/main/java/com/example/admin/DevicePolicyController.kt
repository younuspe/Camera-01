package com.example.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Wrapper over [DevicePolicyManager] for the client (camera) flavor. Every
 * elevated action silently no-ops unless the app is provisioned as the
 * Device Owner (see [isDeviceOwner]). Provision once with:
 *
 *   adb shell dpm set-device-owner com.example.client/.admin.ClientDeviceAdminReceiver
 *
 * All methods are safe to call regardless of provisioning state — they
 * return a boolean indicating whether the action actually executed.
 */
class DevicePolicyController(private val context: Context) {

    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent: ComponentName =
        ComponentName(context, ClientDeviceAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean = try {
        dpm.isDeviceOwnerApp(context.packageName)
    } catch (e: Exception) {
        Log.w(TAG, "isDeviceOwner check failed: ${e.message}")
        false
    }

    fun isAdminActive(): Boolean = try {
        dpm.isAdminActive(adminComponent)
    } catch (e: Exception) {
        false
    }

    // ------------------------------------------------------------------
    // Kiosk / Lock-Task mode
    // ------------------------------------------------------------------

    /**
     * Pin the app to the foreground (kiosk). Only effective when the app is
     * Device Owner; otherwise the Activity must use screen pinning manually.
     */
    fun setKioskMode(enabled: Boolean): Boolean {
        if (!isDeviceOwner()) {
            Log.w(TAG, "setKioskMode ignored — not device owner")
            return false
        }
        return try {
            if (enabled) {
                dpm.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
            } else {
                dpm.setLockTaskPackages(adminComponent, arrayOf())
            }
            Log.i(TAG, "Kiosk mode ${if (enabled) "enabled" else "disabled"}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "setKioskMode failed", e)
            false
        }
    }

    fun isKioskActive(): Boolean = try {
        dpm.isLockTaskPermitted(context.packageName)
    } catch (e: Exception) {
        false
    }

    // ------------------------------------------------------------------
    // Package install / uninstall (silent only when Device Owner)
    // ------------------------------------------------------------------

    /**
     * Silently install an APK whose bytes were received remotely. Requires
     * Device Owner; otherwise the install is offered via the system installer
     * (which shows a prompt).
     *
     * @param apkBytes raw APK file bytes
     * @return true if the install session was started
     */
    fun installPackage(apkBytes: ByteArray): Boolean {
        if (!isDeviceOwner()) {
            Log.w(TAG, "installPackage ignored — not device owner")
            return false
        }
        return try {
            val packageInstaller = context.packageManager.packageInstaller
            val params = android.content.pm.PackageInstaller.SessionParams(
                android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            val sessionId = packageInstaller.createSession(params)
            packageInstaller.openSession(sessionId).use { session ->
                val tmp = File(context.cacheDir, "remote_install_${System.currentTimeMillis()}.apk")
                FileOutputStream(tmp).use { it.write(apkBytes) }
                tmp.inputStream().use { input ->
                    session.openWrite("camguard_apk", 0, apkBytes.size.toLong()).use { out ->
                        input.copyTo(out)
                        session.fsync(out)
                    }
                }
                tmp.delete()
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    Intent(context, ClientDeviceAdminReceiver::class.java),
                    android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
                session.commit(pendingIntent.intentSender)
            }
            Log.i(TAG, "Package install session $sessionId started")
            true
        } catch (e: Exception) {
            Log.e(TAG, "installPackage failed", e)
            false
        }
    }

    /**
     * Silently uninstall a package. Requires Device Owner.
     */
    fun uninstallPackage(packageName: String): Boolean {
        if (!isDeviceOwner()) {
            Log.w(TAG, "uninstallPackage ignored — not device owner")
            return false
        }
        return try {
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                packageName.hashCode(),
                Intent(context, ClientDeviceAdminReceiver::class.java),
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            context.packageManager.packageInstaller.uninstall(
                packageName,
                pendingIntent.intentSender
            )
            Log.i(TAG, "Uninstall requested for $packageName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "uninstallPackage failed", e)
            false
        }
    }

    // ------------------------------------------------------------------
    // Camera enable/disable, lock, wipe
    // ------------------------------------------------------------------

    /** Disable (or re-enable) the device camera hardware. Device Owner only. */
    fun setCameraDisabled(disabled: Boolean): Boolean {
        if (!isDeviceOwner()) {
            Log.w(TAG, "setCameraDisabled ignored — not device owner")
            return false
        }
        return try {
            dpm.setCameraDisabled(adminComponent, disabled)
            Log.i(TAG, "Camera ${if (disabled) "disabled" else "enabled"}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "setCameraDisabled failed", e)
            false
        }
    }

    /** Lock the device immediately. Device Owner or Profile Owner. */
    fun lockNow(): Boolean {
        if (!isDeviceOwner() && !isAdminActive()) {
            Log.w(TAG, "lockNow ignored — not admin")
            return false
        }
        return try {
            dpm.lockNow()
            Log.i(TAG, "Device locked")
            true
        } catch (e: Exception) {
            Log.e(TAG, "lockNow failed", e)
            false
        }
    }

    /**
     * Factory-reset the device. DANGEROUS. Device Owner only.
     * @param wipeExternalStorage also wipe the shared storage (SD/sdcard)
     */
    fun wipeDevice(wipeExternalStorage: Boolean = false): Boolean {
        if (!isDeviceOwner()) {
            Log.w(TAG, "wipeDevice ignored — not device owner")
            return false
        }
        return try {
            val flags = if (wipeExternalStorage)
                DevicePolicyManager.WIPE_EXTERNAL_STORAGE
            else 0
            dpm.wipeData(flags)
            true
        } catch (e: Exception) {
            Log.e(TAG, "wipeDevice failed", e)
            false
        }
    }

    companion object {
        private const val TAG = "DevicePolicyController"
    }
}
