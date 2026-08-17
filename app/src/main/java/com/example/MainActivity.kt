package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                cameraPermissionRequestCode
            )
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
