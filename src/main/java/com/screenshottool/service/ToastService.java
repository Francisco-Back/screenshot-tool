package com.screenshottool.service;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.util.function.Consumer;

/**
 * ToastService — notificación Fluent Design dentro del StackPane raíz.
 *
 * No crea ningún Stage extra: se superpone como overlay al contenido
 * existente → funciona igual en Windows 10, Windows 11 y Linux.
 *
 * Uso:
 *   ToastService toastService = new ToastService(rootPane, stage, servicio);
 *   toastService.mostrar(archivoGuardado);
 *
 * Requisito FXML: nodo raíz debe ser <StackPane fx:id="rootPane">
 */
public class ToastService {

    private final StackPane rootPane;
    private final Stage stage;
    private final com.screenshottool.service.ScreenshotService servicio;

    // Wrapper actual en el rootPane (para evitar duplicados)
    private VBox wrapperActual = null;

    public ToastService(StackPane rootPane, Stage stage,
                        com.screenshottool.service.ScreenshotService servicio) {
        this.rootPane = rootPane;
        this.stage    = stage;
        this.servicio = servicio;
    }

    // ════════════════════════════════════════════════════════
    //  API pública
    // ════════════════════════════════════════════════════════
    public void mostrar(File archivo) {

        // Quitar toast previo sin animación
        if (wrapperActual != null) {
            rootPane.getChildren().remove(wrapperActual);
            wrapperActual = null;
        }

        // ── Construir nodos UI ───────────────────────────────
        Region progressBar = crearProgressBar();
        HBox   header      = crearHeader(archivo);

        Button btnAbrir   = new Button("Abrir archivo");
        Button btnCarpeta = new Button("Mostrar en carpeta");
        btnAbrir  .getStyleClass().add("toast-btn-primary");
        btnCarpeta.getStyleClass().add("toast-btn-ghost");

        HBox acciones = new HBox(6, btnAbrir, btnCarpeta);
        acciones.setPadding(new Insets(8, 12, 10, 12));

        // Recuperar btnCerrar que buildHeader pone en índice 2
        Button btnCerrar = (Button) header.getChildren().get(2);

        // ── Toast VBox ───────────────────────────────────────
        VBox toast = new VBox(0, progressBar, header, new Separator(), acciones);
        toast.getStyleClass().add("toast-container");
        toast.setPrefWidth(300);
        toast.setMaxWidth(300);  // CRÍTICO: sin esto StackPane lo estira
        toast.setMinWidth(260);

        // ── Wrapper de posicionamiento ───────────────────────
        // Problema: StackPane.setAlignment ignora maxWidth y estira el hijo.
        // Solución: wrapper VBox que ocupa todo el StackPane y ubica el
        //           toast en su esquina inferior-derecha internamente.
        //
        //   VBox wrapper (fill StackPane, pickOnBounds=false)
        //   └── Region vSpacer (vgrow=ALWAYS) ← empuja hacia abajo
        //   └── HBox hRow (pickOnBounds=false)
        //       ├── Region hSpacer (hgrow=ALWAYS) ← empuja a la derecha
        //       └── VBox toast (maxWidth=300) ← el toast real

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        hSpacer.setPickOnBounds(false);

        HBox hRow = new HBox(hSpacer, toast);
        hRow.setPickOnBounds(false);

        Region vSpacer = new Region();
        VBox.setVgrow(vSpacer, Priority.ALWAYS);
        vSpacer.setPickOnBounds(false);

        VBox wrapper = new VBox(vSpacer, hRow);
        wrapper.setPickOnBounds(false);  // no bloquea clicks al contenido de abajo
        wrapper.setPadding(new Insets(0, 16, 16, 0));

        // El wrapper sí ocupa todo el StackPane → setMaxSize
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setMaxHeight(Double.MAX_VALUE);

        rootPane.getChildren().add(wrapper);
        wrapperActual = wrapper;

        // ── Animación de entrada ─────────────────────────────
        toast.setOpacity(0);
        toast.setTranslateY(14);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(220), toast);
        slideIn.setFromY(14);
        slideIn.setToY(0);

        new ParallelTransition(fadeIn, slideIn).play();

        // ── Barra de progreso scaleX 1→0 en 4 s ─────────────
        progressBar.layoutBoundsProperty().addListener((obs, o, n) -> {
            if (n.getWidth() > 0)
                progressBar.setTranslateX(
                    -(n.getWidth() / 2.0) * (1.0 - progressBar.getScaleX()));
        });

        Timeline progressAnim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(progressBar.scaleXProperty(), 1.0)),
            new KeyFrame(Duration.seconds(4),
                new KeyValue(progressBar.scaleXProperty(), 0.0, Interpolator.LINEAR))
        );
        progressAnim.play();

        // ── Lógica de cierre ─────────────────────────────────
        Consumer<Boolean> cerrar = buildCerrar(toast, wrapper, progressAnim);

        PauseTransition autoCierre = new PauseTransition(Duration.seconds(4));
        autoCierre.setOnFinished(e -> cerrar.accept(true));
        autoCierre.play();

        // ── Handlers de botones ──────────────────────────────
        btnCerrar.setOnAction(e -> {
            progressAnim.stop();
            autoCierre.stop();
            cerrar.accept(true);
        });

        btnAbrir.setOnAction(e -> {
            progressAnim.stop();
            autoCierre.stop();
            cerrar.accept(true);
            servicio.abrirArchivo(archivo);
        });

        btnCarpeta.setOnAction(e -> {
            progressAnim.stop();
            autoCierre.stop();
            cerrar.accept(true);
            try { Desktop.getDesktop().open(archivo.getParentFile()); }
            catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    // ════════════════════════════════════════════════════════
    //  Helpers privados
    // ════════════════════════════════════════════════════════

    /**
     * Crea el Consumer de cierre.
     * accept(true)  → quita toast + cierra la ventana principal
     * accept(false) → solo quita el toast (reservado para uso futuro)
     */
    private Consumer<Boolean> buildCerrar(VBox toast, VBox wrapper,
                                          Timeline progressAnim) {
        return (cerrarStage) -> {
            if (!rootPane.getChildren().contains(wrapper)) return;

            FadeTransition fo = new FadeTransition(Duration.millis(160), toast);
            fo.setFromValue(1);
            fo.setToValue(0);

            TranslateTransition so = new TranslateTransition(Duration.millis(160), toast);
            so.setFromY(0);
            so.setToY(8);

            ParallelTransition out = new ParallelTransition(fo, so);
            out.setOnFinished(ev -> {
                progressAnim.stop();
                rootPane.getChildren().remove(wrapper);
                wrapperActual = null;
                if (cerrarStage && stage.isShowing()) stage.close();
            });
            out.play();
        };
    }

    private Region crearProgressBar() {
        Region bar = new Region();
        bar.getStyleClass().add("toast-progress-bar");
        bar.setPrefHeight(3);
        bar.setMaxWidth(Double.MAX_VALUE);
        return bar;
    }

    private HBox crearHeader(File archivo) {
        // Círculo con check
        Label iconoLbl = new Label("✓");
        iconoLbl.getStyleClass().add("toast-check-icon");
        StackPane iconoWrap = new StackPane(iconoLbl);
        iconoWrap.getStyleClass().add("toast-check-circle");
        iconoWrap.setMinSize(28, 28);
        iconoWrap.setMaxSize(28, 28);

        // Título + nombre archivo
        Label lblTitulo  = new Label("Captura guardada");
        lblTitulo.getStyleClass().add("toast-title");
        Label lblArchivo = new Label(archivo.getName());
        lblArchivo.getStyleClass().add("toast-filename");
        lblArchivo.setMaxWidth(190);
        VBox textos = new VBox(2, lblTitulo, lblArchivo);
        HBox.setHgrow(textos, Priority.ALWAYS);

        // Botón X — la acción se asigna en mostrar()
        Button btnCerrar = new Button("✕");
        btnCerrar.getStyleClass().add("toast-btn-close");

        HBox header = new HBox(10, iconoWrap, textos, btnCerrar);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 12, 8, 12));
        return header;
    }
}
