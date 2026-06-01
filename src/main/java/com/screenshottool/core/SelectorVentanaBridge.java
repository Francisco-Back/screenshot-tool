package com.screenshottool.core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;

/**
 * SelectorVentanaBridge - Selector de monitor activo.
 *
 * Recibe el fondo ya capturado desde AppContext (hilo secundario).
 * El constructor NO hace captura ni Thread.sleep — es puro UI.
 *
 * Flujo:
 *   AppContext captura escritorio en hilo secundario
 *   → pasa BufferedImage + bounds al constructor
 *   → constructor solo configura UI y muestra el selector
 *   → usuario hace clic en monitor → captura ese monitor
 */
class SelectorVentanaBridge extends JFrame {

    private final AppContext app;
    private final Rectangle boundsTotal;
    private final BufferedImage fondoPantalla;
    private final GraphicsDevice[] pantallas;

    SelectorVentanaBridge(AppContext app, BufferedImage fondoPantalla, Rectangle boundsTotal) throws Exception {
        this.app           = app;
        this.fondoPantalla = fondoPantalla;
        this.boundsTotal   = boundsTotal;

        // Detectar pantallas
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        this.pantallas = ge.getScreenDevices();

        setUndecorated(true);
        setAlwaysOnTop(true);
        setBounds(boundsTotal);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        // ── Panel de dibujo ───────────────────────────────────
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                // Fondo: captura real oscurecida
                g2.drawImage(fondoPantalla, 0, 0, null);
                g2.setColor(new Color(0, 0, 0, 100));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Resaltar monitor bajo el cursor
                Point cursorPos = MouseInfo.getPointerInfo().getLocation();
                Rectangle monitorActivo = getMonitorBajoCursor(cursorPos);

                if (monitorActivo != null) {
                    int rx = monitorActivo.x - boundsTotal.x;
                    int ry = monitorActivo.y - boundsTotal.y;
                    int rw = monitorActivo.width;
                    int rh = monitorActivo.height;

                    // Mostrar monitor sin oscurecer
                    g2.drawImage(fondoPantalla,
                            rx, ry, rx + rw, ry + rh,
                            rx, ry, rx + rw, ry + rh, null);

                    // Borde azul
                    g2.setColor(new Color(30, 144, 255));
                    g2.setStroke(new BasicStroke(4));
                    g2.drawRect(rx + 2, ry + 2, rw - 4, rh - 4);

                    // Esquinas decorativas
                    int cs = 20;
                    g2.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND));
                    g2.drawLine(rx, ry, rx + cs, ry);
                    g2.drawLine(rx, ry, rx, ry + cs);
                    g2.drawLine(rx + rw - cs, ry, rx + rw, ry);
                    g2.drawLine(rx + rw, ry, rx + rw, ry + cs);
                    g2.drawLine(rx, ry + rh - cs, rx, ry + rh);
                    g2.drawLine(rx, ry + rh, rx + cs, ry + rh);
                    g2.drawLine(rx + rw, ry + rh - cs, rx + rw, ry + rh);
                    g2.drawLine(rx + rw - cs, ry + rh, rx + rw, ry + rh);

                    // Etiqueta centrada
                    String label = rw + " × " + rh + " px  —  Clic para capturar";
                    g2.setFont(new Font("SansSerif", Font.BOLD, 16));
                    FontMetrics fm = g2.getFontMetrics();
                    int lw = fm.stringWidth(label) + 24;
                    int lh = 34;
                    int lx = rx + (rw - lw) / 2;
                    int ly = ry + (rh - lh) / 2;
                    g2.setColor(new Color(30, 144, 255, 220));
                    g2.fillRoundRect(lx, ly, lw, lh, 10, 10);
                    g2.setColor(Color.WHITE);
                    g2.drawString(label, lx + 12, ly + 23);
                }

                // Barra inferior ESC
                String esc = "ESC para cancelar";
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int ew = fm.stringWidth(esc) + 20;
                int ex = (getWidth() - ew) / 2;
                g2.setColor(new Color(0, 0, 0, 170));
                g2.fillRoundRect(ex, getHeight() - 46, ew, 30, 10, 10);
                g2.setColor(Color.WHITE);
                g2.drawString(esc, ex + 10, getHeight() - 26);
            }
        };

        panel.setOpaque(false);
        add(panel);
        panel.setFocusable(true);

        // ── Repintar al mover mouse ───────────────────────────
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                panel.repaint();
            }
        });

        // ── Clic → capturar monitor ───────────────────────────
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point cursorPantalla = new Point(
                        boundsTotal.x + e.getX(),
                        boundsTotal.y + e.getY());
                Rectangle monitor = getMonitorBajoCursor(cursorPantalla);
                if (monitor != null) {
                    cerrar();
                    app.mostrarDialogoConArea(monitor);
                }
            }
        });

        // ── ESC cancela ───────────────────────────────────────
        KeyAdapter escAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) cerrar();
            }
        };
        addKeyListener(escAdapter);
        panel.addKeyListener(escAdapter);

        // ── Mostrar ───────────────────────────────────────────
        setFocusableWindowState(true);
        setVisible(true);
        SwingUtilities.invokeLater(() -> panel.requestFocusInWindow());
    }

    // ── Detectar monitor bajo el cursor ──────────────────────
    private Rectangle getMonitorBajoCursor(Point cursor) {
        for (GraphicsDevice gd : pantallas) {
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            if (bounds.contains(cursor)) return bounds;
        }
        return null;
    }

    // ── Método estático requerido por AppContext ───────────────
    static List<Rectangle> detectarVentanasEstatico() {
        return new ArrayList<>();
    }

    // ── Cerrar limpiamente ────────────────────────────────────
    private void cerrar() {
        setFocusableWindowState(false);
        dispose();
    }
}
