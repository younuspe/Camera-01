package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.service.CameraForegroundService
import com.example.ui.navigation.MainNavContainer
import com.example.ui.navigation.NavTab
import com.example.ui.theme.CamGuardTheme
import com.example.ui.viewmodel.SecurityViewModel

class MainActivity : ComponentActivity() {

    private val cameraPermissionRequestCode = 1001
    private val viewModel by viewModels<SecurityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestCameraPermissionIfNeeded()
        startCameraForegroundServiceIfClient()
        renderUi()
    }

    private fun requestCameraPermissionIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        // RECORD_AUDIO is used by the cry/sound detection feature and must be
        // requested at runtime on Android 6.0+.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        // POST_NOTIFICATIONS is required on Android 13+ for the foreground-service
        // notification that keeps the camera alive in the background.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                cameraPermissionRequestCode
            )
        }
    }

    /**
     * On the client (camera) flavor, start the foreground service that keeps the
     * camera/mic alive while the app is backgrounded or the screen is off.
     */
    private fun startCameraForegroundServiceIfClient() {
        if (BuildConfig.IS_CLIENT_DEVICE) {
            CameraForegroundService.start(this)
        }
    }

    private fun renderUi() {
        // The control mobile (viewer/monitor side) opens on the dashboard so the
        // user immediately sees stream metrics, motion logs and remote controls.
        // The client mobile (camera device being monitored) opens on the live
        // camera preview so it starts capturing right away.
        val initialTab = if (BuildConfig.IS_CONTROL_DEVICE) {
            NavTab.DASHBOARD
        } else {
            NavTab.CAMERA
        }

        setContent {
            CamGuardTheme {
                MainNavContainer(
                    viewModel = viewModel,
                    initialTab = initialTab
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequestCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission granted; the Compose camera screen will bind the camera
            // on its next composition.
        }
    }
}
