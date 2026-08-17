package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.SecurityUiState
import com.example.ui.viewmodel.SecurityViewModel

@Composable
fun SettingsScreen(
    viewModel: SecurityViewModel,
    uiState: SecurityUiState,
    onOpenPairing: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Settings Header
        Column {
            Text(
                text = "MONITOR SETTINGS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Configure Cry Sound Detection, Stream Quality & Remote Links",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Firebase Pairing card — opens the setup screen where the user types
        // the Firebase project config + shared device ID on both phones.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("firebase_pairing_card"),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (uiState.isRemoteBusConnected) androidx.compose.ui.graphics.Color(0xFF3DDC84).copy(alpha = 0.5f)
                else SlateBorder
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (uiState.isRemoteBusConnected) androidx.compose.ui.graphics.Color(0xFF3DDC84) else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Firebase Remote Pairing",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (uiState.isRemoteBusConnected)
                                "Connected • Device: ${uiState.remoteDeviceId}"
                            else "Not connected — tap to set up",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isRemoteBusConnected) TextSecondary else TextMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenPairing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_pairing_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF00BCD4)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (uiState.isRemoteBusConnected) "Edit Pairing" else "Set Up Pairing",
                        color = SlateDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Baby Cry Sound Detection Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Cry Detection",
                            tint = WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Baby Cry & Sound Sensor",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Switch(
                        checked = uiState.enableSoundSensing,
                        onCheckedChange = { viewModel.toggleSoundSensing() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SlateDark,
                            checkedTrackColor = WarningAmber,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SlateDark
                        ),
                        modifier = Modifier.testTag("sound_sensing_settings_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Triggers immediate alerts and log sync to Dubai Web Browser when noise levels cross threshold (${uiState.soundSensitivityDb.toInt()} dB)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))
                Slider(
                    value = uiState.soundSensitivityDb,
                    onValueChange = { viewModel.setSoundSensitivityDb(it) },
                    valueRange = 40f..90f,
                    steps = 10,
                    colors = SliderDefaults.colors(
                        thumbColor = WarningAmber,
                        activeTrackColor = WarningAmber,
                        inactiveTrackColor = SlateDark
                    ),
                    modifier = Modifier.testTag("sound_db_slider")
                )
            }
        }

        // Motion Sensitivity Tuning Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Motion Sensitivity",
                        tint = WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Motion Sensor Calibration",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sensitivity Level: ${uiState.motionSensitivity.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Slider(
                    value = uiState.motionSensitivity,
                    onValueChange = { viewModel.setSensitivity(it) },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = WarningAmber,
                        activeTrackColor = WarningAmber,
                        inactiveTrackColor = SlateDark
                    ),
                    modifier = Modifier.testTag("settings_sensitivity_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { viewModel.setSensitivity(3f) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                    ) {
                        Text("Low (3)")
                    }
                    OutlinedButton(
                        onClick = { viewModel.setSensitivity(5f) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                    ) {
                        Text("Normal (5)")
                    }
                    OutlinedButton(
                        onClick = { viewModel.setSensitivity(8f) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                    ) {
                        Text("High (8)")
                    }
                }
            }
        }

        // Security & Network Encryption Standard Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encryption Standard",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "End-to-End Encryption Mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SlateDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "AES-256-GCM Hardware Encrypted",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stream payload is protected with end-to-end cryptographic handshakes. Local database snapshots are encrypted via SQLite Room security protocols.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Storage & Cache Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Storage",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Storage & Database Maintenance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.clearMotionLogs()
                        Toast.makeText(context, "Motion event database cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed.copy(alpha = 0.2f), contentColor = LiveRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_database_logs_button")
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Motion Event History Logs")
                }
            }
        }

        // Transparency Disclosures Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CamGuard Monitor v1.0.0",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Built with Jetpack Compose, CameraX, Room Database, and Material 3 design system. Operates with explicit visual HUD camera status indicators and standard Android runtime permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}
