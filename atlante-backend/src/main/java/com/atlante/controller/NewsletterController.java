package com.atlante.controller;

import com.atlante.service.NewsletterService;
import com.atlante.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;

@RestController
public class NewsletterController {

    private final NewsletterService newsletter;
    private final UserService userService;
    private final String frontendUrl;

    public NewsletterController(NewsletterService newsletter, UserService userService,
                                @Value("${app.frontend-url}") String frontendUrl) {
        this.newsletter = newsletter;
        this.userService = userService;
        this.frontendUrl = frontendUrl;
    }

    /** Link nella mail: disiscrive con un clic e lo conferma con una pagina minima. */
    @GetMapping(value = "/api/newsletter/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public String unsubscribe(@RequestParam("u") long userId, @RequestParam("t") String token) {
        String text = newsletter.unsubscribe(userId, token)
                ? "Non riceverai più la newsletter di VisuArt. Puoi riattivarla quando vuoi dalle impostazioni."
                : "Il link non è valido. Puoi gestire la newsletter dalle impostazioni del tuo account.";
        return """
                <!doctype html><html lang="it"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width"><title>Newsletter VisuArt</title></head>
                <body style="margin:0;background:#0e0d12;color:#f2efe6;font-family:Georgia,serif;display:grid;place-items:center;min-height:100vh">
                <div style="max-width:440px;padding:32px;text-align:center">
                <p style="display:inline-block;background:#f5c542;color:#141414;padding:6px 18px;font-weight:700;font-size:20px">&#9733; &#9478; VisuArt</p>
                <p style="font-size:18px;line-height:1.5">%s</p>
                <p><a href="%s" style="color:#f5c542">Torna all&rsquo;archivio</a></p>
                </div></body></html>
                """.formatted(text, HtmlUtils.htmlEscape(frontendUrl));
    }

    @GetMapping(value = "/api/admin/newsletter/preview", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public String preview(@AuthenticationPrincipal UserDetails principal) {
        return newsletter.preview(userService.findByUsername(principal.getUsername()));
    }

    /** Spedisce subito la newsletter a tutti gli iscritti, oltre all'invio automatico del lunedì. */
    @PostMapping("/api/admin/newsletter/send")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> send() {
        return Map.of("recipients", newsletter.sendToSubscribers());
    }
}
