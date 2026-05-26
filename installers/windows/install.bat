@echo off
:: ═══════════════════════════════════════════════════════════
::  ScreenshotTool v2 - Windows Installer
::  - Verifies Java installation
::  - Builds JAR with Maven
::  - Installs to %LOCALAPPDATA%\screenshot-tool\
::  - Creates global shortcut with hotkey
::  - Configures autostart on login
::  - Creates global 'screenshot' command
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
echo    Screenshot Tool v2 - Installer
echo ═══════════════════════════════════════
echo.

:: ── Verificar Java ────────────────────────────────────────
echo [INFO] Checking Java installation...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Please install Java 21 from:
    echo         https://adoptium.net
    echo         Then run this installer again.
    pause
    exit /b 1
)

for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VER=%%g
)
echo [OK] Java found: !JAVA_VER!

:: ── Verificar Maven ───────────────────────────────────────
echo [INFO] Checking Maven installation...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven not found. Please install Maven from:
    echo         https://maven.apache.org
    echo         Then run this installer again.
    pause
    exit /b 1
)
echo [OK] Maven found

:: ── Verificar pom.xml ─────────────────────────────────────
if not exist "pom.xml" (
    echo [ERROR] pom.xml not found.
    echo         Run this installer from the project root directory.
    pause
    exit /b 1
)
echo [OK] Source files found

:: ── Seleccionar atajo de teclado ──────────────────────────
echo.
echo ═══════════════════════════════════════
echo    Configure keyboard shortcut
echo ═══════════════════════════════════════
echo.
echo   1. Ctrl+Alt+S  (recommended - one hand)
echo   2. Custom      (type your own)
echo.
set /p HOTKEY_OPTION=Choose an option (1-2): 

if "%HOTKEY_OPTION%"=="1" (
    set HOTKEY=CTRL+ALT+S
    set HOTKEY_DISPLAY=Ctrl+Alt+S
    goto :hotkey_done
)

if "%HOTKEY_OPTION%"=="2" (
    echo.
    echo Enter your custom hotkey combination.
    echo Format examples: CTRL+ALT+S, CTRL+F9, ALT+F10
    echo Note: Avoid hotkeys used by browsers or system apps.
    echo.
    set /p HOTKEY=Enter hotkey: 
    set HOTKEY_DISPLAY=!HOTKEY!
    goto :hotkey_done
)

:: Default si no eligio nada valido
set HOTKEY=CTRL+ALT+S
set HOTKEY_DISPLAY=Ctrl+Alt+S

:hotkey_done
echo [OK] Hotkey selected: !HOTKEY_DISPLAY!

:: ── Crear directorio de instalación ──────────────────────
echo.
echo [INFO] Creating installation directory...
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
echo [OK] Directory created: %INSTALL_DIR%

:: ── Compilar con Maven ────────────────────────────────────
echo.
echo [INFO] Building project with Maven...
call mvn clean package -q
if errorlevel 1 (
    echo [ERROR] Maven build failed. Run 'mvn package' to see details.
    pause
    exit /b 1
)

if not exist "target\%JAR_NAME%" (
    echo [ERROR] JAR not found after build: target\%JAR_NAME%
    pause
    exit /b 1
)

copy "target\%JAR_NAME%" "%INSTALL_DIR%\%JAR_NAME%" >nul
echo [OK] Built and installed: %INSTALL_DIR%\%JAR_NAME%

:: ── Copiar ícono ──────────────────────────────────────────
if exist "src\main\resources\com\screenshottool\img\captura-de-pantalla.png" (
    copy "src\main\resources\com\screenshottool\img\captura-de-pantalla.png" "%INSTALL_DIR%\icon.png" >nul
    echo [OK] Icon copied
)

:: ── Crear acceso directo con atajo en menú de inicio ──────
echo [INFO] Creating Start Menu shortcut with hotkey !HOTKEY_DISPLAY!...
powershell -Command ^
    "$WshShell = New-Object -comObject WScript.Shell; ^
     $Shortcut = $WshShell.CreateShortcut('%START_MENU%\%SHORTCUT_NAME%.lnk'); ^
     $Shortcut.TargetPath = 'javaw'; ^
     $Shortcut.Arguments = '-jar \"%INSTALL_DIR%\%JAR_NAME%\"'; ^
     $Shortcut.WorkingDirectory = '%INSTALL_DIR%'; ^
     $Shortcut.Hotkey = '!HOTKEY!'; ^
     $Shortcut.Description = 'Screenshot Tool - Capture screen areas'; ^
     if (Test-Path '%INSTALL_DIR%\icon.png') { $Shortcut.IconLocation = '%INSTALL_DIR%\icon.png' }; ^
     $Shortcut.Save()"
echo [OK] Start Menu shortcut created with hotkey !HOTKEY_DISPLAY!

:: ── Configurar autostart ──────────────────────────────────
echo [INFO] Configuring autostart...
powershell -Command ^
    "$WshShell = New-Object -comObject WScript.Shell; ^
     $Shortcut = $WshShell.CreateShortcut('%AUTOSTART%\%SHORTCUT_NAME%.lnk'); ^
     $Shortcut.TargetPath = 'javaw'; ^
     $Shortcut.Arguments = '-jar \"%INSTALL_DIR%\%JAR_NAME%\"'; ^
     $Shortcut.WorkingDirectory = '%INSTALL_DIR%'; ^
     $Shortcut.Description = 'Screenshot Tool autostart'; ^
     $Shortcut.Save()"
echo [OK] Autostart configured (will launch on login)

:: ── Crear comando global 'screenshot' ────────────────────
echo [INFO] Creating global 'screenshot' command...
if not exist "%LOCAL_BIN%" mkdir "%LOCAL_BIN%"

:: Crear script .bat que detecta instancia activa
(
    echo @echo off
    echo tasklist /fi "imagename eq javaw.exe" /fo csv 2^>nul ^| find /i "screenshot-tool" ^>nul
    echo if not errorlevel 1 ^(
    echo     echo Screenshot Tool is already running. Use !HOTKEY_DISPLAY! to capture.
    echo ^) else ^(
    echo     start "" javaw -jar "%INSTALL_DIR%\%JAR_NAME%"
    echo     echo Screenshot Tool started. Use !HOTKEY_DISPLAY! to capture.
    echo ^)
) > "%LOCAL_BIN%\screenshot.bat"

:: Agregar al PATH si no está
powershell -Command ^
    "$path = [Environment]::GetEnvironmentVariable('PATH', 'User'); ^
     if ($path -notlike '*%LOCAL_BIN%*') { ^
         [Environment]::SetEnvironmentVariable('PATH', $path + ';%LOCAL_BIN%', 'User'); ^
         Write-Host '[OK] Added %LOCAL_BIN% to PATH' ^
     } else { ^
         Write-Host '[OK] %LOCAL_BIN% already in PATH' ^
     }"

:: ── Resumen ───────────────────────────────────────────────
echo.
echo ═══════════════════════════════════════
echo    Installation complete
echo ═══════════════════════════════════════
echo.
echo   Installed at:  %INSTALL_DIR%
echo   Start Menu:    %START_MENU%
echo   Autostart:     %AUTOSTART%
echo   Command:       %LOCAL_BIN%\screenshot.bat
echo.
echo   Hotkey:        !HOTKEY_DISPLAY!
echo   Run command:   screenshot
echo.
echo   NOTE: Log out and back in for the hotkey to activate.
echo         Or press Win+R, type 'shell:startup' and check the shortcut.
echo.

:: ── Ejecutar ahora ────────────────────────────────────────
set /p RUN_NOW=Run Screenshot Tool now? (y/n): 
if /i "%RUN_NOW%"=="y" (
    start "" javaw -jar "%INSTALL_DIR%\%JAR_NAME%"
    echo [OK] Running in background...
)

echo.
pause
endlocal
