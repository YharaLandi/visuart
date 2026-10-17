package com.atlante.service;

import com.atlante.dto.Dtos.WorkResponse;
import com.atlante.exception.ResourceNotFoundException;
import com.atlante.model.Favorite;
import com.atlante.model.User;
import com.atlante.repository.FavoriteRepository;
import com.atlante.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final WorkService workService;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           UserRepository userRepository,
                           WorkService workService) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.workService = workService;
    }

    @Transactional(readOnly = true)
    public List<WorkResponse> list(String username) {
        User user = requireUser(username);
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(f -> WorkResponse.from(f.getWork())).toList();
    }

    public void add(String username, Long workId) {
        User user = requireUser(username);
        if (!favoriteRepository.existsByUserIdAndWorkId(user.getId(), workId)) {
            favoriteRepository.save(new Favorite(user, workService.getEntity(workId)));
        }
    }

    public void remove(String username, Long workId) {
        User user = requireUser(username);
        favoriteRepository.findByUserIdAndWorkId(user.getId(), workId)
                .ifPresent(favoriteRepository::delete);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
    }
}
