#!/bin/bash
# ═══════════════════════════════════════════════════════════
#  Screenshot Tool - Build .deb script
#  Compila con Maven y empaqueta con jpackage en un solo paso
#
#  Uso: bash build-deb.sh
#  Resultado: installers/linux/screenshot-tool_2.0.0_amd64.deb
# ═══════════════════════════════════════════════════════════

set -e

VERSION="2.0.0"
APP_NAME="screenshot-tool"
MAIN_CLASS="com.screenshottool.ScreenshotApp"
RESOURCES_DIR="/tmp/jpackage-resources"

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[!]${NC} $1"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ── Verificar desde raíz del proyecto ────────────────────
if [ ! -f "pom.xml" ]; then
    log_error "Ejecuta desde la raíz del proyecto (donde está pom.xml)"
fi

echo ""
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo -e "${BLUE}   Screenshot Tool - Build .deb        ${NC}"
echo -e "${BLUE}═══════════════════════════════════════${NC}"
echo ""

# ── Verificar herramientas ────────────────────────────────
command -v mvn      &>/dev/null || log_error "Maven no encontrado"
command -v jpackage &>/dev/null || log_error "jpackage no encontrado (requiere Java 14+)"
command -v fakeroot &>/dev/null || log_error "fakeroot no encontrado: sudo apt install fakeroot"
command -v dpkg-deb &>/dev/null || log_error "dpkg-deb no encontrado: sudo apt install binutils"

log_success "Herramientas verificadas"

# ── Compilar con Maven ────────────────────────────────────
log_info "Compilando con Maven..."
mvn clean package -q || log_error "Falló la compilación Maven. Ejecuta 'mvn package' para ver detalles."
log_success "Compilación exitosa"

# ── Buscar JARs de JavaFX ─────────────────────────────────
log_info "Buscando JavaFX runtime..."
JAVAFX_MODS="/tmp/javafx-mods-$$"
mkdir -p "$JAVAFX_MODS"

for MODULE in javafx-base javafx-graphics javafx-controls javafx-fxml javafx-swing; do
    JAR=$(find ~/.m2/repository/org/openjfx/$MODULE -name "*-linux.jar" \
        ! -name "*sources*" ! -name "*javadoc*" 2>/dev/null | sort -V | tail -1)
    if [ -z "$JAR" ]; then
        log_error "No se encontró $MODULE-linux.jar en Maven cache. Ejecuta 'mvn package' primero."
    fi
    cp "$JAR" "$JAVAFX_MODS/"
done
log_success "JavaFX modules encontrados"

# ── Buscar ícono ──────────────────────────────────────────
ICON_PATH=""
for f in "assets/icon.png" "captura-de-pantalla.png" "icon.png" "src/main/resources/com/screenshottool/img/icon-128.png"; do
    if [ -f "$f" ]; then
        ICON_PATH="$f"
        break
    fi
done
[ -z "$ICON_PATH" ] && log_warn "No se encontró ícono. Se usará el default."

# ── Preparar recursos para jpackage ──────────────────────
# postinst y postrm se incluyen aquí para que dpkg los ejecute
log_info "Preparando scripts de instalación..."
mkdir -p "$RESOURCES_DIR"

SCRIPT_DIR="$(dirname "$0")"
if [ -f "$SCRIPT_DIR/postinst" ]; then
    cp "$SCRIPT_DIR/postinst" "$RESOURCES_DIR/"
    chmod 755 "$RESOURCES_DIR/postinst"
    log_success "postinst incluido"
else
    log_warn "postinst no encontrado en $SCRIPT_DIR — atajos GNOME no se configurarán automáticamente"
fi

if [ -f "$SCRIPT_DIR/postrm" ]; then
    cp "$SCRIPT_DIR/postrm" "$RESOURCES_DIR/"
    chmod 755 "$RESOURCES_DIR/postrm"
    log_success "postrm incluido"
fi

# ── Crear directorio de salida ────────────────────────────
mkdir -p installers/linux

# ── Empaquetar con jpackage ───────────────────────────────
log_info "Empaquetando .deb con jpackage..."

JPACKAGE_CMD=(
    jpackage
    --type deb
    --name "$APP_NAME"
    --app-version "$VERSION"
    --vendor "Francisco Back"
    --description "Lightweight screen capture tool for Linux and Windows"
    --input target
    --main-jar screenshot-tool.jar
    --main-class "$MAIN_CLASS"
    --module-path "$JAVAFX_MODS"
    --add-modules "javafx.controls,javafx.fxml,javafx.swing,javafx.graphics,javafx.base"
    --dest installers/linux
    --linux-shortcut
    --linux-menu-group "Graphics"
    --linux-app-category "Graphics"
    --resource-dir "$RESOURCES_DIR"
)

# Agregar ícono si existe
if [ -n "$ICON_PATH" ]; then
    JPACKAGE_CMD+=(--icon "$ICON_PATH")
fi

"${JPACKAGE_CMD[@]}" || log_error "Falló jpackage"

# ── Limpiar temporales ────────────────────────────────────
rm -rf "$JAVAFX_MODS" "$RESOURCES_DIR"

# ── Resultado ─────────────────────────────────────────────
DEB_FILE=$(ls installers/linux/*.deb 2>/dev/null | tail -1)

echo ""
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo -e "${GREEN}   ✅ .deb generado exitosamente        ${NC}"
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo ""
echo -e "  Archivo: ${BLUE}$DEB_FILE${NC}"
echo -e "  Tamaño:  ${BLUE}$(du -sh "$DEB_FILE" | cut -f1)${NC}"
echo ""
echo -e "  Para instalar:"
echo -e "  ${YELLOW}sudo dpkg -i $DEB_FILE${NC}"
echo ""
echo -e "  Para desinstalar:"
echo -e "  ${YELLOW}sudo dpkg -r screenshot-tool${NC}"
echo ""
