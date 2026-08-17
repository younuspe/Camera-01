@echo off
title Cam Guard - Android Deployment Engine
echo =======================================================
echo         CAM GUARD - ANDROID DEPLOYMENT PIPELINE
echo   Installs the Control or Camera (client) APK via ADB
echo =======================================================
echo.

:: Make sure we run from the folder this .bat lives in
cd /d "%~dp0"

:: Where is adb.exe? Try the bundled platform-tools folder first,
:: otherwise fall back to adb on the system PATH.
set "ADB=.\platform-tools\adb.exe"
if not exist "%ADB%" (
    echo [*] platform-tools not found next to this script; using system 'adb'.
    set "ADB=adb"
)

:: ----------------------------------------------------------------
:: Pick which APK to deploy: Control mobile or Client (Camera) mobile
:: ----------------------------------------------------------------
:askrole
set /p ROLE="Deploy [C]ontrol mobile or [K]lient (Camera) mobile? (C/K): "
if /i "%ROLE%"=="C" goto :control
if /i "%ROLE%"=="K" goto :client
echo [!] Please enter C or K.
goto :askrole

:control
set "APP_NAME=Cam Guard - Control"
set "APP_PKG=com.example.control"
set "APK_FILE=app-control-debug.apk"
goto :connect

:client
set "APP_NAME=Cam Guard - Camera (Client)"
set "APP_PKG=com.example.client"
set "APK_FILE=app-client-debug.apk"
goto :connect

:connect
echo.
echo Target : %APP_NAME%
echo Package: %APP_PKG%
echo APK    : %APK_FILE%
echo.

:: ----------------------------------------------------------------
:: STEP 1: Connect to the device (Wi-Fi or USB)
:: ----------------------------------------------------------------
set /p USEWIFI="Connect over Wi-Fi? (Y/N, default N=USB): "
if /i not "%USEWIFI%"=="Y" goto :usb

:wifi
set /p PhoneIP="Enter your phone's wireless IP and Port (e.g., 192.168.1.5:5555): "
echo [*] Connecting to %PhoneIP% ...
"%ADB%" connect %PhoneIP%
if %errorlevel% neq 0 (
    echo [X] Failed to connect over Wi-Fi.
    pause
    exit /b
)
goto :checkdevice

:usb
echo [*] Checking for USB-connected devices...
"%ADB%" devices
echo.
echo [!] If your phone shows "unauthorized", unlock it and tap "Allow USB Debugging".
pause

:checkdevice
echo.
echo [*] Verifying device connection...
"%ADB%" get-state >nul 2>&1
if %errorlevel% neq 0 (
    echo [X] No device detected. Connect a phone with USB debugging enabled and retry.
    pause
    exit /b
)
echo [v] Device connected.
echo.

:: ----------------------------------------------------------------
:: STEP 2: Locate the APK file
:: ----------------------------------------------------------------
:: Look for the APK next to this script, or in the standard Gradle
:: output folder: app\build\outputs\apk\<flavor>\debug\
set "APK_PATH=%APK_FILE%"
if not exist "%APK_PATH%" (
    if /i "%ROLE%"=="C" set "APK_PATH=app\build\outputs\apk\control\debug\%APK_FILE%"
    if /i "%ROLE%"=="K" set "APK_PATH=app\build\outputs\apk\client\debug\%APK_FILE%"
)
if not exist "%APK_PATH%" (
    echo [X] Could not find %APK_FILE%.
    echo     Place it next to this script, or build the project with:
    echo        gradlew assembleControlDebug   ^(for Control^)
    echo        gradlew assembleClientDebug    ^(for Client^)
    pause
    exit /b
)
echo [v] Found APK: %APK_PATH%
echo.

:: ----------------------------------------------------------------
:: STEP 3: Install the APK (-r reinstall, -g grant runtime permissions)
:: ----------------------------------------------------------------
echo [*] Installing %APP_NAME% on the device...
"%ADB%" install -r -g "%APK_PATH%"
if %errorlevel% neq 0 (
    echo [X] Installation failed. Make sure the phone is unlocked and connected.
    pause
    exit /b
)
echo [v] %APP_NAME% installed successfully.
echo.

:: ----------------------------------------------------------------
:: STEP 4: Launch the app
:: ----------------------------------------------------------------
echo [*] Launching %APP_NAME% ...
"%ADB%" shell am start -n %APP_PKG%/.MainActivity
if %errorlevel% neq 0 (
    echo [X] Launch failed. Try opening the app manually from the launcher.
    pause
    exit /b
)
echo.
echo =======================================================
echo [v] SUCCESS: %APP_NAME% is deployed and running.
echo =======================================================
echo.
echo Note: If the app asks for Camera / Microphone permission on
echo first launch, tap "Allow" on the phone screen.
echo.
pause
