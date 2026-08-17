package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MotionEvent
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateDark
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.SecurityUiState
import com.example.ui.viewmodel.SecurityViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityDashboardScreen(
    viewModel: SecurityViewModel,
    uiState: SecurityUiState,
    motionEvents: List<MotionEvent>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dashboard Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MONITOR DASHBOARD",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Real-Time Sensor & Stream Security",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleE2EDialog(true) },
                    modifier = Modifier.testTag("e2e_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Security Info",
                        tint = CyanAccent
                    )
                }
            }
        }

        // Live view from the client camera. On the control phone we decode the
        // latest base64 JPEG the client streamed over Firebase and show it here
        // so the user can actually see the monitored device.
        item {
            LiveSnapshotCard(uiState = uiState)
        }

        // System State & Active Monitoring Controls Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.isSystemArmed) ActiveGreen.copy(alpha = 0.5f) else SlateBorder
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isSystemArmed) ActiveGreen.copy(alpha = 0.2f) else SlateDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Armed State",
                                    tint = if (uiState.isSystemArmed) ActiveGreen else TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (uiState.isSystemArmed) "SYSTEM ARMED" else "SYSTEM DISARMED",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isSystemArmed) ActiveGreen else TextSecondary
                                )
                                Text(
                                    text = if (uiState.isSystemArmed) "Motion detection active" else "Motion triggers disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        Switch(
                            checked = uiState.isSystemArmed,
                            onCheckedChange = { viewModel.toggleSystemArm() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ActiveGreen,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SlateDark
                            ),
                            modifier = Modifier.testTag("system_arm_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(SlateBorder)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Camera Stream Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Camera Stream",
                                tint = if (uiState.isMonitoringActive) CyanAccent else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Camera Feed Monitoring",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }

                        Switch(
                            checked = uiState.isMonitoringActive,
                            onCheckedChange = { viewModel.toggleMonitoringActive() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyanAccent,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SlateDark
                            ),
                            modifier = Modifier.testTag("camera_monitoring_switch")
                        )
                    }
                }
            }
        }

        // Security Stream Metrics & Dubai Laptop Link Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Encryption",
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DUBAI REMOTE LAPTOP & MOBILE LINK",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CyanAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "INDIA <-> DUBAI",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stream Link box
                    val streamUrl = uiState.remoteWebBrowserUrl
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SlateDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "WEB BROWSER DASHBOARD URL (LAPTOP/PHONE)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = streamUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyanAccent,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Dubai Web Monitor URL", streamUrl))
                                    Toast.makeText(context, "Copied Dubai Web Monitor Link!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("copy_stream_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Link",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics Grid Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBadge("Resolution", uiState.streamMetrics.resolution)
                        MetricBadge("Quality", "Ultra HD Clear")
                        MetricBadge("Latency", "${uiState.streamMetrics.latencyms} ms")
                        MetricBadge("Viewers", "${uiState.streamMetrics.activeViewers} Active")
                    }
                }
            }
        }

        // Target Camera Remote Controls Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Remote Camera Control",
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TARGET CAMERA REMOTE CONTROL",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SlateDark
                        ) {
                            Text(
                                text = if (uiState.lensFacing == androidx.camera.core.CameraSelector.LENS_FACING_FRONT) "FRONT LENS" else "REAR LENS",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Remotely control camera lens and illumination from Dubai laptop or phone:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.remoteToggleCameraLens() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("remote_switch_camera_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                        ) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Flip Lens", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.remoteToggleFlash() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("remote_toggle_flash_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (uiState.flashMode != 0) CyanAccent else TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.flashMode != 0) CyanAccent else SlateBorder
                            )
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                when (uiState.flashMode) {
                                    1 -> "Flash: ON"
                                    2 -> "Torch: ON"
                                    else -> "Flash: OFF"
                                },
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Additional remote controls for full control over the client
                    // camera device: arm/disarm, recording and snapshot.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.remoteToggleArm() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("remote_toggle_arm_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (uiState.isSystemArmed) ActiveGreen else TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.isSystemArmed) ActiveGreen else SlateBorder
                            )
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uiState.isSystemArmed) "Armed" else "Disarm", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (uiState.isRecording) viewModel.remoteStopRecording()
                                else viewModel.remoteStartRecording()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("remote_record_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (uiState.isRecording) LiveRed else TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (uiState.isRecording) LiveRed else SlateBorder
                            )
                        ) {
                            Icon(
                                imageVector = if (uiState.isRecording) Icons.Default.Stop else Icons.Default.RadioButtonChecked,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (uiState.isRecording) "Stop Rec" else "Rec", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.remoteCapturePhoto() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("remote_capture_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Snap", fontSize = 12.sp)
                        }
                    }

                    uiState.lastRemoteCommandText?.let { cmd ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cmd,
                            style = MaterialTheme.typography.labelSmall,
                            color = ActiveGreen,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Device Owner controls (kiosk / install / wipe). Only shown on the
        // control flavor; the client only executes them when provisioned as
        // Device Owner (see README -> Device Owner setup).
        if (uiState.isControlDevice) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = ActiveGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Device Control",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.remoteStatus?.isDeviceOwner == true)
                                "Client is Device Owner — commands active"
                            else
                                "Client is not Device Owner — commands will no-op until provisioned",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.remoteStatus?.isDeviceOwner == true) ActiveGreen else TextSecondary,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.remoteSetKioskMode() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kiosk", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.remoteUnsetKioskMode() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exit Kiosk", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.remoteLockDevice() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lock", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    if (uiState.remoteStatus?.isDeviceOwner == true)
                                        viewModel.remoteDisableCamera()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cam Off", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { viewModel.remoteEnableCamera() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cam On", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Mobile Alert Ringtone & Vibration Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = "Alarm Notification",
                                tint = LiveRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CRY ALERT RING & VIBRATION",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Ringtone Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ring Mobile Alarm on Cry",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Plays loud alarm tone when abnormal sound occurs",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = uiState.enableRingtoneAlert,
                            onCheckedChange = { viewModel.toggleRingtoneAlert() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = LiveRed,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SlateDark
                            ),
                            modifier = Modifier.testTag("ringtone_alert_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Vibration Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vibrate Mobile Phone on Cry",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Vibrates phone repeatedly during cry alert",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = uiState.enableVibrationAlert,
                            onCheckedChange = { viewModel.toggleVibrationAlert() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = LiveRed,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SlateDark
                            ),
                            modifier = Modifier.testTag("vibration_alert_switch")
                        )
                    }
                }
            }
        }

        // Sound & Baby Cry Sensor Calibration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Audio Sensor",
                                tint = WarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BABY CRY & SOUND SENSOR",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Switch(
                            checked = uiState.enableSoundSensing,
                            onCheckedChange = { viewModel.toggleSoundSensing() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = WarningAmber,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SlateDark
                            ),
                            modifier = Modifier.testTag("sound_sensing_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Trigger Alert Level: ${uiState.soundSensitivityDb.toInt()} dB (Current: ${uiState.currentDecibels.toInt()} dB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

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
                        modifier = Modifier.testTag("sound_sensitivity_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("40 dB (Quiet)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("60 dB (Cry Spike)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("90 dB (Loud)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }
        }

        // Motion Activity Log Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT MOTION ALERTS (${motionEvents.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )

                if (motionEvents.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearMotionLogs() },
                        modifier = Modifier.testTag("clear_motion_logs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Logs",
                            tint = LiveRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Logs", color = LiveRed, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Motion Log List Items
        if (motionEvents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Clear",
                                tint = ActiveGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No motion activity detected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "Camera sensor is currently quiet",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        } else {
            items(motionEvents.take(15)) { event ->
                MotionEventRow(event = event)
            }
        }
    }

    // Security Dialog
    if (uiState.showE2EInfoDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleE2EDialog(false) },
            confirmButton = {
                Button(
                    onClick = { viewModel.toggleE2EDialog(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Got It", color = SlateDark)
                }
            },
            title = {
                Text("End-to-End Encryption & Security", color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "CamGuard uses AES-GCM-256 encryption for local storage and peer streaming channels.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Active camera viewfinder clearly displays status indicator.\n• Stream automatically halts when native system camera is opened.\n• Watermarking embeds ISO timestamp tags directly into snapshot media files.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            containerColor = SlateCard
        )
    }
}

@Composable
fun MetricBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            fontSize = 9.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
    }
}

@Composable
fun MotionEventRow(event: MotionEvent) {
    val dateFormat = SimpleDateFormat("HH:mm:ss - MMM dd", Locale.US)
    val timeStr = dateFormat.format(Date(event.timestamp))

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SlateCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(LiveRed)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = event.notes,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = LiveRed.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "${event.motionScore.toInt()}% INTENSITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = LiveRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Shows the latest live JPEG streamed from the client (camera) phone over
 * Firebase. Decoded from base64 into a Bitmap and shown with aspect ratio
 * preserved. Shows a "waiting for client" placeholder until the first frame.
 */
@Composable
fun LiveSnapshotCard(uiState: SecurityUiState) {
    val base64 = uiState.remoteSnapshotBase64
    val bitmap = remember(base64) {
        if (base64.isNullOrEmpty()) null
        else try {
            val bytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = if (uiState.isClientOnline) ActiveGreen else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Live View",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (uiState.isClientOnline) "● LIVE" else "● OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (uiState.isClientOnline) LiveRed else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateDark),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Live camera snapshot",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.isClientOnline)
                                "Waiting for first frame…"
                            else "Client offline — connect to start",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
