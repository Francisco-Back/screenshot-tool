package com.screenshottool.controller;

import com.screenshottool.model.CapturaModel;
import com.screenshottool.service.ScreenshotService;
import com.screenshottool.service.ToastService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * ScreenshotController — diálogo principal (main.fxml)
 *
 * Responsabilidades:
 * - Mostrar preview, nombre, formato, carpeta
 * - Guardar / copiar / cancelar
 * - Delegar el toast a ToastService
 *
 * El nodo raíz del FXML debe ser <StackPane fx:id="rootPane">
 * para que ToastService pueda inyectar el overlay.
 */
public class ScreenshotController implements Initializable {

    // ── FXML bindings ─────────────────────────────────────────
    @FXML
    private StackPane rootPane; // raíz = StackPane (requerido por ToastService)
    @FXML
    private ImageView imgPreview;
    @FXML
    private Label lblDimensiones;
    @FXML
    private TextField txtNombre;
    @FXML
    private ComboBox<String> cmbFormato;
    @FXML
    private Label lblCarpeta;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnCopiar;
    @FXML
    private Button btnCancelar;
    @FXML
    private Label lblEstado;

    // ── Dependencias ──────────────────────────────────────────
    private CapturaModel modelo;
    private ScreenshotService servicio;
    private BufferedImage imagenOriginal;
    private Stage stage;
    // private Runnable onCancelar;
    private ToastService toastService; // ← delegado para el toast

    // ── Initializable ─────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbFormato.getItems().addAll(CapturaModel.FORMATOS);
        cmbFormato.getSelectionModel().selectFirst();
        txtNombre.setOnAction(e -> onGuardar());
        txtNombre.textProperty().addListener((obs, old, val) -> {
            txtNombre.setStyle("");
            lblEstado.setText("");
        });
    }

    // ── Inyección desde ScreenshotApp ─────────────────────────
    public void init(CapturaModel modelo, ScreenshotService servicio,
            BufferedImage imagenOriginal, Stage stage, Runnable onCancelar) {
        this.modelo = modelo;
        this.servicio = servicio;
        this.imagenOriginal = imagenOriginal;
        this.stage = stage;
        // this.onCancelar = onCancelar;

        // ToastService se crea aquí, cuando ya tenemos rootPane + stage + servicio
        this.toastService = new ToastService(rootPane, stage, servicio);

        bindUI();
        autocopiarPortapapeles();
    }

    // ── Binding modelo ↔ UI ───────────────────────────────────
    private void bindUI() {
        imgPreview.setImage(modelo.getImagen());
        imgPreview.setPreserveRatio(true);
        imgPreview.setFitWidth(500);
        imgPreview.setFitHeight(260);

        lblDimensiones.setText(
                modelo.getAnchoReal() + " × " + modelo.getAltoReal()
                        + " px  —  Ya copiado al portapapeles");

        txtNombre.textProperty().bindBidirectional(modelo.nombreProperty());
        cmbFormato.valueProperty().bindBidirectional(modelo.formatoProperty());

        actualizarLblCarpeta();
        modelo.carpetaDestinoProperty().addListener(
                (obs, old, nueva) -> actualizarLblCarpeta());

        Platform.runLater(() -> {
            txtNombre.requestFocus();
            txtNombre.selectAll();
        });
        registrarAtajosTeclado();
    }

    private void actualizarLblCarpeta() {
        java.io.File carpeta = modelo.getCarpetaDestino();
        String nombre = carpeta.getName();
        if (nombre == null || nombre.isEmpty()) {
            nombre = carpeta.getAbsolutePath();
        }
        lblCarpeta.setText(nombre);
        lblCarpeta.setTooltip(new Tooltip(carpeta.getAbsolutePath()));
    }

    private void autocopiarPortapapeles() {
        javax.swing.SwingUtilities.invokeLater(
                () -> servicio.copiarAlPortapapeles(imagenOriginal));
    }

    // ── Acciones FXML ────────────────────────────────────────

    @FXML
    private void onGuardar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            txtNombre.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            txtNombre.requestFocus();
            return;
        }

        File archivo = modelo.getArchivoDestino();

        if (archivo.exists()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Archivo existente");
            confirm.setHeaderText(null);
            confirm.setContentText("'" + archivo.getName() + "' ya existe. ¿Reemplazar?");
            confirm.initOwner(stage);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                return;
        }

        try {
            File guardado = servicio.guardarImagen(imagenOriginal, modelo);
            mostrarEstado("Guardado: " + guardado.getName(), false);
            btnGuardar.setDisable(true);
            toastService.mostrar(guardado); // ← una línea, sin lógica de UI aquí
        } catch (IOException ex) {
            mostrarEstado("Error: " + ex.getMessage(), true);
        }
    }

    @FXML
    private void onCopiar() {
        new Thread(() -> {
            servicio.copiarAlPortapapeles(imagenOriginal);
            Platform.runLater(() -> toastService.mostrarMensaje("📋 Copiado al portapapeles"));
        }).start();
    }

    @FXML
    private void onCambiarCarpeta() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/screenshottool/fxml/folder-picker.fxml"));
            Parent root = loader.load();

            FolderPickerController picker = loader.getController();
            picker.init(modelo);

            Stage pickerStage = new Stage();
            pickerStage.setTitle("Elegir carpeta de destino");
            pickerStage.setResizable(false);
            pickerStage.initModality(Modality.WINDOW_MODAL);
            pickerStage.initOwner(stage); // hereda comportamiento del padre

            // ── Ícono: heredar los mismos íconos del stage principal ──
            pickerStage.getIcons().addAll(stage.getIcons());

            Scene scene = new Scene(root);
            String estiloActual = stage.getScene().getRoot().getStyle();
            if (estiloActual != null && estiloActual.contains("1a1a1a")) {
                root.setStyle("-fx-base: #1a1a1a; -fx-background: #2b2b2b;");
            }

            pickerStage.setScene(scene);

            // ── Centrar sobre la ventana principal, no en pantalla ────
            // Calcular posición ANTES de show() — el stage aún no tiene
            // tamaño, así que usamos showAndWait() con un listener de una sola vez.
            pickerStage.setOnShown(e -> {
                double cx = stage.getX() + (stage.getWidth() - pickerStage.getWidth()) / 2;
                double cy = stage.getY() + (stage.getHeight() - pickerStage.getHeight()) / 2;

                // Obtener bounds de la pantalla donde está el stage principal
                javafx.stage.Screen pantalla = javafx.stage.Screen
                        .getScreensForRectangle(stage.getX(), stage.getY(),
                                stage.getWidth(), stage.getHeight())
                        .stream().findFirst()
                        .orElse(javafx.stage.Screen.getPrimary());

                javafx.geometry.Rectangle2D bounds = pantalla.getVisualBounds();

                // Clamp para que no se salga de la pantalla
                cx = Math.max(bounds.getMinX(), Math.min(cx, bounds.getMaxX() - pickerStage.getWidth()));
                cy = Math.max(bounds.getMinY(), Math.min(cy, bounds.getMaxY() - pickerStage.getHeight()));

                pickerStage.setX(cx);
                pickerStage.setY(cy);
            });

            pickerStage.showAndWait();

        } catch (IOException ex) {
            mostrarEstado("No se pudo abrir el selector de carpeta", true);
        }
    }

    @FXML
    private void onCancelar() {
        // Cerrar solo la ventana — NO reiniciar captura ni llamar callback
        stage.close();
    }

    // ── Helpers ───────────────────────────────────────────────
    private void mostrarEstado(String texto, boolean esError) {
        lblEstado.setText(texto);
        lblEstado.setStyle(esError
                ? "-fx-text-fill: #e74c3c;"
                : "-fx-text-fill: derive(-fx-text-base-color, 30%);");
    }

    private void registrarAtajosTeclado() {
        Platform.runLater(() -> {
            Scene scene = rootPane.getScene();
            if (scene == null)
                return;

            // Ctrl+S → guardar
            scene.getAccelerators().put(
                    javafx.scene.input.KeyCombination.keyCombination("Ctrl+S"),
                    this::onGuardar);

            // ESC → cerrar diálogo
            scene.getAccelerators().put(
                    javafx.scene.input.KeyCombination.keyCombination("ESC"),
                    this::onCancelar);

            // Ctrl+C → copiar (solo si el foco NO está en el campo de texto)
            scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                if (e.isControlDown() && e.getCode() == javafx.scene.input.KeyCode.C
                        && !txtNombre.isFocused()) {
                    onCopiar();
                    e.consume();
                }
            });

            // Delete → limpiar nombre completo
            scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.DELETE
                        && !txtNombre.isFocused()) {
                    txtNombre.clear();
                    txtNombre.requestFocus();
                    e.consume();
                }
            });

            // F2 → poner foco en nombre para editar
            scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.F2) {
                    txtNombre.requestFocus();
                    txtNombre.selectAll();
                    e.consume();
                }
            });

        }); // ← cierre del Platform.runLater
    }
}
