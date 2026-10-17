package com.atlante.controller;

import com.atlante.dto.Dtos.CommentRequest;
import com.atlante.dto.Dtos.CommentResponse;
import com.atlante.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/connections/{connectionId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable Long connectionId) {
        return commentService.list(connectionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@PathVariable Long connectionId,
                               @Valid @RequestBody CommentRequest req,
                               Authentication auth) {
        return commentService.add(connectionId, auth.getName(), req.text());
    }

    @PutMapping("/{commentId}")
    public CommentResponse edit(@PathVariable Long connectionId, @PathVariable Long commentId,
                                @Valid @RequestBody CommentRequest req, Authentication auth) {
        return commentService.edit(connectionId, commentId, auth.getName(), req.text());
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long connectionId, @PathVariable Long commentId, Authentication auth) {
        commentService.delete(connectionId, commentId, auth.getName());
    }
}
