package com.atlante.service;

import com.atlante.dto.Dtos.GraphEdge;
import com.atlante.dto.Dtos.GraphResponse;
import com.atlante.dto.Dtos.WorkResponse;
import com.atlante.model.Connection;
import com.atlante.model.Work;
import com.atlante.repository.ConnectionRepository;
import com.atlante.repository.WorkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class GraphService {

    private final ConnectionRepository connectionRepository;
    private final WorkRepository workRepository;

    public GraphService(ConnectionRepository connectionRepository, WorkRepository workRepository) {
        this.connectionRepository = connectionRepository;
        this.workRepository = workRepository;
    }

    public GraphResponse buildGraph(Long workId, int depth, String categorySlug) {
        List<Connection> edges;
        if (workId == null) {
            edges = categorySlug != null && !categorySlug.isBlank()
                    ? connectionRepository.findByCategorySlug(categorySlug)
                    : connectionRepository.findAll();
        } else {
            edges = bfs(workId, Math.max(1, Math.min(depth, 4)),
                    categorySlug != null && !categorySlug.isBlank() ? categorySlug : null);
        }

        Map<Long, Work> nodes = new LinkedHashMap<>();
        if (workId != null) {
            workRepository.findById(workId).ifPresent(w -> nodes.put(w.getId(), w));
        }
        for (Connection c : edges) {
            nodes.putIfAbsent(c.getSource().getId(), c.getSource());
            nodes.putIfAbsent(c.getTarget().getId(), c.getTarget());
        }

        return new GraphResponse(
                nodes.values().stream().map(WorkResponse::from).toList(),
                edges.stream().map(c -> new GraphEdge(c.getId(),
                        c.getSource().getId(), c.getTarget().getId(),
                        c.getCategory().getName(), c.getCategory().getSlug(),
                        c.getCriticalNote())).toList());
    }

    private List<Connection> bfs(Long startId, int depth, String categorySlug) {
        Set<Long> visited = new HashSet<>(List.of(startId));
        Set<Long> edgeIds = new HashSet<>();
        List<Connection> result = new ArrayList<>();
        List<Long> frontier = List.of(startId);

        for (int level = 0; level < depth && !frontier.isEmpty(); level++) {
            List<Connection> found = connectionRepository.findTouchingAny(frontier, categorySlug);
            List<Long> next = new ArrayList<>();
            for (Connection c : found) {
                if (edgeIds.add(c.getId())) {
                    result.add(c);
                }
                for (Long id : List.of(c.getSource().getId(), c.getTarget().getId())) {
                    if (visited.add(id)) {
                        next.add(id);
                    }
                }
            }
            frontier = next;
        }
        return result;
    }
}
