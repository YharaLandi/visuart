package com.atlante.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import com.atlante.config.AsyncConfig;
import com.atlante.dto.Dtos.UserStats;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.web.util.HtmlUtils;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender sender;
    private final String from;
    private final String password;
    private final boolean enabled;
    private final TemplateEngine templates;
    private final String frontendUrl;

    public record Row(String label, long value) {
    }

    public MailService(JavaMailSender sender,
                       @Value("${app.mail.from}") String from,
                       @Value("${spring.mail.password:}") String password,
                       @Value("${app.mail.enabled:true}") boolean enabled,
                       TemplateEngine templates,
                       @Value("${app.frontend-url}") String frontendUrl) {
        this.sender = sender;
        this.from = from;
        this.password = password;
        this.enabled = enabled;
        this.templates = templates;
        this.frontendUrl = frontendUrl;
    }

    /** Senza MAIL_PASSWORD l'invio è disattivato (sviluppo): il contenuto finisce nel log. */
    public boolean isEnabled() {
        return enabled && password != null && !password.isBlank();
    }

    /** Report personale in HTML (template stats-email.html). Parte su un thread suo. */
    @Async(AsyncConfig.MAIL_EXECUTOR)
    public void sendStats(String to, String displayName, UserStats stats) {
        Context ctx = new Context();
        ctx.setVariable("name", displayName);
        ctx.setVariable("date", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        ctx.setVariable("link", frontendUrl);
        ctx.setVariable("rows", List.of(
                new Row("Connessioni pubblicate", stats.connections()),
                new Row("Mi piace ricevuti", stats.likesReceived()),
                new Row("Commenti scritti", stats.comments()),
                new Row("Opere in collezione", stats.favorites()),
                new Row("Messaggi inviati", stats.messagesSent()),
                new Row("Messaggi ricevuti", stats.messagesReceived())));
        String html = templates.process("stats-email", ctx);
        if (!isEnabled()) {
            log.warn("Invio mail disattivato (MAIL_PASSWORD vuota): report statistiche per {} non spedito", to);
            return;
        }
        send(to, "Il tuo riepilogo su VisuArt", html);
    }

    public void sendPasswordReset(String to, String displayName, String link, long minutes) {
        if (!isEnabled()) {
            log.warn("Invio mail disattivato (MAIL_PASSWORD vuota). Link di reset per {}: {}", to, link);
            return;
        }
        String name = HtmlUtils.htmlEscape(displayName != null ? displayName : "");
        String html = """
                <div style="font-family:sans-serif;max-width:520px">
                  <h2>VisuArt</h2>
                  <p>Ciao %s,</p>
                  <p>abbiamo ricevuto una richiesta di reimpostazione della password.</p>
                  <p><a href="%s" style="background:#55e0d2;color:#000;padding:10px 18px;
                        border-radius:999px;text-decoration:none">Scegli una nuova password</a></p>
                  <p>Il link vale %d minuti e si può usare una sola volta.
                     Se non sei stato tu, ignora questa mail: la password resta quella di prima.</p>
                </div>
                """.formatted(name, HtmlUtils.htmlEscape(link), minutes);
        send(to, "Reimposta la tua password", html);
    }

    /** Invio sincrono, per chi gestisce già il proprio ciclo (newsletter). */
    public void sendHtml(String to, String subject, String html) {
        if (!isEnabled()) {
            log.warn("Invio mail disattivato (MAIL_PASSWORD vuota): \"{}\" per {} non spedita", subject, to);
            return;
        }
        send(to, subject, html);
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            log.info("Mail \"{}\" inviata a {}", subject, to);
        } catch (MessagingException | MailException e) {
            log.error("Invio mail \"{}\" a {} fallito: {}", subject, to, e.getMessage());
        }
    }
}
