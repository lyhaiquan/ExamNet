package labcast.server.net;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import labcast.common.protocol.Frame;

/**
 * Hàng đợi ghi của một kết nối, có giới hạn và bốn lớp ưu tiên (spec §11).
 *
 * <p>Một máy học viên đọc chậm làm hàng của nó đầy; luồng nghiệp vụ gửi cho máy đó sẽ chờ tối đa
 * {@code normalWait} rồi bỏ cuộc, còn các máy khác không bị ảnh hưởng. Đó là backpressure theo từng
 * kết nối.
 */
public final class OutboundQueue {

    private static final class Entry {
        Frame frame;
        final Priority priority;
        final String key;

        Entry(Frame frame, Priority priority, String key) {
            this.frame = frame;
            this.priority = priority;
            this.key = key;
        }
    }

    private final int capacity;
    private final ArrayDeque<Entry> critical = new ArrayDeque<>();
    /** NORMAL, CONFLATABLE, DROPPABLE xếp chung theo thứ tự đến. */
    private final ArrayDeque<Entry> main = new ArrayDeque<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    public OutboundQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity phải > 0");
        }
        this.capacity = capacity;
    }

    /**
     * CRITICAL: chen lên đầu (sau các CRITICAL khác), không bao giờ bỏ, được vượt capacity. NORMAL:
     * đầy thì chờ tối đa {@code normalWait}, hết giờ trả false. CONFLATABLE: đang có frame cùng key
     * chờ gửi thì thay tại chỗ; không có thì như NORMAL. DROPPABLE: đầy thì bỏ DROPPABLE cũ nhất rồi
     * thêm vào (không còn DROPPABLE nào để bỏ thì bỏ chính frame mới, trả false).
     */
    public boolean offer(Frame f, Priority p, String key, Duration normalWait) throws InterruptedException {
        Objects.requireNonNull(f);
        lock.lock();
        try {
            switch (p) {
                case CRITICAL -> {
                    critical.addLast(new Entry(f, p, key));
                    notEmpty.signal();
                    return true;
                }
                case CONFLATABLE -> {
                    if (key != null) {
                        for (Entry e : main) {
                            if (e.priority == Priority.CONFLATABLE && key.equals(e.key)) {
                                e.frame = f;
                                return true;
                            }
                        }
                    }
                    return offerNormal(f, p, key, normalWait);
                }
                case NORMAL -> {
                    return offerNormal(f, p, key, normalWait);
                }
                case DROPPABLE -> {
                    if (main.size() >= capacity && !dropOldestDroppable()) {
                        return false;
                    }
                    main.addLast(new Entry(f, p, key));
                    notEmpty.signal();
                    return true;
                }
                default -> throw new IllegalStateException(p.toString());
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean offerNormal(Frame f, Priority p, String key, Duration wait) throws InterruptedException {
        long nanos = wait.toNanos();
        while (main.size() >= capacity) {
            if (nanos <= 0) {
                return false;
            }
            nanos = notFull.awaitNanos(nanos);
        }
        main.addLast(new Entry(f, p, key));
        notEmpty.signal();
        return true;
    }

    private boolean dropOldestDroppable() {
        for (Iterator<Entry> it = main.iterator(); it.hasNext(); ) {
            if (it.next().priority == Priority.DROPPABLE) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /** Luồng ghi gọi: chờ tới khi có frame. CRITICAL luôn ra trước. */
    public Frame take() throws InterruptedException {
        lock.lock();
        try {
            while (critical.isEmpty() && main.isEmpty()) {
                notEmpty.await();
            }
            return pollLocked();
        } finally {
            lock.unlock();
        }
    }

    /** Như {@link #take} nhưng chờ có hạn; hết giờ trả null. */
    public Frame poll(long timeout, TimeUnit unit) throws InterruptedException {
        lock.lock();
        try {
            long nanos = unit.toNanos(timeout);
            while (critical.isEmpty() && main.isEmpty()) {
                if (nanos <= 0) {
                    return null;
                }
                nanos = notEmpty.awaitNanos(nanos);
            }
            return pollLocked();
        } finally {
            lock.unlock();
        }
    }

    private Frame pollLocked() {
        Entry e = !critical.isEmpty() ? critical.pollFirst() : main.pollFirst();
        notFull.signal();
        return e.frame;
    }

    /** Số frame có khoá {@code key} đang chờ gửi (mirror dùng để biết giáo viên xem chậm). */
    public int pending(String key) {
        lock.lock();
        try {
            int n = 0;
            for (Entry e : main) {
                if (key.equals(e.key)) {
                    n++;
                }
            }
            for (Entry e : critical) {
                if (key.equals(e.key)) {
                    n++;
                }
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    /** Bỏ mọi frame có khoá {@code key} đang chờ gửi; trả số frame đã bỏ. */
    public int dropPending(String key) {
        lock.lock();
        try {
            int n = 0;
            for (Iterator<Entry> it = main.iterator(); it.hasNext(); ) {
                if (key.equals(it.next().key)) {
                    it.remove();
                    n++;
                }
            }
            if (n > 0) {
                notFull.signalAll();
            }
            return n;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return critical.size() + main.size();
        } finally {
            lock.unlock();
        }
    }
}
