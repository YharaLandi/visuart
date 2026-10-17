package com.atlante.service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Estrae i colori dominanti da un'immagine con una quantizzazione semplice
 * (bucket a 4 bit per canale, media dei pixel di ogni bucket).
 * Nessuna dipendenza esterna: solo java.awt/ImageIO.
 */
public final class PaletteExtractor {

    private static final int SAMPLE_SIZE = 64;

    private PaletteExtractor() {
    }

    public static String extract(Path imagePath, int colorCount) {
        try {
            BufferedImage original = ImageIO.read(imagePath.toFile());
            if (original == null) {
                return null;
            }
            BufferedImage img = new BufferedImage(SAMPLE_SIZE, SAMPLE_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.drawImage(original, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE, null);
            g.dispose();

            Map<Integer, long[]> buckets = new HashMap<>();
            for (int y = 0; y < SAMPLE_SIZE; y++) {
                for (int x = 0; x < SAMPLE_SIZE; x++) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int gr = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    int key = ((r >> 4) << 8) | ((gr >> 4) << 4) | (b >> 4);
                    long[] acc = buckets.computeIfAbsent(key, k -> new long[4]);
                    acc[0] += r;
                    acc[1] += gr;
                    acc[2] += b;
                    acc[3]++;
                }
            }

            return buckets.values().stream()
                    .sorted(Comparator.comparingLong((long[] a) -> a[3]).reversed())
                    .limit(colorCount)
                    .map(a -> String.format("#%02x%02x%02x",
                            (int) (a[0] / a[3]), (int) (a[1] / a[3]), (int) (a[2] / a[3])))
                    .collect(Collectors.joining(","));
        } catch (Exception e) {
            return null;
        }
    }
}
