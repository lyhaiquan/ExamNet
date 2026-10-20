package labcast.server.net;

import labcast.common.protocol.Frame;

/** Xử lý một loại thông điệp. Chạy trên pool nghiệp vụ, các frame của cùng một kết nối chạy lần lượt. */
@FunctionalInterface
public interface Handler {
    void handle(Ctx ctx, Frame f) throws Exception;
}
