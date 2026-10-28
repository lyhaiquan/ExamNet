package labcast.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordRulesTest {
    @Test
    void kiemTraMatKhauMoi() {
        assertNull(PasswordRules.check("abcd", "abcd"));
        assertNull(PasswordRules.check("abcd", null));
        assertNotNull(PasswordRules.check("abc", "abc"));
        assertNotNull(PasswordRules.check("   ", null));
        assertNotNull(PasswordRules.check("abcd", "abce"));
        assertNotNull(PasswordRules.check("x".repeat(PasswordRules.MAX + 1), null));
    }

    @Test
    void goiYMatKhau_deDoc_quaDuocKiemTra() {
        for (int i = 0; i < 50; i++) {
            String p = PasswordRules.suggest();
            assertTrue(p.matches("[0-9]{6}"), p);
            assertNull(PasswordRules.check(p, null));
        }
        assertEquals(6, PasswordRules.suggest().length());
    }
}
