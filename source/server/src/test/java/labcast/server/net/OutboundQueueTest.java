package labcast.server.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import org.junit.jupiter.api.Test;

class OutboundQueueTest {
    private final Duration w = Duration.ofMillis(50);

    private Frame f(MessageType t) {
        return Frame.of(t, new byte[0]);
    }

    @Test
    void critical_chenLenDauHang() throws Exception {
        var q = new OutboundQueue(10);
        q.offer(f(MessageType.LESSON_DATA), Priority.NORMAL, null, w);
        q.offer(f(MessageType.QUIZ_OPEN), Priority.CRITICAL, null, w);
        assertEquals(MessageType.QUIZ_OPEN, q.take().type());
        assertEquals(MessageType.LESSON_DATA, q.take().type());
    }

    @Test
    void critical_vuotSucChua() throws Exception {
        var q = new OutboundQueue(1);
        q.offer(f(MessageType.LESSON_DATA), Priority.NORMAL, null, w);
        assertTrue(q.offer(f(MessageType.QUIZ_OPEN), Priority.CRITICAL, null, w));
        assertEquals(2, q.size());
    }

    @Test
    void conflatable_thayTaiChoKhiCungKey() throws Exception {
        var q = new OutboundQueue(10);
        q.offer(new Frame(MessageType.CLASS_STATE, (byte) 0, 0, new byte[] {1}), Priority.CONFLATABLE, "lop", w);
        q.offer(new Frame(MessageType.CLASS_STATE, (byte) 0, 0, new byte[] {2}), Priority.CONFLATABLE, "lop", w);
        assertEquals(1, q.size());
        assertArrayEquals(new byte[] {2}, q.take().payload());
    }

    @Test
    void conflatable_khacKey_khongThay() throws Exception {
        var q = new OutboundQueue(10);
        q.offer(f(MessageType.VIEW_STATE), Priority.CONFLATABLE, "view:a", w);
        q.offer(f(MessageType.VIEW_STATE), Priority.CONFLATABLE, "view:b", w);
        assertEquals(2, q.size());
    }

    @Test
    void droppable_hangDay_boCaiCuNhat() throws Exception {
        var q = new OutboundQueue(2);
        q.offer(new Frame(MessageType.CAST_DATA, (byte) 0, 1, new byte[0]), Priority.DROPPABLE, null, w);
        q.offer(new Frame(MessageType.CAST_DATA, (byte) 0, 2, new byte[0]), Priority.DROPPABLE, null, w);
        q.offer(new Frame(MessageType.CAST_DATA, (byte) 0, 3, new byte[0]), Priority.DROPPABLE, null, w);
        assertEquals(2, q.take().seq());
        assertEquals(3, q.take().seq());
    }

    @Test
    void normal_hangDay_hetGioTraFalse() throws Exception {
        var q = new OutboundQueue(1);
        assertTrue(q.offer(f(MessageType.LESSON_DATA), Priority.NORMAL, null, w));
        assertFalse(q.offer(f(MessageType.LESSON_DATA), Priority.NORMAL, null, w));
    }

    @Test
    void normal_hangDay_choCoChoThiVao() throws Exception {
        var q = new OutboundQueue(1);
        q.offer(f(MessageType.LESSON_DATA), Priority.NORMAL, null, w);
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(30);
                q.take();
            } catch (InterruptedException ignored) {
                // test
            }
        });
        t.start();
        assertTrue(q.offer(f(MessageType.LESSON_LIST), Priority.NORMAL, null, Duration.ofSeconds(2)));
        t.join();
    }

    @Test
    void dropPending_xoaDungKey() throws Exception {
        var q = new OutboundQueue(10);
        for (int i = 0; i < 5; i++) {
            q.offer(f(MessageType.MIRROR_DELTA), Priority.NORMAL, "mirror:7", w);
        }
        q.offer(f(MessageType.LESSON_DATA), Priority.NORMAL, null, w);
        assertEquals(5, q.pending("mirror:7"));
        assertEquals(5, q.dropPending("mirror:7"));
        assertEquals(1, q.size());
        assertEquals(0, q.pending("mirror:7"));
    }

    @Test
    void poll_hetGioTraNull() throws Exception {
        assertNull(new OutboundQueue(1).poll(20, TimeUnit.MILLISECONDS));
    }
}
