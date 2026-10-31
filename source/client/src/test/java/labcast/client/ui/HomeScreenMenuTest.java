package labcast.client.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import labcast.common.protocol.Role;
import org.junit.jupiter.api.Test;

class HomeScreenMenuTest {
    @Test
    void admin_khongCoCodeTuDo() {
        assertFalse(HomeScreen.hasPlayground(Role.ADMIN));
    }

    @Test
    void giaoVien_vanCo() {
        assertTrue(HomeScreen.hasPlayground(Role.TEACHER));
    }

    @Test
    void hocVien_vanCo() {
        assertTrue(HomeScreen.hasPlayground(Role.STUDENT));
    }
}
