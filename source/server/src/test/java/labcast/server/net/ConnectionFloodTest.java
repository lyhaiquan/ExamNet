package labcast.server.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import labcast.common.protocol.Frame;
import labcast.common.protocol.FrameCodec;
import labcast.common.protocol.MessageType;
import org.junit.jupiter.api.Test;

/** Gửi dồn quá 500 thông điệp/s suốt 5 s → {@code BYE(TOO_MANY_MESSAGES)} rồi đóng (spec §12). */
class ConnectionFloodTest {

    /**
     * Một kết nối thật qua localhost, đồng hồ giả tăng {@code msPerFrame} mỗi frame đọc được — gửi 6 000
     * frame là "giả" được nhiều giây mà test chạy trong chớp mắt.
     */
    record KetQua(int handled, boolean flooded, Frame last) {}

    static KetQua gui(int frames, long msPerFrame) throws Exception {
        AtomicLong clock = new AtomicLong();
        AtomicInteger handled = new AtomicInteger();
        CountDownLatch flooded = new CountDownLatch(1);
        Router router = new Router();
        router.on(MessageType.HELLO, (c, f) -> handled.incrementAndGet());
        try (ServerSocket ss = new ServerSocket(0);
                Socket client = new Socket("127.0.0.1", ss.getLocalPort());
                Socket server = ss.accept()) {
            Connection conn = new Connection(
                    server,
                    router,
                    Runnable::run,
                    new Connection.Events() {
                        @Override
                        public void closed(Connection c) {}

                        @Override
                        public void flooded(Connection c) {
                            flooded.countDown();
                        }
                    },
                    () -> clock.addAndGet(msPerFrame));
            conn.start();
            // Gửi trên luồng riêng: server ngừng đọc giữa chừng thì luồng gửi có thể bị chặn vì đầy bộ đệm.
            Thread writer = new Thread(() -> {
                try {
                    OutputStream out = new BufferedOutputStream(client.getOutputStream());
                    Frame hello = Frame.of(MessageType.HELLO, new byte[0]);
                    for (int i = 0; i < frames; i++) {
                        FrameCodec.write(out, hello);
                    }
                    out.flush();
                } catch (Exception ignored) {
                    // server đã đóng
                }
            });
            writer.setDaemon(true);
            writer.start();
            client.setSoTimeout(5_000);
            InputStream in = client.getInputStream();
            boolean gotFlood = flooded.await(msPerFrame >= 2 ? 1 : 5, TimeUnit.SECONDS);
            Frame last = null;
            if (gotFlood) {
                last = FrameCodec.read(in);
            } else {
                writer.join(5_000);
                // đợi luồng đọc xử lý hết các frame đã gửi
                for (int i = 0; i < 100 && handled.get() < frames; i++) {
                    Thread.sleep(20);
                }
            }
            conn.close();
            return new KetQua(handled.get(), gotFlood, last);
        }
    }

    @Test
    void dong1000MoiGiay_suot5Giay_nhanByeRoiBiDong() throws Exception {
        KetQua kq = gui(8_000, 1); // 1 ms mỗi frame = 1 000 frame/giây
        assertTrue(kq.flooded(), "phải bị đóng vì gửi dồn");
        assertEquals(MessageType.BYE, kq.last().type());
        assertEquals("TOO_MANY_MESSAGES", kq.last().reader().str());
        // 4 giây đầu (4 000 frame) đã qua; tới frame thứ 501 của giây thứ 5 thì đóng
        assertEquals(4_500, kq.handled());
    }

    @Test
    void duoi500MoiGiay_khongBiDong() throws Exception {
        KetQua kq = gui(6_000, 3); // 3 ms mỗi frame ≈ 333 frame/giây, kéo dài 18 "giây"
        assertTrue(!kq.flooded(), "không được đóng");
        assertEquals(6_000, kq.handled());
    }
}
