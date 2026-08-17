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
    // "client" mobile (the camera device being monitored). The client flavor's
    // internal name is "client" (matches IS_CLIENT_DEVICE / .client appId used
    // throughout the codebase); its user-facing label and version suffix say
    // "Camera" to match the "Cam Guard – Camera" branding. Each flavor gets a
    // distinct applicationId suffix so both APKs install side-by-side, and a
    // BuildConfig flag so the app can branch behavior at runtime.
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
            versionNameSuffix = "-camera"
            buildConfigField("boolean", "IS_CONTROL_DEVICE", "false")
            buildConfigField("boolean", "IS_CLIENT_DEVICE", "true")
            resValue("string", "app_role_label", "Camera Mobile")
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

    // Release signing. Reads keystore details from environment variables so CI
    // (or a local machine) can sign release APKs without committing secrets.
    // When the env vars are absent (e.g. a fresh CI run), we fall back to the
    // debug keystore so the build still succeeds and produces a *signed* APK
    // (fewer "unknown developer" Play Protect warnings than an unsigned one).
    val keystorePath = System.getenv("CAMGUARD_KEYSTORE_PATH") ?: "debug.keystore"
    val keystorePass = System.getenv("CAMGUARD_KEYSTORE_PASS") ?: "android"
    val keyAliasVal = System.getenv("CAMGUARD_KEY_ALIAS") ?: "androiddebugkey"
    val keyPassVal = System.getenv("CAMGUARD_KEY_PASS") ?: "android"
    // Resolve the keystore relative to the project root (not the module dir)
    // so a `debug.keystore` created at the repo root by CI is found.
    val keystoreFile = rootProject.file(keystorePath).let { f ->
        if (f.exists()) f else file(keystorePath)
    }
    signingConfigs {
        create("release") {
            storeFile = keystoreFile
            storePassword = keystorePass
            keyAlias = keyAliasVal
            keyPassword = keyPassVal
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation("com.google.firebase:firebase-storage-ktx")
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
