package labcast.server.practice;

import labcast.server.ServerContext;
import labcast.server.ServerModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import labcast.common.protocol.ErrorCode;
import labcast.common.protocol.JsonPayload;
import labcast.common.protocol.MessageType;
import labcast.common.protocol.Role;
import labcast.server.net.Ctx;
import labcast.server.net.Priority;

/**
 * Luật luyện tập của server trọng tài (spec §13). Hiện cài giới hạn tốc độ
 * (task 3.11); bậc dự đoán với mã
 * đề dùng một lần (task 3.10) sẽ thêm handler {@code PREDICT_*} ở đây. Chủ:
 * Người 3.
 */
public final class PracticeModule implements ServerModule {
    private PracticeService service;

    @Override
    public void install(ServerContext ctx) {
        ctx.router().setPolicy(new RateLimitPolicy(System::currentTimeMillis));
        service = new PracticeService(ctx.db());
        ctx.register(PracticeService.class, service);
        ctx.router().on(MessageType.PRACTICE_LIST, this::list);
        ctx.router().on(MessageType.PRACTICE_CREATE, Set.of(Role.TEACHER), this::create);
        ctx.router().on(MessageType.PRACTICE_DELETE, Set.of(Role.TEACHER), this::delete);
        ctx.router().on(MessageType.PRACTICE_START, this::state);
        ctx.router().on(MessageType.PRACTICE_SUBMIT, Set.of(Role.STUDENT), this::submit);
    }

    private void list(Ctx c, labcast.common.protocol.Frame ignored) throws Exception {
        long now = System.currentTimeMillis();
        ObjectNode root = JsonPayload.MAPPER.createObjectNode().put("kind", "practice-list");
        var items = root.putArray("sessions");
        for (PracticeService.Session s : service.list()) {
            if (c.session().role() == Role.STUDENT
                    && !service.studentEnrolled(s.id(), c.session().userId())) {
                continue;
            }
            ObjectNode n = items.addObject().put("id", s.id()).put("name", s.name()).put("classId", s.classId())
                    .put("startsAt", s.startsAt())
                    .put("closesAt", s.closesAt()).put("questionCount", s.lessons().size());
            if (c.session().role() == Role.STUDENT) {
                n.put("ended", service.finished(s.id(), c.session().userId()));
            } else {
                n.put("studentCount", service.studentProgress(s.id(), c.session().userId()).size());
            }
            if (c.session().role() == Role.TEACHER || s.visible(now)) {
                var lessons = n.putArray("lessons");
                s.lessons().forEach(lessons::add);
            }
        }
        // Đã sửa: Bổ sung đóng ngoặc cho vòng lặp, thêm logic trả về client và đóng
        // ngoặc method list
        c.reply(JsonPayload.frame(MessageType.PRACTICE_STATE, root), Priority.NORMAL);
    }

    private void submit(Ctx c, labcast.common.protocol.Frame f) throws Exception {
        long practiceId = f.reader().i64();
        if (service.get(practiceId) == null) {
            c.error(ErrorCode.NOT_FOUND, "Không có đợt thực hành", 0);
            return;
        }
        service.finish(practiceId, c.session().userId(), System.currentTimeMillis());
        service.clearActive(practiceId, c.session().userId());
        c.reply(JsonPayload.frame(MessageType.PRACTICE_STATE,
                JsonPayload.MAPPER.createObjectNode().put("id", practiceId).put("ended", true)), Priority.NORMAL);
    }

    private void create(Ctx c, labcast.common.protocol.Frame f) throws Exception {
        var n = JsonPayload.tree(f);
        List<String> ids = new ArrayList<>();
        n.path("lessons").forEach(x -> ids.add(x.asText()));
        long id = service.create(c.session().userId(), n.path("classId").asLong(),
                n.path("name").asText("Đợt thực hành"), ids, n.path("startsAt").asLong(),
                n.path("durationMs").asLong(), System.currentTimeMillis());
        c.reply(JsonPayload.frame(MessageType.PRACTICE_STATE,
                JsonPayload.MAPPER.createObjectNode().put("id", id)), Priority.NORMAL);
    }

    private void delete(Ctx c, labcast.common.protocol.Frame f) throws Exception {
        long practiceId = f.reader().i64();
        service.delete(practiceId, c.session().userId());
        c.reply(JsonPayload.frame(MessageType.PRACTICE_STATE,
                JsonPayload.MAPPER.createObjectNode().put("id", practiceId).put("deleted", true)),
                Priority.NORMAL);
    }

    private void state(Ctx c, labcast.common.protocol.Frame f) throws Exception {
        PracticeService.Session s = service.get(f.reader().i64());
        if (s == null) {
            c.error(ErrorCode.NOT_FOUND, "Không có đợt thực hành", 0);
            return;
        }
        long now = System.currentTimeMillis();
        if (c.session().role() == Role.STUDENT) {
            if (!service.studentEnrolled(s.id(), c.session().userId())) {
                c.error(ErrorCode.FORBIDDEN, "Bạn không thuộc lớp của đợt thực hành", 0);
                return;
            }
            if (!s.visible(now)) {
                c.error(ErrorCode.FORBIDDEN, "Chưa đến giờ thực hành", 0);
                return;
            }
        }
        if (c.session().role() == Role.STUDENT) {
            service.touch(s.id(), c.session().userId());
        }
        ObjectNode n = JsonPayload.MAPPER.createObjectNode().put("id", s.id()).put("name", s.name()).put("startsAt", s.startsAt())
                .put("closesAt", s.closesAt()).put("open", s.open(now));
        var lessons = n.putArray("lessons");
        s.lessons().forEach(lessons::add);
        if (c.session().role() == Role.TEACHER) {
            var students = n.putArray("students");
            for (PracticeService.StudentProgress p : service.studentProgress(s.id(), c.session().userId())) {
                students.addObject().put("fullName", p.fullName()).put("username", p.username())
                        .put("completed", p.completed()).put("total", p.total()).put("active", p.active())
                        .putPOJO("completedLessons", p.completedLessons());
            }
        }
        c.reply(JsonPayload.frame(MessageType.PRACTICE_STATE, n), Priority.NORMAL);
    }

    public static void requireOpen(ServerContext ctx, Ctx c, String lessonId) throws Exception {
        if (c.session().role() != Role.STUDENT)
            return;
        PracticeService service = ctx.find(PracticeService.class).orElse(null);
        if (service != null && !service.studentCanAccess(lessonId, System.currentTimeMillis())) {
            throw new labcast.common.protocol.ProtocolException(
                    ErrorCode.FORBIDDEN, "Bài thực hành chưa mở hoặc đã hết giờ");
        }
    }
}