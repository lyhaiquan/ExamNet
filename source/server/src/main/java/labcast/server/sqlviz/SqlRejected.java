package labcast.server.sqlviz;

/**
 * Câu SQL của học viên không chạy được: bị chặn, sai cú pháp, lỗi khi chạy, quá giờ. Lời nhắn viết cho
 * học viên đọc, hiện ở khung kết quả như lỗi Python.
 */
public final class SqlRejected extends Exception {
    public SqlRejected(String message) {
        super(message);
    }
}
