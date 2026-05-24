package com.screenshottool.service;

import com.screenshottool.model.CapturaModel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * ScreenshotService - Toda la lógica de negocio.
 *
 * Jerarquía de backends para captura:
 *   1. gnome-screenshot → GNOME/Wayland/VirtualBox con Guest Additions
 *   2. scrot            → X11, funciona en VirtualBox sin Guest Additions
 *   3. xwd              → X11 alternativo, también funciona en VirtualBox
 *   4. import           → ImageMagick, alternativa X11
 *   5. Robot            → fallback final (Windows, macOS, Linux sin herramientas)
 *
 * VirtualBox: instalar Guest Additions mejora la calidad.
 *             Sin ellas, scrot o xwd capturan correctamente via X11.
 */
public class ScreenshotService {

    private static final File TMP_FILE =
        new File(System.getProperty("java.io.tmpdir"), "screenshottool_tmp.png");

    private final Backend backendActivo;

    public enum Backend {
        GNOME_SCREENSHOT,
        SCROT,
        XWD,
        IMPORT_IMAGEMAGICK,
        ROBOT
    }

    public ScreenshotService() {
        this.backendActivo = detectarBackend();
        System.out.println("[ScreenshotService] Backend activo: " + backendActivo);

        // Advertencia si estamos en VirtualBox y no hay backend nativo
        if (backendActivo == Backend.ROBOT && enVirtualBox()) {
            System.err.println("[ADVERTENCIA] VirtualBox detectado con backend Robot.");
            System.err.println("  Para mejor calidad instala Guest Additions:");
            System.err.println("  sudo apt install virtualbox-guest-x11 virtualbox-guest-utils");
            System.err.println("  O instala scrot: sudo apt install scrot");
        }
    }

    // ── Detección de backend ──────────────────────────────
    private Backend detectarBackend() {
        if (comandoDisponible("gnome-screenshot")) return Backend.GNOME_SCREENSHOT;
        if (comandoDisponible("scrot"))             return Backend.SCROT;
        if (comandoDisponible("xwd"))               return Backend.XWD;
        if (comandoDisponible("import"))            return Backend.IMPORT_IMAGEMAGICK;
        return Backend.ROBOT;
    }

    private boolean comandoDisponible(String cmd) {
        try {
            Process p = new ProcessBuilder("which", cmd)
                .redirectErrorStream(true)
                .start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Detectar si estamos en VirtualBox ─────────────────
    public boolean enVirtualBox() {
        try {
            Process p = new ProcessBuilder("systemd-detect-virt")
                .redirectErrorStream(true)
                .start();
            String output = new String(p.getInputStream().readAllBytes()).trim();
            return output.contains("oracle") || output.contains("virtualbox");
        } catch (Exception e) {
            return false;
        }
    }

    public Backend getBackendActivo() { return backendActivo; }

    // ── Captura con selección de área ─────────────────────
    public BufferedImage capturarConSeleccion() throws Exception {
        // Todos los backends usan SelectorDeAreaBridge para mostrar
        // nuestro selector personalizado y luego capturan el área exacta
        return null;
    }

    // ── Capturar área con el backend activo ───────────────
    public BufferedImage capturarAreaConBackend(Rectangle area) throws Exception {
        return switch (backendActivo) {
            case GNOME_SCREENSHOT   -> capturarAreaConGnome(area);
            case SCROT              -> capturarAreaConScrot(area);
            case XWD                -> capturarAreaConXwd(area);
            case IMPORT_IMAGEMAGICK -> capturarAreaConImport(area);
            case ROBOT              -> capturarAreaConRobot(area);
        };
    }

    // ── Backend 1: gnome-screenshot con coordenadas ───────
    private BufferedImage capturarAreaConGnome(Rectangle area) throws Exception {
        String coords = area.x + "," + area.y + "," + area.width + "," + area.height;
        Process p = new ProcessBuilder(
            "gnome-screenshot",
            "--area=" + coords,
            "--file=" + TMP_FILE.getAbsolutePath())
            .redirectErrorStream(true)
            .start();
        int exitCode = p.waitFor();
        Thread.sleep(200);
        if (exitCode != 0 || !TMP_FILE.exists()) return capturarAreaConRobot(area);
        BufferedImage img = ImageIO.read(TMP_FILE);
        TMP_FILE.delete();
        return img;
    }

    // ── Backend 2: scrot con coordenadas ──────────────────
    // Funciona en VirtualBox via X11 sin necesitar Guest Additions
    private BufferedImage capturarAreaConScrot(Rectangle area) throws Exception {
        Process p = new ProcessBuilder(
            "scrot",
            "-a", area.x + "," + area.y + "," + area.width + "," + area.height,
            TMP_FILE.getAbsolutePath())
            .redirectErrorStream(true)
            .start();
        int exitCode = p.waitFor();
        if (exitCode != 0 || !TMP_FILE.exists()) return capturarAreaConRobot(area);
        BufferedImage img = ImageIO.read(TMP_FILE);
        TMP_FILE.delete();
        return img;
    }

    // ── Backend 3: xwd con coordenadas ────────────────────
    // xwd captura via X11 directamente, funciona en VirtualBox
    // Requiere: sudo apt install x11-utils imagemagick
    private BufferedImage capturarAreaConXwd(Rectangle area) throws Exception {
        File xwdTmp = new File(System.getProperty("java.io.tmpdir"), "screenshottool_tmp.xwd");
        try {
            // Capturar pantalla completa con xwd
            Process p = new ProcessBuilder(
                "xwd", "-root", "-silent", "-out", xwdTmp.getAbsolutePath())
                .redirectErrorStream(true)
                .start();
            int exitCode = p.waitFor();
            if (exitCode != 0 || !xwdTmp.exists()) return capturarAreaConRobot(area);

            // Convertir xwd a png y recortar con convert (ImageMagick)
            String crop = area.width + "x" + area.height + "+" + area.x + "+" + area.y;
            Process p2 = new ProcessBuilder(
                "convert",
                xwdTmp.getAbsolutePath(),
                "-crop", crop,
                "+repage",
                TMP_FILE.getAbsolutePath())
                .redirectErrorStream(true)
                .start();
            int exitCode2 = p2.waitFor();
            if (exitCode2 != 0 || !TMP_FILE.exists()) return capturarAreaConRobot(area);

            BufferedImage img = ImageIO.read(TMP_FILE);
            TMP_FILE.delete();
            return img;
        } finally {
            xwdTmp.delete();
        }
    }

    // ── Backend 4: import (ImageMagick) con coordenadas ───
    private BufferedImage capturarAreaConImport(Rectangle area) throws Exception {
        String crop = area.width + "x" + area.height + "+" + area.x + "+" + area.y;
        Process p = new ProcessBuilder(
            "import", "-window", "root", "-crop", crop,
            TMP_FILE.getAbsolutePath())
            .redirectErrorStream(true)
            .start();
        int exitCode = p.waitFor();
        if (exitCode != 0 || !TMP_FILE.exists()) return capturarAreaConRobot(area);
        BufferedImage img = ImageIO.read(TMP_FILE);
        TMP_FILE.delete();
        return img;
    }

    // ── Backend 5: Robot con coordenadas ──────────────────
    public BufferedImage capturarAreaConRobot(Rectangle area) throws Exception {
        Robot robot = new Robot();
        Thread.sleep(300);
        robot.waitForIdle();

        // Detectar escala HiDPI
        GraphicsDevice gd = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getDefaultScreenDevice();
        double scaleX = gd.getDefaultConfiguration().getDefaultTransform().getScaleX();
        double scaleY = gd.getDefaultConfiguration().getDefaultTransform().getScaleY();

        Rectangle areaFisica = new Rectangle(
            (int)(area.x      * scaleX),
            (int)(area.y      * scaleY),
            (int)(area.width  * scaleX),
            (int)(area.height * scaleY));

        return robot.createScreenCapture(areaFisica);
    }

    // ── Conversión BufferedImage → JavaFX Image ───────────
    public Image toFXImage(BufferedImage bi) {
        return SwingFXUtils.toFXImage(bi, null);
    }

    // ── Portapapeles ──────────────────────────────────────
    public void copiarAlPortapapeles(BufferedImage imagen) {
        TransferableImage transferable = new TransferableImage(imagen);
        for (int i = 0; i < 5; i++) {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(transferable, null);
                return;
            } catch (IllegalStateException e) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        }
        System.err.println("[WARN] No se pudo copiar al portapapeles");
    }

    // ── Guardar archivo ───────────────────────────────────
    public File guardarImagen(BufferedImage imagen, CapturaModel modelo) throws IOException {
        File archivo = modelo.getArchivoDestino();
        String formato = modelo.getFormato();

        Files.createDirectories(archivo.getParentFile().toPath());

        BufferedImage imgAGuardar = imagen;
        if ("jpg".equals(formato)) {
            imgAGuardar = new BufferedImage(
                imagen.getWidth(), imagen.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = imgAGuardar.createGraphics();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, imagen.getWidth(), imagen.getHeight());
            g2.drawImage(imagen, 0, 0, null);
            g2.dispose();
        }

        boolean ok = ImageIO.write(imgAGuardar, formato, archivo);
        if (!ok) throw new IOException("Formato '" + formato + "' no soportado.");
        return archivo;
    }

    // ── Abrir archivo con visor del sistema ──────────────
    public void abrirArchivo(File archivo) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(archivo);
            } else {
                new ProcessBuilder("xdg-open", archivo.getAbsolutePath()).start();
            }
        } catch (IOException e) {
            System.err.println("No se pudo abrir el archivo: " + e.getMessage());
        }
    }

    // ── Verificar si imagen parece negra (VirtualBox) ────
    public boolean imagenPareceBlancoNegro(BufferedImage img) {
        int muestras = 20, oscuros = 0;
        int w = img.getWidth(), h = img.getHeight();
        for (int i = 0; i < muestras; i++) {
            int rgb = img.getRGB((int)(Math.random() * w), (int)(Math.random() * h));
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >>  8) & 0xFF;
            int b =  rgb        & 0xFF;
            if (r < 10 && g < 10 && b < 10) oscuros++;
        }
        return oscuros > muestras * 0.9;
    }

    // ── Captura con selector nativo de gnome-screenshot ───
    public BufferedImage capturarConGnomeSelector() throws Exception {
        Process p = new ProcessBuilder(
            "gnome-screenshot", "--area",
            "--file=" + TMP_FILE.getAbsolutePath())
            .redirectErrorStream(true)
            .start();
        int exitCode = p.waitFor();
        Thread.sleep(200);
        if (exitCode != 0 || !TMP_FILE.exists()) return null;
        BufferedImage img = ImageIO.read(TMP_FILE);
        TMP_FILE.delete();
        return img;
    }

    // ── Transferable interno ──────────────────────────────
    private static class TransferableImage implements Transferable {
        private final java.awt.Image imagen;
        TransferableImage(java.awt.Image img) { this.imagen = img; }

        @Override public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }
        @Override public boolean isDataFlavorSupported(DataFlavor f) {
            return DataFlavor.imageFlavor.equals(f);
        }
        @Override public Object getTransferData(DataFlavor f) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(f)) throw new UnsupportedFlavorException(f);
            return imagen;
        }
    }
}
