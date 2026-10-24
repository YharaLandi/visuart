package com.atlante.dto;

import com.atlante.model.Connection;
import com.atlante.model.ExternalSource;
import com.atlante.model.Work;
import com.atlante.model.WorkType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class Dtos {

    private Dtos() {
    }

    public record RegisterRequest(@Email @NotBlank String email,
                                  @NotBlank @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{12,}$", message = "almeno 12 caratteri, una maiuscola e un numero") String password,
                                  @NotBlank String displayName,
                                  @Size(min = 3, max = 60) String username) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record ForgotPasswordRequest(@Email @NotBlank String email) {
    }

    public record ResetPasswordRequest(@NotBlank String token,
                                       @NotBlank @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{12,}$", message = "almeno 12 caratteri, una maiuscola e un numero") String password) {
    }

    public record UserResponse(Long id, String username, String email, String displayName, String role,
                               String bio, java.time.Instant createdAt, boolean hasAvatar,
                               java.util.List<String> roles) {

        public static UserResponse from(com.atlante.model.User u) {
            return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getDisplayName(),
                    u.getRoleName(), u.getBio(), u.getCreatedAt(), u.getAvatarFile() != null, u.getRoleNames());
        }
    }

    /** Profilo pubblico minimo: niente email, che resta privata. */
    public record UserSummary(String username, String displayName) {
    }

    /** Profilo visibile a tutti: niente email. */
    public record PublicProfile(String username, String displayName, String role, String bio,
                                java.time.Instant createdAt, long connections, long likesReceived,
                                long comments, boolean hasAvatar) {
    }

    public record UpdateProfileRequest(@NotBlank @Size(max = 120) String displayName,
                                       @Size(max = 300) String bio) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).{12,}$", message = "almeno 12 caratteri, una maiuscola e un numero") String newPassword) {
    }

    public record DeleteAccountRequest(@NotBlank String password) {
    }

    /** Ruolo principale da assegnare (USER resta sempre). */
    public record ChangeRoleRequest(@NotBlank String role) {
    }

    /** Insieme completo dei ruoli di un utente. */
    public record AssignRolesRequest(@NotNull java.util.List<String> roles) {
    }

    public record RoleRequest(@NotBlank @Size(max = 30) String name, @Size(max = 200) String description) {
    }

    public record UserStats(long connections, long likesReceived, long comments,
                            long favorites, long messagesSent, long messagesReceived) {
    }

    public record NotificationResponse(Long id, String type, String actor, Long connectionId,
                                       String preview, boolean read, java.time.Instant createdAt) {

        public static NotificationResponse from(com.atlante.model.Notification n) {
            return new NotificationResponse(n.getId(), n.getType().name(), n.getActorUsername(),
                    n.getConnectionId(), n.getPreview(), n.isRead(), n.getCreatedAt());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

        public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(),
                    p.getTotalElements(), p.getTotalPages());
        }
    }

    public record AuthResponse(String token, String username, String email,
                               String displayName, String role) {
    }

    public record WorkRequest(@NotNull WorkType type,
                              @NotBlank String title,
                              String creator,
                              Integer year,
                              String imageUrl,
                              String description,
                              String palette) {
    }

    public record WorkResponse(Long id, WorkType type, String title, String creator,
                               Integer year, String imageUrl, ExternalSource externalSource,
                               String externalId, String description, String palette) {

        public static WorkResponse from(Work w) {
            return new WorkResponse(w.getId(), w.getType(), w.getTitle(), w.getCreator(),
                    w.getYear(), w.getImageUrl(), w.getExternalSource(), w.getExternalId(),
                    w.getDescription(), w.getPalette());
        }
    }

    public record ConnectionRequest(@NotNull Long sourceWorkId,
                                    @NotNull Long targetWorkId,
                                    @NotBlank String categorySlug,
                                    @NotBlank @Size(max = 3000) String criticalNote,
                                    String sourceImageUrl,
                                    String targetImageUrl,
                                    @Pattern(regexp = PALETTE, message = "da 3 a 8 colori esadecimali separati da virgola") String sourcePalette,
                                    @Pattern(regexp = PALETTE, message = "da 3 a 8 colori esadecimali separati da virgola") String targetPalette) {
        /** "#1a2b3c,#445566,…": da 3 a 8 colori. */
        public static final String PALETTE = "^#[0-9a-fA-F]{6}(,#[0-9a-fA-F]{6}){2,7}$";
    }

    public record ConnectionResponse(Long id, WorkResponse source, WorkResponse target,
                                     String category, String categorySlug, String criticalNote,
                                     String authorName, String authorUsername, String authorRole,
                                     java.time.Instant createdAt, long viewCount,
                                     long likeCount, boolean likedByMe, long commentCount,
                                     String sourceImageUrl, String targetImageUrl,
                                     String sourcePalette, String targetPalette) {

        public static ConnectionResponse from(Connection c, long likeCount, boolean likedByMe, long commentCount) {
            return new ConnectionResponse(c.getId(),
                    WorkResponse.from(c.getSource()), WorkResponse.from(c.getTarget()),
                    c.getCategory().getName(), c.getCategory().getSlug(), c.getCriticalNote(),
                    c.getAuthor() != null ? c.getAuthor().getDisplayName() : null,
                    c.getAuthor() != null ? c.getAuthor().getUsername() : null,
                    c.getAuthor() != null ? c.getAuthor().getRoleName() : null,
                    c.getCreatedAt(), c.getViewCount(), likeCount, likedByMe, commentCount,
                    c.getSourceImageUrl(), c.getTargetImageUrl(),
                    c.getSourcePalette(), c.getTargetPalette());
        }
    }

    public record LikeResponse(long likeCount, boolean likedByMe) {
    }

    public record GraphEdge(Long id, Long sourceId, Long targetId, String category,
                            String categorySlug, String criticalNote) {
    }

    public record GraphResponse(List<WorkResponse> nodes, List<GraphEdge> edges) {
    }

    public record ExternalSearchResult(String externalId, String title, String creator,
                                       Integer year, String imageUrl, String source) {
    }

    public record AffinityResponse(WorkResponse work, int similarity) {
    }

    public record CommentRequest(@NotBlank String text) {
    }

    public record CommentResponse(Long id, String text, String authorName, String authorUsername,
                                  java.time.Instant createdAt, java.time.Instant editedAt) {

        public static CommentResponse from(com.atlante.model.Comment c) {
            return new CommentResponse(c.getId(), c.getText(),
                    c.getAuthor().getDisplayName(), c.getAuthor().getUsername(),
                    c.getCreatedAt(), c.getEditedAt());
        }
    }

    public record AiAnalysisResponse(List<String> filmThemes, List<String> artworkThemes,
                                     String comparison) {
    }

    public record AiSuggestion(String title, String creator, Integer year,
                               String linkType, String reason, String imageUrl) {
    }

    public record ExternalImportRequest(String source,
                                        String externalId,
                                        @NotNull WorkType type,
                                        @NotBlank String title,
                                        String creator,
                                        Integer year,
                                        String imageUrl) {
    }

    public record AiDiscoveryResponse(String queryTitle, String queryCreator,
                                      Integer queryYear, String queryImageUrl,
                                      String queryPalette, List<AiSuggestion> suggestions) {
    }
}
