package com.becommerce.crm.clinical.trichoscopy.live;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transmissão ao vivo da câmera de tricoscopia para uma segunda tela (tablet do
 * paciente). Fica só em memória: guarda o último quadro JPEG de cada sessão, nada
 * é gravado em banco.
 *
 * <p>Cada sessão tem duas chaves: a de envio (só o computador da clínica conhece)
 * e a de visualização (vai no link/QR do tablet e só permite ver). A sessão expira
 * sem quadros novos por {@link #IDLE_TIMEOUT} ou após {@link #MAX_DURATION}.
 */
@Component
public class LiveViewRegistry {

    static final Duration IDLE_TIMEOUT = Duration.ofMinutes(10);
    static final Duration MAX_DURATION = Duration.ofHours(4);
    static final int MAX_FRAME_BYTES = 1_500_000;
    static final int MAX_SESSIONS_PER_COMPANY = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Session> byPublishToken = new ConcurrentHashMap<>();
    private final Map<String, Session> byViewToken = new ConcurrentHashMap<>();
    private final Clock clock;

    public LiveViewRegistry() {
        this(Clock.systemUTC());
    }

    LiveViewRegistry(Clock clock) {
        this.clock = clock;
    }

    public record Tokens(String publishToken, String viewToken) {
    }

    public record Frame(byte[] jpeg, long seq) {
    }

    private static final class Session {
        final UUID companyId;
        final String publishToken;
        final String viewToken;
        final Instant createdAt;
        volatile Instant lastActivity;
        volatile Frame frame;

        Session(UUID companyId, String publishToken, String viewToken, Instant now) {
            this.companyId = companyId;
            this.publishToken = publishToken;
            this.viewToken = viewToken;
            this.createdAt = now;
            this.lastActivity = now;
        }
    }

    public Tokens open(UUID companyId) {
        purgeExpired();
        long active = byPublishToken.values().stream().filter(s -> s.companyId.equals(companyId)).count();
        if (active >= MAX_SESSIONS_PER_COMPANY) {
            byPublishToken.values().stream()
                    .filter(s -> s.companyId.equals(companyId))
                    .min((a, b) -> a.lastActivity.compareTo(b.lastActivity))
                    .ifPresent(this::remove);
        }
        Session session = new Session(companyId, newToken(), newToken(), clock.instant());
        byPublishToken.put(session.publishToken, session);
        byViewToken.put(session.viewToken, session);
        return new Tokens(session.publishToken, session.viewToken);
    }

    /** Guarda o novo quadro; false se a sessão não existe mais. */
    public boolean push(String publishToken, byte[] jpeg) {
        if (jpeg == null || jpeg.length == 0 || jpeg.length > MAX_FRAME_BYTES) {
            throw new IllegalArgumentException("Quadro inválido.");
        }
        Session session = active(byPublishToken.get(publishToken));
        if (session == null) {
            return false;
        }
        Frame previous = session.frame;
        session.frame = new Frame(jpeg, previous == null ? 1 : previous.seq() + 1);
        session.lastActivity = clock.instant();
        return true;
    }

    /** Sessão de visualização ativa? (vazio = link expirado ou encerrado). */
    public Optional<Optional<Frame>> latest(String viewToken) {
        Session session = active(byViewToken.get(viewToken));
        if (session == null) {
            return Optional.empty();
        }
        return Optional.of(Optional.ofNullable(session.frame));
    }

    public void close(String publishToken) {
        Session session = byPublishToken.get(publishToken);
        if (session != null) {
            remove(session);
        }
    }

    private Session active(Session session) {
        if (session == null) {
            return null;
        }
        Instant now = clock.instant();
        if (now.isAfter(session.lastActivity.plus(IDLE_TIMEOUT)) || now.isAfter(session.createdAt.plus(MAX_DURATION))) {
            remove(session);
            return null;
        }
        return session;
    }

    private void purgeExpired() {
        byPublishToken.values().forEach(this::active);
    }

    private void remove(Session session) {
        byPublishToken.remove(session.publishToken);
        byViewToken.remove(session.viewToken);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
