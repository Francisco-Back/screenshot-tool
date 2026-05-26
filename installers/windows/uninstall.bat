@echo off
:: ═══════════════════════════════════════════════════════════
::  ScreenshotTool v2 - Windows Uninstaller
:: ═══════════════════════════════════════════════════════════

setlocal enabledelayedexpansion

set INSTALL_DIR=%LOCALAPPDATA%\screenshot-tool
set JAR_NAME=screenshot-tool.jar
set SHORTCUT_NAME=Screenshot Tool
set START_MENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs
set AUTOSTART=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
set LOCAL_BIN=%USERPROFILE%\bin

echo.
echo ═══════════════════════════════════════
echo    Screenshot Tool v2 - Uninstaller
echo ═══════════════════════════════════════
echo.

set /p CONFIRM=Are you sure you want to uninstall Screenshot Tool? (y/n): 
if /i not "%CONFIRM%"=="y" (
    echo Uninstall cancelled.
    pause
    exit /b 0
)

echo.

:: ── Detener instancia activa ──────────────────────────────
echo [INFO] Stopping running instance...
taskkill /fi "windowtitle eq Screenshot Tool*" /f >nul 2>&1
taskkill /fi "imagename eq javaw.exe" /f >nul 2>&1
echo [OK] Process stopped

:: ── Eliminar archivos de instalación ─────────────────────
echo [INFO] Removing files...

if exist "%INSTALL_DIR%" (
    rmdir /s /q "%INSTALL_DIR%"
    echo [OK] Install directory removed: %INSTALL_DIR%
) else (
    echo [!] Not found: %INSTALL_DIR%
)

:: ── Eliminar accesos directos ─────────────────────────────
if exist "%START_MENU%\%SHORTCUT_NAME%.lnk" (
    del /f /q "%START_MENU%\%SHORTCUT_NAME%.lnk"
    echo [OK] Start Menu shortcut removed
) else (
    echo [!] Start Menu shortcut not found
)

if exist "%AUTOSTART%\%SHORTCUT_NAME%.lnk" (
    del /f /q "%AUTOSTART%\%SHORTCUT_NAME%.lnk"
    echo [OK] Autostart entry removed
) else (
    echo [!] Autostart entry not found
)

:: ── Eliminar comando global ───────────────────────────────
if exist "%LOCAL_BIN%\screenshot.bat" (
    del /f /q "%LOCAL_BIN%\screenshot.bat"
    echo [OK] 'screenshot' command removed
) else (
    echo [!] 'screenshot' command not found
)

:: ── Limpiar PATH ──────────────────────────────────────────
powershell -Command ^
    "$path = [Environment]::GetEnvironmentVariable('PATH', 'User'); ^
     $newPath = ($path -split ';' ^| Where-Object { $_ -ne '%LOCAL_BIN%' }) -join ';'; ^
     [Environment]::SetEnvironmentVariable('PATH', $newPath, 'User'); ^
     Write-Host '[OK] PATH cleaned'"

:: ── Resumen ───────────────────────────────────────────────
echo.
echo ═══════════════════════════════════════
echo    Uninstall complete
echo ═══════════════════════════════════════
echo.
echo   Screenshot Tool has been completely removed.
echo   Java and Maven remain installed on the system.
echo.
pause
endlocal
