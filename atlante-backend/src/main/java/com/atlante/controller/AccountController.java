package com.atlante.controller;

import com.atlante.dto.Dtos.AuthResponse;
import com.atlante.dto.Dtos.ChangePasswordRequest;
import com.atlante.dto.Dtos.DeleteAccountRequest;
import com.atlante.dto.Dtos.UpdateProfileRequest;
import com.atlante.dto.Dtos.UserResponse;
import com.atlante.dto.Dtos.UserStats;
import com.atlante.model.User;
import com.atlante.service.AccountService;
import com.atlante.service.AuthService;
import com.atlante.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final AccountService accountService;
    private final AuthService authService;
    private final com.atlante.service.MailService mailService;

    private User current(UserDetails principal) {
        return userService.findByUsername(principal.getUsername());
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal UserDetails principal) {
        return UserResponse.from(current(principal));
    }

    @PutMapping
    public UserResponse update(@Valid @RequestBody UpdateProfileRequest req,
                               @AuthenticationPrincipal UserDetails principal) {
        return UserResponse.from(accountService.updateProfile(current(principal), req.displayName(), req.bio()));
    }

    /** Dopo il cambio tutte le sessioni sono revocate: si restituisce subito un token nuovo. */
    @PutMapping("/password")
    public AuthResponse changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                       @AuthenticationPrincipal UserDetails principal) {
        User user = accountService.changePassword(current(principal), req.currentPassword(), req.newPassword());
        return authService.toAuthResponse(user);
    }

    @GetMapping("/stats")
    public UserStats stats(@AuthenticationPrincipal UserDetails principal) {
        return accountService.stats(current(principal));
    }

    /** Manda per mail il riepilogo delle statistiche personali. */
    @PostMapping("/stats/email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public java.util.Map<String, String> emailStats(@AuthenticationPrincipal UserDetails principal) {
        User user = current(principal);
        mailService.sendStats(user.getEmail(), user.getDisplayName(), accountService.stats(user));
        return java.util.Map.of("message", "Riepilogo in arrivo su " + user.getEmail());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Valid @RequestBody DeleteAccountRequest req,
                       @AuthenticationPrincipal UserDetails principal) {
        accountService.deleteOwnAccount(current(principal), req.password());
    }
}
