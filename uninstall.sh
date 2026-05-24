#!/bin/bash
# ═══════════════════════════════════════════════════════════
#  ScreenshotTool v2 - Uninstaller
# ═══════════════════════════════════════════════════════════

set -e

INSTALL_DIR="$HOME/.local/share/screenshot-tool"
AUTOSTART_DIR="$HOME/.config/autostart"
DESKTOP_APPS_DIR="$HOME/.local/share/applications"
ICON_DIR="$HOME/.local/share/icons/hicolor/256x256/apps"
LOCAL_BIN="$HOME/.local/bin"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[!]${NC} $1"; }
log_info()    { echo -e "${BLUE}[INFO]${NC} $1"; }

echo ""
echo -e "${RED}═══════════════════════════════════════${NC}"
echo -e "${RED}   Screenshot Tool v2 - Uninstaller    ${NC}"
echo -e "${RED}═══════════════════════════════════════${NC}"
echo ""

read -p "Are you sure you want to uninstall Screenshot Tool? (y/n): " RESP
if [[ ! "$RESP" =~ ^[Yy]$ ]]; then
    echo "Uninstall cancelled."
    exit 0
fi

echo ""

# ── Stop running instance ────────────────────────────────
log_info "Checking for running instances..."
if pgrep -f "screenshot-tool.jar" > /dev/null 2>&1; then
    pkill -f "screenshot-tool.jar" && log_success "Process stopped"
else
    log_warn "No running instance found"
fi

# ── Remove installed files ───────────────────────────────
log_info "Removing files..."

if [ -d "$INSTALL_DIR" ]; then
    rm -rf "$INSTALL_DIR"
    log_success "Install directory removed: $INSTALL_DIR"
else
    log_warn "Not found: $INSTALL_DIR"
fi

if [ -f "$AUTOSTART_DIR/screenshot-tool.desktop" ]; then
    rm -f "$AUTOSTART_DIR/screenshot-tool.desktop"
    log_success "Autostart entry removed"
else
    log_warn "Autostart entry not found"
fi

if [ -f "$DESKTOP_APPS_DIR/screenshot-tool.desktop" ]; then
    rm -f "$DESKTOP_APPS_DIR/screenshot-tool.desktop"
    log_success "Application menu entry removed"
else
    log_warn "Application menu entry not found"
fi

if [ -f "$ICON_DIR/screenshot-tool.png" ]; then
    rm -f "$ICON_DIR/screenshot-tool.png"
    log_success "Icon removed"
else
    log_warn "Icon not found"
fi

if [ -f "$LOCAL_BIN/screenshot" ]; then
    rm -f "$LOCAL_BIN/screenshot"
    log_success "'screenshot' command removed"
else
    log_warn "'screenshot' command not found"
fi

# ── Clean PATH from shell config ─────────────────────────
for RC in "$HOME/.bashrc" "$HOME/.zshrc"; do
    if [ -f "$RC" ] && grep -q "\.local/bin" "$RC"; then
        sed -i '/export PATH.*\.local\/bin/d' "$RC"
        log_success "PATH cleaned in $RC"
    fi
done

# ── Update caches ────────────────────────────────────────
command -v gtk-update-icon-cache &>/dev/null && \
    gtk-update-icon-cache -f "$HOME/.local/share/icons/hicolor/" 2>/dev/null || true
command -v update-desktop-database &>/dev/null && \
    update-desktop-database "$DESKTOP_APPS_DIR" 2>/dev/null || true

echo ""
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo -e "${GREEN}   ✅ Uninstall complete                ${NC}"
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo ""
echo -e "  Screenshot Tool has been completely removed."
echo -e "  Java, JavaFX and Maven remain installed on the system."
echo ""
