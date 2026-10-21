package com.atlante.service;

import com.atlante.model.User;
import com.atlante.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Foto profilo: salvate accanto alle immagini delle opere, in una sottocartella avatars. */
@Slf4j
@Service
public class AvatarService {

    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final UserRepository userRepository;
    private final Path dir;

    public AvatarService(UserRepository userRepository, @Value("${app.images.storage-dir}") String storageDir) {
        this.userRepository = userRepository;
        this.dir = Path.of(storageDir, "avatars").toAbsolutePath();
    }

    @Transactional
    public User upload(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Scegli un'immagine da caricare.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("La foto supera i 2 MB: scegline una più leggera.");
        }
        String ext = EXTENSIONS.get(file.getContentType());
        if (ext == null || !looksLikeImage(file, ext)) {
            throw new IllegalArgumentException("Formato non supportato: usa JPG, PNG o WebP.");
        }
        String name = user.getId() + "-" + UUID.randomUUID() + "." + ext;
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(name));
        } catch (IOException e) {
            throw new IllegalStateException("Non è stato possibile salvare la foto.", e);
        }
        deleteFile(user.getAvatarFile());
        user.setAvatarFile(name);
        return userRepository.save(user);
    }

    @Transactional
    public User remove(User user) {
        deleteFile(user.getAvatarFile());
        user.setAvatarFile(null);
        return userRepository.save(user);
    }

    /** Il file della foto, se l'utente ne ha una e il file esiste ancora. */
    public Optional<Path> find(String username) {
        return userRepository.findByUsername(username)
                .map(User::getAvatarFile)
                .map(dir::resolve)
                .filter(Files::isRegularFile);
    }

    /** Controlla i primi byte: il content-type dichiarato dal browser non basta. */
    private boolean looksLikeImage(MultipartFile file, String ext) {
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

    private void deleteFile(String name) {
        if (name == null) return;
        try {
            Files.deleteIfExists(dir.resolve(name));
        } catch (IOException e) {
            log.warn("Foto profilo {} non eliminata: {}", name, e.getMessage());
        }
    }
}
