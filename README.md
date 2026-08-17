# Cam Guard

A Kotlin/Jetpack Compose Android security-camera app that builds **two separate
APKs** from a single codebase using Gradle product flavors:

| Flavor | App name | applicationId | Role |
|--------|----------|---------------|------|
| `control` | **Cam Guard – Control** | `com.example.control` | The viewer/monitor mobile. Opens on the Dashboard and can remotely command the camera device. |
| `client` | **Cam Guard – Camera** | `com.example.client` | The camera device being monitored. Opens on the live camera preview, listens for remote commands, and reports status back. |

Both APKs can be installed side-by-side because each has a distinct
`applicationId` suffix.

---

## Building the APKs

### Locally (Android Studio / Gradle)

```bash
# Control mobile (viewer/monitor) APK
./gradlew assembleControlDebug
# -> app/build/outputs/apk/control/debug/app-control-debug.apk

# Client mobile (camera device) APK
./gradlew assembleClientDebug
# -> app/build/outputs/apk/client/debug/app-client-debug.apk
```

### CI (GitHub Actions)

`.github/workflows/android-build.yml` builds both flavors on every push/PR and
uploads them as separate artifacts:

- `CamGuard-Control-mobile-apk` → `CamGuard-Control-debug.apk`
- `CamGuard-Client-mobile-apk` → `CamGuard-Client-debug.apk`

Download them from the Actions run's **Artifacts** section.

---

## Remote control architecture (control → client)

The control mobile commands the client (camera) mobile over **Firebase Realtime
Database**. No custom signaling server is required.

```
Control mobile                       Firebase RTDB                  Client mobile
─────────────                       ─────────────                  ─────────────
sendRemoteCommand()  ───── push ───▶ /devices/{deviceId}/commands ──▶ observeCommands()
                                                                         │
                                                         executes locally (toggle lens,
                                                         flash, arm, record, capture…)
                                                                         │
observeStatus()  ◀── value event ── /devices/{deviceId}/status ◀──── publishStatus()
```

- The **client** listens on `/devices/{deviceId}/commands`, executes each
  command via the existing ViewModel actions, and publishes its status (lens,
  flash, armed, recording, decibels, motion/cry flags) to
  `/devices/{deviceId}/status` every 2 s.
- The **control** mobile observes `/devices/{deviceId}/status` and sends
  commands from the Dashboard's remote-control buttons (Flip Lens, Flash,
  Arm, Record, Snap).

Both devices must use the **same `deviceId`** to rendezvous. A device id is
auto-generated on first launch and persisted; you can also set a custom one in
code via `FirebaseCommandBus.setDeviceId(...)` or pair them to the same value.

### Supported remote commands

`ToggleLens`, `CycleFlash`, `ToggleTorch`, `ToggleArm`, `StartRecording`,
`StopRecording`, `CapturePhoto`, `ToggleSoundSensing`, `ToggleMonitoring`,
`SetSoundSensitivity(db)`, `SetMotionSensitivity(level)`.

---

## Firebase setup (required for remote control)

The app **builds and runs in local-only mode without any Firebase config** —
the command bus no-ops gracefully until a config is present. To enable remote
control you provide Firebase credentials at runtime via **manual
configuration** (Option A below). The google-services Gradle plugin is
intentionally NOT applied so the build never depends on a `google-services.json`
file.

### Option A — configure manually (recommended, no JSON in the repo)

Call this at startup (e.g. in a custom `Application` class) with values from
the Firebase console → Project settings:

```kotlin
FirebaseCommandBus.initManual(
    context,
    DevicePairing(
        deviceId = "my-shared-id",
        firebaseApiKey = "...",
        firebaseDatabaseUrl = "https://<project>.firebaseio.com",
        firebaseAppId = "...",
        firebaseProjectId = "<project>"
    )
)
```

The pairing is persisted and restored on later launches, so you only need to
set it once (e.g. from a Settings screen via `FirebaseCommandBus.savePairing`).

### Option B — ship `google-services.json`

If you prefer the standard Firebase setup, add the
`com.google.gms.google-services` plugin back in `build.gradle.kts`, drop a
`google-services.json` into `app/`, and `FirebaseCommandBus.initFromDefaultApp`
will pick it up. This is **not** the default because it makes the build fail
when the JSON is missing (e.g. in CI / forks).

### Recommended Realtime Database rules

```json
{
  "rules": {
    "devices": {
      "$deviceId": {
        "commands": { ".read": true, ".write": true },
        "status":   { ".read": true, ".write": true }
      }
    }
  }
}
```

> ⚠️ Lock these rules down to authenticated users before shipping. The rules
> above are open for development/testing only.

---

## Permissions

Declared in `AndroidManifest.xml` and requested at runtime:

- `CAMERA` — camera preview & capture
- `RECORD_AUDIO` — cry/sound detection
- `VIBRATE` — alert vibration

No `INTERNET` permission is declared manually — Firebase brings its own and
only connects when configured.

---

## Project structure

```
app/
  build.gradle.kts              # control/client product flavors + Firebase deps
  src/
    main/                       # shared code (UI, camera, audio, Room, remote bus)
      java/com/example/
        remote/                 # RemoteCommand, FirebaseCommandBus, DevicePairing, ClientStatus
        ui/viewmodel/SecurityViewModel.kt   # wires command bus + flavor behavior
      AndroidManifest.xml
    control/res/values/strings.xml   # app_name = "Cam Guard – Control"
    client/res/values/strings.xml   # app_name = "Cam Guard – Camera"
.github/workflows/android-build.yml  # builds both APKs
```

