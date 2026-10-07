package labcast.server;

import labcast.server.ops.BuildInfo;
import labcast.server.ops.StatusEndpoint;

/**
 * SERVER MẪU CỦA GIAI ĐOẠN 0 — chỉ để kiểm chứng đường CI/CD và VPS từ ngày đầu.
 *
 * <p>Giai đoạn 1 thay thân hàm {@code main} bằng server thật (cổng TCP 7000, vòng accept, tìm server
 * qua UDP…), nhưng phải GIỮ dòng {@code StatusEndpoint.start(...)}: bước deploy dựa vào nó để biết
 * VPS đang chạy đúng commit.
 */
public final class ServerMain {

    private ServerMain() {}

    public static void main(String[] args) throws Exception {
        BuildInfo info = BuildInfo.current();
        StatusEndpoint status = StatusEndpoint.start(StatusEndpoint.DEFAULT_PORT, info);
        System.out.println("LabCast " + info.version() + " (commit " + info.commit()
                + ") — trạng thái tại http://0.0.0.0:" + status.port() + "/version");

        // Giữ tiến trình sống; server thật ở giai đoạn 1 sẽ là vòng accept() thay cho dòng này.
        Thread.currentThread().join();
    }
}
