package com.atlante.config;

import com.atlante.repository.WorkImageRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Su Render il disco si svuota a ogni deploy: se la copia locale di un'immagine non c'è più,
 * il browser viene mandato all'originale (TMDB o Wikimedia) invece di ricevere un 404.
 */
@Component
public class MissingImageFallbackFilter extends OncePerRequestFilter {

    private final WorkImageRepository workImages;
    private final Path storage;

    public MissingImageFallbackFilter(WorkImageRepository workImages,
                                      @Value("${app.images.storage-dir}") String storageDir) {
        this.workImages = workImages;
        this.storage = Path.of(storageDir).toAbsolutePath().normalize();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"GET".equals(request.getMethod()) || !request.getRequestURI().startsWith("/images/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        Path file = storage.resolve(path.substring("/images/".length())).normalize();
        if (file.startsWith(storage) && !Files.exists(file)) {
            var original = workImages.findFirstByWork_ImageUrlOrderByIdDesc(path);
            if (original.isPresent() && original.get().getUrlOriginal() != null) {
                response.sendRedirect(original.get().getUrlOriginal());
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
