#!/bin/bash
# ═══════════════════════════════════════════════════════════
#  ScreenshotTool v2 - Universal installer
#  Compatible: Debian, Ubuntu, Fedora, Arch, Manjaro, openSUSE
#  - Installs Java 21, JavaFX (openjfx), Maven and gnome-screenshot
#  - Configures GNOME hotkey automatically
#  - Detects existing instance and stops it cleanly
#  - Icon: looks for captura-de-pantalla.png or any PNG
# ═══════════════════════════════════════════════════════════

set -e

INSTALL_DIR="$HOME/.local/share/screenshot-tool"
AUTOSTART_DIR="$HOME/.config/autostart"
DESKTOP_APPS_DIR="$HOME/.local/share/applications"
ICON_DIR="$HOME/.local/share/icons/hicolor/256x256/apps"
ICON_NAME="screenshot-tool"
JAR_NAME="screenshot-tool.jar"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[!]${NC} $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

ORIG_DIR="$(pwd)"

echo ""
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo -e "${BLUE}   Screenshot Tool v2 - Installer      ${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo ""

# ── Detect Linux distribution ────────────────────────────
detect_distro() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        DISTRO=$ID
    elif command -v lsb_release &>/dev/null; then
        DISTRO=$(lsb_release -si | tr '[:upper:]' '[:lower:]')
    else
        DISTRO="unknown"
    fi
    log_info "Detected distribution: $DISTRO"
}

# ── Install Java 21 if missing ───────────────────────────
install_java() {
    if command -v java &>/dev/null && command -v javac &>/dev/null; then
        JAVA_VER=$(java -version 2>&1 | head -1)
        log_success "Java already installed: $JAVA_VER"
        return
    fi

    log_warn "Java not found. Installing..."
    case "$DISTRO" in
        debian|ubuntu|linuxmint|pop|elementary|zorin|kali|raspbian)
            sudo apt-get update -qq
            sudo apt-get install -y openjdk-21-jdk
            ;;
        fedora)
            sudo dnf install -y java-21-openjdk-devel
            ;;
        centos|rhel|almalinux|rocky)
            sudo dnf install -y java-21-openjdk-devel
            ;;
        arch|manjaro|endeavouros|garuda)
            sudo pacman -Sy --noconfirm jdk21-openjdk
            ;;
        opensuse*|sles)
            sudo zypper install -y java-21-openjdk-devel
            ;;
        *)
            log_error "Distribution '$DISTRO' not recognized. Install Java 21 manually."
            ;;
    esac
    log_success "Java installed successfully"
}

# ── Install JavaFX (openjfx) if missing ─────────────────
install_javafx() {
    if [ -d "$HOME/.m2/repository/org/openjfx" ]; then
        log_success "JavaFX already in Maven cache"
        return
    fi

    log_warn "Installing JavaFX (openjfx)..."
    case "$DISTRO" in
        debian|ubuntu|linuxmint|pop|elementary|zorin|kali|raspbian)
            sudo apt-get install -y openjfx 2>/dev/null || \
                log_warn "openjfx not available via apt - Maven will download it automatically"
            ;;
        fedora)
            log_warn "Fedora: JavaFX will be downloaded by Maven automatically"
            ;;
        arch|manjaro|endeavouros|garuda)
            sudo pacman -Sy --noconfirm java-openjfx 2>/dev/null || \
                log_warn "JavaFX not found in pacman - Maven will download it"
            ;;
        opensuse*|sles)
            sudo zypper install -y java-21-openjfx 2>/dev/null || \
                log_warn "JavaFX not found in zypper - Maven will download it"
            ;;
        *)
            log_warn "JavaFX will be downloaded by Maven automatically"
            ;;
    esac
}

# ── Install gnome-screenshot if missing ──────────────────
install_gnome_screenshot
install_scrot
detect_virtualbox() {
    if command -v gnome-screenshot &>/dev/null; then
        log_success "gnome-screenshot already installed"
        return
    fi

    log_warn "Installing gnome-screenshot..."
    case "$DISTRO" in
        debian|ubuntu|linuxmint|pop|elementary|zorin|kali|raspbian)
            sudo apt-get install -y gnome-screenshot
            ;;
        fedora|centos|rhel|almalinux|rocky)
            sudo dnf install -y gnome-screenshot
            ;;
        arch|manjaro|endeavouros|garuda)
            sudo pacman -Sy --noconfirm gnome-screenshot
            ;;
        opensuse*|sles)
            sudo zypper install -y gnome-screenshot
            ;;
        *)
            log_warn "Could not install gnome-screenshot. Install it manually."
            ;;
    esac
    log_success "gnome-screenshot installed"
}

# ── Install scrot (VirtualBox fallback) ────────────────
install_scrot() {
    if command -v scrot &>/dev/null; then
        log_success "scrot already installed"
        return
    fi

    log_warn "Installing scrot (X11 capture backend)..."
    case "$DISTRO" in
        debian|ubuntu|linuxmint|pop|elementary|zorin|kali|raspbian)
            sudo apt-get install -y scrot
            ;;
        fedora|centos|rhel|almalinux|rocky)
            sudo dnf install -y scrot
            ;;
        arch|manjaro|endeavouros|garuda)
            sudo pacman -Sy --noconfirm scrot
            ;;
        opensuse*|sles)
            sudo zypper install -y scrot
            ;;
        *)
            log_warn "Could not install scrot. Install it manually."
            ;;
    esac
    log_success "scrot installed"
}

# ── Detect VirtualBox && warn ───────────────────────────
detect_virtualbox() {
    if systemd-detect-virt 2>/dev/null | grep -qiE "oracle|virtualbox"; then
        echo ""
        echo -e "${YELLOW}════════════════════════════════════════${NC}"
        echo -e "${YELLOW}  VirtualBox detected                    ${NC}"
        echo -e "${YELLOW}════════════════════════════════════════${NC}"
        echo -e "  scrot will be used as capture backend."
        echo -e "  For best quality install Guest Additions:"
        echo -e "  ${BLUE}sudo apt install virtualbox-guest-x11 virtualbox-guest-utils${NC}"
        echo -e "  ${BLUE}sudo reboot${NC}"
        echo ""
    fi
}

# ── Install Maven if missing ─────────────────────────────
install_maven() {
    if command -v mvn &>/dev/null; then
        MVN_VER=$(mvn -version 2>&1 | head -1)
        log_success "Maven already installed: $MVN_VER"
        return
    fi

    log_warn "Maven not found. Installing..."
    case "$DISTRO" in
        debian|ubuntu|linuxmint|pop|elementary|zorin|kali|raspbian)
            sudo apt-get install -y maven
            ;;
        fedora|centos|rhel|almalinux|rocky)
            sudo dnf install -y maven
            ;;
        arch|manjaro|endeavouros|garuda)
            sudo pacman -Sy --noconfirm maven
            ;;
        opensuse*|sles)
            sudo zypper install -y maven
            ;;
        *)
            log_error "Could not install Maven. Install it manually: https://maven.apache.org"
            ;;
    esac
    log_success "Maven installed successfully"
}

# ── Verify required source files ─────────────────────────
verify_files() {
    if [ ! -f "$ORIG_DIR/pom.xml" ]; then
        log_error "pom.xml not found. Run the installer from the project root directory."
    fi
    if [ ! -d "$ORIG_DIR/src" ]; then
        log_error "src/ directory not found. Run the installer from the project root directory."
    fi
    log_success "Source files found"
}

# ── Find icon ────────────────────────────────────────────
find_icon() {
    ICON_SOURCE=""
    if [ -f "$ORIG_DIR/captura-de-pantalla.png" ]; then
        ICON_SOURCE="$ORIG_DIR/captura-de-pantalla.png"
        log_success "Icon found: captura-de-pantalla.png"
        return
    fi
    for f in "$ORIG_DIR"/*.png; do
        if [ -f "$f" ]; then
            ICON_SOURCE="$f"
            log_warn "Using icon: $(basename "$f")"
            return
        fi
    done
    log_warn "No PNG found. A basic icon will be generated."
}

# ── Create directories ───────────────────────────────────
create_dirs() {
    mkdir -p "$INSTALL_DIR"
    mkdir -p "$AUTOSTART_DIR"
    mkdir -p "$DESKTOP_APPS_DIR"
    mkdir -p "$ICON_DIR"
    log_success "Directories created"
}

# ── Install icon (resize to 256x256 with Java) ───────────
install_icon() {
    ICON_DEST="$ICON_DIR/${ICON_NAME}.png"

    if [ -n "$ICON_SOURCE" ]; then
        cat > /tmp/ResizeIcon_$$.java << 'JAVA'
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
public class ResizeIcon_PID {
    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        if (src == null) { System.exit(1); }
        int size = 256;
        BufferedImage dest = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, size, size, null);
        g.dispose();
        ImageIO.write(dest, "png", new File(args[1]));
    }
}
JAVA
        RESIZE_FILE="/tmp/ResizeIcon_$$.java"
        RESIZE_CLASS="ResizeIcon_$$"
        sed -i "s/ResizeIcon_PID/ResizeIcon_$$/g" "$RESIZE_FILE"

        if javac "$RESIZE_FILE" -d /tmp/ 2>/dev/null && \
           java -cp /tmp "$RESIZE_CLASS" "$ICON_SOURCE" "$ICON_DEST" 2>/dev/null; then
            log_success "Icon resized to 256x256"
        else
            cp "$ICON_SOURCE" "$ICON_DEST"
            log_warn "Could not resize icon. Copied original."
        fi
        rm -f "$RESIZE_FILE" "/tmp/${RESIZE_CLASS}.class" 2>/dev/null || true
    else
        cat > /tmp/GenIcon.java << 'JAVA'
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
public class GenIcon {
    public static void main(String[] args) throws Exception {
        int sz = 256;
        BufferedImage img = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(30, 144, 255));
        g.fillRoundRect(0, 0, sz, sz, 50, 50);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawRoundRect(30, 64, 196, 130, 24, 24);
        g.fillRoundRect(88, 42, 56, 30, 14, 14);
        g.fillRoundRect(38, 72, 32, 20, 6, 6);
        g.drawOval(78, 72, 100, 100);
        g.setColor(new Color(30, 144, 255, 200));
        g.fillOval(92, 86, 72, 72);
        g.setColor(new Color(255, 255, 255, 130));
        g.fillOval(100, 92, 24, 24);
        g.dispose();
        ImageIO.write(img, "png", new File(args[0]));
    }
}
JAVA
        javac /tmp/GenIcon.java -d /tmp/ 2>/dev/null && \
            java -cp /tmp GenIcon "$ICON_DEST" 2>/dev/null || true
        rm -f /tmp/GenIcon.java /tmp/GenIcon.class 2>/dev/null || true
        [ -f "$ICON_DEST" ] && log_success "Basic icon generated" || \
            log_warn "Could not generate icon. Continuing without one."
    fi
}

# ── Build JAR with Maven ─────────────────────────────────
build() {
    log_info "Building project with Maven..."
    cd "$ORIG_DIR"
    mvn clean package -q || log_error "Maven build failed. Run 'mvn package' to see details."

    BUILT_JAR="$ORIG_DIR/target/screenshot-tool.jar"
    if [ ! -f "$BUILT_JAR" ]; then
        log_error "JAR not found after build: $BUILT_JAR"
    fi

    cp "$BUILT_JAR" "$INSTALL_DIR/$JAR_NAME"
    log_success "Built and installed → $INSTALL_DIR/$JAR_NAME"
    cd "$ORIG_DIR"
}

# ── Create .desktop entry ────────────────────────────────
create_desktop_entry() {
    ICON_PATH="$ICON_DIR/${ICON_NAME}.png"
    [ ! -f "$ICON_PATH" ] && ICON_PATH="$ICON_NAME"

    cat > "$DESKTOP_APPS_DIR/screenshot-tool.desktop" << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=Screenshot Tool
GenericName=Screen capture
Comment=Capture screen areas with Ctrl+Shift+S
Exec=java -jar $INSTALL_DIR/$JAR_NAME
Icon=$ICON_PATH
Terminal=false
Categories=Graphics;Utility;
Keywords=screenshot;capture;screen;snip;
StartupNotify=false
EOF
    chmod +x "$DESKTOP_APPS_DIR/screenshot-tool.desktop"
    log_success "Application menu entry created"
}

# ── Create autostart entry ───────────────────────────────
create_autostart() {
    ICON_PATH="$ICON_DIR/${ICON_NAME}.png"
    [ ! -f "$ICON_PATH" ] && ICON_PATH="$ICON_NAME"

    cat > "$AUTOSTART_DIR/screenshot-tool.desktop" << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=Screenshot Tool
Comment=Capture screen areas with Ctrl+Shift+S
Exec=java -jar $INSTALL_DIR/$JAR_NAME
Icon=$ICON_PATH
Terminal=false
Hidden=false
X-GNOME-Autostart-enabled=true
X-GNOME-Autostart-Delay=3
EOF
    log_success "Autostart configured (will launch on login)"
}

# ── Configure GNOME hotkey ────────────────────────────────
configure_gnome_hotkey() {
    if ! command -v gsettings &>/dev/null; then
        log_warn "gsettings not found. Configure hotkey manually in GNOME Settings."
        return
    fi

    echo ""
    echo -e "${BLUE}Configure keyboard shortcut:${NC}"
    echo ""
    echo -e "  ${YELLOW}1.${NC} Ctrl+Shift+S     (standard keyboard)"
    echo -e "  ${YELLOW}2.${NC} Super+Shift+S    (mouse macro)"
    echo -e "  ${YELLOW}3.${NC} Custom combination"
    echo -e "  ${YELLOW}4.${NC} Skip"
    echo ""
    read -p "Choose an option (1-4): " HOTKEY_OPT

    case $HOTKEY_OPT in
        1) BINDING="<Control><Shift>s" ;;
        2) BINDING="<Super><Shift>s" ;;
        3)
            echo ""
            echo -e "  Format: ${YELLOW}<Control><Shift>s${NC} or ${YELLOW}<Super><Shift>s${NC}"
            read -p "  Enter combination: " BINDING
            ;;
        4)
            log_warn "Hotkey configuration skipped."
            return
            ;;
        *)
            log_warn "Invalid option. Hotkey configuration skipped."
            return
            ;;
    esac

    BINDING_PATH="/org/gnome/settings-daemon/plugins/media-keys/custom-keybindings/screenshot-tool/"

    # Agregar a la lista de atajos personalizados
    CURRENT=$(gsettings get org.gnome.settings-daemon.plugins.media-keys custom-keybindings 2>/dev/null || echo "@as []")

    if [[ "$CURRENT" == "@as []" ]]; then
        gsettings set org.gnome.settings-daemon.plugins.media-keys custom-keybindings "['$BINDING_PATH']"
    elif [[ "$CURRENT" != *"$BINDING_PATH"* ]]; then
        NEW=$(echo "$CURRENT" | sed "s/]$/, '$BINDING_PATH']/")
        gsettings set org.gnome.settings-daemon.plugins.media-keys custom-keybindings "$NEW"
    fi

    # Configurar nombre, comando y atajo
    gsettings set org.gnome.settings-daemon.plugins.media-keys.custom-keybinding:$BINDING_PATH \
        name "Screenshot Tool"
    gsettings set org.gnome.settings-daemon.plugins.media-keys.custom-keybinding:$BINDING_PATH \
        command "touch /tmp/screenshottool.trigger"
    gsettings set org.gnome.settings-daemon.plugins.media-keys.custom-keybinding:$BINDING_PATH \
        binding "$BINDING"

    log_success "Hotkey '$BINDING' configured in GNOME"
}

# ── Create global 'screenshot' command ───────────────────
create_global_command() {
    LOCAL_BIN="$HOME/.local/bin"
    mkdir -p "$LOCAL_BIN"

    cat > "$LOCAL_BIN/screenshot" << EOF
#!/bin/bash
if pgrep -f "screenshot-tool.jar" > /dev/null 2>&1; then
    touch /tmp/screenshottool.trigger
    echo "Capturing..."
else
    java -jar $INSTALL_DIR/$JAR_NAME &
    echo "Screenshot Tool started. Use your configured hotkey to capture."
fi
EOF
    chmod +x "$LOCAL_BIN/screenshot"

    if [[ ":$PATH:" != *":$LOCAL_BIN:"* ]]; then
        SHELL_RC="$HOME/.bashrc"
        [ -f "$HOME/.zshrc" ] && SHELL_RC="$HOME/.zshrc"
        echo "export PATH=\"\$PATH:$LOCAL_BIN\"" >> "$SHELL_RC"
        log_warn "Added $LOCAL_BIN to PATH in $SHELL_RC (restart terminal to apply)"
    fi
    log_success "'screenshot' command available in terminal"
}

# ── Update icon and desktop caches ───────────────────────
update_caches() {
    command -v gtk-update-icon-cache &>/dev/null && \
        gtk-update-icon-cache -f "$HOME/.local/share/icons/hicolor/" 2>/dev/null || true
    command -v update-desktop-database &>/dev/null && \
        update-desktop-database "$DESKTOP_APPS_DIR" 2>/dev/null || true
}

# ── Summary ──────────────────────────────────────────────
show_summary() {
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════${NC}"
    echo -e "${GREEN}   ✅ Installation complete             ${NC}"
    echo -e "${GREEN}═══════════════════════════════════════${NC}"
    echo ""
    echo -e "  📁 Installed at:  ${BLUE}$INSTALL_DIR${NC}"
    echo -e "  🖼️  Icon at:       ${BLUE}$ICON_DIR${NC}"
    echo -e "  🚀 Autostart:     ${BLUE}$AUTOSTART_DIR${NC}"
    echo -e "  📋 App menu:      ${BLUE}$DESKTOP_APPS_DIR${NC}"
    echo ""
    echo -e "  Run from terminal:      ${YELLOW}screenshot${NC}"
    echo -e "  Or directly:            ${YELLOW}java -jar $INSTALL_DIR/$JAR_NAME${NC}"
    echo ""
    echo -e "  ${BLUE}Configured hotkey${NC}   → trigger capture"
    echo -e "  ${BLUE}screenshot${NC}          → launch or trigger capture"
    echo -e "  Multi-monitor support included"
    echo ""
}

# ── Main flow ────────────────────────────────────────────
detect_distro
install_java
install_javafx
install_gnome_screenshot
install_scrot
detect_virtualbox
install_maven
verify_files
find_icon
create_dirs
install_icon
build
create_desktop_entry
create_autostart
update_caches
configure_gnome_hotkey
create_global_command
show_summary

read -p "Run Screenshot Tool now? (y/n): " RESP
if [[ "$RESP" =~ ^[Yy]$ ]]; then
    java -jar "$INSTALL_DIR/$JAR_NAME" &
    echo -e "${GREEN}✓ Running in background...${NC}"
fi
