package com.screenshottool;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import com.screenshottool.core.AppContext;

/**
 * ScreenshotApp - Punto de entrada JavaFX.
 *
 * Solo responsable de:
 *   - Configurar propiedades del sistema antes de lanzar JavaFX
 *   - Llamar a AppContext que orquesta todo lo demás
 */
public class ScreenshotApp extends Application {

    private AppContext appContext;

    public static void main(String[] args) {
        // Necesario para que JavaFX coexista con AWT (bandeja del sistema)
        System.setProperty("javafx.embed.singleThread", "false");
        // No salir al cerrar el diálogo — la app vive en la bandeja
        Platform.setImplicitExit(false);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // La ventana principal no se usa — todo va por la bandeja
        primaryStage.hide();

        appContext = new AppContext();
        appContext.iniciar();
    }
}
