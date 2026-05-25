# Screenshot Tool

A lightweight screen capture tool for Linux and Windows built with Java 21 and JavaFX. Lives in the system tray and captures screen areas with a global keyboard shortcut.

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![Maven](https://img.shields.io/badge/Maven-4.0-red)
![Platform](https://img.shields.io/badge/Platform-Linux%20%7C%20Windows-green)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## Features

- **System tray** integration — runs silently in the background
- **Global hotkey** `Ctrl+Alt+S` to trigger capture
- **Area selection** with visual overlay, blue border, corner markers and ✕ cancel button
- **Multi-monitor** support — covers all screens simultaneously with screen labels
- **Native capture backends** with automatic fallback:
  - `gnome-screenshot` → GNOME/Wayland/VirtualBox with Guest Additions
  - `scrot` → X11, works in VirtualBox without Guest Additions
  - `xwd` → X11 alternative, also works in VirtualBox
  - `import` (ImageMagick) → X11 alternative
  - `Robot` (Java AWT) → fallback for Windows and any platform
- **Auto-copy** to clipboard immediately after capture
- **HiDPI support** — correct resolution on high DPI screens (125%, 150%, 200%)
- **Save dialog** with:
  - Live preview of the capture
  - Filename input with timestamp default (`captura_2026-05-24_13-04-01`)
  - Format selector: `png`, `jpg`, `bmp`, `gif`
  - Folder selector (remembers last used, defaults to `~/Pictures`)
  - Toast notification after saving with "Open file" button (auto-closes in 4s)
- **Dark/light mode** — automatically adapts to system theme
- **Trigger file** support — send capture command to running instance via `/tmp/screenshottool.trigger`

---

## Platform Compatibility

| Feature | Linux (GNOME) | Windows |
|---|---|---|
| Area selector | ✅ | ✅ |
| Save dialog | ✅ | ✅ |
| Clipboard | ✅ | ✅ |
| Dark/light theme | ✅ | ✅ |
| System tray | ✅ | ✅ |
| HiDPI capture | ✅ | ✅ |
| `Ctrl+Alt+S` hotkey | ✅ GNOME | ✅ |
| `gnome-screenshot` backend | ✅ | ❌ |
| `scrot` / `xwd` backend | ✅ | ❌ |
| `Robot` backend | ✅ | ✅ |
| `screenshot` command | ✅ | ❌ |
| Installer script | ✅ | ❌ |

---

## Requirements

- Java 21+
- Maven 3.6+
- Linux (GNOME recommended) or Windows 10+

---

## Installation (Linux)

```bash
git clone https://github.com/Francisco-Back/screenshot-tool.git
cd screenshot-tool
bash instalar.sh
```

The installer automatically:
- Installs Java 21, JavaFX (openjfx), Maven, gnome-screenshot and scrot if missing
- Detects VirtualBox and shows Guest Additions recommendation
- Builds the project with Maven
- Installs the app to `~/.local/share/screenshot-tool/`
- Creates a `.desktop` entry in the application menu
- Configures autostart on login
- Registers the `screenshot` terminal command
- Configures the `Ctrl+Alt+S` hotkey in GNOME

## Uninstall (Linux)

```bash
bash uninstall.sh
```

---

## Usage (Windows)

```bash
# Run directly
mvn javafx:run

# Or build and run JAR
mvn clean package
java -jar target/screenshot-tool.jar
```

---

## Usage

### First run (Linux)
```bash
screenshot
```
Launches the app in the background. The tray icon appears in the system tray.

### Capture
- Press `Ctrl+Alt+S`
- Or run `screenshot` from terminal (Linux — triggers capture if already running)
- Or click the tray icon → **Capture area**

### Cancel selection
- Press `ESC` (Linux)
- Click the `✕` button (top-right corner of the overlay, works on both platforms)

---

## VirtualBox

If running inside VirtualBox and the capture appears black, install Guest Additions for best quality:

```bash
sudo apt install virtualbox-guest-x11 virtualbox-guest-utils
sudo reboot
```

Without Guest Additions, `scrot` or `xwd` will be used automatically as fallback backends via X11.

---

## Development

```bash
# Run in development mode
mvn javafx:run

# Build JAR
mvn clean package
```

### Project structure

```
screenshot-tool/
├── instalar.sh                         # Linux installer
├── uninstall.sh                        # Linux uninstaller
├── pom.xml                             # Maven build file
└── src/main/
    ├── java/com/screenshottool/
    │   ├── ScreenshotApp.java              # JavaFX entry point
    │   ├── core/
    │   │   ├── AppContext.java             # Main orchestrator
    │   │   ├── TrayManager.java            # System tray icon and menu
    │   │   ├── HotkeyManager.java          # Hotkey (Windows) / GNOME trigger (Linux)
    │   │   └── SelectorDeAreaBridge.java   # Swing area selector
    │   ├── controller/
    │   │   ├── ScreenshotController.java   # Save dialog controller
    │   │   └── FolderPickerController.java # Folder picker controller
    │   ├── service/
    │   │   └── ScreenshotService.java      # Capture backends, clipboard, save
    │   └── model/
    │       └── CapturaModel.java           # Data model with JavaFX properties
    └── resources/com/screenshottool/
        ├── fxml/
        │   ├── main.fxml                   # Save dialog UI
        │   └── folder-picker.fxml          # Folder picker UI
        └── css/
            └── style.css                   # Adaptive theme styles
```

---

## How the trigger system works

The `screenshot` command avoids launching multiple instances via a file-based trigger:

```
screenshot (1st run)  → launches app in background
screenshot (2nd run)  → creates /tmp/screenshottool.trigger
                      → AppContext detects it within 300ms
                      → triggers capture immediately
```

The app stays running in the tray for the entire session — no need to relaunch it.

---

## License

MIT License — feel free to use, modify and distribute.
