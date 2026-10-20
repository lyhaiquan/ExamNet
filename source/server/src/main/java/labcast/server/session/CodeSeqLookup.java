package labcast.server.session;

/** {@code seq} liền mạch cuối cùng của code một bài trên server; −1 nếu chưa có. Người 1 cài. */
@FunctionalInterface
public interface CodeSeqLookup {
    long lastSeq(long userId, String lessonId);

    CodeSeqLookup NONE = (u, l) -> -1;
}
