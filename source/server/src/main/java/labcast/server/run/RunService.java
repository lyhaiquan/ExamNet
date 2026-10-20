package labcast.server.run;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * Hàng đợi chạy code công bằng (spec §11, task 3.11), trên các luồng riêng để lượt chạy lâu không giữ chân
 * luồng nghiệp vụ. {@code max(1, số nhân − 1)} lượt chạy cùng lúc.
 *
 * <ul>
 *   <li><b>Việc ưu tiên</b> ({@link #submit}): chấm bài nộp, chạy lời giải mẫu. Xếp hàng trước mọi lượt chạy
 *       thử, không bao giờ bị thay.
 *   <li><b>Chạy thử</b> ({@link #run}, {@link #tryRun}): mỗi người tối đa <b>một</b> lượt đang chờ. Lượt mới
 *       thay lượt cũ còn chờ (giữ chỗ trong hàng của lượt cũ); lượt cũ kết thúc bằng {@link Replaced}. Một
 *       người bấm Chạy thử 50 lần cũng chỉ chiếm một chỗ trong hàng, không đẩy lùi người khác.
 * </ul>
 */
public final class RunService implements AutoCloseable {

    /** Lượt chạy thử bị lượt mới hơn của cùng người thay khi còn đang chờ, chưa chạy. */
    public static final class Replaced extends RuntimeException {
        Replaced() {
            super("Lượt chạy thử bị lượt mới hơn thay", null, false, false);
        }
    }

    /** {@code err} của một lượt chạy thử là do bị thay (không phải lỗi sandbox). */
    public static boolean isReplaced(Throwable err) {
        return err instanceof Replaced || (err instanceof CompletionException && err.getCause() instanceof Replaced);
    }

    private static final class Task<T> {
        final Callable<T> work;
        final CompletableFuture<T> future = new CompletableFuture<>();

        Task(Callable<T> work) {
            this.work = work;
        }

        void run() {
            if (future.isDone()) {
                return;
            }
            try {
                future.complete(work.call());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(e);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }
    }

    private final Sandbox sandbox;
    private final Object lock = new Object();
    private final ArrayDeque<Task<?>> urgent = new ArrayDeque<>();
    /** Lượt chạy thử đang chờ, mỗi người một; thứ tự chèn = thứ tự hàng. */
    private final LinkedHashMap<Long, Task<?>> tries = new LinkedHashMap<>();

    private final List<Thread> workers = new ArrayList<>();
    private boolean closed;

    public RunService(Sandbox sandbox) {
        this(sandbox, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    }

    public RunService(Sandbox sandbox, int workers) {
        this.sandbox = sandbox;
        for (int i = 1; i <= workers; i++) {
            Thread t = new Thread(this::work, "chay-code-" + i);
            t.setDaemon(true);
            this.workers.add(t);
            t.start();
        }
    }

    /** Chạy thử code của học viên {@code userId} trong sandbox. */
    public CompletableFuture<Outcome> run(long userId, Job job, Consumer<String> onLine) {
        return tryRun(userId, () -> sandbox.run(job, onLine));
    }

    /** Một lượt chạy thử bất kỳ (bài SQL chạy bằng bộ máy SQL, không qua sandbox) của học viên {@code userId}. */
    public <T> CompletableFuture<T> tryRun(long userId, Callable<T> work) {
        Task<T> t = new Task<>(work);
        Task<?> old;
        synchronized (lock) {
            if (closed) {
                return CompletableFuture.failedFuture(new IllegalStateException("Server đang tắt"));
            }
            old = tries.put(userId, t); // khoá đã có thì LinkedHashMap giữ nguyên vị trí trong hàng
            lock.notify();
        }
        if (old != null) {
            old.future.completeExceptionally(new Replaced());
        }
        return t.future;
    }

    /** Việc ưu tiên (chấm bài nộp, lời giải mẫu): chạy trước mọi lượt chạy thử đang chờ. */
    public <T> CompletableFuture<T> submit(Callable<T> work) {
        Task<T> t = new Task<>(work);
        synchronized (lock) {
            if (closed) {
                return CompletableFuture.failedFuture(new IllegalStateException("Server đang tắt"));
            }
            urgent.add(t);
            lock.notify();
        }
        return t.future;
    }

    /** Số việc đang chờ (chưa chạy): ưu tiên + chạy thử. */
    public int waiting() {
        synchronized (lock) {
            return urgent.size() + tries.size();
        }
    }

    public Sandbox sandbox() {
        return sandbox;
    }

    private void work() {
        while (true) {
            Task<?> t;
            synchronized (lock) {
                while (!closed && urgent.isEmpty() && tries.isEmpty()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (closed) {
                    return;
                }
                if (!urgent.isEmpty()) {
                    t = urgent.poll();
                } else {
                    Iterator<Task<?>> it = tries.values().iterator();
                    t = it.next();
                    it.remove();
                }
            }
            t.run();
        }
    }

    /** Dừng nhận việc, ngắt các lượt đang chạy; việc còn chờ kết thúc bằng lỗi để không ai chờ mãi. */
    @Override
    public void close() {
        List<Task<?>> left = new ArrayList<>();
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            left.addAll(urgent);
            left.addAll(tries.values());
            urgent.clear();
            tries.clear();
            lock.notifyAll();
        }
        workers.forEach(Thread::interrupt);
        for (Task<?> t : left) {
            t.future.completeExceptionally(new IllegalStateException("Server đang tắt"));
        }
    }
}
