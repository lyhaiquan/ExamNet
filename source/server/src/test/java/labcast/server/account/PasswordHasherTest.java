package labcast.server.account;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void cungMatKhau_haiSaltKhacNhau() {
        var a = PasswordHasher.hash("matkhau");
        var b = PasswordHasher.hash("matkhau");
        assertFalse(Arrays.equals(a.salt(), b.salt()));
        assertFalse(Arrays.equals(a.hash(), b.hash()));
    }

    @Test
    void verify_dungVaSai() {
        var h = PasswordHasher.hash("Mật khẩu có dấu");
        assertTrue(PasswordHasher.verify("Mật khẩu có dấu", h));
        assertFalse(PasswordHasher.verify("mat khau co dau", h));
    }

    @Test
    void doiMotByteHash_thiSai() {
        var h = PasswordHasher.hash("abc");
        byte[] hash = h.hash().clone();
        hash[0] ^= 1;
        assertFalse(PasswordHasher.verify("abc", new PasswordHasher.Hashed(hash, h.salt(), h.iterations())));
    }
}
