package com.screenshottool.core;

import javafx.application.Platform;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * TrayManager - Maneja el ícono y menú de la bandeja del sistema.
 *
 * Responsabilidades:
 *   - Cargar o generar el ícono de la bandeja
 *   - Crear el menú contextual (Capturar, Salir)
 *   - Agregar el ícono al SystemTray
 */
public class TrayManager {

    private final Runnable onCapturar;
    private TrayIcon trayIcon;

    public TrayManager(Runnable onCapturar) {
        this.onCapturar = onCapturar;
    }

    // ── Inicializar bandeja ───────────────────────────────
    public void iniciar() {
        if (!SystemTray.isSupported()) {
            System.err.println("[TrayManager] SystemTray no soportado en este sistema.");
            return;
        }
        try {
            SystemTray tray = SystemTray.getSystemTray();
            java.awt.Image icono = cargarIcono();

            trayIcon = new TrayIcon(icono, "Screenshot Tool");
            trayIcon.setImageAutoSize(true);
            trayIcon.setPopupMenu(crearMenu(tray));
            trayIcon.addActionListener(e -> Platform.runLater(onCapturar));
            tray.add(trayIcon);

            System.out.println("[Screenshot Tool] Activo. Ctrl+Alt+S para capturar.");
        } catch (AWTException e) {
            System.err.println("[TrayManager] No se pudo agregar a la bandeja: " + e.getMessage());
        }
    }

    // ── Menú contextual ───────────────────────────────────
    private PopupMenu crearMenu(SystemTray tray) {
        PopupMenu menu = new PopupMenu();

        MenuItem capturar = new MenuItem("Capturar area (Ctrl+Alt+S)");
        capturar.addActionListener(e -> Platform.runLater(onCapturar));

        MenuItem salir = new MenuItem("Salir");
        salir.addActionListener(e -> {
            tray.remove(trayIcon);
            Platform.exit();
            System.exit(0);
        });

        menu.add(capturar);
        menu.addSeparator();
        menu.add(salir);
        return menu;
    }

    // ── Cargar ícono ──────────────────────────────────────
    private java.awt.Image cargarIcono() {
        try {
            URL url = getClass().getResource("/com/screenshottool/img/captura-de-pantalla.png");
            if (url != null) return ImageIO.read(url);
        } catch (IOException ignored) {}
        return generarIcono();
    }

    // ── Generar ícono programáticamente (fallback) ────────
    private java.awt.Image generarIcono() {
        int sz = 64;
        BufferedImage img = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(30, 144, 255));
        g.fillRoundRect(0, 0, sz, sz, 14, 14);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawRoundRect(8, 16, 48, 34, 8, 8);
        g.fillRoundRect(22, 11, 14, 8, 4, 4);
        g.fillRoundRect(10, 18, 8, 6, 2, 2);
        g.drawOval(20, 20, 24, 24);
        g.setColor(new Color(30, 144, 255, 180));
        g.fillOval(23, 23, 18, 18);
        g.setColor(new Color(255, 255, 255, 120));
        g.fillOval(25, 25, 6, 6);
        g.dispose();
        return img;
    }
}
