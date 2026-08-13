// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}
dependencies {
    // ... existing dependencies ...

    // WebRTC for live video streaming
    implementation("com.github.webrtc-sdk:android:104.5112.09")

    // OkHttp for WebSocket connection to signaling server
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
