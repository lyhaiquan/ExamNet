package labcast.server.net;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Chạy các việc của <b>một</b> kết nối lần lượt, theo đúng thứ tự frame đến, trên pool nghiệp vụ dùng
 * chung. Không có lớp này thì {@code AUTH} và {@code LESSON_LIST} gửi liền nhau có thể chạy song song
 * và {@code LESSON_LIST} bị từ chối vì "chưa đăng nhập".
 *
 * <p>Việc chạy lâu (chạy code học viên) phải trả {@code CompletableFuture} và chạy ở pool riêng, nếu
 * không sẽ giữ chân các frame sau của cùng kết nối.
 */
final class SerialExecutor implements Executor {
    private static final Logger LOG = Logger.getLogger(SerialExecutor.class.getName());
    /** Chạy tối đa ngần này việc rồi nhường luồng, để một kết nối dồn việc không chiếm mãi một luồng. */
    private static final int BATCH = 32;

    private final Executor pool;
    private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
    private boolean running;

    SerialExecutor(Executor pool) {
        this.pool = pool;
    }

    @Override
    public void execute(Runnable r) {
        synchronized (this) {
            tasks.addLast(r);
            if (running) {
                return;
            }
            running = true;
        }
        pool.execute(this::drain);
    }

    private void drain() {
        for (int i = 0; i < BATCH; i++) {
            Runnable r;
            synchronized (this) {
                r = tasks.pollFirst();
                if (r == null) {
                    running = false;
                    return;
                }
            }
            try {
                r.run();
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "Việc nghiệp vụ lỗi", t);
            }
        }
        pool.execute(this::drain); // còn việc: xếp lại cuối hàng của pool
    }
}
