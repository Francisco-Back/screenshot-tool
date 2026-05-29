package com.screenshottool.core;

import javafx.application.Platform;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * HotkeyManager - Registra atajos globales.
 *
 * Linux: atajos manejados por GNOME via gsettings (postinst los configura)
 *        No se crea ninguna ventana oculta.
 *
 * Windows: ventana oculta con Timer de re-foco.
 *   Ctrl+Alt+S → captura de área
 *   Ctrl+Alt+W → captura de ventana activa
 */
public class HotkeyManager {

    private final Runnable onCapturarArea;
    private final Runnable onCapturarVentana;
    private javax.swing.JFrame ventanaOculta;

    public HotkeyManager(Runnable onCapturarArea, Runnable onCapturarVentana) {
        this.onCapturarArea    = onCapturarArea;
        this.onCapturarVentana = onCapturarVentana;
    }

    // ── Inicializar ───────────────────────────────────────
    public void iniciar() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            System.out.println("[HotkeyManager] Linux: hotkeys managed by GNOME gsettings");
            return;
        }

        // Windows: ventana oculta con dos atajos
        javax.swing.SwingUtilities.invokeLater(() -> {
            ventanaOculta = crearVentanaOculta();
            registrarAtajos(ventanaOculta);
            iniciarTimerFoco(ventanaOculta);
        });
    }

    // ── Ventana oculta (solo Windows) ─────────────────────
    private javax.swing.JFrame crearVentanaOculta() {
        javax.swing.JFrame frame = new javax.swing.JFrame();
        frame.setSize(1, 1);
        frame.setLocation(-200, -200);
        frame.setUndecorated(true);
        frame.setType(Window.Type.UTILITY);
        frame.setAutoRequestFocus(true);
        frame.setFocusableWindowState(true);
        frame.setVisible(true);
        return frame;
    }

    // ── Registrar Ctrl+Alt+S y Ctrl+Alt+W ────────────────
    private void registrarAtajos(javax.swing.JFrame frame) {
        // Ctrl+Alt+S → captura de área
        javax.swing.KeyStroke atajoArea = javax.swing.KeyStroke.getKeyStroke(
                KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);
        frame.getRootPane()
                .getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(atajoArea, "capturarArea");
        frame.getRootPane().getActionMap()
                .put("capturarArea", new javax.swing.AbstractAction() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        Platform.runLater(onCapturarArea);
                    }
                });

        // Ctrl+Alt+W → captura de ventana activa
        javax.swing.KeyStroke atajoVentana = javax.swing.KeyStroke.getKeyStroke(
                KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);
        frame.getRootPane()
                .getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(atajoVentana, "capturarVentana");
        frame.getRootPane().getActionMap()
                .put("capturarVentana", new javax.swing.AbstractAction() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        Platform.runLater(onCapturarVentana);
                    }
                });
    }

    // ── Timer de re-foco (solo Windows) ───────────────────
    private void iniciarTimerFoco(javax.swing.JFrame frame) {
        javax.swing.Timer timer = new javax.swing.Timer(800, e -> {
            Window focusedWindow = KeyboardFocusManager
                    .getCurrentKeyboardFocusManager().getFocusedWindow();
            if (focusedWindow == null && !hayDialogoActivo()) {
                frame.toFront();
                frame.requestFocus();
            }
        });
        timer.start();
    }

    private boolean hayDialogoActivo() {
        return javafx.stage.Window.getWindows().stream()
                .anyMatch(w -> w.isShowing() && w instanceof javafx.stage.Stage);
    }

    public void recuperarFoco() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win") && ventanaOculta != null) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                ventanaOculta.toFront();
                ventanaOculta.requestFocus();
            });
        }
    }
}
