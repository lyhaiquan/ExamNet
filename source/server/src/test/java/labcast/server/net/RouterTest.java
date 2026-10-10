package labcast.server.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Socket;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.ErrorMessage;
import labcast.common.protocol.Frame;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.ProtocolException;
import labcast.common.protocol.Role;
import labcast.server.session.Session;
import org.junit.jupiter.api.Test;

class RouterTest {

    /** Kết nối chưa chạy luồng nào: frame trả về nằm trong hàng đợi để test đọc ra. */
    private static Connection ketNoiGia(Router r) {
        return new Connection(new Socket(), r, Runnable::run, Connection.Events.NONE, () -> 0L);
    }

    private static Frame f(MessageType t) {
        return Frame.of(t, new byte[0]);
    }

    private static ErrorCode loiTraVe(Connection c) throws Exception {
        Frame reply = c.queue().poll(1, TimeUnit.SECONDS);
        assertNotNull(reply, "không có frame trả về");
        assertEquals(MessageType.ERROR, reply.type());
        return ErrorMessage.from(reply).code();
    }

    @Test
    void goiDungHandlerTheoLoai() {
        var r = new Router();
        var dem = new AtomicInteger();
        r.on(MessageType.HELLO, (ctx, fr) -> dem.incrementAndGet());
        r.on(MessageType.AUTH, (ctx, fr) -> dem.addAndGet(100));
        var c = ketNoiGia(r);
        r.route(new Ctx(c, 0), f(MessageType.HELLO));
        assertEquals(1, dem.get());
    }

    @Test
    void chuaDangNhap_biTuChoi() throws Exception {
        var r = new Router();
        r.on(MessageType.QUIZ_OPEN, (ctx, fr) -> {});
        var c = ketNoiGia(r);
        r.route(new Ctx(c, 0), f(MessageType.QUIZ_OPEN));
        assertEquals(ErrorCode.AUTH_REQUIRED, loiTraVe(c));
    }

    @Test
    void hocVienGoiHandlerCuaGiaoVien_biCam() throws Exception {
        var r = new Router();
        var goi = new AtomicInteger();
        r.on(MessageType.QUIZ_OPEN, Set.of(Role.TEACHER), (ctx, fr) -> goi.incrementAndGet());
        var c = ketNoiGia(r);
        c.bindSession(new Session(1, "sv", "Sinh viên", Role.STUDENT, "t", 1));
        r.route(new Ctx(c, 0), f(MessageType.QUIZ_OPEN));
        assertEquals(ErrorCode.FORBIDDEN, loiTraVe(c));
        assertEquals(0, goi.get());

        c.bindSession(new Session(2, "gv", "Giáo viên", Role.TEACHER, "t2", 1));
        r.route(new Ctx(c, 0), f(MessageType.QUIZ_OPEN));
        assertEquals(1, goi.get());
    }

    @Test
    void handlerNemLoi_thanhErrorInternal_khongNemRaNgoai() throws Exception {
        var r = new Router();
        r.on(MessageType.HELLO, (ctx, fr) -> {
            throw new IllegalStateException("hỏng");
        });
        var c = ketNoiGia(r);
        r.route(new Ctx(c, 0), f(MessageType.HELLO));
        assertEquals(ErrorCode.INTERNAL, loiTraVe(c));
    }

    @Test
    void payloadHong_thanhBadFrame() throws Exception {
        var r = new Router();
        r.on(MessageType.HELLO, (ctx, fr) -> fr.reader().str());
        var c = ketNoiGia(r);
        r.route(new Ctx(c, 0), Frame.of(MessageType.HELLO, new byte[] {0, 9}));
        assertEquals(ErrorCode.BAD_FRAME, loiTraVe(c));
    }

    @Test
    void loaiKhongCoHandler_baoLoi() throws Exception {
        var r = new Router();
        var c = ketNoiGia(r);
        r.route(new Ctx(c, 0), f(MessageType.HELLO));
        assertEquals(ErrorCode.BAD_FRAME, loiTraVe(c));
    }

    @Test
    void dangKyTrung_laLoiLapTrinh() {
        var r = new Router();
        r.on(MessageType.HELLO, (ctx, fr) -> {});
        assertThrows(IllegalStateException.class, () -> r.on(MessageType.HELLO, (ctx, fr) -> {}));
    }

    @Test
    void policyMacDinh_choQua() {
        assertTrue(new Router().policy().check(null, MessageType.HELLO).allowed());
    }

    @Test
    void protocolExceptionGiuMaLoi() throws Exception {
        var r = new Router();
        r.on(MessageType.HELLO, (ctx, fr) -> {
            throw new ProtocolException(ErrorCode.NOT_FOUND, "không có");
        });
        var c = ketNoiGia(r);
        r.route(new Ctx(c, 0), f(MessageType.HELLO));
        assertEquals(ErrorCode.NOT_FOUND, loiTraVe(c));
    }
}
