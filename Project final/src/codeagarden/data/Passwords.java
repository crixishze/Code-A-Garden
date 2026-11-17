package codeagarden.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Turns a password into a "hash" so the real password is never stored in the database.
 * hash = SHA-256(salt + password). The salt is random per player, so two players with the
 * same password still get different hashes. Checking a login = hash the typed password again
 * with the stored salt and compare.
 */
public final class Passwords {

    private static final int SALT_BYTES = 16;   // 16 bytes = 32 hex characters
    private static final SecureRandom RANDOM = new SecureRandom();

    private Passwords() {
        // Only static methods.
    }

    /** Makes a new random salt: 16 random bytes written as 32 hex characters. */
    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    /** SHA-256 of salt + password, written as 64 hex characters. */
    public static String hash(String salt, char[] password) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest((salt + new String(password)).getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is missing from this Java", e);   // every Java has it
        }
    }

    /** True if the typed password, hashed with the stored salt, gives the stored hash. */
    public static boolean matches(String salt, String storedHash, char[] password) {
        byte[] typed = hash(salt, password).getBytes(StandardCharsets.UTF_8);
        byte[] stored = storedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(typed, stored);   // compares every byte, so timing gives nothing away
    }

    // Writes each byte as two hex characters (0-9, a-f).
    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
