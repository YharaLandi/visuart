package com.atlante.service;

import com.atlante.dto.Dtos.ConnectionRequest;
import com.atlante.dto.Dtos.ConnectionResponse;
import com.atlante.exception.ResourceNotFoundException;
import com.atlante.exception.UnauthorizedException;
import com.atlante.model.Category;
import com.atlante.model.Connection;
import com.atlante.model.Role;
import com.atlante.model.User;
import com.atlante.repository.CategoryRepository;
import com.atlante.repository.CommentRepository;
import com.atlante.repository.ConnectionRepository;
import com.atlante.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final UserService userService;
    private final WorkService workService;
    private final LikeService likeService;
    private final com.atlante.repository.NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<ConnectionResponse> findAll(String categorySlug, String viewerUsername) {
        List<Connection> list = categorySlug != null && !categorySlug.isBlank()
                ? connectionRepository.findByCategorySlug(categorySlug)
                : connectionRepository.findAll();
        return toResponses(list, viewerUsername);
    }

    /** Connessioni ordinate per numero di visualizzazioni (le più aperte prima). */
    @Transactional(readOnly = true)
    public List<ConnectionResponse> findAllByViews(String categorySlug, String viewerUsername) {
        List<Connection> list = categorySlug != null && !categorySlug.isBlank()
                ? connectionRepository.findByCategorySlugOrderByViewCountDesc(categorySlug)
                : connectionRepository.findAllByOrderByViewCountDesc();
        return toResponses(list, viewerUsername);
    }

    private static final int MAX_PAGE_SIZE = 50;

    private static final Map<String, org.springframework.data.domain.Sort> SORTS = Map.of(
            "views", org.springframework.data.domain.Sort.by("viewCount").descending().and(
                    org.springframework.data.domain.Sort.by("id").descending()),
            "recent", org.springframework.data.domain.Sort.by("createdAt").descending().and(
                    org.springframework.data.domain.Sort.by("id").descending()),
            "oldest", org.springframework.data.domain.Sort.by("createdAt").ascending().and(
                    org.springframework.data.domain.Sort.by("id").ascending()));

    /** Ricerca paginata con filtri combinabili; l'ordinamento è scelto da una lista chiusa. */
    @Transactional(readOnly = true)
    public com.atlante.dto.Dtos.PageResponse<ConnectionResponse> search(
            String q, String categorySlug, com.atlante.model.WorkType type, String author,
            String sort, int page, int size, String viewerUsername) {
        org.springframework.data.domain.Sort order = SORTS.get(sort == null ? "views" : sort.toLowerCase());
        if (order == null) {
            throw new IllegalArgumentException("sort: valori ammessi " + String.join(", ", SORTS.keySet()));
        }
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page deve essere >= 0 e size >= 1");
        }
        var spec = org.springframework.data.jpa.domain.Specification
                .where(ConnectionSpecifications.matches(q))
                .and(ConnectionSpecifications.inCategory(categorySlug))
                .and(ConnectionSpecifications.involving(type))
                .and(ConnectionSpecifications.byAuthor(author));
        var result = connectionRepository.findAll(spec,
                org.springframework.data.domain.PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), order));
        List<ConnectionResponse> content = toResponses(result.getContent(), viewerUsername);
        return new com.atlante.dto.Dtos.PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ConnectionResponse findById(Long id, String viewerUsername) {
        return toResponses(List.of(getEntity(id)), viewerUsername).getFirst();
    }

    @Transactional(readOnly = true)
    public Connection getEntity(Long id) {
        return connectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Connessione non trovata: " + id));
    }

    /** Incrementa il contatore quando l'utente apre il dettaglio di una connessione. */
    public void registerView(Long id) {
        if (!connectionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Connessione non trovata: " + id);
        }
        connectionRepository.incrementViewCount(id);
    }

    public ConnectionResponse create(ConnectionRequest req, String authorUsername) {
        if (req.sourceWorkId().equals(req.targetWorkId())) {
            throw new IllegalArgumentException("Un'opera non può richiamare sé stessa");
        }
        Category category = findCategory(req.categorySlug());
        User author = userService.findByUsername(authorUsername);
        Connection connection = new Connection(
                workService.getEntity(req.sourceWorkId()),
                workService.getEntity(req.targetWorkId()),
                category, req.criticalNote(), author);
        return toResponses(List.of(connectionRepository.save(connection)), authorUsername).getFirst();
    }

    public ConnectionResponse update(Long id, ConnectionRequest req, String username) {
        Connection connection = getEntity(id);
        checkOwnership(connection, username, "Puoi modificare solo le tue connessioni");
        connection.setCategory(findCategory(req.categorySlug()));
        connection.setCriticalNote(req.criticalNote());
        return toResponses(List.of(connection), username).getFirst();
    }

    public void delete(Long id, String username) {
        Connection connection = getEntity(id);
        checkOwnership(connection, username, "Puoi eliminare solo le tue connessioni");
        likeRepository.deleteAll(likeRepository.findByConnection(connection));
        commentRepository.deleteAll(commentRepository.findByConnectionIdOrderByCreatedAtAsc(id));
        notificationRepository.deleteByConnectionId(id);
        connectionRepository.delete(connection);
    }

    private Category findCategory(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria non trovata: " + slug));
    }

    /** Modifica ed eliminazione: solo l'autore della connessione oppure un ADMIN. */
    private void checkOwnership(Connection connection, String username, String message) {
        User user = userService.findByUsername(username);
        boolean owner = connection.getAuthor() != null
                && connection.getAuthor().getId().equals(user.getId());
        if (!owner && user.getRole() != Role.ADMIN) {
            throw new UnauthorizedException(message);
        }
    }

    private List<ConnectionResponse> toResponses(List<Connection> list, String viewerUsername) {
        List<Long> ids = list.stream().map(Connection::getId).toList();
        Map<Long, Long> counts = likeService.countsFor(ids);
        Set<Long> liked = likeService.likedBy(viewerUsername, ids);
        Map<Long, Long> comments = new java.util.HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : commentRepository.countByConnectionIds(ids)) {
                comments.put((Long) row[0], (Long) row[1]);
            }
        }
        return list.stream()
                .map(c -> ConnectionResponse.from(c, counts.getOrDefault(c.getId(), 0L),
                        liked.contains(c.getId()), comments.getOrDefault(c.getId(), 0L)))
                .toList();
    }
}
