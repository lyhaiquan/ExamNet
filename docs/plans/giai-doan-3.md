# Giai đoạn 3 — Trợ giúp và hoàn thiện (tuần 7–8)

**Mục tiêu:** buổi học chạy đủ 6 giai đoạn ở spec §6 với bộ demo tối thiểu (nổi bọt, trung tố → hậu tố, N-Queens n = 4, LCS, SQL GROUP BY) trên ≥ 4 máy cắm dây. Đóng gói được bộ cài cho phòng máy.

**Phụ thuộc vào giai đoạn 2:** chiếu multicast, dòng code có nhật ký và mốc lưu, chấm bài, bậc dự đoán, giới hạn tốc độ, bộ máy SQL bản đầu, sơ đồ lớp, câu hỏi nhanh.

Quy ước chung, bảng mã và ký hiệu payload: xem [README.md](README.md).

---

## Thứ tự và phụ thuộc

Cùng luật "giao diện trước" như giai đoạn 2: người cung cấp merge chữ ký và bản rỗng trong ngày 1–2.

| Giao diện | Người cung cấp, task | Cần trước ngày | Người dùng, task |
| --- | --- | --- | --- |
| `RunService.lastTrace(userId, lessonId)` | 3, task 3.20 | 1 (bản thật, việc nhỏ — làm **đầu tiên** dù đánh số cuối) | 1 (task 1.13), 4 (task 4.12) |
| `viz.cat(nhan)` và sự kiện `call`/`ret`/`prune` từ tracer | 3, task 3.17 | 4 | 1 (task 1.15, cây lời gọi N-Queens) |
| Bộ vẽ `luoi` | 3, task 3.18 | 6 | 1 (task 1.15, bàn cờ N-Queens) |
| Sự kiện `line`, `vars` từ tracer | 3, task 3.17 | 4 | 4 (task 4.13; trước đó dùng trace viết tay) |
| `CastState` (dựng lại khúc `CAST_DATA` bất kỳ) | 4, task 4.9 | 3 | 4 (task 4.11) |
| `CodeStore.onAccepted(listener)` | 1, task 1.11 | 3 | 1 (task 1.11, 1.14) |
| Kết quả chuẩn bị bài có 200 test nhỏ kèm output | 3, task 3.7 (giai đoạn 2) | đã có | 3 (task 3.16, 3.19) |

**Lịch theo ngày** (10 ngày làm việc của tuần 7–8; ngày ghi là ngày **merge**):

```text
           Tuần 7 (ngày 1–5)                          Tuần 8 (ngày 6–10)
Người 1    1.10(1–2) · 1.11(3–5)                      1.12(6) · 1.13(7–8) · 1.14(8) · 1.15(9–10)
Người 2    2.16(1–2) · 2.15(3–5)                      2.17(6–7) · 2.18(8–9) · dựng phòng thử M3(10)
Người 3    3.20(1) · 3.17(2–4) · 3.18(5–6)            3.14(7–8) · 3.15(9) · 3.16(9–10) · 3.19(10)
Người 4    4.9(1–3) · 4.10(4–5)                       4.11(6) · 4.12(7) · 4.13(8–10)
Ngày 5     Ghép thử: giơ tay → phản chiếu (1.10, 1.11) và sửa gói mất (4.9) trên 3 máy
Ngày 10    Nghiệm thu M3 trên ≥ 4 máy
```

> **Lệch tải:** Người 3 có 7 task trong 10 ngày, ba task cuối dồn vào ngày 9–10. Xem ghi chú ở [giai-doan-2.md](giai-doan-2.md#thứ-tự-và-phụ-thuộc). Nếu nhóm giữ nguyên phân công và Người 3 trễ, thứ tự lùi sang giai đoạn 4 theo spec §1 R5: task 3.15 (tự mô phỏng) rồi 3.16 (phản ví dụ). Task 3.19 (kiểm nội dung trên CI) **không lùi**, vì giai đoạn 4 đổ nội dung dựa vào nó.

---

## Người 1 — trợ giúp trực tiếp

### Task 1.10: Giơ tay và hàng chờ trợ giúp

**Files:**
- Create: `source/server/src/main/java/labcast/server/mirror/HandRaiseService.java`, `MirrorModule.java`
- Create: `source/client/src/main/java/labcast/client/mirror/HandButton.java`, `HelpQueuePane.java`
- Test: `HandRaiseServiceTest`

**Payload:** `HAND_RAISE` `str lessonId`. `HAND_LOWER` `str lessonId, i64 studentId` (học viên gửi `studentId = 0`; giáo viên hạ tay giúp thì ghi id học viên).

- Lưu bảng `hand_raises`; giáo viên hạ tay thì ghi `handled_by`, `handled_at`.
- Gọi `ClassEvents.publish(userId, lessonId, GIO_TAY / HA_TAY)` để sơ đồ lớp hiện ✋.
- Hàng chờ của giáo viên sắp theo `raised_at`, ai giơ trước lên trước.

**Test:** giơ tay hai lần liền → chỉ một dòng đang chờ; học viên hạ tay người khác → `ERROR(FORBIDDEN)`; giáo viên hạ tay → `handled_by` là id giáo viên.

**Commit:** `feat(server): giơ tay và hàng chờ trợ giúp`

### Task 1.11: Phản chiếu code có gộp thay đổi

**Files:**
- Create: `source/server/src/main/java/labcast/server/mirror/MirrorService.java`
- Modify: `source/server/src/main/java/labcast/server/journal/CodeStore.java` — thêm `onAccepted(listener)`
- Create: `source/client/src/main/java/labcast/client/mirror/MirrorPane.java`
- Test: `MirrorServiceTest`, `MirrorSlowTeacherTest`

**Payload:** `MIRROR_SUBSCRIBE` `i64 studentId, str lessonId` (chỉ giáo viên). `MIRROR_UNSUBSCRIBE` `i64 studentId`. `MIRROR_SNAPSHOT` `i64 studentId, str lessonId, i64 seq, str code`. `MIRROR_DELTA` `i64 studentId` rồi đúng các trường của `CodeDelta`.

**Cơ chế gộp** (spec §11, lớp CONFLATABLE):

```java
void forward(Connection teacher, long studentId, CodeDelta d) {
    String key = "mirror:" + studentId;
    if (teacher.queue().pending(key) > 32) {            // giáo viên không theo kịp
        teacher.queue().dropPending(key);                // bỏ các delta đang chờ
        var snap = codeStore.current(studentId, d.lessonId());
        teacher.send(snapshotFrame(studentId, snap), Priority.NORMAL, key);   // thay bằng một bản chụp
    } else {
        teacher.send(deltaFrame(studentId, d), Priority.NORMAL, key);
    }
}
```

- `CodeStore.onAccepted` gọi listener **sau** khi delta được áp liền mạch, trên luồng của `CodeStore`; `forward` chỉ đưa vào hàng đợi, không bao giờ chặn.
- App giáo viên áp `MIRROR_DELTA` bằng `TextOps` lên bản đang hiện; `seq` không liền với bản đang có → bỏ và chờ snapshot.

**Test:**
- `MirrorServiceTest`: đăng ký → nhận snapshot rồi các delta theo thứ tự; học viên đăng ký → `ERROR(FORBIDDEN)`.
- `MirrorSlowTeacherTest` (socket thật): giáo viên **không đọc** socket; học viên gửi 2 000 delta → `pending("mirror:<id>") ≤ 33` suốt quá trình; thời gian học viên nhận `CODE_ACK` không tăng quá 20% so với khi không có giáo viên; giáo viên đọc lại thì code cuối cùng khớp code học viên.

**Commit:** `feat(server): phản chiếu code tới giáo viên, gộp thành bản chụp khi giáo viên xem chậm`

### Task 1.12: Bình luận theo dòng

**Files:**
- Create: `source/server/src/main/java/labcast/server/mirror/CommentService.java`
- Create: `source/client/src/main/java/labcast/client/mirror/CommentGutter.java`
- Test: `CommentServiceTest`

**Payload:** `COMMENT_ADD` `i64 studentId, str lessonId, i32 line, str text` (chỉ giáo viên, `text` ≤ 500 ký tự). `COMMENT` `JSON{id, lessonId, line, text, author, createdAt}` — gửi cho học viên và gửi lại cho giáo viên làm xác nhận.

Ô soạn của học viên hiện dấu 💬 ở lề dòng có bình luận; rê chuột hiện nội dung. Học viên sửa code làm dòng dịch đi thì bình luận vẫn ghi dòng cũ — chấp nhận ở bản này, ghi vào giới hạn.

**Test:** bình luận lưu bảng `comments`; học viên offline lúc giáo viên bình luận → mở lại bài nhận đủ bình luận (gửi kèm khi `CHECKPOINT_FETCH` hiện tại); text > 500 ký tự → bị từ chối.

**Commit:** `feat(server): giáo viên bình luận theo dòng code của học viên`

### Task 1.13: Phiên xem chung

**Files:**
- Create: `source/server/src/main/java/labcast/server/mirror/ViewSessionService.java`
- Create: `source/client/src/main/java/labcast/client/mirror/SharedViewController.java`
- Test: `ViewSessionServiceTest`

**Payload:** `VIEW_JOIN` `str viewId` (dạng `run:<userId>:<lessonId>` — trace lần chạy thử gần nhất của học viên đó). `VIEW_CONTROL` `str viewId, str action, i32 step, i32 speedMilli` (`PLAY`, `PAUSE`, `SEEK`, `SPEED`). `VIEW_STATE` `JSON{viewId, order, step, playing, speedMilli, at}`.

- Server giữ một `ViewSession` mỗi `viewId`: thành viên, trạng thái, bộ đếm `order`.
- Lệnh từ **bất kỳ** thành viên nào được đánh số `order` tăng dần, cập nhật trạng thái, rồi phát `VIEW_STATE` cho mọi thành viên (CONFLATABLE, key `view:<viewId>`). Hai người bấm cùng lúc thì ai đến server trước được số nhỏ hơn; cả hai cùng thấy một kết quả cuối.
- Trace cho người vào sau lấy từ `RunService.lastTrace(userId, lessonId)` (Người 3, task 3.20), gửi bằng `TRACE_CHUNK`.
- App bỏ `VIEW_STATE` có `order` nhỏ hơn bản đang có.

**Test:** hai thành viên gửi `SEEK 10` và `SEEK 20` gần như cùng lúc → cả hai nhận `VIEW_STATE` cuối giống hệt nhau; người vào sau nhận trace và trạng thái hiện tại; thành viên rời phiên thì không nhận nữa.

**Commit:** `feat(server): phiên xem chung, server xếp thứ tự lệnh điều khiển`

### Task 1.14: Đánh dấu dán khối lớn

**Files:**
- Modify: `source/server/src/main/java/labcast/server/journal/CodeStore.java`
- Test: bổ sung `CodeStoreTest`

Một delta chèn > 200 ký tự → `ClassEvents.publish` kèm cờ `pasteFlag` (sơ đồ lớp hiện biểu tượng 📋). Chỉ là gợi ý cho giáo viên, không phạt (spec §8.4, §19).

**Test:** chèn 201 ký tự → có sự kiện cờ; chèn 200 → không.

**Commit:** `feat(server): đánh dấu dán khối lớn trên sơ đồ lớp`

### Task 1.15: Bộ vẽ `cay-goi`, `do-thi` và bài N-Queens

**Files:**
- Create: `source/client/src/main/java/labcast/client/views/caygoi/CallTreeModel.java`, `CallTreeView.java`
- Create: `source/client/src/main/java/labcast/client/views/dothi/GraphModel.java`, `GraphLayout.java`, `GraphView.java`
- Create: `content/dsa/quay-lui/n-queens/` (cùng Người 3: bàn cờ dùng bộ vẽ `luoi`)
- Create: `runner/viz/do_thi.py` (`DoThi`)
- Test: `CallTreeModelTest`, `GraphModelTest`, `GraphLayoutTest`, `runner/tests/test_do_thi.py`

**`cay-goi`** nhận `call` (`id`, `p` = id cha, `f`, `args`), `ret` (`id`, `x`), `prune` (`p`, `s` = nhãn nhánh bị cắt, do `viz.cat(...)` sinh — task 3.17):
- Nút đang chạy: vàng. Trả về giá trị đúng (khác `False`/`None`): xanh. Trả về mà không thành công: đỏ (quay lui). Nhánh `prune`: xám, dấu ✗.
- Cây con đã xong và không chứa lời giải thì **thu gọn** thành một nút "…".

**`do-thi`** nhận `node`, `edge` (`u`, `w`, `wt`), `mark` (`id` hoặc `e`, `s` = trạng thái); hoặc chụp biến `adj` (danh sách kề). Bố trí: đỉnh xếp vòng tròn khi ≤ 20 đỉnh; trọng số ghi giữa cạnh.

**Phía Python:** `runner/viz/do_thi.py` (Người 3 duyệt PR, vì nằm trong `runner/`):

```python
class DoThi:
    """Đồ thị gắn camera. Tạo xong sinh node + edge cho mọi đỉnh, cạnh."""
    def __init__(self, n, canh, co_huong=False, ten="g"): ...   # canh: [(u, w, trong_so), …]
    def ke(self, u): ...       # trả [(w, trong_so), …]; sinh mark u "dang-xet" và mark từng cạnh (u, w) "dang-xet"
    def danh_dau(self, u, trang_thai): ...                         # → mark đỉnh: xong | loai | trong-cay
    def danh_dau_canh(self, u, w, trang_thai): ...                 # → mark cạnh (Prim, Kruskal, đường đi ngắn nhất)
```

Bài tập đồ thị đưa `DoThi` sẵn trong `khung.py`; học viên gọi `g.ke(u)` thay cho `adj[u]` là thấy được thứ tự duyệt. Mảng `dist`, `da_tham` dùng `viz.Mang` như thường.

**Test Python:** `DoThi(3, [(0,1,1),(1,2,1)])` → 3 `node`, 2 `edge`; `g.ke(1)` → `mark 1` và 2 `mark` cạnh (đồ thị vô hướng).

**Test:** `call 1`, `call 2 (p=1)`, `ret 2 False`, `prune p=1 "cột 2"`, `call 3 (p=1)`, `ret 3 True`, `ret 1 True` → nút 2 đỏ, nút xám dưới 1, nút 3 xanh; danh sách kề `{0:[1,2],1:[2]}` → 3 đỉnh, 3 cạnh; layout 6 đỉnh → các đỉnh cách tâm bằng nhau.

**Commit:** `feat(client): bộ vẽ cây lời gọi, đồ thị và bài N-Queens`

---

## Người 4 — ĐG1 hoàn thiện

### Task 4.9: Sửa gói mất và gộp NACK

**Files:**
- Create: `source/server/src/main/java/labcast/server/cast/NackAggregator.java`
- Create: `source/server/src/main/java/labcast/server/cast/CastState.java`
- Modify: `CastModule.java`
- Test: `NackAggregatorTest`, `CastRepairTest`

**Payload:** `CAST_NACK` (TCP) `i32 castId, i32 fromSeq, i32 toSeq` (tối đa 256 gói một lần). `CAST_REPAIR` (TCP) `blob datagram` — đúng byte của datagram gốc, app đọc bằng `FrameCodec.fromBytes` rồi cho vào `SeqTracker` như gói multicast.

- `CastState` giữ trace đang chiếu (đã nén) và `firstDataSeq`, nên dựng lại được **bất kỳ** khúc `CAST_DATA` nào, kể cả khi đã trôi khỏi `RecentFrames`.
- **Gộp NACK:** đếm số máy xin cùng một `SEQ` trong cửa sổ 50 ms. Nếu ≥ 20% số máy đang trong lớp → phát lại gói đó **một lần qua multicast**; ngược lại gửi `CAST_REPAIR` qua TCP cho từng máy xin.

```java
final class NackAggregator {
    NackAggregator(int classSize, LongSupplier now);
    /** Quyết định cho từng seq: UNICAST (gửi riêng máy này), MULTICAST (phát lại cho cả lớp),
     *  NONE (đã phát lại trong cửa sổ này, máy này sẽ nhận bản đó). */
    Map<Integer, Mode> onNack(long userId, int from, int to);
    enum Mode { UNICAST, MULTICAST, NONE }
}
```

**Test** (lớp 10 máy, ngưỡng 20% = 2 máy): máy thứ nhất NACK seq 5 → `UNICAST`; máy thứ hai NACK seq 5 trong 50 ms → `MULTICAST`; máy thứ ba trong cùng cửa sổ → `NONE`; NACK seq 5 sau 50 ms → đếm lại từ đầu, `UNICAST`. `CastRepairTest`: xin khúc đã trôi khỏi vòng đệm → vẫn nhận đúng byte.

**Commit:** `feat(server): sửa gói chiếu bị mất, gộp NACK thành một lần phát lại`

### Task 4.10: Máy không nhận được multicast tự chuyển sang TCP

**Files:**
- Create: `source/server/src/main/java/labcast/server/cast/TcpFallback.java`
- Modify: `CastModule.java` — đăng ký `presence.onHeartbeat(...)`
- Modify: `source/client/src/main/java/labcast/client/cast/CastReceiver.java` — gắn `heartbeat.addField("lastCastSeq", …)`
- Test: `TcpFallbackTest`

- Đang chiếu mà một máy báo `lastCastSeq` tụt sau server quá 3 khung keyframe (≈ 6 s) trong 3 heartbeat liền, hoặc luôn là −1 → chuyển máy đó sang chế độ TCP: mọi gói chiếu cũng được đưa vào hàng đợi TCP của máy đó với cờ `FLAG_VIA_TCP` (keyframe `NORMAL`, còn lại `DROPPABLE`).
- App nhận gói từ cả hai đường; `SeqTracker` bỏ trùng theo `SEQ`.
- Ở chế độ TCP tới khi máy đó nối lại.

**Test** (heartbeat giả): máy báo −1 ba lần khi đang chiếu → được chuyển; máy chỉ chậm 1 gói → không chuyển; khi không có buổi chiếu → không chuyển ai.

**Commit:** `feat(server): máy không nhận được multicast tự chuyển sang nhận chiếu qua TCP`

### Task 4.11: Vào lớp muộn

**Files:**
- Modify: `source/client/src/main/java/labcast/client/cast/CastSession.java`
- Test: bổ sung `CastSessionTest`

Máy mở app khi đang chiếu: nhận `CAST_KEYFRAME` trong ≤ 2 s → biết `traceId`, `totalChunks`, `firstDataSeq` → xin các khúc thiếu bằng `CAST_NACK(firstDataSeq + i …)` → đủ khúc thì nạp trace và `followAt(...)` theo keyframe.

**Test:** chỉ có keyframe, chưa có khúc nào → NACK đúng dải `[firstDataSeq, firstDataSeq + totalChunks − 1]`; nhận đủ → vị trí đúng theo `anchorAt` và tốc độ.

**Commit:** `feat(client): máy vào lớp muộn bắt kịp bằng keyframe`

### Task 4.12: Chiếu bài học viên đã ẩn tên

**Files:**
- Modify: `source/client/src/main/java/labcast/client/presenter/PresenterController.java`
- Modify: `CastModule.java`
- Test: bổ sung `PresenterControllerTest`

Giáo viên chọn một học viên trên sơ đồ lớp → "Chiếu bài này (ẩn tên)" → server lấy `RunService.lastTrace(userId, lessonId)` (Người 3) → `LOAD` với `traceId = "an-danh-<số tăng dần>"`. Trace không chứa tên; server không đưa `userId` vào bất kỳ gói chiếu nào.

**Test:** gói `CAST_CONTROL(LOAD)` và `CAST_KEYFRAME` không chứa `userId` hay tên học viên (kiểm chuỗi byte).

**Commit:** `feat(server): chiếu bài của học viên lên cả lớp, đã ẩn tên`

### Task 4.13: Bộ vẽ `code-bien`

**Files:**
- Create: `source/client/src/main/java/labcast/client/views/codebien/CodeVarModel.java`, `CodeVarView.java`
- Test: `CodeVarModelTest`

Hiện code (lấy từ ô soạn hoặc `LESSON_DATA`), tô sáng dòng theo sự kiện `line`; bảng biến cập nhật theo `vars` (biến vừa đổi tô xanh dương 1 bước). Dùng cho chế độ **chạy từng dòng** của mọi bài (spec §7.2).

**Test:** `line 3`, `vars {i: 0}`, `line 4`, `vars {i: 1}` → dòng hiện tại 4, `i = 1`, `i` được đánh dấu vừa đổi.

**Commit:** `feat(client): bộ vẽ code và bảng biến cho chế độ chạy từng dòng`

---

## Người 3 — SQL đầy đủ, tự mô phỏng, phản ví dụ, tracer, kiểm nội dung

### Task 3.14: SQL — các bước còn lại

**Files:**
- Modify: `source/server/src/main/java/labcast/server/sqlviz/SqliteEngine.java`
- Create: `source/server/src/main/java/labcast/server/sqlviz/StepPlanner.java`, `Grouping.java`
- Create: `content/sql/gom-nhom/diem-trung-binh-lop/` (bài mẫu GROUP BY + HAVING, đề ở [danh-muc-bai.md](danh-muc-bai.md))
- Test: `StepPlannerTest`, `GroupingTest`

Làm đủ bảng tách bước ở spec §7.6. Chỗ khó:

- **GROUP BY:** chạy `SELECT <rowid các bảng>, <biểu thức khoá nhóm> FROM … WHERE …` rồi **gom bằng Java** (`Grouping.group(rows, keyColumns)`) để biết dòng nào thuộc nhóm nào. Không lấy được bằng cách cắt câu SQL.
- **Hàm gộp và HAVING:** chạy câu có `GROUP BY` thật để lấy giá trị (`SELECT khoá, COUNT(*), AVG(x), (biểu thức HAVING) AS __qua FROM … GROUP BY …`).
- **Truy vấn con tương quan (EXISTS):** chỉ làm animation khi bảng ngoài ≤ 30 dòng; lớn hơn thì hiện kết quả cuối kèm ghi chú.
- **Phép tập hợp:** chạy hai vế riêng → `sql.table` cho từng vế → `sql.set` (`op`: `UNION`/`INTERSECT`/`EXCEPT`, dòng giữ, dòng bỏ).
- **DML:** chụp các bảng bị đụng trước và sau trên bản sao, so theo `rowid` → `sql.diff` (thêm, sửa, xoá).
- **Ngoài phạm vi animation** (CTE, bảng dẫn xuất trong FROM, hàm cửa sổ): vẫn chạy và chấm, `explainSteps` chỉ trả `sql.table` của kết quả cuối kèm `say` giải thích.

**Test:** với bảng `sinh_vien`, `ket_qua` dựng trong test: `GROUP BY lop` ra đúng số nhóm và thành viên; `HAVING AVG(diem) > 7` loại đúng nhóm; `LEFT JOIN` có dòng `rowid` vế phải rỗng; `UPDATE … WHERE id = 2` → `sql.diff` có đúng một dòng sửa; câu có `WITH` → không lỗi, chỉ có bước kết quả.

**Commit:** `feat(server): tách đủ các bước SQL — GROUP BY, HAVING, truy vấn con, tập hợp, DML`

### Task 3.15: Bậc tự mô phỏng

**Files:**
- Create: `source/server/src/main/java/labcast/server/practice/SimService.java`, `SimScript.java`
- Create: `source/client/src/main/java/labcast/client/practice/SimPane.java`
- Test: `SimScriptTest`, `SimServiceTest`

**Payload:** `SIM_START` `str lessonId`. `SIM_STEP` server → app `JSON{nonce, stepIndex, state, choices}`; app → server `str nonce, str actionJson`. `SIM_RESULT` `JSON{ok, atStep, expected}` (`expected` chỉ gửi khi sai, để học viên học từ chỗ sai).

`SimScript.from(trace, kieu)` rút từ trace của **lời giải mẫu** trên input của đề (spec §8.3) ra chuỗi lựa chọn đúng:
- `mang` (sắp xếp): mỗi cặp `read i, read j` là một lần so sánh; lựa chọn đúng là `"doi"` nếu ngay sau đó có cặp `set` đổi chỗ i, j, ngược lại `"giu"`.
- `ngan-xep`: mỗi ký tự đọc vào là một bước; lựa chọn là chuỗi thao tác `push x` / `pop` / `xuat x` đúng thứ tự trace.

Sai một bước → `SIM_RESULT{ok:false}`, đề hết hiệu lực, muốn làm tiếp phải `SIM_START` lấy đề mới.

**Test:** trace nổi bọt `[3,1,2]` → chuỗi `doi, doi, giu`; gửi đúng hết → `ok`; sai ở bước 2 → `atStep = 2` và nonce không dùng tiếp được.

**Commit:** `feat(server): bậc tự mô phỏng, kiểm từng bước theo trace lời giải mẫu`

### Task 3.16: Tìm phản ví dụ nhỏ

**Files:**
- Create: `source/server/src/main/java/labcast/server/grade/CounterexampleFinder.java`
- Test: `CounterexampleFinderTest`

**Payload:** `HINT_REQ` `str lessonId`. `HINT_DATA` `JSON{input, expected, got}` rồi `TRACE_CHUNK` của lần chạy có tracer trên input đó.

- Chỉ mở sau 3 lần nộp sai liên tiếp; trừ 10 điểm (ghi `submissions.kind = HINT`).
- Lấy 200 test cỡ `nho` **đã sinh sẵn kèm output** ở bước chuẩn bị bài (giai đoạn 2, task 3.7), sắp theo độ dài input tăng dần, chạy code học viên lần lượt (không tracer) tới khi gặp test đầu tiên sai, tối đa 5 s. Lời giải mẫu **không** chạy cạnh code học viên.
- Có test sai → chạy lại test đó **có tracer** để học viên xem animation chỗ sai.

**Test** (sandbox giả): code sai với mảng có phần tử trùng → trả đúng test trùng nhỏ nhất; code đúng → `HINT_DATA{input: null}`; gọi khi mới sai 2 lần → `ERROR(LOCKED)`.

**Commit:** `feat(server): tìm phản ví dụ nhỏ nhất cho bài nộp sai`

### Task 3.17: Tracer — chạy từng dòng, chụp biến, cây lời gọi

**Files:**
- Create: `runner/tracer.py`
- Modify: `runner/run.py` — job thêm `line_mode`, `khung_nhin`, `cay_goi`
- Modify: `runner/viz/__init__.py` — thêm `viz.cat(nhan)`
- Test: `runner/tests/test_tracer.py`

- `sys.settrace` chỉ theo các khung của `bai_lam.py` (bỏ qua khung của `viz` và thư viện).
- `line_mode`: mỗi dòng mới → `line n`; biến cục bộ đổi giá trị so với lần trước → `vars {tên: repr rút gọn ≤ 80 ký tự}`.
- `khung_nhin` cho biến kiểu `list`/`deque`/`dict` thường (không phải `viz.*`): sau mỗi dòng, chụp biến (`copy.deepcopy`, chỉ khi ≤ 200 phần tử), so với lần chụp trước:
  - `list` dài thêm 1 ở cuối → `push`; ngắn đi 1 ở cuối → `pop`; cùng độ dài, khác ở ô i → `set i`.
  - `deque` mất phần tử đầu → `deq`.
  - `dict` có khoá mới/đổi giá trị → `set` với `k` thay cho `i`.
- `cay_goi: [ten_ham]`: sự kiện `call` (`id`, `p`, `f`, `args` dạng `repr` rút gọn) và `ret` (`id`, `x`) cho đúng các hàm đó; `viz.cat("nhãn")` → `prune` với `p` là lời gọi đang chạy.

**Test:** code 3 dòng gán `i` → đúng 3 `line` và `vars` của `i`; `st.append(1); st.append(2); st.pop()` với `khung_nhin: st` → `push, push, pop`; hàm đệ quy `f(2)` gọi `f(1)`, `f(0)` → cây `call` đúng cha con.

**Commit:** `feat(runner): chạy từng dòng, chụp biến thường và dựng cây lời gọi`

### Task 3.18: Bộ vẽ `luoi`, bài LCS

**Files:**
- Create: `source/client/src/main/java/labcast/client/views/luoi/GridModel.java`, `GridView.java`
- Create: `content/dsa/quy-hoach-dong/lcs/` (dùng `viz.Bang`)
- Test: `GridModelTest`

- Các `cell_r` đứng ngay trước một `cell_w` là **các ô phụ thuộc** của ô vừa ghi → vẽ mũi tên từ ô phụ thuộc tới ô đang ghi.
- Sự kiện `key "truy-vet"` của lời giải mẫu bật chế độ tô đường truy vết: các `cell_r` sau đó được tô xanh lá.

**Test:** `cell_r(1,1)`, `cell_r(0,1)`, `cell_w(1,2,3)` → ô (1,2) = 3, mũi tên từ (1,1) và (0,1); `cell_w` không có `cell_r` đứng trước → không mũi tên.

**Commit:** `feat(client): bộ vẽ bảng 2 chiều có mũi tên phụ thuộc và bài LCS`

### Task 3.19: Kiểm nội dung trên CI

**Files:**
- Create: `source/server/src/main/java/labcast/server/lesson/ContentCheck.java` (có `main`)
- Modify: `.github/workflows/ci.yml` — thêm bước sau khi build image runner (PR nhỏ, leader duyệt)
- Test: `ContentCheckTest`

`ContentCheck <thư mục content>` cho từng bài:

1. `lesson.yaml` hợp lệ: đủ trường bắt buộc, `kieu` thuộc danh sách bộ vẽ, `mo_khoa` trỏ tới bài có thật.
2. Chạy bước chuẩn bị (task 3.7 giai đoạn 2): `loi_giai.py` cho đúng output của mọi test cố định trong `tests/`.
3. Trace giảng giải không `truncated`.
4. Bài SQL: `dap_an.sql` qua `SqlGuard` và chạy được trên database mẫu; `schema.sql` khác bản ở `content/sql/_csdl/` mà không khai báo bảng thêm → cảnh báo (không làm hỏng CI).

Bỏ qua mọi thư mục có tên bắt đầu bằng `_` (`_mau`, `_csdl`).

In mọi lỗi kèm tên thư mục, thoát mã 1 nếu có lỗi. Bước CI:

```yaml
      - name: Kiểm nội dung bài học
        working-directory: source
        run: java -cp server/target/labcast-server.jar labcast.server.lesson.ContentCheck ../content
```

**Test:** thư mục giả có một bài sai output → báo đúng tên bài và test; bài trỏ `mo_khoa` tới bài không có → báo lỗi.

**Commit:** `ci(content): kiểm từng bài học trên CI`

### Task 3.20: `RunService.lastTrace`

**Files:**
- Modify: `source/server/src/main/java/labcast/server/run/RunService.java`
- Test: bổ sung `RunServiceTest`

```java
public Optional<List<String>> lastTrace(long userId, String lessonId);   // trace lần chạy thử gần nhất, giữ trong bộ nhớ
```

Người 1 (phiên xem chung) và Người 4 (chiếu bài học viên) dùng hàm này. Giữ tối đa 1 trace mỗi (người, bài), xoá khi phiên kết thúc.

**Commit:** `feat(server): giữ trace lần chạy gần nhất cho xem chung và chiếu`

---

## Người 2 — bảo mật, xử lý lỗi, đóng gói

### Task 2.15: TLS tuỳ chọn và ghim vân tay

**Files:**
- Create: `source/common/src/main/java/labcast/common/transport/Tls.java`
- Create: `source/client/src/main/java/labcast/client/net/PinnedTrustManager.java`
- Modify: `TcpServer`, `LcpClient`, `DiscoveryResponder`
- Create: `packaging/tao-chung-chi.bat`
- Test: `TlsTest`

- Server `--tls labcast.p12` (mật khẩu đọc từ biến môi trường `LABCAST_TLS_PASS`): mở `SSLServerSocket`, chỉ TLS 1.3. File `.p12` **không bao giờ** commit (`.gitignore` đã chặn).
- `tao-chung-chi.bat` (chạy một lần trên máy giáo viên có JDK):

```bat
@echo off
rem Tạo chứng chỉ tự ký cho chế độ TLS. Không commit file .p12 ra repo.
if "%LABCAST_TLS_PASS%"=="" (echo Hay dat bien LABCAST_TLS_PASS truoc & exit /b 1)
keytool -genkeypair -alias labcast -keyalg EC -groupname secp256r1 -validity 365 ^
  -storetype PKCS12 -keystore labcast.p12 -storepass %LABCAST_TLS_PASS% -dname "CN=LabCast"
```

- `DISCOVER_REPLY` mang `tls = true` và vân tay SHA-256 của chứng chỉ. App dùng `PinnedTrustManager`: chỉ tin chứng chỉ có đúng vân tay đó. Màn giáo viên hiện vân tay để học viên đối chiếu khi cần (gói UDP tìm server có thể bị giả mạo — ghi vào giới hạn).

**Test:** server TLS + client ghim đúng vân tay → `HELLO_ACK`; ghim sai vân tay → bắt tay thất bại; Wireshark (kiểm tay) không còn đọc được mật khẩu trong `AUTH`.

**Commit:** `feat(common): TLS 1.3 tuỳ chọn, app ghim vân tay chứng chỉ`

### Task 2.16: Rà bảng xử lý lỗi

**Files:**
- Test: `source/server/src/test/java/labcast/server/ErrorHandlingTest.java`

Mỗi dòng của spec §12 thuộc tầng mạng chung có một test:

| Tình huống | Kỳ vọng |
| --- | --- |
| TCP trả về từng byte | Frame vẫn đúng (đã có ở giai đoạn 1) |
| `MAGIC` sai, `LEN` > 8 MB, `TYPE` lạ | `ERROR(BAD_FRAME)` rồi đóng; có dòng `audit_log` |
| Im lặng quá `SO_TIMEOUT` | Đóng kết nối, phiên vẫn còn để `RESUME` |
| > 500 thông điệp/s trong 5 s | `BYE(TOO_MANY_MESSAGES)` |
| Đăng nhập hai máy | Phiên cũ nhận `BYE(LOGGED_IN_ELSEWHERE)` |
| Server khởi động lại | App tự nối lại, `RESUME_REQ` với token cũ thành công (token lưu bảng `sessions`) |

**Commit:** `test(server): kiểm các tình huống lỗi của tầng mạng chung`

### Task 2.17: Xuất tiến độ và nhật ký kiểm toán

**Files:**
- Create: `source/server/src/main/java/labcast/server/classroom/ProgressExport.java`
- Create: `source/server/src/main/java/labcast/server/store/AuditLog.java`
- Test: `ProgressExportTest`, `AuditLogTest`

- `ADMIN_PROGRESS_EXPORT` `i64 classId` → `ADMIN_OK` dạng `JSON{message, csv}` (CSV có thể dài hơn giới hạn 65 535 byte của `str`). Mỗi học viên **có trong danh sách lớp** một dòng, kể cả chưa làm bài nào.
- `AuditLog.record(actor, action, detail)` ghi: đăng nhập đúng/sai, frame hỏng, bị đá do đăng nhập nơi khác, bị đóng do spam, mọi lệnh `ADMIN_*`.

**Test:** lớp 3 học viên, một người chưa làm gì → CSV vẫn 3 dòng; ký tự tiếng Việt trong họ tên đúng UTF-8 có BOM (để Excel mở được).

**Commit:** `feat(server): xuất tiến độ lớp ra CSV và nhật ký kiểm toán`

### Task 2.18: Bộ cài cho phòng máy

**Files:**
- Create: `packaging/mo-tuong-lua.bat`
- Modify: `.github/workflows/release.yml` (đã có từ giai đoạn 0)
- Modify: `packaging/README.md`

```bat
@echo off
rem Chạy MỘT LẦN bằng quyền admin trên mỗi máy phòng lab (người quản lý phòng máy làm).
rem Mở cổng cho LabCast: TCP 7000 (phiên làm việc), UDP 7001 (chiếu), UDP 7002 (tìm server).
netsh advfirewall firewall add rule name="LabCast TCP 7000" dir=in action=allow protocol=TCP localport=7000
netsh advfirewall firewall add rule name="LabCast UDP 7001" dir=in action=allow protocol=UDP localport=7001
netsh advfirewall firewall add rule name="LabCast UDP 7002" dir=in action=allow protocol=UDP localport=7002
echo Da mo cong cho LabCast.
```

- Bản chạy thẳng (app-image) nén `.zip`: giải nén ra USB hoặc thư mục dùng chung, chạy `LabCast.exe` — dùng cho phòng máy có phần mềm đóng băng ổ cứng.
- Máy giáo viên cần thêm: Docker Desktop, image `labcast-runner:py3.12` (`docker build -t labcast-runner:py3.12 runner`, hoặc `docker load` từ file `.tar` đính kèm release), và jar server.

**Kiểm:** cài trên một máy Windows sạch (không Java) → app mở, tìm thấy server, đăng nhập được; chạy `mo-tuong-lua.bat` bằng quyền admin → máy đó nhận được multicast.

**Commit:** `build(packaging): script mở tường lửa và bản chạy thẳng cho phòng máy`

---

## Nghiệm thu M3

≥ 4 máy cắm dây chung switch (1 giáo viên + ≥ 3 học viên), chạy đúng kịch bản spec §6:

- [ ] **Giảng:** chiếu nổi bọt và trung tố → hậu tố; mọi máy khớp nhau.
- [ ] **Hỏi nhanh:** một câu, kết quả hiện ngay ở màn giáo viên.
- [ ] **Làm bài:** giao N-Queens n = 4 và LCS; học viên qua bậc dự đoán, bậc tự mô phỏng (nổi bọt), nộp code; chạy từng dòng hoạt động.
- [ ] **Theo dõi:** sơ đồ lớp đúng màu; thống kê "N bạn sai test X" đúng.
- [ ] **Trợ giúp:** học viên giơ tay → giáo viên phản chiếu, xem dòng thời gian, cùng xem animation, bình luận → học viên sửa → qua bài → sơ đồ xanh.
- [ ] **Chữa chung:** giáo viên chiếu một bài sai đã ẩn tên.
- [ ] Bài SQL GROUP BY: đủ các bước FROM → WHERE → GROUP BY → HAVING → SELECT.
- [ ] Một máy bật tường lửa chặn multicast vẫn xem chiếu được (qua TCP).
- [ ] Một máy mở app giữa lúc đang chiếu bắt kịp trong ≤ 2 s.
- [ ] Bộ cài từ GitHub Releases cài được trên máy phòng lab không có Java.
- [ ] CI xanh, bước kiểm nội dung chạy qua mọi bài hiện có.
