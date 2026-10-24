package com.atlante.service;

import com.atlante.dto.Dtos.UserStats;
import com.atlante.model.Connection;
import com.atlante.model.Conversation;
import com.atlante.model.User;
import com.atlante.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Operazioni dell'utente sul proprio account e cancellazione a cascata (usata anche dall'admin). */
@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    private final UserRepository userRepository;
    private final ConnectionRepository connectionRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final FavoriteRepository favoriteRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final IssuedTokenRepository issuedTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public User updateProfile(User user, String displayName, String bio) {
        user.setDisplayName(displayName.trim());
        user.setBio(bio == null || bio.isBlank() ? null : bio.trim());
        return userRepository.save(user);
    }

    public User setNewsletter(User user, boolean enabled) {
        user.setNewsletter(enabled);
        return userRepository.save(user);
    }

    /** Cambia password e chiude tutte le altre sessioni; restituisce l'utente per emettere un token nuovo. */
    public User changePassword(User user, String currentPassword, String newPassword) {
        requirePassword(user, currentPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenService.revokeAll(user);
        return user;
    }

    public void deleteOwnAccount(User user, String password) {
        requirePassword(user, password);
        delete(user);
    }

    /**
     * Elimina l'utente e tutto ciò che gli appartiene: like e commenti dati, preferiti,
     * le sue connessioni (con like, commenti e notifiche), chat, notifiche e token.
     */
    public void delete(User user) {
        likeRepository.deleteAll(likeRepository.findByUser(user));
        commentRepository.deleteAll(commentRepository.findByAuthor(user));
        favoriteRepository.deleteAll(favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));

        for (Connection c : connectionRepository.findByAuthor(user)) {
            likeRepository.deleteAll(likeRepository.findByConnection(c));
            commentRepository.deleteAll(commentRepository.findByConnectionIdOrderByCreatedAtAsc(c.getId()));
            notificationRepository.deleteByConnectionId(c.getId());
            connectionRepository.delete(c);
        }

        List<Conversation> conversations = conversationRepository.ofUser(user.getUsername());
        if (!conversations.isEmpty()) {
            messageRepository.deleteByConversationIds(conversations.stream().map(Conversation::getId).toList());
            conversationRepository.deleteAll(conversations);
        }

        notificationRepository.deleteByRecipient(user);
        resetTokenRepository.deleteByUser(user);
        issuedTokenRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public UserStats stats(User user) {
        return new UserStats(
                connectionRepository.findByAuthor(user).size(),
                likeRepository.countReceivedBy(user),
                commentRepository.countByAuthor(user),
                favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size(),
                messageRepository.countSent(user.getUsername()),
                messageRepository.countReceived(user.getUsername()));
    }

    private void requirePassword(User user, String password) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Password attuale non corretta");
        }
    }
}
