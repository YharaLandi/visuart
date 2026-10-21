package com.atlante.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * Immagini caricate dagli utenti: un fotogramma scelto, il dettaglio di un quadro, la
 * riproduzione di un'opera che non è negli archivi. Finiscono in storage/images/uploads e si
 * servono come /images/uploads/...; il formato si controlla sui primi byte del file.
 */
@Service
public class ImageUploadService {

    public static final String PREFIX = "/images/uploads/";
    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    private final Path dir;
    private final Path storage;

    public ImageUploadService(@Value("${app.images.storage-dir}") String storageDir) {
        this.storage = Path.of(storageDir).toAbsolutePath();
        this.dir = storage.resolve("uploads");
    }

    public record Uploaded(String url, String palette) {
    }

    public Uploaded save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Scegli un'immagine da caricare.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("L'immagine supera i 5 MB: scegline una più leggera.");
        }
        String ext = EXTENSIONS.get(file.getContentType());
        if (ext == null || !looksLikeImage(file, ext)) {
            throw new IllegalArgumentException("Formato non supportato: usa JPG, PNG o WebP.");
        }
        String name = UUID.randomUUID() + "." + ext;
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(name));
        } catch (IOException e) {
            throw new IllegalStateException("Non è stato possibile salvare l'immagine.", e);
        }
        return new Uploaded(PREFIX + name, PaletteExtractor.extract(dir.resolve(name), 6));
    }

    /** Solo immagini caricate qui: un URL esterno arbitrario non si accetta come immagine di una connessione. */
    public String requireUploaded(String url) {
        if (url == null || url.isBlank()) return null;
        if (!url.startsWith(PREFIX) || url.contains("..") || !Files.isRegularFile(storage.resolve(url.substring("/images/".length())))) {
            throw new IllegalArgumentException("Immagine non valida: caricala di nuovo.");
        }
        return url;
    }

    /** Palette di un'immagine caricata (null se il file non c'è). */
    public String paletteOf(String url) {
        if (url == null || !url.startsWith(PREFIX)) return null;
        Path file = storage.resolve(url.substring("/images/".length()));
        return Files.isRegularFile(file) ? PaletteExtractor.extract(file, 6) : null;
    }

    static boolean looksLikeImage(MultipartFile file, String ext) {
        try (InputStream in = file.getInputStream()) {
            byte[] h = in.readNBytes(12);
            return switch (ext) {
                case "jpg" -> h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
                case "png" -> h.length >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
                case "webp" -> h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[8] == 'W' && h[9] == 'E';
                default -> false;
            };
        } catch (IOException e) {
            return false;
        }
    }
}
