plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Two build targets: the "control" mobile (viewer/monitor side) and the
    // "client" mobile (the camera device being monitored). Each flavor gets a
    // distinct applicationId suffix so both APKs can be installed side-by-side,
    // and a BuildConfig flag so the app can branch behavior at runtime.
    flavorDimensions += "role"
    productFlavors {
        create("control") {
            dimension = "role"
            applicationIdSuffix = ".control"
            versionNameSuffix = "-control"
            buildConfigField("boolean", "IS_CONTROL_DEVICE", "true")
            buildConfigField("boolean", "IS_CLIENT_DEVICE", "false")
            resValue("string", "app_role_label", "Control Mobile")
        }
        create("client") {
            dimension = "role"
            applicationIdSuffix = ".client"
            versionNameSuffix = "-client"
            buildConfigField("boolean", "IS_CONTROL_DEVICE", "false")
            buildConfigField("boolean", "IS_CLIENT_DEVICE", "true")
            resValue("string", "app_role_label", "Camera Mobile (Client)")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // AndroidX activity/lifecycle/navigation
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.2")
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.camera:camera-lifecycle:1.3.2")
    implementation("androidx.camera:camera-view:1.3.2")
    // VideoCapture use case + Recorder API for real .mp4 video recording.
    implementation("androidx.camera:camera-video:1.3.2")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Coroutines / networking / images
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase Realtime Database — used for control -> client remote commands
    // and client -> control status updates. The google-services Gradle plugin
    // is intentionally NOT applied: it hard-fails the build when
    // google-services.json is absent. Instead Firebase is initialized at
    // runtime via FirebaseCommandBus.initManual(...) using values the user
    // enters in Settings (persisted). When not configured the bus no-ops
    // gracefully and the app stays in local-only mode.
    implementation(platform("com.google.firebase:firebase-bom:32.7.3"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-common-ktx")

    // Unit/instrumentation tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
