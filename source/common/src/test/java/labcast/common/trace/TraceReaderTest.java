package labcast.common.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class TraceReaderTest {

    private static Trace mau() throws IOException {
        try (var in = TraceReaderTest.class.getResourceAsStream("/trace/noi-bot-4.jsonl")) {
            return TraceReader.read(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    @Test
    void docFileMau_dungSoSuKienVaHeader() throws Exception {
        Trace t = mau();
        assertEquals("dsa.sap-xep.noi-bot", t.header().get("lesson").asText());
        assertEquals(16, t.events().size()); // bỏ hdr, end và dòng trống
        assertTrue(t.ended());
        assertFalse(t.truncated());
        assertEquals("a", t.events().get(2).var());
        assertEquals(1, t.events().get(4).i("x"));
        assertNull(t.events().get(1).var());
    }

    @Test
    void snapshotBefore_vaKeySteps() throws Exception {
        Trace t = mau();
        assertEquals(0, t.snapshotBefore(5));
        assertEquals(0, t.snapshotBefore(0));
        assertEquals(List.of(15), t.keySteps());
    }

    @Test
    void khongCoSnapshot_traAmMot() throws Exception {
        Trace t = TraceReader.read(new StringReader("{\"t\":\"read\",\"v\":\"a\",\"i\":0}\n"));
        assertEquals(-1, t.snapshotBefore(0));
    }

    @Test
    void endCoTruncated() throws Exception {
        Trace t = TraceReader.read(
                new StringReader("{\"t\":\"read\",\"v\":\"a\",\"i\":0}\n{\"t\":\"end\",\"truncated\":true}"));
        assertTrue(t.truncated());
    }

    @Test
    void dongHong_baoSoDong() {
        var ex = assertThrows(
                IOException.class,
                () -> TraceReader.read(new StringReader("{\"t\":\"hdr\"}\n{\"t\":\"read\"}\n{khong phai json\n")));
        assertTrue(ex.getMessage().contains("dòng 3"), ex.getMessage());
    }

    @Test
    void append_themKhuc() throws Exception {
        Trace t = TraceReader.empty();
        TraceReader.append(
                t, List.of("{\"t\":\"snap\",\"state\":{\"a\":[1]}}", "{\"t\":\"read\",\"v\":\"a\",\"i\":0}"));
        TraceReader.append(t, List.of("{\"t\":\"key\",\"s\":\"xong\"}", "{\"t\":\"end\",\"truncated\":false}"));
        assertEquals(3, t.size());
        assertEquals(List.of(2), t.keySteps());
        assertTrue(t.ended());
    }
}
