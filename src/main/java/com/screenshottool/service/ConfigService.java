package com.screenshottool.service;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * ConfigService - Persiste preferencias del usuario entre sesiones.
 *
 * Guarda y carga configuración en:
 *   Linux:   ~/.config/screenshot-tool/config.properties
 *   Windows: %APPDATA%\screenshot-tool\config.properties
 *
 * Uso:
 *   ConfigService.guardar("carpeta.destino", "/home/user/Imágenes");
 *   String carpeta = ConfigService.obtener("carpeta.destino", "/home/user");
 */
public class ConfigService {

    private static final String APP_NAME = "screenshot-tool";
    private static final String FILE_NAME = "config.properties";

    // Claves de configuración
    public static final String KEY_CARPETA  = "carpeta.destino";
    public static final String KEY_FORMATO  = "formato.default";

    private static final Path CONFIG_FILE = resolverRuta();

    private ConfigService() {} // clase utilitaria

    // ── Resolver ruta del archivo de configuración ────────────
    private static Path resolverRuta() {
        String os = System.getProperty("os.name").toLowerCase();
        String base;

        if (os.contains("win")) {
            // Windows: %APPDATA%\screenshot-tool\
            base = System.getenv("APPDATA");
            if (base == null) base = System.getProperty("user.home");
        } else {
            // Linux/Mac: ~/.config/screenshot-tool/
            String xdgConfig = System.getenv("XDG_CONFIG_HOME");
            base = (xdgConfig != null && !xdgConfig.isEmpty())
                    ? xdgConfig
                    : System.getProperty("user.home") + "/.config";
        }

        return Paths.get(base, APP_NAME, FILE_NAME);
    }

    // ── Cargar todas las propiedades ──────────────────────────
    private static Properties cargar() {
        Properties props = new Properties();
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
                props.load(in);
            } catch (Exception e) {
                System.err.println("[ConfigService] Error al cargar config: " + e.getMessage());
            }
        }
        return props;
    }

    // ── Obtener valor ─────────────────────────────────────────
    public static String obtener(String clave, String valorDefault) {
        return cargar().getProperty(clave, valorDefault);
    }

    // ── Guardar valor ─────────────────────────────────────────
    public static void guardar(String clave, String valor) {
        try {
            // Crear directorio si no existe
            Files.createDirectories(CONFIG_FILE.getParent());

            Properties props = cargar();
            props.setProperty(clave, valor);

            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "Screenshot Tool - User preferences");
            }
        } catch (Exception e) {
            System.err.println("[ConfigService] Error al guardar config: " + e.getMessage());
        }
    }

    // ── Obtener carpeta destino ───────────────────────────────
    public static java.io.File getCarpetaDestino() {
        String carpetaGuardada = obtener(KEY_CARPETA, null);
        if (carpetaGuardada != null) {
            java.io.File carpeta = new java.io.File(carpetaGuardada);
            if (carpeta.exists() && carpeta.isDirectory()) return carpeta;
        }
        return null; // usar default del modelo
    }

    // ── Obtener formato ───────────────────────────────────────
    public static String getFormato() {
        return obtener(KEY_FORMATO, "png");
    }

    // ── Guardar carpeta destino ───────────────────────────────
    public static void guardarCarpeta(java.io.File carpeta) {
        if (carpeta != null && carpeta.exists()) {
            guardar(KEY_CARPETA, carpeta.getAbsolutePath());
        }
    }

    // ── Guardar formato ───────────────────────────────────────
    public static void guardarFormato(String formato) {
        if (formato != null && !formato.isEmpty()) {
            guardar(KEY_FORMATO, formato);
        }
    }
}
