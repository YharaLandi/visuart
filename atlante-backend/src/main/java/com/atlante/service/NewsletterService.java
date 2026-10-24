package com.atlante.service;

import com.atlante.model.Connection;
import com.atlante.model.User;
import com.atlante.model.Work;
import com.atlante.repository.ConnectionRepository;
import com.atlante.repository.LikeRepository;
import com.atlante.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Newsletter del lunedì: classifica delle connessioni più votate negli ultimi sette giorni
 * e le ultime arrivate. Va a chi non si è disiscritto; ogni mail ha il suo link di uscita firmato.
 */
@Service
public class NewsletterService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterService.class);
    private static final int CHART = 5;
    private static final ZoneId ROME = ZoneId.of("Europe/Rome");

    public record Entry(int rank, long id, String title, String pair, String author, long likes,
                        String sourceImage, String targetImage, String link) {
    }

    private final LikeRepository likes;
    private final ConnectionRepository connections;
    private final UserRepository users;
    private final MailService mail;
    private final TemplateEngine templates;
    private final String frontendUrl;
    private final String backendUrl;
    private final byte[] secret;
    private final List<String> skipDomains;

    public NewsletterService(LikeRepository likes, ConnectionRepository connections, UserRepository users,
                             MailService mail, TemplateEngine templates,
                             @Value("${app.frontend-url}") String frontendUrl,
                             @Value("${app.backend-url}") String backendUrl,
                             @Value("${app.jwt.secret}") String secret,
                             @Value("${app.newsletter.skip-domains}") List<String> skipDomains) {
        this.likes = likes;
        this.connections = connections;
        this.users = users;
        this.mail = mail;
        this.templates = templates;
        this.frontendUrl = trim(frontendUrl);
        this.backendUrl = trim(backendUrl);
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.skipDomains = skipDomains;
    }

    private static String trim(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Ogni lunedì alle 9, ora italiana. */
    @Scheduled(cron = "0 0 9 * * MON", zone = "Europe/Rome")
    public void weekly() {
        log.info("Newsletter settimanale inviata a {} iscritti", sendToSubscribers());
    }

    @Transactional(readOnly = true)
    public int sendToSubscribers() {
        // gli account dimostrativi hanno indirizzi inventati: a quelli non si scrive
        List<User> subscribers = users.findByNewsletterTrue().stream()
                .filter(u -> skipDomains.stream().noneMatch(d -> u.getEmail().toLowerCase(Locale.ROOT).endsWith("@" + d)))
                .toList();
        if (subscribers.isEmpty()) return 0;
        Context base = baseContext();
        String subject = "La settimana di VisuArt · " + base.getVariable("week");
        for (User u : subscribers) {
            mail.sendHtml(u.getEmail(), subject, render(base, u));
        }
        return subscribers.size();
    }

    /** Anteprima per l'admin: la stessa mail che riceverebbe lui. */
    @Transactional(readOnly = true)
    public String preview(User user) {
        return render(baseContext(), user);
    }

    private String render(Context base, User user) {
        Context ctx = new Context(Locale.ITALIAN);
        for (String name : base.getVariableNames()) ctx.setVariable(name, base.getVariable(name));
        ctx.setVariable("name", user.getDisplayName());
        ctx.setVariable("unsubscribe", backendUrl + "/api/newsletter/unsubscribe?u=" + user.getId() + "&t=" + token(user.getId()));
        return templates.process("newsletter-email", ctx);
    }

    private Context baseContext() {
        Instant since = Instant.now().minus(Duration.ofDays(7));
        List<Entry> chart = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        fill(chart, seen, since);
        // settimana tranquilla: la classifica si completa con le più votate di sempre
        if (chart.size() < CHART) fill(chart, seen, Instant.EPOCH);

        List<Entry> fresh = connections.findTop3ByCreatedAtAfterOrderByCreatedAtDesc(since).stream()
                .map(c -> entry(0, c, likes.countByConnectionId(c.getId()))).toList();

        LocalDate today = LocalDate.now(ROME);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMMM", Locale.ITALIAN);
        Context ctx = new Context(Locale.ITALIAN);
        ctx.setVariable("week", today.minusDays(7).format(fmt) + " – " + today.format(fmt));
        ctx.setVariable("chart", chart);
        ctx.setVariable("fresh", fresh);
        ctx.setVariable("newCount", connections.countByCreatedAtAfter(since));
        ctx.setVariable("site", frontendUrl);
        return ctx;
    }

    private void fill(List<Entry> chart, Set<Long> seen, Instant since) {
        for (Object[] row : likes.topSince(since, PageRequest.of(0, CHART * 2))) {
            Connection c = (Connection) row[0];
            if (chart.size() < CHART && seen.add(c.getId())) chart.add(entry(chart.size() + 1, c, (Long) row[1]));
        }
    }

    private Entry entry(int rank, Connection c, long likeCount) {
        Work s = c.getSource(), t = c.getTarget();
        String author = c.getAuthor() != null ? c.getAuthor().getDisplayName() : "VisuArt";
        return new Entry(rank, c.getId(), s.getTitle() + " × " + t.getTitle(),
                (s.getCreator() != null ? s.getCreator() : "") + " · " + (t.getCreator() != null ? t.getCreator() : ""),
                author, likeCount,
                image(c.getSourceImageUrl() != null ? c.getSourceImageUrl() : s.getImageUrl()),
                image(c.getTargetImageUrl() != null ? c.getTargetImageUrl() : t.getImageUrl()),
                frontendUrl + "/connessioni/" + c.getId());
    }

    /** Nelle mail servono indirizzi assoluti: le copie locali passano dal backend. */
    private String image(String url) {
        if (url == null) return null;
        return url.startsWith("/") ? backendUrl + url : url;
    }

    /** Firma del link di disiscrizione: niente login, ma nessuno può disiscrivere un altro. */
    public String token(long userId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("newsletter:" + userId).getBytes(StandardCharsets.UTF_8))).substring(0, 32);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public boolean unsubscribe(long userId, String token) {
        if (token == null || !MessageDigest.isEqual(token(userId).getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }
        return users.findById(userId).map(u -> {
            u.setNewsletter(false);
            return true;
        }).orElse(false);
    }
}
