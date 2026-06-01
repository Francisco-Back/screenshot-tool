@echo off
:: ═══════════════════════════════════════════════════════════
::  Screenshot Tool - Post-removal script (Windows)
::  Ejecutado automáticamente por el .msi al desinstalar
::
::  IMPORTANTE: Todos los comandos usan 2>nul para no fallar
::  si el proceso o archivos no existen — evita que el .msi
::  quede en estado inconsistente al desinstalar.
:: ═══════════════════════════════════════════════════════════

setlocal enabledelayedexpansion

set START_MENU=%APPDATA%\Microsoft\Windows\Start Menu\Programs
set AUTOSTART=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
set LOCAL_BIN=%USERPROFILE%\bin

echo [postremove] Desinstalando Screenshot Tool...

:: ── Detener proceso activo (no falla si no está corriendo)
echo [postremove] Deteniendo proceso...
taskkill /f /im screenshot-tool.exe >nul 2>&1
echo [postremove] Proceso detenido

:: ── Eliminar accesos directos ─────────────────────────────
echo [postremove] Eliminando accesos directos...

if exist "%START_MENU%\Screenshot Tool - Area.lnk" (
    del /f /q "%START_MENU%\Screenshot Tool - Area.lnk" >nul 2>&1
    echo [postremove] Atajo Ctrl+Alt+S eliminado
)

if exist "%START_MENU%\Screenshot Tool - Window.lnk" (
    del /f /q "%START_MENU%\Screenshot Tool - Window.lnk" >nul 2>&1
    echo [postremove] Atajo Ctrl+Alt+W eliminado
)

if exist "%START_MENU%\Screenshot Tool.lnk" (
    del /f /q "%START_MENU%\Screenshot Tool.lnk" >nul 2>&1
    echo [postremove] Acceso directo del menu eliminado
)

:: ── Eliminar autostart ────────────────────────────────────
if exist "%AUTOSTART%\Screenshot Tool.lnk" (
    del /f /q "%AUTOSTART%\Screenshot Tool.lnk" >nul 2>&1
    echo [postremove] Autostart eliminado
)

:: ── Eliminar comando global screenshot ───────────────────
if exist "%LOCAL_BIN%\screenshot.bat" (
    del /f /q "%LOCAL_BIN%\screenshot.bat" >nul 2>&1
    echo [postremove] Comando screenshot eliminado
)

:: ── Limpiar PATH (no falla si ya no está) ────────────────
powershell -Command ^
  "try { ^
   $path = [Environment]::GetEnvironmentVariable('PATH', 'User'); ^
   $newPath = ($path -split ';' | Where-Object { $_ -ne '%LOCAL_BIN%' }) -join ';'; ^
   [Environment]::SetEnvironmentVariable('PATH', $newPath, 'User'); ^
   Write-Host '[postremove] PATH limpiado' ^
   } catch { Write-Host '[postremove] PATH ya limpio' }" >nul 2>&1

echo.
echo [postremove] Screenshot Tool desinstalado correctamente.
echo [postremove] Java y Maven permanecen instalados en el sistema.
echo.

:: Siempre salir con codigo 0 para no bloquear la desinstalacion
exit /b 0
