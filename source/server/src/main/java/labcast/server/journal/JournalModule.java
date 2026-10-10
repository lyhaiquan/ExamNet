package labcast.server.journal;

import labcast.common.protocol.CodeDelta;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.PayloadWriter;
import labcast.server.ServerContext;
import labcast.server.ServerModule;

/** Dòng code tin cậy (ĐG2). Chủ: Người 1. */
public final class JournalModule implements ServerModule {

    @Override
    public void install(ServerContext ctx) {
        // Code ban đầu lấy qua dịch vụ của module bài học; chưa có module đó thì là chuỗi rỗng.
        StarterCode starter =
                lessonId -> ctx.find(StarterCode.class).map(s -> s.of(lessonId)).orElse("");
        CodeStore store = new CodeStore(ctx.db(), starter, System::currentTimeMillis);
        ctx.register(CodeStore.class, store);
        ctx.sessions().setCodeSeqLookup(store::lastSeq);

        // CODE_DELTA(str lessonId, i64 seq, i32 pos, i32 delLen, str insText, i64 clientTs)
        //   → CODE_ACK(str lessonId, i64 lastSeq)
        ctx.router().on(MessageType.CODE_DELTA, (c, f) -> {
            CodeDelta d = CodeDelta.from(f);
            long last = store.accept(c.session().userId(), d);
            c.reply(
                    MessageType.CODE_ACK,
                    new PayloadWriter().str(d.lessonId()).i64(last).toBytes());
        });
    }
}
