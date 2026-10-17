package com.atlante.service;

import com.atlante.dto.Dtos.AffinityResponse;
import com.atlante.dto.Dtos.WorkResponse;
import com.atlante.model.Work;
import com.atlante.repository.WorkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Suggerimenti di accostamento per somiglianza cromatica: confronta le palette
 * (già estratte o curate) e propone opere dell'altro tipo con colori affini.
 * La critica resta umana: questi sono solo punti di partenza.
 */
@Service
@Transactional(readOnly = true)
public class AffinityService {

    private static final double MAX_DISTANCE = 441.7; // distanza RGB massima

    private final WorkRepository workRepository;
    private final WorkService workService;

    public AffinityService(WorkRepository workRepository, WorkService workService) {
        this.workRepository = workRepository;
        this.workService = workService;
    }

    public List<AffinityResponse> affinities(Long workId, int limit) {
        Work base = workService.getEntity(workId);
        int[][] basePalette = parse(base.getPalette());
        if (basePalette.length == 0) {
            return List.of();
        }

        return workRepository.findAll().stream()
                .filter(w -> !w.getId().equals(base.getId()))
                .filter(w -> w.getType() != base.getType())
                .filter(w -> w.getPalette() != null)
                .map(w -> new AffinityResponse(WorkResponse.from(w),
                        similarity(basePalette, parse(w.getPalette()))))
                .sorted(Comparator.comparingInt(AffinityResponse::similarity).reversed())
                .limit(Math.max(1, Math.min(limit, 10)))
                .toList();
    }

    private int similarity(int[][] a, int[][] b) {
        if (a.length == 0 || b.length == 0) {
            return 0;
        }
        double d = (avgMinDistance(a, b) + avgMinDistance(b, a)) / 2.0;
        return (int) Math.round(100.0 * (1.0 - d / MAX_DISTANCE));
    }

    private double avgMinDistance(int[][] from, int[][] to) {
        double sum = 0;
        for (int[] c1 : from) {
            double min = Double.MAX_VALUE;
            for (int[] c2 : to) {
                double dr = c1[0] - c2[0];
                double dg = c1[1] - c2[1];
                double db = c1[2] - c2[2];
                min = Math.min(min, Math.sqrt(dr * dr + dg * dg + db * db));
            }
            sum += min;
        }
        return sum / from.length;
    }

    private int[][] parse(String palette) {
        if (palette == null || palette.isBlank()) {
            return new int[0][];
        }
        return java.util.Arrays.stream(palette.split(","))
                .map(String::trim)
                .filter(s -> s.matches("#[0-9a-fA-F]{6}"))
                .map(s -> new int[]{
                        Integer.parseInt(s.substring(1, 3), 16),
                        Integer.parseInt(s.substring(3, 5), 16),
                        Integer.parseInt(s.substring(5, 7), 16)})
                .toArray(int[][]::new);
    }
}
