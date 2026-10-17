package com.atlante.config;

import com.atlante.security.JwtUtil;
import com.atlante.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Autentica la sessione STOMP leggendo il JWT dal frame CONNECT (header Authorization).
 * Un CONNECT senza token valido viene rifiutato con un frame ERROR.
 */
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthInterceptor.class);
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(PREFIX) || !tokenService.isActive(header.substring(PREFIX.length()))) {
            log.warn("CONNECT rifiutato: JWT mancante o non valido");
            throw new MessagingException("Autenticazione richiesta");
        }

        String username = jwtUtil.extractUsername(header.substring(PREFIX.length()));
        UserDetails user = userDetailsService.loadUserByUsername(username);
        accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()) {
            // Principal.getName() deve restituire lo username: e' la chiave dei destinatari /user/**
            @Override
            public String getName() {
                return user.getUsername();
            }
        });
        log.info("CONNECT accettato utente={}", username);
        return message;
    }
}
