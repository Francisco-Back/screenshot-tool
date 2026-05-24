package com.screenshottool.model;

import javafx.beans.property.*;
import javafx.scene.image.Image;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modelo que representa una captura de pantalla.
 * Usa propiedades JavaFX para binding directo con el FXML.
 */
public class CapturaModel {

    // ── Imagen capturada ──────────────────────────────────
    private Image imagen;
    private int anchoReal;
    private int altoReal;

    // ── Propiedades bindeables con el FXML ───────────────
    private final StringProperty nombre = new SimpleStringProperty();
    private final StringProperty formato = new SimpleStringProperty("png");
    private final ObjectProperty<File> carpetaDestino = new SimpleObjectProperty<>(obtenerCarpetaImagenes());

    // ── Formatos soportados ───────────────────────────────
    public static final String[] FORMATOS = { "png", "jpg", "bmp", "gif" };

    public CapturaModel() {
        nombre.set(generarNombreDefault());
    }

    // ── Nombre por defecto con fecha/hora ─────────────────
    private String generarNombreDefault() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        return "captura_" + LocalDateTime.now().format(fmt);
    }

    // Regenera el nombre (útil al hacer nueva captura)
    public void refrescarNombre() {
        nombre.set(generarNombreDefault());
    }

    // ── Ruta final del archivo a guardar ──────────────────
    public File getArchivoDestino() {
        String n = nombre.get().trim();
        // Quitar extensión si el usuario la escribió
        for (String ext : new String[] { ".png", ".jpg", ".jpeg", ".bmp", ".gif" }) {
            if (n.toLowerCase().endsWith(ext)) {
                n = n.substring(0, n.lastIndexOf('.'));
                break;
            }
        }
        return new File(carpetaDestino.get(), n + "." + formato.get());
    }
    private static File obtenerCarpetaImagenes() {
    // Intenta carpeta Imágenes estándar en Windows y Linux
    String home = System.getProperty("user.home");
    String[] posibles = {"Pictures", "Imágenes", "Imagenes", "Images"};
    for (String nombre : posibles) {
        File carpeta = new File(home, nombre);
        if (carpeta.exists() && carpeta.isDirectory()) return carpeta;
    }
    return new File(home); // fallback: home
}

    // ── Getters / Setters ─────────────────────────────────
    public Image getImagen() {
        return imagen;
    }

    public void setImagen(Image img) {
        this.imagen = img;
    }

    public int getAnchoReal() {
        return anchoReal;
    }

    public void setAnchoReal(int w) {
        this.anchoReal = w;
    }

    public int getAltoReal() {
        return altoReal;
    }

    public void setAltoReal(int h) {
        this.altoReal = h;
    }

    // Properties para binding
    public StringProperty nombreProperty() {
        return nombre;
    }

    public StringProperty formatoProperty() {
        return formato;
    }

    public ObjectProperty<File> carpetaDestinoProperty() {
        return carpetaDestino;
    }

    public String getNombre() {
        return nombre.get();
    }

    public void setNombre(String n) {
        nombre.set(n);
    }

    public String getFormato() {
        return formato.get();
    }

    public void setFormato(String f) {
        formato.set(f);
    }

    public File getCarpetaDestino() {
        return carpetaDestino.get();
    }

    public void setCarpetaDestino(File f) {
        carpetaDestino.set(f);
    }
}
