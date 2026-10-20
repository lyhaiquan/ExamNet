package labcast.server.practice;

import labcast.server.ServerContext;
import labcast.server.ServerModule;

/**
 * Luật luyện tập của server trọng tài (spec §13). Hiện cài giới hạn tốc độ (task 3.11); bậc dự đoán với mã
 * đề dùng một lần (task 3.10) sẽ thêm handler {@code PREDICT_*} ở đây. Chủ: Người 3.
 */
public final class PracticeModule implements ServerModule {

    @Override
    public void install(ServerContext ctx) {
        ctx.router().setPolicy(new RateLimitPolicy(System::currentTimeMillis));
    }
}
