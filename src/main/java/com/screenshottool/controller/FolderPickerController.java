package com.screenshottool.controller;

import com.screenshottool.model.CapturaModel;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * FolderPickerController - Controlador de folder-picker.fxml.
 *
 * Abre una ventana modal donde el usuario puede:
 *   - Ver la carpeta actual
 *   - Navegar con DirectoryChooser para elegir una nueva
 *   - Confirmar o cancelar
 *
 * Al confirmar actualiza modelo.carpetaDestino, lo que automáticamente
 * actualiza el lblCarpeta en ScreenshotController via binding.
 */
public class FolderPickerController implements Initializable {

    // ── FXML bindings ─────────────────────────────────────
    @FXML private Label  lblCarpetaActual;
    @FXML private Button btnExaminar;
    @FXML private Button btnConfirmar;
    @FXML private Button btnCancelar;

    // ── Estado interno ────────────────────────────────────
    private CapturaModel modelo;
    private File carpetaSeleccionada; // puede diferir del modelo hasta confirmar

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // nada que inicializar sin modelo
    }

    // ── Inyección desde ScreenshotController ─────────────
    public void init(CapturaModel modelo) {
        this.modelo             = modelo;
        this.carpetaSeleccionada = modelo.getCarpetaDestino();
        lblCarpetaActual.setText(carpetaSeleccionada.getAbsolutePath());
    }

    // ── Acción: Examinar ──────────────────────────────────
    @FXML
    private void onExaminar() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Elegir carpeta de destino");
        chooser.setInitialDirectory(carpetaSeleccionada.exists()
            ? carpetaSeleccionada
            : new File(System.getProperty("user.home")));

        Stage stage = (Stage) btnExaminar.getScene().getWindow();
        File seleccionada = chooser.showDialog(stage);

        if (seleccionada != null) {
            carpetaSeleccionada = seleccionada;
            lblCarpetaActual.setText(carpetaSeleccionada.getAbsolutePath());
        }
    }

    // ── Acción: Confirmar ─────────────────────────────────
    @FXML
    private void onConfirmar() {
        modelo.setCarpetaDestino(carpetaSeleccionada);
        cerrar();
    }

    // ── Acción: Cancelar ─────────────────────────────────
    @FXML
    private void onCancelar() {
        // No modifica el modelo, simplemente cierra
        cerrar();
    }

    // ── Helper ────────────────────────────────────────────
    private void cerrar() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }
}
