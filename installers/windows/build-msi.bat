@echo off
:: ═══════════════════════════════════════════════════════════
::  Screenshot Tool - Build .msi script
::  Compila con Maven y empaqueta con jpackage en un solo paso
::
::  Uso: build-msi.bat (desde la raiz del proyecto)
::  Resultado: installers\windows\screenshot-tool-2.0.0.msi
:: ═══════════════════════════════════════════════════════════

setlocal enabledelayedexpansion

set VERSION=2.0.0
set APP_NAME=screenshot-tool
set MAIN_CLASS=com.screenshottool.ScreenshotApp
set JAVAFX_MODS=%TEMP%\javafx-mods
set RESOURCES_DIR=%TEMP%\jpackage-resources
set OUTPUT_DIR=installers\windows

echo.
echo ═══════════════════════════════════════
echo    Screenshot Tool - Build .msi
echo ═══════════════════════════════════════
echo.

:: ── Verificar desde raiz del proyecto ────────────────────
if not exist "pom.xml" (
    echo [ERROR] Ejecuta desde la raiz del proyecto donde esta pom.xml
    pause
    exit /b 1
)

:: ── Verificar herramientas ────────────────────────────────
echo [INFO] Verificando herramientas...

java -version >nul 2>&1
if errorlevel 1 ( echo [ERROR] Java no encontrado & pause & exit /b 1 )

mvn -version >nul 2>&1
if errorlevel 1 ( echo [ERROR] Maven no encontrado & pause & exit /b 1 )

jpackage --version >nul 2>&1
if errorlevel 1 ( echo [ERROR] jpackage no encontrado. Requiere Java 14+ & pause & exit /b 1 )

candle --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] WiX Toolset no encontrado.
    echo         Instala desde: https://github.com/wixtoolset/wix3/releases
    echo         O ejecuta: winget install WiXToolset.WiX.v3
    pause
    exit /b 1
)

echo [OK] Herramientas verificadas

:: ── Compilar con Maven ────────────────────────────────────
echo [INFO] Compilando con Maven...
call mvn clean package -q
if errorlevel 1 (
    echo [ERROR] Fallo la compilacion Maven. Ejecuta 'mvn package' para ver detalles.
    pause
    exit /b 1
)
echo [OK] Compilacion exitosa

:: ── Buscar JARs de JavaFX para Windows ───────────────────
echo [INFO] Buscando JavaFX runtime...
if exist "%JAVAFX_MODS%" rmdir /s /q "%JAVAFX_MODS%"
mkdir "%JAVAFX_MODS%"

set JAVAFX_BASE=%USERPROFILE%\.m2\repository\org\openjfx
set FOUND_ALL=true

for %%M in (javafx-base javafx-graphics javafx-controls javafx-fxml javafx-swing) do (
    set JAR_FOUND=false
    for /f "delims=" %%F in ('dir /b /s "%JAVAFX_BASE%\%%M\*-win.jar" 2^>nul ^| findstr /v "sources javadoc"') do (
        if "!JAR_FOUND!"=="false" (
            copy "%%F" "%JAVAFX_MODS%\" >nul
            echo [OK] %%M encontrado
            set JAR_FOUND=true
        )
    )
    if "!JAR_FOUND!"=="false" (
        echo [ERROR] No se encontro %%M-win.jar en Maven cache.
        echo         Ejecuta 'mvn package' primero para descargar dependencias.
        set FOUND_ALL=false
    )
)

if "!FOUND_ALL!"=="false" (
    rmdir /s /q "%JAVAFX_MODS%"
    pause
    exit /b 1
)
echo [OK] JavaFX modules encontrados

:: ── Buscar icono ──────────────────────────────────────────
set ICON_PATH=
if exist "assets\icon.ico" set ICON_PATH=assets\icon.ico
if "!ICON_PATH!"=="" if exist "assets\icon.png" set ICON_PATH=assets\icon.png
if "!ICON_PATH!"=="" echo [!] No se encontro icono. Se usara el default.

:: ── Preparar recursos para jpackage ──────────────────────
echo [INFO] Preparando scripts de instalacion...
if exist "%RESOURCES_DIR%" rmdir /s /q "%RESOURCES_DIR%"
mkdir "%RESOURCES_DIR%"

set SCRIPT_DIR=%~dp0
if exist "%SCRIPT_DIR%postinstall.bat" (
    copy "%SCRIPT_DIR%postinstall.bat" "%RESOURCES_DIR%\" >nul
    echo [OK] postinstall.bat incluido
) else (
    echo [!] postinstall.bat no encontrado en %SCRIPT_DIR%
)

if exist "%SCRIPT_DIR%postremove.bat" (
    copy "%SCRIPT_DIR%postremove.bat" "%RESOURCES_DIR%\" >nul
    echo [OK] postremove.bat incluido
)

:: ── Crear directorio de salida ────────────────────────────
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

:: ── Empaquetar con jpackage ───────────────────────────────
echo [INFO] Empaquetando .msi con jpackage...

set JPACKAGE_CMD=jpackage ^
  --type msi ^
  --name "%APP_NAME%" ^
  --app-version "%VERSION%" ^
  --vendor "Francisco Back" ^
  --description "Lightweight screen capture tool for Linux and Windows" ^
  --input target ^
  --main-jar screenshot-tool.jar ^
  --main-class "%MAIN_CLASS%" ^
  --module-path "%JAVAFX_MODS%" ^
  --add-modules "javafx.controls,javafx.fxml,javafx.swing,javafx.graphics,javafx.base" ^
  --dest "%OUTPUT_DIR%" ^
  --win-shortcut ^
  --win-menu ^
  --win-menu-group "Screenshot Tool" ^
  --win-dir-chooser ^
  --win-shortcut-prompt ^
  --resource-dir "%RESOURCES_DIR%"

if not "!ICON_PATH!"=="" (
    set JPACKAGE_CMD=!JPACKAGE_CMD! --icon "!ICON_PATH!"
)

call !JPACKAGE_CMD!

if errorlevel 1 (
    echo [ERROR] Fallo jpackage
    rmdir /s /q "%JAVAFX_MODS%" 2>nul
    rmdir /s /q "%RESOURCES_DIR%" 2>nul
    pause
    exit /b 1
)

:: ── Limpiar temporales ────────────────────────────────────
rmdir /s /q "%JAVAFX_MODS%" 2>nul
rmdir /s /q "%RESOURCES_DIR%" 2>nul

:: ── Resultado ─────────────────────────────────────────────
echo.
echo ═══════════════════════════════════════
echo    .msi generado exitosamente
echo ═══════════════════════════════════════
echo.
echo   Archivo: %OUTPUT_DIR%\%APP_NAME%-%VERSION%.msi
echo.
echo   Para instalar:
echo   %OUTPUT_DIR%\%APP_NAME%-%VERSION%.msi
echo.
echo   Para desinstalar:
echo   Panel de Control -^> Programas -^> Screenshot Tool
echo.
pause
endlocal
