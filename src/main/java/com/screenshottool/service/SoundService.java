package com.screenshottool.service;

import javax.sound.sampled.*;

/**
 * SoundService - Reproduce sonidos sintetizados sin archivos externos.
 *
 * Genera sonidos programáticamente usando javax.sound.sampled.
 * No requiere archivos .wav ni dependencias adicionales.
 *
 * Uso:
 *   SoundService.reproducirCaptura(); // sonido de cámara
 */
public class SoundService {

    private SoundService() {} // clase utilitaria — no instanciar

    // ── Sonido de captura tipo cámara ─────────────────────────
    
    public static void reproducirCaptura() {
    new Thread(() -> {
        try {
            var url = SoundService.class.getResource(
                "/com/screenshottool/sounds/capture.wav");
            if (url == null) return;

            AudioInputStream audio = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            clip.start();
        } catch (Exception e) {
            System.err.println("[SoundService] Error: " + e.getMessage());
        }
    }, "sound-thread").start();
}
}
