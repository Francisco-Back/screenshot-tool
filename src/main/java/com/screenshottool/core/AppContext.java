package com.screenshottool.core;

import com.screenshottool.controller.ScreenshotController;
import com.screenshottool.model.CapturaModel;
import com.screenshottool.service.ScreenshotService;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.io.File;

/**
 * AppContext - Orquesta el flujo principal de la aplicación.
 *
 * Responsabilidades:
 * - Inicializar el servicio, bandeja y atajo global
 * - Gestionar el ciclo: capturar → mostrar diálogo → volver a capturar
 * - Detectar el tema del sistema (claro/oscuro)
 * - Mostrar errores
 *
 * No extiende Application — es instanciado por ScreenshotApp.
 */
public class AppContext {

    private final ScreenshotService servicio;
    private final TrayManager trayManager;
    private final HotkeyManager hotkeyManager;
    private Stage dialogoActual = null;
    private boolean dialogoCargando = false;

    public AppContext() {
        this.servicio = new ScreenshotService();
        this.trayManager = new TrayManager(this::iniciarCaptura);
        this.hotkeyManager = new HotkeyManager(this::iniciarCaptura);
    }

    // ── Iniciar la aplicación ─────────────────────────────
    public void iniciar() {
        if (SystemTray.isSupported()) {
            trayManager.iniciar();
        } else {
            // Sin bandeja: capturar directamente al iniciar
            iniciarCaptura();
        }
        hotkeyManager.iniciar();
        iniciarMonitorTrigger();
    }

    /// ── Flujo principal de captura ────────────────────────
    public void iniciarCaptura() {
        // Cerrar diálogo previo si existe
        if (dialogoActual != null && dialogoActual.isShowing()) {
            dialogoActual.close();
        }
        dialogoActual = null; // ← siempre limpiar

        new Thread(() -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("linux")) {
                    // Linux: usar gnome-screenshot con su selector nativo
                    BufferedImage imagen = servicio.capturarConGnomeSelector();
                    if (imagen == null)
                        return;
                    Platform.runLater(() -> mostrarDialogo(imagen));
                } else {
                    // Windows: usar nuestro selector Java
                    Platform.runLater(this::iniciarCapturaConSelector);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ── Abrir selector de área (todos los backends) ───────
    private void iniciarCapturaConSelector() {
        try {
            new SelectorDeAreaBridge(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Llamado desde SelectorDeAreaBridge con el área seleccionada
    void mostrarDialogoConArea(Rectangle area) {
        new Thread(() -> {
            try {
                BufferedImage imagen = servicio.capturarAreaConBackend(area);
                if (imagen == null)
                    return;
                if (servicio.imagenPareceBlancoNegro(imagen)) {
                    System.err.println("[ADVERTENCIA] Captura en negro.");
                }
                Platform.runLater(() -> mostrarDialogo(imagen));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> mostrarError("Error al capturar: " + e.getMessage()));
            }
        }).start();
    }

    // ── Mostrar diálogo de guardado ───────────────────────
    private void mostrarDialogo(BufferedImage imagenAWT) {
        // Guard: evitar doble carga simultánea del FXML (causa "Duplicate fx:id")
        if (dialogoCargando) return;
        dialogoCargando = true;
        try {
            servicio.copiarAlPortapapeles(imagenAWT);

            // Preparar modelo
            CapturaModel modelo = new CapturaModel();
            modelo.setImagen(SwingFXUtils.toFXImage(imagenAWT, null));
            modelo.setAnchoReal(imagenAWT.getWidth());
            modelo.setAltoReal(imagenAWT.getHeight());

            // Cargar FXML
            URL fxmlUrl = getClass().getResource("/com/screenshottool/fxml/main.fxml");
            if (fxmlUrl == null) {
                System.err.println("[ERROR] No se encontró main.fxml en el classpath");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            ScreenshotController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Captura de pantalla");
            stage.setResizable(false);
            stage.initStyle(StageStyle.DECORATED);

            // Ícono de la ventana
            try {
                URL iconUrl = getClass().getResource("/com/screenshottool/img/captura-de-pantalla.png");
                if (iconUrl != null) {
                    stage.getIcons().add(new javafx.scene.image.Image(iconUrl.toExternalForm()));
                }
            } catch (Exception ignored) {
            }

            // Tema claro/oscuro
            Scene scene = new Scene(root);
            if (isDarkMode()) {
                root.setStyle("-fx-base: #1a1a1a; -fx-background: #2b2b2b;");
            }
            stage.setScene(scene);
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("win")) {
                stage.setAlwaysOnTop(true);
            }

            // Inyectar dependencias en el controlador
            controller.init(
                    modelo,
                    servicio,
                    imagenAWT,
                    stage,
                    this::iniciarCaptura // callback al presionar Cancelar
            );

            dialogoCargando = false; // liberar — diálogo ya en pantalla
            dialogoActual = stage;
            stage.show();
            //stage.centerOnScreen();

            // Limpiar dialogoActual al cerrar con el botón X
            stage.setOnCloseRequest(e -> {
                dialogoActual = null;
                // Notificar al HotkeyManager para recuperar el foco
                hotkeyManager.recuperarFoco();
            });

            // También al cerrar normalmente via código
            stage.setOnHidden(e -> {
                dialogoActual = null;
                dialogoCargando = false;
                hotkeyManager.recuperarFoco();
            });

        dialogoCargando = false; // liberar en error
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error al abrir el diálogo: " + e.getMessage());
        }
    }

    // ── Detectar tema oscuro del sistema ──────────────────
    private boolean isDarkMode() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                ProcessBuilder pb = new ProcessBuilder(
                        "reg", "query",
                        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "/v", "AppsUseLightTheme");
                Process p = pb.start();
                String output = new String(p.getInputStream().readAllBytes());
                return output.contains("0x0");
            }
            if (os.contains("linux")) {
                // Primero intentar gsettings (GNOME)
                ProcessBuilder pb = new ProcessBuilder(
                        "gsettings", "get",
                        "org.gnome.desktop.interface", "color-scheme");
                Process p = pb.start();
                String output = new String(p.getInputStream().readAllBytes()).trim();
                if (output.contains("prefer-dark"))
                    return true;

                // Fallback: variable GTK_THEME
                String gtkTheme = System.getenv("GTK_THEME");
                if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark"))
                    return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    // ── Monitor de trigger para comando 'screenshot' ──────
    // Vigila /tmp/screenshottool.trigger cada 300ms
    // Cuando existe lo elimina y dispara la captura
    // setDaemon(true) garantiza que muere cuando la app cierra
    private void iniciarMonitorTrigger() {
        File trigger = new File("/tmp/screenshottool.trigger");
        Thread monitor = new Thread(() -> {
            while (true) {
                try {
                    if (trigger.exists()) {
                        trigger.delete();
                        Platform.runLater(this::iniciarCaptura);
                    }
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {
                }
            }
        });
        monitor.setDaemon(true);
        monitor.setName("trigger-monitor");
        monitor.start();
    }

    // ── Mostrar error ─────────────────────────────────────
    private void mostrarError(String mensaje) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Screenshot Tool");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.initOwner(null);
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            ((javafx.stage.Stage) alert.getDialogPane().getScene().getWindow())
                    .setAlwaysOnTop(true);
        }
        alert.show();
    }


}
