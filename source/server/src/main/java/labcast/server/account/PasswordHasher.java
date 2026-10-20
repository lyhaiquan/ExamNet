package labcast.server.account;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Băm mật khẩu bằng PBKDF2-HMAC-SHA256, salt riêng từng người. Không bao giờ lưu mật khẩu thô. */
public final class PasswordHasher {
    public static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Hashed(byte[] hash, byte[] salt, int iterations) {}

    private PasswordHasher() {}

    public static Hashed hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return new Hashed(derive(password, salt, ITERATIONS), salt, ITERATIONS);
    }

    /** So bằng {@link MessageDigest#isEqual} — thời gian so không lộ đúng tới byte thứ mấy. */
    public static boolean verify(String password, Hashed stored) {
        byte[] got = derive(password, stored.salt(), stored.iterations());
        return MessageDigest.isEqual(got, stored.hash());
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JVM thiếu PBKDF2WithHmacSHA256", e);
        } finally {
            spec.clearPassword();
        }
    }
}
