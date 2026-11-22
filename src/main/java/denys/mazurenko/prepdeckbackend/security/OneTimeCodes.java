package denys.mazurenko.prepdeckbackend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * Six-digit codes for letters. Stored as SHA-256 of "<user id>:<code>": the user id keeps two
 * people's equal codes from sharing a hash. A million codes are quick to try offline, so what
 * really protects a code is its few minutes and few guesses, not the hash.
 */
public final class OneTimeCodes {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OneTimeCodes() {
    }

    public static String generate() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    public static String hash(UUID userId, String code) {
        return OpaqueTokens.hash(userId + ":" + code);
    }

    /** Compares in constant time, so response times say nothing about how close a guess was. */
    public static boolean matches(UUID userId, String code, String expectedHash) {
        byte[] actual = hash(userId, code).getBytes(StandardCharsets.US_ASCII);
        byte[] expected = expectedHash.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(actual, expected);
    }
}
