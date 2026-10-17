package com.atlante.service;

import com.atlante.exception.DuplicateLikeException;
import com.atlante.exception.ResourceNotFoundException;
import com.atlante.model.Connection;
import com.atlante.model.Like;
import com.atlante.model.Notification;
import com.atlante.model.User;
import com.atlante.repository.LikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class LikeService {

    private final LikeRepository likeRepository;
    private final NotificationService notificationService;

    public Like addLike(User user, Connection connection) {
        if (likeRepository.findByUserAndConnection(user, connection).isPresent()) {
            throw new DuplicateLikeException("Hai già messo like a questa connessione");
        }
        Like like = Like.builder()
                .user(user)
                .connection(connection)
                .build();
        Like saved = likeRepository.save(like);
        notificationService.notify(user, connection, Notification.Type.LIKE, null);
        return saved;
    }

    public void removeLike(User user, Connection connection) {
        Like like = likeRepository.findByUserAndConnection(user, connection)
                .orElseThrow(() -> new ResourceNotFoundException("Like non trovato"));
        likeRepository.delete(like);
    }

    @Transactional(readOnly = true)
    public List<Like> findByConnection(Connection connection) {
        return likeRepository.findByConnection(connection);
    }

    @Transactional(readOnly = true)
    public long count(Long connectionId) {
        return likeRepository.countByConnectionId(connectionId);
    }

    /** Conteggi per un gruppo di connessioni in una sola query (evita N+1 nelle liste). */
    @Transactional(readOnly = true)
    public Map<Long, Long> countsFor(Collection<Long> connectionIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (connectionIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : likeRepository.countByConnectionIds(connectionIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public Set<Long> likedBy(String username, Collection<Long> connectionIds) {
        if (username == null || connectionIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(likeRepository.likedConnectionIds(username, connectionIds));
    }
}
