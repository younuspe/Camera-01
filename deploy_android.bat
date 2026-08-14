@echo off
title Supru AI Android Deployment Engine
echo =======================================================
echo         SUPRU AI - ANDROID AUTOMATED PIPELINE          
echo =======================================================
echo.

:: STEP 1: VERIFY HARDWARE DEVICE IS CONNECTED VIA WI-FI
echo [*] Initializing Android Debug Bridge Wireless Connection...
cd /d "%~dp0"

:: Ask your client to type in their phone's IP address and Port
set /p PhoneIP="Enter your phone's wireless IP and Port (e.g., 192.168.1.5:5555): "

echo [*] Pairing and connecting to %PhoneIP%...
.\platform-tools\adb.exe connect %PhoneIP%
echo.



:: STEP 1: VERIFY HARDWARE DEVICE IS CONNECTED VIA USB
echo [*] Initializing Android Debug Bridge connection...
cd /d "%~dp0"
.\platform-tools\adb.exe devices
echo.
echo [!] Please look at your phone screen and click "Allow USB Debugging" if prompted.
pause
echo.

:: STEP 2: DOWNLOADING INSTRUCTION (AUTO-DOWNLOAD NEW FILE)
echo [*] Downloading the newest build package...
:: In production, curl fetches the compiled mobile APK file silently to the local folder
curl -Lo "SupruAi_Initial.apk" "https://yourserver.com"
if %errorlevel% neq 0 (
    echo [X] Error: Failed to download the update package from the server.
    pause
    exit /b
)
echo [✓] Download complete.
echo.

:: STEP 3: AUTO-INSTALLING (SILENTLY GRANTED SYSTEM PERMISSIONS)
echo [*] Pushing 'SupruAi_Initial.apk' to your device...
echo [*] Installing silently and bypassing system permission prompts...
.\platform-tools\adb.exe install -r -g "SupruAi_Initial.apk"
if %errorlevel% neq 0 (
    echo [X] Error: Installation failed. Ensure your phone is unlocked and connected.
    pause
    exit /b
)
echo [✓] App successfully pushed and installed.
echo.

:: STEP 4: WIPING INSTRUCTION (AUTO-DELETE LOCAL DATA AND TEMPS)
echo [*] Initiating wipe protocols...
echo [*] Erasing local application data, cache, and password configurations...
:: This completely clears the app's internal sandbox storage database on the phone
.\platform-tools\adb.exe shell pm clear com.supru.ai

echo [*] Purging local temporal installation file from host computer...
:: This removes the temporary .apk file from the PC folder to clean up space
del /f /q "SupruAi_Initial.apk"
echo [✓] All storage contexts and temporary packages successfully wiped.
echo.

:: STEP 5: INSTANT ACTIVATION
echo [*] Launching Supru AI with Device Authentication Active...
.\platform-tools\adb.exe shell am start -n com.supru.ai/.MainActivity
echo.
echo =======================================================
echo [✓] SUCCESS: Supru AI is deployed and ready on your device.
echo =======================================================
pause
