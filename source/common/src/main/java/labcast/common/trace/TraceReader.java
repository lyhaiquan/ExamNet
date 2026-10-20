package labcast.common.trace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/** Đọc trace JSON từng dòng (spec §7.3). */
public final class TraceReader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TraceReader() {}

    /** Dòng trống bỏ qua; dòng hỏng → IOException ghi rõ số dòng. */
    public static Trace read(Reader r) throws IOException {
        Trace t = new Trace();
        BufferedReader br = r instanceof BufferedReader b ? b : new BufferedReader(r);
        String line;
        int no = 0;
        while ((line = br.readLine()) != null) {
            no++;
            add(t, line, no);
        }
        return t;
    }

    /** Thêm các dòng của một khúc {@code TRACE_CHUNK} vào trace đang nhận; trả chính {@code t}. */
    public static Trace append(Trace t, List<String> lines) throws IOException {
        int no = t.size();
        for (String line : lines) {
            no++;
            add(t, line, no);
        }
        return t;
    }

    public static Trace empty() {
        return new Trace();
    }

    private static void add(Trace t, String line, int no) throws IOException {
        if (line.isBlank()) {
            return;
        }
        JsonNode n;
        try {
            n = MAPPER.readTree(line);
        } catch (JsonProcessingException e) {
            throw new IOException("Trace hỏng ở dòng " + no + ": " + e.getOriginalMessage());
        }
        if (!(n instanceof ObjectNode o) || o.get("t") == null) {
            throw new IOException("Trace hỏng ở dòng " + no + ": thiếu trường \"t\"");
        }
        t.accept(new TraceEvent(o.get("t").asText(), o));
    }
}
