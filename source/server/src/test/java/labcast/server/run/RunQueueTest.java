package labcast.server.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RunQueueTest {
    static final Sandbox KHONG_DUNG = (job, onLine) -> {
        throw new AssertionError("không dùng sandbox");
    };

    /** Một worker, chặn ở việc đầu tiên tới khi test mở cửa — các việc sau nằm chờ trong hàng. */
    static final class CuaChan {
        final CountDownLatch dangChay = new CountDownLatch(1);
        final CountDownLatch mo = new CountDownLatch(1);

        String chan() throws InterruptedException {
            dangChay.countDown();
            assertTrue(mo.await(5, TimeUnit.SECONDS));
            return "chặn";
        }
    }

    static boolean biThay(CompletableFuture<?> f) {
        try {
            f.get(5, TimeUnit.SECONDS);
            return false;
        } catch (ExecutionException e) {
            return RunService.isReplaced(e.getCause());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void motNguoiGui5LuotKhiWorkerBan_chiLuotCuoiChay() throws Exception {
        try (var rs = new RunService(KHONG_DUNG, 1)) {
            var cua = new CuaChan();
            var dau = rs.submit(cua::chan);
            assertTrue(cua.dangChay.await(5, TimeUnit.SECONDS));

            List<String> daChay = new ArrayList<>();
            List<CompletableFuture<String>> luot = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                String ten = "lượt " + i;
                luot.add(rs.tryRun(7, () -> {
                    synchronized (daChay) {
                        daChay.add(ten);
                    }
                    return ten;
                }));
            }
            assertEquals(1, rs.waiting(), "mỗi người chỉ một chỗ trong hàng");
            cua.mo.countDown();
            dau.get(5, TimeUnit.SECONDS);

            assertEquals("lượt 5", luot.get(4).get(5, TimeUnit.SECONDS));
            for (int i = 0; i < 4; i++) {
                assertTrue(biThay(luot.get(i)), "lượt " + (i + 1) + " phải bị thay");
            }
            assertEquals(List.of("lượt 5"), daChay);
        }
    }

    @Test
    void luotNop_chenVao_chayTruocCacLuotChayThu() throws Exception {
        try (var rs = new RunService(KHONG_DUNG, 1)) {
            var cua = new CuaChan();
            rs.submit(cua::chan);
            assertTrue(cua.dangChay.await(5, TimeUnit.SECONDS));

            List<String> thuTu = new ArrayList<>();
            List<CompletableFuture<?>> all = new ArrayList<>();
            for (long user = 1; user <= 3; user++) {
                String ten = "chạy thử " + user;
                all.add(rs.tryRun(user, () -> ghi(thuTu, ten)));
            }
            all.add(rs.submit(() -> ghi(thuTu, "nộp")));
            cua.mo.countDown();
            for (var f : all) {
                f.get(5, TimeUnit.SECONDS);
            }
            assertEquals(List.of("nộp", "chạy thử 1", "chạy thử 2", "chạy thử 3"), thuTu);
        }
    }

    @Test
    void luotBiThay_giuChoCuTrongHang() throws Exception {
        try (var rs = new RunService(KHONG_DUNG, 1)) {
            var cua = new CuaChan();
            rs.submit(cua::chan);
            assertTrue(cua.dangChay.await(5, TimeUnit.SECONDS));

            List<String> thuTu = new ArrayList<>();
            rs.tryRun(1, () -> ghi(thuTu, "A cũ"));
            var b = rs.tryRun(2, () -> ghi(thuTu, "B"));
            var aMoi = rs.tryRun(1, () -> ghi(thuTu, "A mới"));
            cua.mo.countDown();
            aMoi.get(5, TimeUnit.SECONDS);
            b.get(5, TimeUnit.SECONDS);
            assertEquals(List.of("A mới", "B"), thuTu, "bấm lại không bị đẩy xuống cuối hàng, cũng không chen lên");
        }
    }

    @Test
    void luotDangChay_khongBiThay() throws Exception {
        try (var rs = new RunService(KHONG_DUNG, 1)) {
            var cua = new CuaChan();
            var dangChay = rs.tryRun(1, cua::chan);
            assertTrue(cua.dangChay.await(5, TimeUnit.SECONDS));
            var moi = rs.tryRun(1, () -> "mới");
            cua.mo.countDown();
            assertEquals("chặn", dangChay.get(5, TimeUnit.SECONDS));
            assertEquals("mới", moi.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void dongService_viecConChoKetThucBangLoi() throws Exception {
        var rs = new RunService(KHONG_DUNG, 1);
        var cua = new CuaChan();
        rs.submit(cua::chan);
        assertTrue(cua.dangChay.await(5, TimeUnit.SECONDS));
        var cho = rs.tryRun(1, () -> "không bao giờ");
        rs.close();
        ExecutionException e = org.junit.jupiter.api.Assertions.assertThrows(
                ExecutionException.class, () -> cho.get(5, TimeUnit.SECONDS));
        assertTrue(e.getCause() instanceof IllegalStateException, e.toString());
    }

    private static String ghi(List<String> thuTu, String ten) {
        synchronized (thuTu) {
            thuTu.add(ten);
        }
        return ten;
    }
}
