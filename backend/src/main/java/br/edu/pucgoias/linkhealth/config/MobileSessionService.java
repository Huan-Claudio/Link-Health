package br.edu.pucgoias.linkhealth.config;

import br.edu.pucgoias.linkhealth.domain.UserAccount;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Mantém sessões temporárias do aplicativo. O token puro só é devolvido no login;
 * no servidor é guardado somente um hash SHA-256, nunca o token original.
 */
@Service
@Profile({"local", "postgres"})
public class MobileSessionService {

    private static final Duration DURATION = Duration.ofHours(12);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ConcurrentMap<String, StoredSession> sessions = new ConcurrentHashMap<>();

    public IssuedSession create(UserAccount account) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(DURATION);
        sessions.put(hash(token), new StoredSession(new MobilePrincipal(account.id(), account.role()), expiresAt));
        return new IssuedSession(token, expiresAt);
    }

    public Optional<MobilePrincipal> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = hash(token);
        StoredSession session = sessions.get(tokenHash);
        if (session == null) {
            return Optional.empty();
        }
        if (!session.expiresAt().isAfter(Instant.now())) {
            sessions.remove(tokenHash, session);
            return Optional.empty();
        }
        return Optional.of(session.principal());
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível na JVM.", exception);
        }
    }

    public record IssuedSession(String token, Instant expiresAt) {
    }

    private record StoredSession(MobilePrincipal principal, Instant expiresAt) {
    }
}
