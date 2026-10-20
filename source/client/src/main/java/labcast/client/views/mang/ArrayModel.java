package labcast.client.views.mang;

import java.util.ArrayList;
import java.util.List;
import labcast.common.trace.TraceEvent;

/**
 * Trạng thái của bộ vẽ mảng, thuần Java để test. Nhận sự kiện trace, trả về thay đổi cần vẽ.
 *
 * <p>Đổi chỗ trong Python ({@code a[i], a[j] = a[j], a[i]}) sinh hai lệnh {@code set} liên tiếp. Model
 * giữ lại {@code set} vừa nhận; nếu {@code set} kế tiếp ghi vào ô khác đúng giá trị cũ của ô trước
 * (và ngược lại) thì gộp thành một {@link Swap} để vẽ hai cột trượt qua nhau.
 *
 * <p>{@code push}/{@code pop}/{@code ins} (thêm, bớt một ô) và {@code all} (đổi cả mảng: {@code sort},
 * {@code reverse}…) đổi độ dài mảng; trước đây model bỏ qua nên hình lệch với dữ liệu thật.
 */
public final class ArrayModel {

    public sealed interface Change permits Read, Set, Swap, Pointer, Resized {}

    /** Tô vàng ô i (đang xét). */
    public record Read(int i) implements Change {}

    /** Ô i nhận giá trị mới. */
    public record Set(int i, int value) implements Change {}

    /** Hai ô đổi giá trị cho nhau. */
    public record Swap(int i, int j) implements Change {}

    /** Con trỏ tên {@code name} trỏ tới ô i (sự kiện {@code ptr}). */
    public record Pointer(String name, int i) implements Change {}

    /**
     * Mảng đổi độ dài hay đổi nhiều ô một lúc ({@code push}, {@code pop}, {@code ins}, {@code all}): vẽ lại cả
     * mảng. {@code i} là ô vừa thêm (tô xanh dương), −1 nếu không có.
     */
    public record Resized(int i) implements Change {}

    private record Pending(int i, int oldValue, int newValue) {}

    private final List<Integer> values = new ArrayList<>();
    private Pending pending;

    public void reset(List<Integer> init) {
        values.clear();
        values.addAll(init);
        pending = null;
    }

    public List<Change> apply(TraceEvent e) {
        List<Change> out = new ArrayList<>(2);
        switch (e.t()) {
            case "set" -> {
                int i = e.i("i");
                int x = e.i("x");
                ensureSize(i);
                int old = values.get(i);
                values.set(i, x);
                if (pending != null) {
                    Pending p = pending;
                    pending = null;
                    if (p.i != i && x == p.oldValue && p.newValue == old) {
                        out.add(new Swap(p.i, i));
                        return out;
                    }
                    out.add(new Set(p.i, p.newValue));
                }
                pending = new Pending(i, old, x);
            }
            case "read" -> {
                out.addAll(flush());
                out.add(new Read(e.i("i")));
            }
            case "ptr" -> {
                out.addAll(flush());
                out.add(new Pointer(e.text("n"), e.i("i")));
            }
            case "push" -> {
                out.addAll(flush());
                values.add(e.i("x"));
                out.add(new Resized(values.size() - 1));
            }
            case "ins" -> {
                out.addAll(flush());
                int i = Math.max(0, Math.min(values.size(), e.i("i")));
                values.add(i, e.i("x"));
                out.add(new Resized(i));
            }
            case "pop" -> {
                out.addAll(flush());
                int i = e.i("i");
                if (i >= 0 && i < values.size()) {
                    values.remove(i);
                }
                out.add(new Resized(-1));
            }
            case "all" -> {
                out.addAll(flush());
                values.clear();
                e.get("ds").forEach(x -> values.add(x.asInt()));
                out.add(new Resized(-1));
            }
            default -> out.addAll(flush());
        }
        return out;
    }

    /** {@code set} đang giữ (nếu có) thành một {@link Set}. Gọi khi hết trace. */
    public List<Change> flush() {
        if (pending == null) {
            return List.of();
        }
        Pending p = pending;
        pending = null;
        return List.of(new Set(p.i, p.newValue));
    }

    public List<Integer> values() {
        return List.copyOf(values);
    }

    private void ensureSize(int i) {
        while (values.size() <= i) {
            values.add(0);
        }
    }
}
