package labcast.common.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class MessageTypeTest {

    @Test
    void maKhongTrungNhau() {
        var seen = new HashSet<Integer>();
        for (MessageType t : MessageType.values()) {
            assertTrue(seen.add(t.code()), "Trùng mã: " + t);
        }
    }

    @Test
    void fromCode_khuHoiDuocMoiLoai() throws Exception {
        for (MessageType t : MessageType.values()) {
            assertEquals(t, MessageType.fromCode(t.code()));
        }
    }

    @Test
    void maLa_nemProtocolException() {
        var ex = assertThrows(ProtocolException.class, () -> MessageType.fromCode(0xFE));
        assertEquals(ErrorCode.BAD_FRAME, ex.code());
        assertThrows(ProtocolException.class, () -> MessageType.fromCode(-1));
        assertThrows(ProtocolException.class, () -> MessageType.fromCode(300));
    }

    @Test
    void maTrongMotByte() {
        for (MessageType t : MessageType.values()) {
            assertTrue(t.code() > 0 && t.code() <= 0xFF, t.toString());
        }
    }

    @Test
    void errorCodeLa_raInternal() {
        assertEquals(ErrorCode.INTERNAL, ErrorCode.fromCode(12345));
        assertEquals(ErrorCode.NONCE_INVALID, ErrorCode.fromCode(7));
    }
}
