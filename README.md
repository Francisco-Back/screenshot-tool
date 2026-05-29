# Screenshot Tool

A lightweight screen capture tool for Linux and Windows built with Java 21 and JavaFX. Lives in the system tray and captures screen areas with global keyboard shortcuts.

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![Maven](https://img.shields.io/badge/Maven-4.0-red)
![Platform](https://img.shields.io/badge/Platform-Linux%20%7C%20Windows-green)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## Features

- **System tray** integration — runs silently in the background
- **Two global hotkeys:**
  - `Ctrl+Alt+S` → area selection capture
  - `Ctrl+Alt+W` → active window capture
- **Area selection** with visual overlay, blue border, corner markers and ✕ cancel button
- **Active window capture** — instantly captures the focused window without manual selection
- **Multi-monitor** support — covers all screens simultaneously with screen labels
- **Single JVM instance** — trigger file system prevents multiple processes accumulating in memory
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
  - Toast notification after saving with "Open file" and "Show in folder" buttons (auto-closes in 4s)
  - Internal keyboard shortcuts: `Ctrl+S` save, `Ctrl+C` copy, `ESC` close, `Delete/F2` clear filename
- **Dark/light mode** — automatically adapts to system theme (Windows registry, Linux gsettings)

---

## Platform Compatibility

| Feature | Linux (GNOME) | Windows |
|---|---|---|
| Area selector | ✅ | ✅ |
| Active window capture | ✅ xdotool/scrot | ✅ Robot |
| Save dialog | ✅ | ✅ |
| Clipboard | ✅ | ✅ |
| Dark/light theme | ✅ | ✅ |
| System tray | ✅ | ✅ |
| HiDPI capture | ✅ | ✅ |
| `Ctrl+Alt+S` hotkey | ✅ GNOME gsettings | ✅ HotkeyManager |
| `Ctrl+Alt+W` hotkey | ✅ GNOME gsettings | ✅ HotkeyManager |
| `gnome-screenshot` backend | ✅ | ❌ |
| `scrot` / `xwd` backend | ✅ | ❌ |
| `Robot` backend | ✅ | ✅ |
| Single JVM instance | ✅ trigger file | ✅ HotkeyManager |
| Native installer | ✅ `.deb` (jpackage) | ✅ `.bat` |

---

## Requirements

### Linux
- Java 21+ (included in `.deb`)
- JavaFX 21+ (included in `.deb`)
- `xdotool` for active window capture (optional, auto-installed)

### Windows
- Java 21+
- Maven 3.6+

---

## Installation (Linux) — recommended

```bash
git clone https://github.com/Francisco-Back/screenshot-tool.git
cd screenshot-tool
sudo dpkg -i installers/linux/screenshot-tool_2.0.0_amd64.deb
```

The `.deb` automatically:
- Installs the app with JRE and JavaFX embedded (no Java required)
- Configures `Ctrl+Alt+S` and `Ctrl+Alt+W` hotkeys in GNOME
- Sets `StartupWMClass` so the correct icon appears in taskbar and dock
- Launches the app in the system tray

### Uninstall (Linux)

```bash
sudo dpkg -r screenshot-tool
```

The uninstaller automatically removes GNOME hotkeys and stops the running process.

---

## Build .deb from source (Linux)

```bash
git clone https://github.com/Francisco-Back/screenshot-tool.git
cd screenshot-tool
bash installers/linux/build-deb.sh
sudo dpkg -i installers/linux/screenshot-tool_2.0.0_amd64.deb
```

Requirements for building: `java 21`, `maven`, `fakeroot`, `binutils`

---

## Installation (Windows)

```batch
cd installers\windows
instalar.bat
```

The installer automatically:
- Verifies Java and Maven installation
- Builds the project with Maven
- Installs to `%LOCALAPPDATA%\screenshot-tool\`
- Creates Start Menu shortcut with `Ctrl+Alt+S` hotkey (or custom)
- Configures autostart on login via Startup folder
- Creates global `screenshot` command in `%USERPROFILE%\bin\`

> **Note:** Log out and back in after installing for the hotkey to activate.

### Uninstall (Windows)

```batch
installers\windows\uninstall.bat
```

---

## Usage

### Capture area
- Press `Ctrl+Alt+S`
- Drag to select the area
- Press `ESC` or click `✕` to cancel

### Capture active window
- Press `Ctrl+Alt+W`
- The focused window is captured instantly — no selection needed

### Save dialog shortcuts
| Shortcut | Action |
|---|---|
| `Ctrl+S` | Save file |
| `Ctrl+C` | Copy to clipboard |
| `ESC` | Close dialog |
| `Delete` / `F2` | Clear filename and focus input |
| `Enter` | Save file |

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

# Build JAR only
mvn clean package

# Build .deb (Linux)
bash installers/linux/build-deb.sh
```

### Project structure

```
screenshot-tool/
├── assets/                         # Icons and resources
├── installers/
│   ├── linux/
│   │   ├── build-deb.sh            # Builds .deb with jpackage
│   │   ├── postinst                # Runs after dpkg install
│   │   ├── postrm                  # Runs after dpkg remove
│   │   └── uninstall.sh            # Manual uninstaller
│   └── windows/
│       ├── instalar.bat            # Windows installer
│       └── uninstall.bat           # Windows uninstaller
├── pom.xml                         # Maven build file
├── README.md
└── src/main/
    ├── java/com/screenshottool/
    │   ├── ScreenshotApp.java              # JavaFX entry point
    │   ├── core/
    │   │   ├── AppContext.java             # Main orchestrator + trigger monitor
    │   │   ├── TrayManager.java            # System tray icon and menu
    │   │   ├── HotkeyManager.java          # Ctrl+Alt+S and Ctrl+Alt+W (Windows)
    │   │   └── SelectorDeAreaBridge.java   # Swing area selector (Robot fallback)
    │   ├── controller/
    │   │   ├── ScreenshotController.java   # Save dialog controller
    │   │   └── FolderPickerController.java # Folder picker controller
    │   ├── service/
    │   │   ├── ScreenshotService.java      # Capture backends, clipboard, save
    │   │   └── ToastService.java           # Fluent Design toast notification
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

The trigger file system ensures only one JVM instance runs at a time:

**Linux:**
```
Ctrl+Alt+S (1st press)  → app not running → launches JVM → stays in tray
Ctrl+Alt+S (2nd press)  → app running → touch /tmp/screenshottool.trigger
                        → AppContext detects within 300ms → area capture

Ctrl+Alt+W              → touch /tmp/screenshottool.trigger.window
                        → AppContext detects within 300ms → window capture
```

**Windows:**
```
Ctrl+Alt+S → HotkeyManager intercepts → calls iniciarCaptura() directly
Ctrl+Alt+W → HotkeyManager intercepts → calls iniciarCapturaVentana() directly
```

No new JVM is ever created after the first launch — memory stays constant.

---

## License

MIT License — feel free to use, modify and distribute.
