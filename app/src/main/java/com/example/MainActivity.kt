package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.navigation.MainNavContainer
import com.example.ui.theme.CamGuardTheme
import com.example.ui.theme.SlateDark
import com.example.ui.viewmodel.SecurityViewModel

class MainActivity : ComponentActivity() {

    private val securityViewModel: SecurityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CamGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SlateDark
                ) {
                    // Directly load the main screen without asking for runtime permissions
                    MainNavContainer(viewModel = securityViewModel)
                }
            }
        }
    }
}
