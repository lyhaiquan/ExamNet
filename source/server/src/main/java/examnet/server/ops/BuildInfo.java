package examnet.server.ops;

import java.time.Instant;

/**
 * Bản build đang chạy: phiên bản, mã commit, thời điểm khởi động.
 *
 * <p>Mã commit lấy từ biến môi trường {@code EXAMNET_COMMIT}, do {@code deploy/Dockerfile}
 * gài vào lúc CI đóng image. Chạy trên máy mình (không qua Docker) thì giá trị là {@code dev}.
 */
public record BuildInfo(String version, String commit, Instant startedAt) {

    public static BuildInfo current() {
        String version = BuildInfo.class.getPackage().getImplementationVersion();
        String commit = System.getenv("EXAMNET_COMMIT");
        return new BuildInfo(
                version == null ? "dev" : version, commit == null || commit.isBlank() ? "dev" : commit, Instant.now());
    }

    /** JSON viết tay — ba trường cố định, không đáng kéo thêm thư viện. */
    public String toJson() {
        return "{\"service\":\"examnet-server\",\"version\":\"" + version + "\",\"commit\":\"" + commit
                + "\",\"startedAt\":\"" + startedAt + "\"}";
    }
}
