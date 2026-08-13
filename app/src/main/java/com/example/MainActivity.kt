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
        finish()
    }
}
