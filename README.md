# Screenshot Tool

A lightweight screen capture tool for Linux (and Windows) built with Java 21 and JavaFX. Lives in the system tray and captures screen areas with a global keyboard shortcut.

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21-blue)
![Maven](https://img.shields.io/badge/Maven-4.0-red)
![Platform](https://img.shields.io/badge/Platform-Linux%20%7C%20Windows-green)

---

## Features

- **System tray** integration — runs silently in the background
- **Global hotkey** `Ctrl+Shift+S` to trigger capture
- **Area selection** with visual overlay, blue border and corner markers
- **Multi-monitor** support — covers all screens simultaneously with screen labels
- **Native capture backends** with automatic fallback:
  - `gnome-screenshot` → best for GNOME/Wayland/VirtualBox
  - `scrot` → X11
  - `import` (ImageMagick) → X11 alternative
  - `Robot` (Java AWT) → fallback for any platform
- **Auto-copy** to clipboard immediately after capture
- **Save dialog** with:
  - Live preview of the capture
  - Filename input with timestamp default (`captura_2026-05-24_13-04-01`)
  - Format selector: `png`, `jpg`, `bmp`, `gif`
  - Folder selector (remembers last used, defaults to `~/Pictures`)
  - Toast notification after saving with "Open file" button
- **Dark/light mode** — automatically adapts to system theme (Windows registry, Linux GTK_THEME)
- **Trigger file** support — send capture command to running instance via `/tmp/screenshottool.trigger`
- **Cancel button (✕)** visible on the selection overlay

---

## Requirements

- Java 21+
- Maven 3.6+
- Linux (GNOME recommended) or Windows

---

## Installation (Linux)

```bash
git clone https://github.com/your-username/screenshot-tool.git
cd screenshot-tool
bash instalar.sh
```

The installer will automatically:
- Install Java 21, JavaFX (openjfx), Maven and gnome-screenshot if missing
- Build the project with Maven
- Install the app to `~/.local/share/screenshot-tool/`
- Create a `.desktop` entry in the application menu
- Configure autostart on login
- Register the `screenshot` terminal command
- Configure the `Ctrl+Shift+S` hotkey in GNOME

---

## Uninstall (Linux)

```bash
bash uninstall.sh
```

---

## Usage

### First run
```bash
screenshot
```
Launches the app in the background. The tray icon appears in the system tray.

### Capture
- Press `Ctrl+Shift+S` (configured in GNOME)
- Or run `screenshot` from terminal (triggers capture if already running)
- Or click the tray icon → **Capture area**

### Cancel selection
- Press `ESC` (Linux)
- Click the `✕` button (top-right corner of the overlay)

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
src/main/java/com/screenshottool/
├── ScreenshotApp.java              # JavaFX entry point
└── core/
    ├── AppContext.java             # Main orchestrator
    ├── TrayManager.java            # System tray icon and menu
    ├── HotkeyManager.java          # Global hotkey (Windows) / GNOME trigger (Linux)
    └── SelectorDeAreaBridge.java   # Swing area selector (Robot fallback)
src/main/java/com/screenshottool/
    ├── controller/
    │   ├── ScreenshotController.java   # Save dialog controller
    │   └── FolderPickerController.java # Folder picker controller
    ├── service/
    │   └── ScreenshotService.java      # Capture backends, clipboard, save logic
    └── model/
        └── CapturaModel.java           # Data model with JavaFX properties
src/main/resources/com/screenshottool/
    ├── fxml/
    │   ├── main.fxml               # Save dialog UI
    │   └── folder-picker.fxml      # Folder picker UI
    └── css/
        └── style.css               # Adaptive theme styles
```

---

## How the trigger system works

The `screenshot` command uses a file-based trigger to avoid launching multiple instances:

```
screenshot (1st run)  → launches app in background
screenshot (2nd run)  → creates /tmp/screenshottool.trigger
                      → AppContext detects it within 300ms
                      → triggers capture immediately
```

---

## License

MIT License — feel free to use, modify and distribute.
