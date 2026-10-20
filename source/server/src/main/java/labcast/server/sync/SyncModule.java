package labcast.server.sync;

import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.server.ServerContext;
import labcast.server.ServerModule;
import labcast.server.net.Priority;

/**
 * Phía server của đồng bộ đồng hồ kiểu NTP (ĐG3). Chủ: Người 3.
 *
 * <p>{@code TIME_SYNC_REQ(i64 t1)} → {@code TIME_SYNC_RESP(i64 t1, i64 t2, i64 t3)}. {@code t2} là lúc
 * <b>luồng đọc</b> nhận frame (không tính thời gian chờ trong pool nghiệp vụ), {@code t3} là ngay
 * trước khi đưa vào hàng đợi; trả bằng CRITICAL để không xếp hàng sau dữ liệu khác.
 */
public final class SyncModule implements ServerModule {

    @Override
    public void install(ServerContext ctx) {
        ctx.router().on(MessageType.TIME_SYNC_REQ, (c, f) -> {
            long t1 = f.reader().i64();
            long t2 = c.receivedAt();
            long t3 = System.currentTimeMillis();
            c.reply(
                    Frame.of(
                            MessageType.TIME_SYNC_RESP,
                            new PayloadWriter().i64(t1).i64(t2).i64(t3).toBytes()),
                    Priority.CRITICAL);
        });
    }
}
