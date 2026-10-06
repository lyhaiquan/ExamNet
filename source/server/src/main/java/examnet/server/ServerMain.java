package examnet.server;

import examnet.server.ops.BuildInfo;
import examnet.server.ops.StatusEndpoint;

/**
 * SERVER MẪU CỦA PHASE 0 — chỉ để kiểm chứng đường CI/CD và VPS từ ngày đầu.
 *
 * <p>Phase 2 thay thân hàm {@code main} bằng server thật (mở cổng 5000, vòng accept, thread
 * pool…), nhưng phải GIỮ dòng {@code StatusEndpoint.start(...)}: bước deploy dựa vào nó để biết
 * VPS đang chạy đúng commit.
 */
public final class ServerMain {

    private ServerMain() {}

    public static void main(String[] args) throws Exception {
        BuildInfo info = BuildInfo.current();
        StatusEndpoint status = StatusEndpoint.start(StatusEndpoint.DEFAULT_PORT, info);
        System.out.println("ExamNet " + info.version() + " (commit " + info.commit()
                + ") — trạng thái tại http://0.0.0.0:" + status.port() + "/version");

        // Giữ tiến trình sống; server thật ở phase 2 sẽ là vòng accept() thay cho dòng này.
        Thread.currentThread().join();
    }
}
