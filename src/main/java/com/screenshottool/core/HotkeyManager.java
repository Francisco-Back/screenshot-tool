package com.screenshottool.core;

import javafx.application.Platform;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * HotkeyManager - Registra el atajo global Ctrl+Shift+S.
 *
 * Linux: el atajo lo maneja GNOME via gsettings (configurado por instalar.sh)
 * No se crea ninguna ventana oculta — nada aparece en el taskbar.
 *
 * Windows: usa una JFrame oculta con Timer de re-foco para mantener
 * el atajo activo mientras el usuario trabaja en otras apps.
 */
public class HotkeyManager {

    private final Runnable onCapturar;

    public HotkeyManager(Runnable onCapturar) {
        this.onCapturar = onCapturar;
    }

    // ── Inicializar atajo ─────────────────────────────────
    public void iniciar() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            // En Linux el atajo lo maneja GNOME via gsettings
            // El instalador configuró: touch /tmp/screenshottool.trigger
            // AppContext.iniciarMonitorTrigger() lo detecta y dispara la captura
            System.out.println("[HotkeyManager] Linux: hotkey managed by GNOME gsettings");
            return;
        }

        // Windows: registrar atajo via ventana oculta
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame ventanaOculta = crearVentanaOculta();
            registrarAtajo(ventanaOculta);
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

    // ── Registrar Ctrl+Shift+S ────────────────────────────
    private void registrarAtajo(javax.swing.JFrame frame) {
        javax.swing.KeyStroke atajo = javax.swing.KeyStroke.getKeyStroke(
                KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK);

        frame.getRootPane()
                .getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(atajo, "capturar");

        frame.getRootPane().getActionMap()
                .put("capturar", new javax.swing.AbstractAction() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        Platform.runLater(onCapturar);
                    }
                });
    }

    // ── Timer de re-foco (solo Windows) ───────────────────
    private void iniciarTimerFoco(javax.swing.JFrame frame) {
        javax.swing.Timer timer = new javax.swing.Timer(800, e -> {
            Window focusedWindow = KeyboardFocusManager
                    .getCurrentKeyboardFocusManager().getFocusedWindow();
            // Si no hay ninguna ventana con foco, tomar foco para recibir el atajo
            if (focusedWindow == null) {
                frame.toFront();
                frame.requestFocus();
            }
        });
        timer.start();
    }
}
