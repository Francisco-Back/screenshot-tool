@echo off
:: ═══════════════════════════════════════════════════════════
::  Screenshot Tool - Post-installation script (Windows)
::  Ejecutado automáticamente por el .msi al instalar
::
::  Configura:
::    - Acceso directo con Ctrl+Alt+S en menú de inicio
::    - Acceso directo con Ctrl+Alt+W en menú de inicio
::    - Autostart al iniciar Windows
::    - Comando global 'screenshot' en PATH
:: ═══════════════════════════════════════════════════════════

setlocal enabledelayedexpansion

set APP_BINARY=%ProgramFiles%\screenshot-tool\screenshot-tool.exe
set START_MENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs
set AUTOSTART=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
set LOCAL_BIN=%USERPROFILE%\bin

echo [postinstall] Configurando Screenshot Tool...

:: ── Acceso directo Ctrl+Alt+S → captura de area ──────────
echo [postinstall] Configurando Ctrl+Alt+S (captura de area)...
powershell -Command ^
  "$WshShell = New-Object -comObject WScript.Shell; ^
   $Shortcut = $WshShell.CreateShortcut('%START_MENU%\Screenshot Tool - Area.lnk'); ^
   $Shortcut.TargetPath = '%APP_BINARY%'; ^
   $Shortcut.Hotkey = 'CTRL+ALT+S'; ^
   $Shortcut.Description = 'Screenshot Tool - Capture area'; ^
   $Shortcut.Save()"
echo [postinstall] Ctrl+Alt+S configurado

:: ── Acceso directo Ctrl+Alt+W → captura de ventana ───────
echo [postinstall] Configurando Ctrl+Alt+W (captura de ventana)...
powershell -Command ^
  "$WshShell = New-Object -comObject WScript.Shell; ^
   $Shortcut = $WshShell.CreateShortcut('%START_MENU%\Screenshot Tool - Window.lnk'); ^
   $Shortcut.TargetPath = '%APP_BINARY%'; ^
   $Shortcut.Arguments = '--trigger-window'; ^
   $Shortcut.Hotkey = 'CTRL+ALT+W'; ^
   $Shortcut.Description = 'Screenshot Tool - Capture active window'; ^
   $Shortcut.Save()"
echo [postinstall] Ctrl+Alt+W configurado

:: ── Autostart al iniciar Windows ─────────────────────────
echo [postinstall] Configurando autostart...
powershell -Command ^
  "$WshShell = New-Object -comObject WScript.Shell; ^
   $Shortcut = $WshShell.CreateShortcut('%AUTOSTART%\Screenshot Tool.lnk'); ^
   $Shortcut.TargetPath = '%APP_BINARY%'; ^
   $Shortcut.Description = 'Screenshot Tool autostart'; ^
   $Shortcut.Save()"
echo [postinstall] Autostart configurado

:: ── Comando global 'screenshot' ──────────────────────────
echo [postinstall] Creando comando global screenshot...
if not exist "%LOCAL_BIN%" mkdir "%LOCAL_BIN%"

(
    echo @echo off
    echo tasklist /fi "imagename eq screenshot-tool.exe" /fo csv 2^>nul ^| find /i "screenshot-tool" ^>nul
    echo if not errorlevel 1 ^(
    echo     echo Screenshot Tool ya esta corriendo. Usa Ctrl+Alt+S para capturar.
    echo ^) else ^(
    echo     start "" "%APP_BINARY%"
    echo     echo Screenshot Tool iniciado. Usa Ctrl+Alt+S para capturar.
    echo ^)
) > "%LOCAL_BIN%\screenshot.bat"

:: Agregar al PATH si no esta
powershell -Command ^
  "$path = [Environment]::GetEnvironmentVariable('PATH', 'User'); ^
   if ($path -notlike '*%LOCAL_BIN%*') { ^
       [Environment]::SetEnvironmentVariable('PATH', $path + ';%LOCAL_BIN%', 'User'); ^
       Write-Host '[postinstall] %LOCAL_BIN% agregado al PATH' ^
   } else { ^
       Write-Host '[postinstall] %LOCAL_BIN% ya esta en PATH' ^
   }"

echo.
echo [postinstall] Instalacion completa.
echo [postinstall]   Ctrl+Alt+S -> captura de area
echo [postinstall]   Ctrl+Alt+W -> captura de ventana activa
echo [postinstall]   La app arranca automaticamente al iniciar Windows.
echo.

endlocal
