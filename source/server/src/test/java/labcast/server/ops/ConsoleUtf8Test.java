package labcast.server.ops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsoleUtf8Test {

    @Test
    void docSoBangMa_moiNgonNguWindows() {
        assertEquals(Optional.of("437"), ConsoleUtf8.parseCodePage("Active code page: 437\r\n"));
        assertEquals(Optional.of("1258"), ConsoleUtf8.parseCodePage("Trang mã hiện hoạt: 1258."));
        assertEquals(Optional.of("65001"), ConsoleUtf8.parseCodePage("Aktive Codepage: 65001."));
        assertTrue(ConsoleUtf8.parseCodePage("").isEmpty());
        assertTrue(ConsoleUtf8.parseCodePage(null).isEmpty());
    }
}
