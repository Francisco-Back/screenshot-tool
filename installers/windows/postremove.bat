@echo off
:: ═══════════════════════════════════════════════════════════
::  Screenshot Tool - Post-removal script (Windows)
::  Ejecutado automáticamente por el .msi al desinstalar
::
::  Limpia:
::    - Proceso activo
::    - Accesos directos con atajos
::    - Autostart
::    - Comando global 'screenshot'
::    - PATH
:: ═══════════════════════════════════════════════════════════

setlocal enabledelayedexpansion

set START_MENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs
set AUTOSTART=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
set LOCAL_BIN=%USERPROFILE%\bin

echo [postremove] Desinstalando Screenshot Tool...

:: ── Detener proceso activo ────────────────────────────────
echo [postremove] Deteniendo proceso...
taskkill /f /im screenshot-tool.exe >nul 2>&1
echo [postremove] Proceso detenido

:: ── Eliminar accesos directos ─────────────────────────────
echo [postremove] Eliminando accesos directos...

if exist "%START_MENU%\Screenshot Tool - Area.lnk" (
    del /f /q "%START_MENU%\Screenshot Tool - Area.lnk"
    echo [postremove] Atajo Ctrl+Alt+S eliminado
)

if exist "%START_MENU%\Screenshot Tool - Window.lnk" (
    del /f /q "%START_MENU%\Screenshot Tool - Window.lnk"
    echo [postremove] Atajo Ctrl+Alt+W eliminado
)

if exist "%START_MENU%\Screenshot Tool.lnk" (
    del /f /q "%START_MENU%\Screenshot Tool.lnk"
    echo [postremove] Acceso directo del menu eliminado
)

:: ── Eliminar autostart ────────────────────────────────────
if exist "%AUTOSTART%\Screenshot Tool.lnk" (
    del /f /q "%AUTOSTART%\Screenshot Tool.lnk"
    echo [postremove] Autostart eliminado
)

:: ── Eliminar comando global screenshot ───────────────────
if exist "%LOCAL_BIN%\screenshot.bat" (
    del /f /q "%LOCAL_BIN%\screenshot.bat"
    echo [postremove] Comando screenshot eliminado
)

:: ── Limpiar PATH ──────────────────────────────────────────
powershell -Command ^
  "$path = [Environment]::GetEnvironmentVariable('PATH', 'User'); ^
   $newPath = ($path -split ';' ^| Where-Object { $_ -ne '%LOCAL_BIN%' }) -join ';'; ^
   [Environment]::SetEnvironmentVariable('PATH', $newPath, 'User'); ^
   Write-Host '[postremove] PATH limpiado'"

echo.
echo [postremove] Screenshot Tool desinstalado correctamente.
echo [postremove] Java y Maven permanecen instalados en el sistema.
echo.

endlocal
