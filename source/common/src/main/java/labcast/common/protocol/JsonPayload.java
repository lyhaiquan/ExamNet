package labcast.common.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/**
 * Payload dạng JSON cho thông điệp có cấu trúc lớn, hay đổi (danh sách bài, kết quả chạy…). Frame
 * mang cờ {@link Frame#FLAG_JSON}. Thông điệp nhỏ và gửi thường xuyên dùng payload nhị phân.
 */
public final class JsonPayload {
    /** Dùng chung, an toàn đa luồng sau khi cấu hình xong. */
    public static final ObjectMapper MAPPER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonPayload() {}

    public static Frame frame(MessageType type, Object value) {
        return new Frame(type, Frame.FLAG_JSON, 0, bytes(value));
    }

    public static byte[] bytes(Object value) {
        try {
            return MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Không chuyển được sang JSON: " + value, e);
        }
    }

    public static <T> T read(Frame f, Class<T> type) throws ProtocolException {
        requireJson(f);
        try {
            return MAPPER.readValue(f.payload(), type);
        } catch (IOException e) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "JSON hỏng trong " + f.type() + ": " + e.getMessage());
        }
    }

    public static JsonNode tree(Frame f) throws ProtocolException {
        requireJson(f);
        try {
            return MAPPER.readTree(f.payload());
        } catch (IOException e) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, "JSON hỏng trong " + f.type() + ": " + e.getMessage());
        }
    }

    private static void requireJson(Frame f) throws ProtocolException {
        if (!f.has(Frame.FLAG_JSON)) {
            throw new ProtocolException(ErrorCode.BAD_FRAME, f.type() + " phải có cờ JSON");
        }
    }
}
