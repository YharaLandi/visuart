package com.atlante.controller;

import com.atlante.dto.Dtos.UserResponse;
import com.atlante.service.AvatarService;
import com.atlante.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/** Foto profilo: caricamento e rimozione per sé, lettura pubblica per chiunque. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarService avatarService;
    private final UserService userService;

    /** Senza foto risponde 204: il client mostra l'iniziale senza un errore in console. */
    @GetMapping("/{username}/avatar")
    public ResponseEntity<Resource> avatar(@PathVariable String username) {
        return avatarService.find(username)
                .<ResponseEntity<Resource>>map(path -> ResponseEntity.ok()
                        .contentType(MediaTypeFactory.getMediaType(path.getFileName().toString())
                                .orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                        .body(new FileSystemResource(path)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public UserResponse upload(@RequestParam("file") MultipartFile file,
                               @AuthenticationPrincipal UserDetails principal) {
        return UserResponse.from(avatarService.upload(userService.findByUsername(principal.getUsername()), file));
    }

    @DeleteMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    public UserResponse remove(@AuthenticationPrincipal UserDetails principal) {
        return UserResponse.from(avatarService.remove(userService.findByUsername(principal.getUsername())));
    }
}
