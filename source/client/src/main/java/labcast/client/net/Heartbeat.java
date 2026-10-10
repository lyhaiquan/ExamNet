package labcast.client.net;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.ProtocolException;

/**
 * Tự trả {@code HEARTBEAT_ACK(i64 echoServerTime, list<{str key, i64 value}> fields)} cho mỗi {@code
 * HEARTBEAT} server gửi. Trường mở rộng (spec §17.3) thêm bằng {@link #addField}, ví dụ Người 4 gửi
 * {@code lastCastSeq} để server biết máy nào không nhận được multicast.
 */
public final class Heartbeat {
    private final Map<String, LongSupplier> fields = new LinkedHashMap<>();

    public synchronized void addField(String key, LongSupplier value) {
        fields.put(key, value);
    }

    synchronized Frame ackFor(Frame heartbeat) throws ProtocolException {
        long echo = heartbeat.reader().i64();
        PayloadWriter w = new PayloadWriter().i64(echo).u16(fields.size());
        for (Map.Entry<String, LongSupplier> e : fields.entrySet()) {
            w.str(e.getKey()).i64(e.getValue().getAsLong());
        }
        return Frame.of(MessageType.HEARTBEAT_ACK, w.toBytes());
    }
}
