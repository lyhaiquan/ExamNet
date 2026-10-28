package labcast.server.journal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import labcast.common.protocol.CodeDelta;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.Language;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.common.protocol.ProtocolException;
import labcast.server.ServerContext;
import labcast.server.ServerModule;
import labcast.server.classroom.ClassEvents;
import labcast.server.session.Session;
import labcast.server.practice.PracticeModule;

/** Dòng code tin cậy (ĐG2). Chủ: Người 1. */
public final class JournalModule implements ServerModule {
    /**
     * Ghi {@code audit_log} tối đa một dòng mỗi giây cho mỗi học viên, như cờ rời
     * app.
     */
    static final long AUDIT_MIN_GAP_MS = 1_000;

    @Override
    public void install(ServerContext ctx) {
        // Code ban đầu lấy qua dịch vụ của module bài học; chưa có module đó thì là
        // chuỗi rỗng.
        StarterCode starter = lessonId -> ctx.find(StarterCode.class).map(s -> s.of(lessonId)).orElse("");
        CodeStore store = new CodeStore(ctx.db(), starter, System::currentTimeMillis);
        ctx.register(CodeStore.class, store);
        ctx.sessions().setCodeSeqLookup(store::lastSeq);

        // Dán khối lớn (task 1.14): cờ trên sơ đồ lớp và dashboard, một dòng nhật ký
        Map<Long, Long> lastAudit = new ConcurrentHashMap<>();
        ctx.find(ClassEvents.class)
                .ifPresent(events -> store.onLargeInsert(li -> {
                    Optional<Session> s = ctx.sessions().byUser(li.userId());
                    if (s.isPresent() && s.get().role().isStaff()) {
                        return; // giáo viên dán code mẫu là chuyện thường
                    }
                    String detail = li.burst()
                            ? li.chars() + " ký tự hiện ra trong " + CodeStore.BURST_MS / 1000
                                    + " s (nhanh hơn người gõ)"
                            : "chèn một khối " + li.chars() + " ký tự";
                    // khoá tài liệu theo ngôn ngữ (dsa.x#cpp): cờ vẫn gắn với bài, nộp đúng bài là
                    // xoá
                    String lessonId = Language.lessonOf(li.lessonId());
                    events.flagPaste(li.userId(), lessonId, li.chars(), detail);
                    long now = System.currentTimeMillis();
                    Long last = lastAudit.get(li.userId());
                    if (last == null || now - last >= AUDIT_MIN_GAP_MS) {
                        lastAudit.put(li.userId(), now);
                        String who = s.map(Session::username).orElse("user#" + li.userId());
                        ctx.audit().record(who, "PASTE_FLAG", li.lessonId() + ", " + detail);
                    }
                }));

        // CODE_DELTA(str docKey, i64 seq, i32 pos, i32 delLen, str insText, i64
        // clientTs)
        // → CODE_ACK(str docKey, i64 lastSeq). docKey: mã bài (Python) hay mã bài#ngôn
        // ngữ (spec đa ngôn ngữ §3)
        ctx.router().on(MessageType.CODE_DELTA, (c, f) -> {
            CodeDelta d = CodeDelta.from(f);
            PracticeModule.requireOpen(ctx, c, Language.lessonOf(d.lessonId()));
            long last = store.accept(c.session().userId(), d);
            c.reply(
                    MessageType.CODE_ACK,
                    new PayloadWriter().str(d.lessonId()).i64(last).toBytes());
        });

        // CODE_FETCH(str docKey) → CODE_DATA(str docKey, i64 seq, str code): đổi ngôn
        // ngữ trong ô code thì app
        // hỏi bản đang lưu của ngôn ngữ đó (chưa có thì là khung)
        ctx.router().on(MessageType.CODE_FETCH, (c, f) -> {
            String key = f.reader().str();
            if (Language.ofDocKey(key) == null) {
                throw new ProtocolException(ErrorCode.BAD_FRAME, "Khoá tài liệu lạ: " + key);
            }
            CodeStore.Snapshot snap = store.current(c.session().userId(), key);
            c.reply(
                    MessageType.CODE_DATA,
                    new PayloadWriter()
                            .str(key)
                            .i64(snap.seq())
                            .str(snap.code())
                            .toBytes());
        });
    }
}
