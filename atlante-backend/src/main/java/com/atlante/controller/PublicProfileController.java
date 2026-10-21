package com.atlante.controller;

import com.atlante.dto.Dtos.PublicProfile;
import com.atlante.model.User;
import com.atlante.repository.CommentRepository;
import com.atlante.repository.ConnectionRepository;
import com.atlante.repository.LikeRepository;
import com.atlante.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profilo pubblico di un utente: visibile a tutti, senza email né dati di account.
 * Le sue critiche si leggono da /api/connections/search?author=username.
 */
@RestController
@RequestMapping("/api/users/{username}/profile")
@RequiredArgsConstructor
public class PublicProfileController {

    private final UserService userService;
    private final ConnectionRepository connectionRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public PublicProfile profile(@PathVariable String username) {
        User user = userService.findByUsername(username);
        return new PublicProfile(
                user.getUsername(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getBio(),
                user.getCreatedAt(),
                connectionRepository.findByAuthor(user).size(),
                likeRepository.countReceivedBy(user),
                commentRepository.countByAuthor(user),
                user.getAvatarFile() != null);
    }
}
