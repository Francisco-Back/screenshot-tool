package com.screenshottool.controller;

import com.screenshottool.model.CapturaModel;
import com.screenshottool.service.ScreenshotService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * ScreenshotController - Controlador del diálogo principal (main.fxml).
 *
 * Recibe el modelo y el servicio desde ScreenshotApp después de capturar.
 * Maneja: preview, nombre, formato, carpeta, guardar, copiar, cancelar.
 */
public class ScreenshotController implements Initializable {

    // ── FXML bindings ─────────────────────────────────────
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

    // ── Dependencias ──────────────────────────────────────
    private CapturaModel modelo;
    private ScreenshotService servicio;
    private BufferedImage imagenOriginal; // buffer AWT para guardar/copiar
    private Stage stage;

    // Callback que se llama al cancelar (para volver a capturar)
    private Runnable onCancelar;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Configurar ComboBox de formatos
        cmbFormato.getItems().addAll(CapturaModel.FORMATOS);
        cmbFormato.getSelectionModel().selectFirst();

        // Enter en el campo nombre dispara guardar
        txtNombre.setOnAction(e -> onGuardar());

        // Limpiar borde de error al escribir
        txtNombre.textProperty().addListener((obs, old, val) -> {
            txtNombre.setStyle("");
            lblEstado.setText("");
        });
    }

    // ── Inyección de dependencias desde ScreenshotApp ────
    /**
     * Llamado por ScreenshotApp tras capturar.
     * 
     * @param modelo         modelo con nombre/formato/carpeta
     * @param servicio       servicio para guardar/copiar
     * @param imagenOriginal BufferedImage para guardar/portapapeles
     * @param stage          stage de esta ventana
     * @param onCancelar     callback al presionar Cancelar
     */
    public void init(CapturaModel modelo, ScreenshotService servicio,
            BufferedImage imagenOriginal, Stage stage, Runnable onCancelar) {
        this.modelo = modelo;
        this.servicio = servicio;
        this.imagenOriginal = imagenOriginal;
        this.stage = stage;
        this.onCancelar = onCancelar;

        bindUI();
        autocopiarPortapapeles();
    }

    // ── Binding modelo ↔ UI ───────────────────────────────
    private void bindUI() {
        // Preview
        imgPreview.setImage(modelo.getImagen());
        imgPreview.setPreserveRatio(true);
        imgPreview.setFitWidth(500);
        imgPreview.setFitHeight(260);

        // Dimensiones
        lblDimensiones.setText(
                modelo.getAnchoReal() + " × " + modelo.getAltoReal() + " px  —  Ya copiado al portapapeles");

        // Nombre (binding bidireccional)
        txtNombre.textProperty().bindBidirectional(modelo.nombreProperty());

        // Formato (binding bidireccional)
        cmbFormato.valueProperty().bindBidirectional(modelo.formatoProperty());

        // Carpeta (solo lectura, se actualiza desde FolderPickerController)
        actualizarLblCarpeta();
        modelo.carpetaDestinoProperty().addListener(
                (obs, old, nueva) -> actualizarLblCarpeta());

        // Seleccionar todo el nombre al abrir
        Platform.runLater(() -> {
            txtNombre.requestFocus();
            txtNombre.selectAll();
        });
    }

    private void actualizarLblCarpeta() {
        lblCarpeta.setText(truncarRuta(modelo.getCarpetaDestino().getAbsolutePath(), 45));
    }

    // ── Auto-copiar al portapapeles al abrir ─────────────
    private void autocopiarPortapapeles() {
        javax.swing.SwingUtilities.invokeLater(() -> servicio.copiarAlPortapapeles(imagenOriginal));
    }

    // ── Acción: Guardar ───────────────────────────────────
    @FXML
    private void onGuardar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            txtNombre.setStyle("-fx-border-color: red; -fx-border-width: 2;");
            txtNombre.requestFocus();
            return;
        }

        File archivo = modelo.getArchivoDestino();

        // Confirmar sobreescritura
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
            mostrarEstado("✅ Guardado: " + guardado.getName(), false);
            btnGuardar.setDisable(true);

            // Toast no bloqueante que se cierra solo
            mostrarToast(guardado);

        } catch (IOException ex) {
            mostrarEstado("❌ Error: " + ex.getMessage(), true);
        }
    }

    // ── Acción: Copiar al portapapeles ────────────────────
    @FXML
    private void onCopiar() {
        new Thread(() -> {
            servicio.copiarAlPortapapeles(imagenOriginal);
            Platform.runLater(() -> mostrarEstado("📋 Copiado al portapapeles", false));
        }).start();
    }

    // ── Acción: Cambiar carpeta → abre folder-picker.fxml ─
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
            pickerStage.initModality(Modality.WINDOW_MODAL);
            pickerStage.initOwner(stage);

            Scene scene = new Scene(root);
            // Heredar tema oscuro del diálogo principal
            String estiloActual = stage.getScene().getRoot().getStyle();
            if (estiloActual != null && estiloActual.contains("1a1a1a")) {
                root.setStyle("-fx-base: #1a1a1a; -fx-background: #2b2b2b;");
            }
            pickerStage.setScene(scene);
            pickerStage.setResizable(false);
            pickerStage.showAndWait();

        } catch (IOException e) {
            mostrarEstado("❌ No se pudo abrir el selector de carpeta", true);
            e.printStackTrace();
        }
    }

    // ── Acción: Cancelar → volver a capturar ─────────────
    @FXML
    private void onCancelar() {
        stage.close();
        if (onCancelar != null) {
            // Pequeña pausa para que el diálogo cierre antes de abrir el selector
            new Thread(() -> {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ignored) {
                }
                Platform.runLater(onCancelar);
            }).start();
        }
    }

    // ── Helpers ───────────────────────────────────────────
    private void mostrarEstado(String msg, boolean esError) {
        lblEstado.setText(msg);
        lblEstado.setStyle(esError
                ? "-fx-text-fill: #e74c3c;"
                : "-fx-text-fill: #27ae60;");
    }

    private String truncarRuta(String ruta, int maxChars) {
        if (ruta.length() <= maxChars)
            return ruta;
        return "…" + ruta.substring(ruta.length() - maxChars + 1);
    }

    private void mostrarToast(File archivo) {
        Stage toast = new Stage();
        toast.initOwner(stage);
        toast.initModality(Modality.NONE);
        toast.initStyle(StageStyle.UNDECORATED);
        toast.setAlwaysOnTop(true);

        Label lbl = new Label("✅ Guardado: " + archivo.getName());
        lbl.getStyleClass().add("toast-label");

        Button btnAbrir = new Button("Abrir archivo");
        btnAbrir.getStyleClass().add("btn-abrir");
        btnAbrir.setOnAction(e -> {
            toast.close();
            servicio.abrirArchivo(archivo);
        });

        Button btnCerrar = new Button("✕");
        btnCerrar.getStyleClass().add("btn-toast-cerrar");
        btnCerrar.setOnAction(e -> toast.close());

        HBox botones = new HBox(8, btnAbrir, btnCerrar);
        botones.setAlignment(javafx.geometry.Pos.CENTER);
        botones.setPadding(new javafx.geometry.Insets(0, 12, 10, 12));

        VBox layout = new VBox(4, lbl, botones);
        layout.getStyleClass().add("toast-container");
        layout.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(layout);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(
                getClass().getResource("/com/screenshottool/css/style.css").toExternalForm());
        toast.setScene(scene);
        toast.show();

        // Centrar en pantalla
        // Centrar en la misma pantalla donde está el diálogo principal
        javafx.stage.Screen pantalla = javafx.stage.Screen.getScreensForRectangle(
                stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())
                .stream().findFirst()
                .orElse(javafx.stage.Screen.getPrimary());

        javafx.geometry.Rectangle2D bounds = pantalla.getVisualBounds();
        toast.setX(bounds.getMinX() + (bounds.getWidth() - toast.getWidth()) / 2);
        toast.setY(bounds.getMinY() + (bounds.getHeight() - toast.getHeight()) / 2);

        // Flag para evitar doble cierre
        final boolean[] cerrado = { false };

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4));
        pause.setOnFinished(e -> {
            if (!cerrado[0]) {
                cerrado[0] = true;
                if (toast.isShowing())
                    toast.close();
                if (stage.isShowing())
                    stage.close();
            }
        });
        pause.play();

        toast.setOnHidden(e -> {
            if (!cerrado[0]) {
                cerrado[0] = true;
                pause.stop();
                if (stage.isShowing())
                    stage.close();
            }
        });
    }
}
