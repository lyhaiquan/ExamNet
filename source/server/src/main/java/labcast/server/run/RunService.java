package labcast.server.run;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Chạy code học viên trên một pool riêng (spec §17.3), để lượt chạy lâu không giữ chân luồng nghiệp
 * vụ. Giai đoạn 1: pool cố định {@code max(1, số nhân − 1)}; hàng đợi công bằng làm ở giai đoạn 2.
 */
public final class RunService implements AutoCloseable {
    private final Sandbox sandbox;
    private final ExecutorService pool;

    public RunService(Sandbox sandbox) {
        this(sandbox, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    }

    public RunService(Sandbox sandbox, int workers) {
        this.sandbox = sandbox;
        AtomicInteger n = new AtomicInteger();
        this.pool = Executors.newFixedThreadPool(workers, r -> {
            Thread t = new Thread(r, "chay-code-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    public CompletableFuture<Outcome> run(long userId, Job job, Consumer<String> onLine) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return sandbox.run(job, onLine);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new CompletionException(e);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                pool);
    }

    /** Chạy việc khác trên cùng pool (chấm bài nộp), để số tiến trình chạy code cùng lúc vẫn bị giới hạn. */
    public <T> CompletableFuture<T> submit(java.util.concurrent.Callable<T> work) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return work.call();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new CompletionException(e);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                pool);
    }

    public Sandbox sandbox() {
        return sandbox;
    }

    @Override
    public void close() {
        pool.shutdownNow();
    }
}
