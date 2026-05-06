package com.events.platform.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Значения сидов, на которые ссылается {@link DataSeed} и документация README (ручные сценарии).
 */
public final class SeedDemoConstants {

    private SeedDemoConstants() {}

    public static final String USER_DEMO_EMAIL = "demo@example.com";
    public static final String USER_ATTENDEE_EMAIL = "attendee@example.com";
    public static final String USER_CHECKER_EMAIL = "checker@example.com";

    public static final String DEMO_PASSWORD_PLAINTEXT = "password123";

    /**
     * Сырой токен приглашения (ровно 32 символа). В SPA: {@code /#/invite?token=<этот_токен>&hostId=<id хоста>}.
     */
    public static final String CHECKER_INVITE_RAW_TOKEN = "11111111111111111111111111111111";

    /** Публичный код билета demo на upcoming «Community Meetup» — для страницы check-in. */
    public static final String TICKET_PUBLIC_CODE_UPCOMING = "A1B2C3D4E5F67890";

    /** Публичный код билета attendee на прошедшем «Past Workshop» — уже «отмечен» в сидах для CSV/отчётов. */
    public static final String TICKET_PUBLIC_CODE_PAST = "B2C3D4E5F67890A1";

    public static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
