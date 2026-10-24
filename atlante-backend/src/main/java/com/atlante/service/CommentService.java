package com.atlante.service;

import com.atlante.dto.Dtos.CommentResponse;
import com.atlante.exception.ResourceNotFoundException;
import com.atlante.model.Comment;
import com.atlante.model.Connection;
import com.atlante.model.Notification;
import com.atlante.model.User;
import com.atlante.repository.CommentRepository;
import com.atlante.repository.ConnectionRepository;
import com.atlante.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository,
                          ConnectionRepository connectionRepository,
                          UserRepository userRepository,
                          NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long connectionId) {
        return commentRepository.findByConnectionIdOrderByCreatedAtAsc(connectionId)
                .stream().map(CommentResponse::from).toList();
    }

    public CommentResponse edit(Long connectionId, Long commentId, String username, String text) {
        Comment comment = find(connectionId, commentId);
        requireOwnerOrAdmin(comment, username, "Puoi modificare solo i tuoi commenti");
        comment.edit(text.trim());
        return CommentResponse.from(comment);
    }

    public void delete(Long connectionId, Long commentId, String username) {
        Comment comment = find(connectionId, commentId);
        requireOwnerOrAdmin(comment, username, "Puoi eliminare solo i tuoi commenti");
        commentRepository.delete(comment);
    }

    private Comment find(Long connectionId, Long commentId) {
        // il commento va cercato dentro la sua connessione: un id di un'altra risponde 404
        return commentRepository.findById(commentId)
                .filter(c -> c.getConnection().getId().equals(connectionId))
                .orElseThrow(() -> new ResourceNotFoundException("Commento non trovato: " + commentId));
    }

    private void requireOwnerOrAdmin(Comment comment, String username, String message) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
        if (!comment.getAuthor().getId().equals(user.getId()) && !user.hasRole(com.atlante.model.Role.ADMIN)) {
            throw new com.atlante.exception.UnauthorizedException(message);
        }
    }

    public CommentResponse add(Long connectionId, String username, String text) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connessione non trovata: " + connectionId));
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));
        Comment saved = commentRepository.save(new Comment(connection, author, text.trim()));
        notificationService.notify(author, connection, Notification.Type.COMMENT, saved.getText());
        return CommentResponse.from(saved);
    }
}
