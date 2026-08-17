package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.BuildConfig
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SecurityViewModel
import androidx.compose.ui.graphics.Color

/**
 * Setup screen where the user types the Firebase project config + a shared
 * device pairing ID on both phones. This avoids shipping google-services.json
 * inside the APK: both the control and client flavors can be paired at runtime
 * against the same Firebase Realtime Database node.
 */
@Composable
fun PairingSetupScreen(
    viewModel: SecurityViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Pre-fill from whatever is already saved so re-opening the screen keeps values.
    val saved = remember { viewModel.currentSavedPairing() }
    var deviceId by remember { mutableStateOf(saved.deviceId) }
    var dbUrl by remember { mutableStateOf(saved.firebaseDatabaseUrl ?: "") }
    var apiKey by remember { mutableStateOf(saved.firebaseApiKey ?: "") }
    var appId by remember { mutableStateOf(saved.firebaseAppId ?: "") }
    var projectId by remember { mutableStateOf(saved.firebaseProjectId ?: "") }
    var apiKeyVisible by remember { mutableStateOf(false) }

    val isConnected = viewModel.uiState.value.isRemoteBusConnected
    val roleLabel = if (BuildConfig.IS_CONTROL_DEVICE) "Control Phone" else "Camera (Client) Phone"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDark)
            .verticalScroll(scrollState)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FIREBASE PAIRING",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Connect $roleLabel to your Firebase project",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            // Connection status pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) ActiveGreen else LiveRed)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isConnected) "CONNECTED" else "NOT CONNECTED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isConnected) ActiveGreen else LiveRed,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Role banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (BuildConfig.IS_CONTROL_DEVICE) CyanAccent.copy(alpha = 0.5f) else ActiveGreen.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (BuildConfig.IS_CONTROL_DEVICE) Icons.Default.QrCode else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = if (BuildConfig.IS_CONTROL_DEVICE) CyanAccent else ActiveGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "This phone: $roleLabel",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (BuildConfig.IS_CONTROL_DEVICE)
                                "Sends commands and views the live camera."
                            else "Runs the camera and obeys remote commands.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Pairing fields card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ENTER THE SAME DEVICE ID ON BOTH PHONES",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                    PairingField(
                        value = deviceId,
                        onValueChange = { deviceId = it },
                        label = "Device ID (shared pairing code)",
                        placeholder = "e.g. camguard-MYROOM",
                        testTag = "pairing_device_id"
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FIREBASE PROJECT DETAILS",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                    PairingField(
                        value = dbUrl,
                        onValueChange = { dbUrl = it },
                        label = "Realtime Database URL",
                        placeholder = "https://your-project-default-rtdb.firebaseio.com",
                        keyboardType = KeyboardType.Uri,
                        testTag = "pairing_db_url"
                    )
                    PairingField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = "Web API Key",
                        placeholder = "AIzaSy...",
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailing = {
                            Text(
                                text = if (apiKeyVisible) "HIDE" else "SHOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("pairing_api_key_toggle")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Text(
                                    text = if (apiKeyVisible) "🙈" else "👁",
                                    color = TextMuted
                                )
                            }
                        },
                        testTag = "pairing_api_key"
                    )
                    PairingField(
                        value = appId,
                        onValueChange = { appId = it },
                        label = "App ID",
                        placeholder = "1:1234567890:android:abcdef",
                        keyboardType = KeyboardType.Ascii,
                        testTag = "pairing_app_id"
                    )
                    PairingField(
                        value = projectId,
                        onValueChange = { projectId = it },
                        label = "Project ID",
                        placeholder = "camguard-12345",
                        keyboardType = KeyboardType.Ascii,
                        testTag = "pairing_project_id"
                    )
                }
            }

            // Connect button
            Button(
                onClick = {
                    val ok = viewModel.applyManualPairing(
                        deviceId = deviceId,
                        firebaseApiKey = apiKey,
                        firebaseDatabaseUrl = dbUrl,
                        firebaseAppId = appId,
                        firebaseProjectId = projectId
                    )
                    val msg = if (ok) "Connected to Firebase!"
                    else "Connection failed — check all fields"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    if (ok) onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("pairing_connect_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ActiveGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "RECONNECT" else "CONNECT & SAVE",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "HOW TO GET THESE VALUES",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "1. Go to console.firebase.google.com and create a project.\n" +
                            "2. Build → Realtime Database → Create database (test mode).\n" +
                            "3. Project Settings (gear): copy Web API Key + Project ID.\n" +
                            "4. Add an Android app: copy the App ID.\n" +
                            "5. Realtime Database tab: copy the Database URL.\n" +
                            "6. Enter the SAME Device ID on both phones so they find each other.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PairingField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null,
    testTag: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        trailingIcon = trailing,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = CyanAccent,
            unfocusedBorderColor = SlateBorder,
            focusedLabelColor = CyanAccent,
            unfocusedLabelColor = TextMuted,
            cursorColor = CyanAccent
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
