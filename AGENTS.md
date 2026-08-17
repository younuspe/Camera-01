# Cam Guard (Camera-01) — Agent Notes

## Architecture
Dual-APK Firebase-remote-controlled baby/camera monitor.

- **Control flavor** (`app/build.gradle.kts` `productFlavors.control`): full dashboard,
  bottom nav (Camera / Dashboard / Gallery / Settings), sends commands + views live
  snapshots. `applicationId` suffix `.control`, `BuildConfig.IS_CONTROL_DEVICE=true`.
- **Client flavor** (`productFlavors.client`): minimal — fullscreen camera only, NO
  bottom nav, runs unattended after install. `applicationId` suffix `.client`,
  `BuildConfig.IS_CLIENT_DEVICE=true`. Cannot be factory-reset / USB-debugged, so
  Device Owner kiosk path is NOT available; kiosk-lite (no nav) is the substitute.
- **Firebase command bus** (`com.example.remote.FirebaseCommandBus`): RTDB node per
  device. Control writes commands; client listens. Client publishes status +
  snapshots. Pairing is via runtime-entered config (no google-services.json shipped).

## Key files
- `app/src/main/java/com/example/remote/FirebaseCommandBus.kt` —
  `initManual(ctx, DevicePairing)`, `restoreSavedPairing(ctx)`, `publishSnapshot`,
  `observeSnapshot`. `DevicePairing` has `isConfigured()`.
- `app/src/main/java/com/example/ui/viewmodel/SecurityViewModel.kt` —
  `setRemoteDeviceId`, `applyManualPairing`, `currentSavedPairing`, `toggleRecording`,
  `observeClientStatus`, `observeClientSnapshot`, `startRecordingTimer`/`stopRecordingTimer`.
- `app/src/main/java/com/example/ui/screens/PairingSetupScreen.kt` — manual Firebase
  config entry (Device ID + DB URL + API Key + App ID + Project ID).
- `app/src/main/java/com/example/ui/screens/CameraViewScreen.kt` — real CameraX video
  recording via `androidx.camera.video` (Recorder/VideoCapture). `LaunchedEffect(
  isRecording)` is the single source of truth — works for local button AND remote
  Firebase Start/Stop. `SNAPSHOT_INTERVAL_MS = 3000L` for live view.
- `app/src/main/java/com/example/ui/navigation/NavGraph.kt` — `MainNavContainer`;
  `showNavBar = isControlDevice`; first-run gating surfaces `PairingSetupScreen`
  when `!isRemoteBusConnected`.
- `.github/workflows/android-build.yml` — builds `assembleControlDebug` +
  `assembleClientDebug`, uploads `apk/control/debug/*.apk` and `apk/client/debug/*.apk`.

## Build
- Flavors: `control`, `client` (dimension `role`). Tasks:
  `assembleControlDebug`, `assembleClientDebug`.
- CameraX 1.3.2 incl. `camera-video` (Recorder/VideoCapture in `androidx.camera.video`
  package — NOT `androidx.camera.core`). Video API: `FileOutputOptions.Builder(file)`,
  `recorder.prepareRecording(ctx, opts).withAudioEnabled().start(exec, Consumer<VideoRecordEvent>)`.

## CI gotcha
The `GITHUB_TOKEN` available to the agent **lacks `workflow` scope**, so commits that
touch `.github/workflows/*` are **rejected on push** ("refusing to allow a Personal
Access Token to create or update workflow ... without `workflow` scope"). To fix CI
workflow files, the user must push them, or grant a token with `workflow` scope.

## Branch / PR
- Branch: `feature/dual-apk-firebase` → PR #2.
- As of last update: Build Debug APK passes ✓. Upload step needs the workflow fix
  (queued locally, blocked by token scope — see above).
