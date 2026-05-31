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
 * Triggers:
 * /tmp/screenshottool.trigger        → captura de área (selector)
 * /tmp/screenshottool.trigger.window → captura de monitor activo
 *
 * Ciclo de vida:
 * App inicia → bandeja → monitor trigger activo → espera
 * Trigger detectado → captura → diálogo → cierra diálogo → vuelve a esperar
 * La JVM nunca cierra hasta que el usuario elige "Salir" en la bandeja
 */
public class AppContext {

    private final ScreenshotService servicio;
    private final TrayManager trayManager;
    private final HotkeyManager hotkeyManager;
    private Stage dialogoActual = null;
    private boolean dialogoCargando = false;

    private static final File TRIGGER_AREA   = new File("/tmp/screenshottool.trigger");
    private static final File TRIGGER_WINDOW = new File("/tmp/screenshottool.trigger.window");

    public AppContext() {
        this.servicio      = new ScreenshotService();
        this.trayManager   = new TrayManager(this::iniciarCaptura, this::iniciarCapturaVentana);
        this.hotkeyManager = new HotkeyManager(this::iniciarCaptura, this::iniciarCapturaVentana);
    }

    // ── Iniciar la aplicación ─────────────────────────────
    public void iniciar() {
        if (SystemTray.isSupported()) {
            trayManager.iniciar();
        } else {
            iniciarCaptura();
        }
        hotkeyManager.iniciar();
        iniciarMonitorTrigger();
    }

    // ── Captura de área (selector manual) ─────────────────
    // Linux: gnome-screenshot con selector nativo
    // Windows: SelectorDeAreaBridge con overlay Swing
    public void iniciarCaptura() {
        System.out.println("[AppContext] iniciarCaptura() - área");
        cerrarDialogoActual();

        new Thread(() -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("linux")) {
                    BufferedImage imagen = servicio.capturarConGnomeSelector();
                    if (imagen == null) return;
                    Platform.runLater(() -> mostrarDialogo(imagen));
                } else {
                    Platform.runLater(this::iniciarCapturaConSelector);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ── Captura de monitor activo ─────────────────────────
    // Linux: detecta monitor donde está el cursor y captura con scrot/import
    // Windows: SelectorVentanaBridge con overlay Swing
    public void iniciarCapturaVentana() {
        System.out.println("[AppContext] iniciarCapturaVentana() - monitor activo");
        cerrarDialogoActual();

        new Thread(() -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("linux")) {
                    BufferedImage imagen = servicio.capturarMonitorActivoLinux();
                    if (imagen == null) return;
                    Platform.runLater(() -> mostrarDialogo(imagen));
                } else {
                    // Windows: selector visual con overlay
                    Robot robot = new Robot();
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    Rectangle boundsTotal = new Rectangle();
                    for (GraphicsDevice gd : ge.getScreenDevices())
                        boundsTotal = boundsTotal.union(gd.getDefaultConfiguration().getBounds());
                    BufferedImage fondo = robot.createScreenCapture(boundsTotal);
                    final BufferedImage f = fondo;
                    final Rectangle b = boundsTotal;
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        try {
                            new SelectorVentanaBridge(AppContext.this, f, b);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ── Cerrar diálogo previo si existe ───────────────────
    private void cerrarDialogoActual() {
        if (dialogoActual != null && dialogoActual.isShowing()) {
            dialogoActual.close();
        }
        dialogoActual = null;
    }

    // ── Selector de área Swing (Windows) ──────────────────
    private void iniciarCapturaConSelector() {
        try {
            new SelectorDeAreaBridge(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Llamado desde SelectorDeAreaBridge / SelectorVentanaBridge
    void mostrarDialogoConArea(Rectangle area) {
        new Thread(() -> {
            try {
                BufferedImage imagen = servicio.capturarAreaConBackend(area);
                if (imagen == null) return;
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

    // ── Stage con ícono pre-cargado ───────────────────────
    private Stage crearStageListo() {
        Stage stage = new Stage();
        stage.setTitle("Captura de pantalla");
        stage.setResizable(false);
        stage.initStyle(StageStyle.DECORATED);

        try {
            for (String path : new String[]{
                    "/com/screenshottool/img/icon-128.png",
                    "/com/screenshottool/img/icon-48.png",
                    "/com/screenshottool/img/icon-32.png",
                    "/com/screenshottool/img/icon-16.png"}) {
                URL iconUrl = getClass().getResource(path);
                if (iconUrl != null)
                    stage.getIcons().add(new javafx.scene.image.Image(iconUrl.toExternalForm()));
            }
        } catch (Exception ignored) {}

        return stage;
    }

    // ── Mostrar diálogo de guardado ───────────────────────
    private void mostrarDialogo(BufferedImage imagenAWT) {
        mostrarDialogo(imagenAWT, null);
    }

    private void mostrarDialogo(BufferedImage imagenAWT, Stage stageExistente) {
        if (dialogoCargando) return;
        dialogoCargando = true;
        try {
            servicio.copiarAlPortapapeles(imagenAWT);

            CapturaModel modelo = new CapturaModel();
            modelo.setImagen(SwingFXUtils.toFXImage(imagenAWT, null));
            modelo.setAnchoReal(imagenAWT.getWidth());
            modelo.setAltoReal(imagenAWT.getHeight());

            URL fxmlUrl = getClass().getResource("/com/screenshottool/fxml/main.fxml");
            if (fxmlUrl == null) {
                System.err.println("[ERROR] No se encontró main.fxml en el classpath");
                dialogoCargando = false;
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            ScreenshotController controller = loader.getController();
            Stage stage = (stageExistente != null) ? stageExistente : crearStageListo();

            Scene scene = new Scene(root);
            if (isDarkMode()) {
                root.setStyle("-fx-base: #1a1a1a; -fx-background: #2b2b2b;");
            }
            stage.setScene(scene);

            controller.init(modelo, servicio, imagenAWT, stage, this::iniciarCaptura);

            dialogoCargando = false;
            dialogoActual = stage;
            stage.show();

            stage.setOnCloseRequest(e -> {
                dialogoActual = null;
                hotkeyManager.recuperarFoco();
            });
            stage.setOnHidden(e -> {
                dialogoActual = null;
                dialogoCargando = false;
                hotkeyManager.recuperarFoco();
            });

        } catch (IOException e) {
            dialogoCargando = false;
            e.printStackTrace();
            mostrarError("Error al abrir el diálogo: " + e.getMessage());
        }
    }

    // ── Monitor de triggers ───────────────────────────────
    private void iniciarMonitorTrigger() {
        Thread monitor = new Thread(() -> {
            while (true) {
                try {
                    if (TRIGGER_WINDOW.exists()) {
                        TRIGGER_WINDOW.delete();
                        Platform.runLater(this::iniciarCapturaVentana);
                    } else if (TRIGGER_AREA.exists()) {
                        TRIGGER_AREA.delete();
                        Platform.runLater(this::iniciarCaptura);
                    }
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {}
            }
        });
        monitor.setDaemon(true);
        monitor.setName("trigger-monitor");
        monitor.start();
    }

    // ── Detectar tema oscuro ──────────────────────────────
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
                ProcessBuilder pb = new ProcessBuilder(
                        "gsettings", "get",
                        "org.gnome.desktop.interface", "color-scheme");
                Process p = pb.start();
                String output = new String(p.getInputStream().readAllBytes()).trim();
                if (output.contains("prefer-dark")) return true;
                String gtkTheme = System.getenv("GTK_THEME");
                if (gtkTheme != null && gtkTheme.toLowerCase().contains("dark")) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    // ── Mostrar error ─────────────────────────────────────
    private void mostrarError(String mensaje) {
        javafx.scene.control.Alert alert =
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
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
