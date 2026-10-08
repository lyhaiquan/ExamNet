# Giai đoạn 2 — Lõi từng người (tuần 4–6)

**Mục tiêu:** mỗi đóng góp ĐG1–ĐG4 chạy được ở mức cơ bản; một buổi học mini trên 3 máy: giáo viên chiếu, hỏi nhanh, giao bài; học viên làm bậc dự đoán và nộp code; sơ đồ lớp đổi màu; rút dây không mất code.

**Phụ thuộc vào giai đoạn 1:** frame, router, `ServerModule`, phiên, `Presence`, `LcpClient`, `Clock` bản đầu, trace và trình phát, runner, `CodeStore`.

Quy ước chung, bảng mã và ký hiệu payload: xem [README.md](README.md).

---

## Thứ tự và phụ thuộc

**Luật giao diện trước:** ai cung cấp một giao diện cho người khác thì trong **ngày 1–2** merge một PR nhỏ chỉ gồm chữ ký hàm, cài đặt rỗng (không làm gì, hoặc trả giá trị mặc định) và một test. Người dùng gọi được ngay, không phải chờ bản thật. Bản thật thay vào sau, không đổi chữ ký.

| Giao diện | Người cung cấp, task | Bản rỗng ngày | Người dùng, task |
| --- | --- | --- | --- |
| `ClassEvents.publish(userId, lessonId, status)` | 2, task 2.12 | 1 | 3 (task 3.8), 1 (giai đoạn 3) |
| `CodeStore.checkpoint(userId, lessonId, reason, verdict)` | 1, task 1.7 | 2 | 3 (task 3.8) |
| `Presence.srtt(userId)` | 2, task 2.11 | 2 | 2 (task 2.13) |
| Kết quả chuẩn bị bài (test ẩn, đề biến thể, test nhỏ có output) | 3, task 3.7 | 3 (đọc từ file JSON viết tay) | 3 (task 3.8, 3.10), giai đoạn 3 (task 3.16, 3.19) |
| `viz.NganXep`, `viz.HangDoi` | 3, task 3.9 | 4 (bản thật, việc nhỏ) | 4 (task 4.8) |
| `Clock.serverNow()` có chỉnh dần | 3, task 3.6 | đã có bản đầu từ giai đoạn 1 | 4 (task 4.6), 2 (task 2.13) |
| `PracticeController`, `PresenterPane`, `QuizPane` | 3 (3.13), 4 (4.7), 2 (2.13) | không cần bản rỗng | 2 (task 2.14, lắp màn) |

Bản đầu của `Clock` từ giai đoạn 1 đủ để Người 4 và Người 2 bắt đầu. Task 3.6 chỉ làm cho số đo chính xác hơn, cần xong trước nghiệm thu M2.

**Lịch theo ngày** (15 ngày làm việc của tuần 4–6; ngày ghi là ngày **merge**):

```text
           Tuần 4 (ngày 1–5)               Tuần 5 (ngày 6–10)                Tuần 6 (ngày 11–15)
Người 2    2.12 rỗng(1) · 2.11(2–4)        2.13 quiz(5–9)                    2.12 thật(10–12) · 2.14(13–15)
Người 1    1.7 rỗng(2) · 1.5(1–4)          1.6(5–7) · 1.7 thật(8–10)         1.8(11–12) · 1.9(13–15)
Người 3    3.7(1–3) · 3.9(4) · 3.8(5–7)    3.10(8–9) · 3.11(10)              3.6(11) · 3.12(12–13) · 3.13(14–15)
Người 4    4.4(1–3) · 4.5(4–6)             4.6(7–9) · 4.7(10–11)             4.8(12–15)
Ngày 10    Ghép thử trên 2 máy: chiếu bài nổi bọt (4.4–4.6) và câu hỏi nhanh (2.13)
Ngày 15    Nghiệm thu M2
```

> **Lệch tải cần quyết:** Người 3 có 8 task ở giai đoạn này và 7 task ở giai đoạn 3, lại đứng đầu nguồn của nhiều người; Người 2 có 4 task mỗi giai đoạn. Lịch trên đã xếp sát cho Người 3, trễ một task là trễ M2. Đây đúng là câu hỏi còn treo "chuyển phần SQL (task 3.12, 3.13, 3.14 và bộ vẽ `bang-sql`) sang Người 4":
>
> - **Giữ nguyên:** Người 3 làm hết; nếu trễ thì SQL bản đầu (3.12, 3.13) lùi sang tuần 7, bỏ dòng SQL khỏi nghiệm thu M2.
> - **Chuyển SQL sang Người 4:** Người 3 còn 6 task ở giai đoạn này, 6 ở giai đoạn 3. Người 4 thêm 2 + 1 task. ĐG3 và ĐG1 không đổi chủ. Người 4 đã có bộ vẽ bảng (`mang`) nên làm `bang-sql` thuận tay.
>
> Nhóm chốt một trong hai **trước ngày 1** của giai đoạn này.

---

## Người 4 — ĐG1: chiếu animation qua multicast

### Task 4.4: Phát multicast phía server

**Files:**
- Create: `source/server/src/main/java/labcast/server/cast/CastSender.java`
- Create: `source/server/src/main/java/labcast/server/cast/Pacer.java`
- Create: `source/server/src/main/java/labcast/server/cast/RecentFrames.java`
- Create: `source/server/src/main/java/labcast/server/cast/CastModule.java`
- Test: `PacerTest`, `RecentFramesTest`, `CastSenderTest`

**Payload:**

| Thông điệp | Kênh | Payload |
| --- | --- | --- |
| `CAST_CONTROL` (giáo viên → server) | TCP | `str action, str traceId, i32 step, i32 speedMilli` (1000 = 1×) |
| `CAST_CONTROL` (server → lớp) | UDP | `i32 castId, str action, str traceId, i32 step, i32 speedMilli, i64 at` |
| `CAST_DATA` | UDP | `i32 castId, str traceId, i32 chunkIndex, i32 totalChunks, blob bytes` |
| `CAST_KEYFRAME` | UDP | `i32 castId, str traceId, i32 totalChunks, i32 firstDataSeq, i32 step, bool playing, i32 speedMilli, i64 anchorAt, i32 lastSeq` |

`firstDataSeq` là `SEQ` của khúc `CAST_DATA` số 0, nên khúc `i` có `SEQ = firstDataSeq + i`. Máy vào lớp muộn nhờ đó biết phải xin lại `SEQ` nào.

`action` ∈ `LOAD`, `PLAY`, `PAUSE`, `SEEK`, `SPEED`, `STOP`. `SEQ` trong header của mỗi datagram là **số thứ tự của luồng chiếu**, tăng 1 mỗi gói. `castId` ngẫu nhiên mỗi lần server khởi động, để app bỏ gói của lần chạy trước.

**Hợp đồng:**

```java
public final class CastSender implements AutoCloseable {
    public CastSender(NicInfo lan, InetSocketAddress group, Pacer pacer);   // 239.255.70.1:7001, TTL 1
    public synchronized int publish(MessageType t, byte[] payload);          // gán SEQ, ghi RecentFrames, gửi; trả SEQ
    public Optional<byte[]> recent(int seq);                                 // datagram gốc để sửa gói
    public int lastSeq();
}

final class Pacer {                      // thùng token theo byte
    Pacer(long bytesPerSecond, long burstBytes, LongSupplier nanoTime, Sleeper sleeper);
    void acquire(int bytes) throws InterruptedException;
}

final class RecentFrames {               // vòng đệm 4096 datagram gần nhất
    void put(int seq, byte[] datagram);
    Optional<byte[]> get(int seq);
}
```

- **Mỗi datagram ≤ 1400 byte.** `CAST_DATA` chia trace (đã nén Deflate) thành khúc ≤ 1300 byte.
- **Giãn nhịp** 2 MB/s, đợt tối đa 64 KB — phát dồn sẽ tràn bộ đệm nhận của máy học viên.
- `CastModule`: handler `CAST_CONTROL` chỉ cho `TEACHER`. Server đóng dấu `at = now + 300 ms` rồi phát. `LOAD` thì phát toàn bộ `CAST_DATA` trước, rồi mới phát `CAST_CONTROL(LOAD)`. Một luồng định kỳ phát `CAST_KEYFRAME` mỗi 2 s khi đang chiếu.

**Test:**
- `PacerTest` (đồng hồ giả): 2 MB/s, xin 4 MB liên tục → tổng thời gian ngủ ≈ 2 s (± 5%); xin 1 KB khi còn token → không ngủ.
- `RecentFramesTest`: đặt 5000 gói → `get(1)` rỗng (đã bị đè), `get(4999)` có; `get` số chưa gửi → rỗng.
- `CastSenderTest`: datagram dài nhất ≤ 1400 byte khi trace 200 KB; `SEQ` liên tiếp; trace 200 KB ra đúng `totalChunks` gói `CAST_DATA`.

**Commit:** `feat(server): phát chiếu qua multicast có giãn nhịp và vòng đệm sửa gói`

### Task 4.5: Nhận multicast phía app

**Files:**
- Create: `source/client/src/main/java/labcast/client/cast/SeqTracker.java`
- Create: `source/client/src/main/java/labcast/client/cast/CastReceiver.java`
- Create: `source/client/src/main/java/labcast/client/cast/LocalNic.java`
- Test: `SeqTrackerTest`, `CastReceiverLoopbackTest`

**Hợp đồng:**

```java
/** Theo dõi SEQ của một luồng chiếu: gói đến sai thứ tự được giữ lại, giao ra theo đúng thứ tự. */
public final class SeqTracker {
    public SeqTracker(int castId, int firstSeq);
    public List<byte[]> accept(int castId, int seq, byte[] datagram);   // các gói giao được ngay, theo thứ tự
    public List<int[]> gaps();                                           // các khoảng [from, to] đang thiếu
    public int lastDelivered();
}

public final class LocalNic {
    /** Card mạng chứa địa chỉ cục bộ của kết nối TCP tới server — tránh nghe nhầm card ảo (spec §9). */
    public static NetworkInterface forSocket(Socket tcp) throws SocketException;
}

public final class CastReceiver implements AutoCloseable {
    public CastReceiver(NetworkInterface nic, InetSocketAddress group, Consumer<Frame> deliver, Consumer<int[]> nack);
    public void start();
    public int lastSeq();               // gắn vào heartbeat: heartbeat.addField("lastCastSeq", receiver::lastSeq)
}
```

- Thấy lỗ hổng → chờ 30 ms (gói có thể chỉ đến muộn) → còn thiếu thì gửi `CAST_NACK(castId, from, to)` qua TCP. Gói `castId` khác → bỏ.
- `SO_RCVBUF = 1 MB`, `IP_MULTICAST_LOOP = true`, `SO_REUSEADDR = true` (để chạy nhiều app trên một máy khi đo).

**Test:**

```java
@Test
void denSaiThuTu_giaoDungThuTu() {
    var t = new SeqTracker(7, 1);
    assertEquals(1, t.accept(7, 1, b(1)).size());
    assertTrue(t.accept(7, 3, b(3)).isEmpty());         // thiếu 2
    assertArrayEquals(new int[] {2, 2}, t.gaps().get(0));
    assertEquals(2, t.accept(7, 2, b(2)).size());       // giao 2 rồi 3
    assertEquals(3, t.lastDelivered());
}

@Test
void goiCuaLanChayKhac_boQua() {
    var t = new SeqTracker(7, 1);
    assertTrue(t.accept(8, 1, b(1)).isEmpty());
}

@Test
void goiTrung_khongGiaoHaiLan() {
    var t = new SeqTracker(7, 1);
    t.accept(7, 1, b(1));
    assertTrue(t.accept(7, 1, b(1)).isEmpty());
}
```

`CastReceiverLoopbackTest`: gửi 50 gói qua multicast trên loopback, nhận đủ 50 theo thứ tự. **Bỏ qua** (`assumeTrue`) nếu máy không gửi được multicast trên loopback (một số runner CI không có route multicast).

**Commit:** `feat(client): nhận chiếu multicast trên đúng card, giao theo thứ tự, phát hiện thiếu gói`

### Task 4.6: Phát hẹn giờ và chế độ "theo giáo viên"

**Files:**
- Create: `source/client/src/main/java/labcast/client/cast/CastSession.java`
- Modify: `source/client/src/main/java/labcast/client/player/Player.java` — thêm `followAt(...)`
- Test: `CastSessionTest`, bổ sung `PlayerTest`

**Hợp đồng:**

```java
// Player
public void followAt(int step, long atServerTime, double speed, boolean playing, Clock clock);
// vị trí khi đang theo = step + floor((clock.serverNow() − atServerTime) / STEP × speed), không vượt cuối trace

public final class CastSession {
    public CastSession(Clock clock, Player player);
    public void onData(Frame castData);        // gom khúc; đủ totalChunks → giải nén → TraceReader → nạp vào player
    public void onControl(Frame castControl);
    public void onKeyframe(Frame keyframe);    // vào muộn: có trace thì followAt ngay; thiếu khúc → NACK các khúc thiếu
    public boolean following();
    public void detach();                      // học viên tự tua, ngừng theo
    public void rejoin();                      // nút "theo giáo viên": nhảy về đúng vị trí theo keyframe/điều khiển gần nhất
}
```

**Test** (đồng hồ giả): `PLAY` với `at` = now + 300 → trước 300 ms vị trí giữ nguyên, ở `at + 800 ms` tốc độ 1× → tiến 2 bước; `PAUSE` ở bước 37 → mọi lần `tick` sau đều ở 37; `detach()` rồi `seek(5)` → `rejoin()` về đúng vị trí đang chiếu; nhận keyframe trước khi có đủ `CAST_DATA` → chưa phát, liệt kê đúng khúc thiếu.

**Commit:** `feat(client): phát hẹn giờ theo đồng hồ server, tách ra và theo lại giáo viên`

### Task 4.7: Bảng điều khiển chiếu của giáo viên

**Files:**
- Create: `source/client/src/main/java/labcast/client/presenter/PresenterController.java`, `PresenterPane.java`
- Test: `PresenterControllerTest`

`PresenterController` giữ trạng thái chiếu (trace đang chọn, bước, tốc độ, đang chạy) và gửi `CAST_CONTROL` qua TCP. Nút: chọn bài (từ `LESSON_LIST`), Phát, Dừng, Tua (thanh trượt), Tốc độ (0,5×/1×/2×), Tới bước quan trọng kế. Màn giáo viên cũng chạy `Player` riêng để giáo viên thấy cùng hình với lớp.

**Test:** bấm Phát → gửi đúng `CAST_CONTROL(PLAY, traceId, step, 1000)`; kéo thanh tua tới 37 rồi thả → một `SEEK` (không gửi liên tục trong lúc kéo).

**Commit:** `feat(client): thêm bảng điều khiển chiếu cho giáo viên`

### Task 4.8: Bộ vẽ `ngan-xep`, `dslk`, `bam` và bài trung tố → hậu tố

**Files:**
- Create: `source/client/src/main/java/labcast/client/views/nganxep/StackModel.java`, `StackView.java`
- Create: `source/client/src/main/java/labcast/client/views/dslk/ListModel.java`, `ListView.java`
- Create: `source/client/src/main/java/labcast/client/views/bam/HashModel.java`, `HashView.java`
- Create: `content/dsa/ngan-xep/trung-to-hau-to/` (đủ file như bài nổi bọt; dùng `viz.NganXep` của Người 3, task 3.9)
- Create: `runner/viz/dslk.py` (`Nut`), `runner/viz/bam.py` (`BangBam`) — trong `runner/` nên Người 3 duyệt PR
- Test: `StackModelTest`, `ListModelTest`, `HashModelTest`, `runner/tests/test_dslk.py`, `runner/tests/test_bam.py`

Sự kiện: `push`, `pop`, `enq`, `deq` (ngăn xếp/hàng đợi); `link`, `unlink` (DSLK); `bucket` (băm: `{"t":"bucket","v":"h","i":3,"x":17}`). Mỗi model trả danh sách thay đổi để view vẽ, giống `ArrayModel`. Ngăn xếp vẽ đứng, hàng đợi vẽ ngang (cùng một model, khác cách bố trí).

**Phía Python** (cấu trúc gắn camera):

- `Nut(gia_tri, ten="ds")`: tạo nút → `node`; gán `.tiep = y` → `unlink` cạnh cũ (nếu có) rồi `link` tới `y`; `ten_dau = …` khai báo trong `khung_nhin` để vẽ con trỏ đầu danh sách.
- `BangBam(m, kieu="day-chuyen", ten="h")`: `them(k)`, `tim(k)`, `xoa(k)` → `bucket` (ô `i = k % m`, dây chuyền) và `read` ở các ô dò (địa chỉ mở, dò tuyến tính). Dùng cho bài giảng giải. Bài tập băm cho học viên tự viết hàm dò trên một `viz.Mang` làm bảng, nên không cần lớp này.

**Test:** `push 1, push 2, pop` → trạng thái `[1]`, thay đổi cuối là `Pop(2)`; `pop` khi rỗng → `IllegalStateException` (trace sai, báo rõ); `deq` lấy phần tử đầu. Python: `a.tiep = b; a.tiep = c` → `link a b`, `unlink a b`, `link a c`; `BangBam(7)` thêm 3 rồi 10 → cùng ô 3, hai sự kiện `bucket`.

**Commit:** `feat(client): bộ vẽ ngăn xếp, danh sách liên kết, bảng băm và bài trung tố → hậu tố`

---

## Người 1 — ĐG2: dòng code tin cậy

### Task 1.5: Nhật ký trên đĩa, ghi trước khi gửi

**Files:**
- Create: `source/client/src/main/java/labcast/client/journal/LocalJournal.java`
- Modify: `source/client/src/main/java/labcast/client/journal/JournalClient.java`
- Test: `LocalJournalTest`

**Hợp đồng:**

```java
public final class LocalJournal implements AutoCloseable {
    /** Một file mỗi (user, bài): <thư mục người dùng>/.labcast/journal/<userId>/<lessonId>.log */
    public static LocalJournal open(Path dir, long userId, String lessonId) throws IOException;
    public void append(CodeDelta d) throws IOException;   // ghi một dòng rồi FileChannel.force(false) — TRƯỚC khi gửi
    public void ackUpTo(long seq) throws IOException;     // đánh dấu; khi > 100 dòng đã ACK thì ghi lại file chỉ còn phần chưa ACK
    public List<CodeDelta> unacked();
}
```

Thứ tự bắt buộc trong `JournalClient.submit`: `journal.append(d)` → `client.send(...)`. Nếu `append` lỗi (đĩa đầy) thì vẫn gửi và hiện cảnh báo, không chặn học viên gõ.

**Test** (thư mục tạm của JUnit `@TempDir`): ghi 3 delta, đóng, mở lại → `unacked()` có 3; `ackUpTo(2)` rồi mở lại → còn 1; dòng cuối bị cắt dở (giả lập mất điện giữa lúc ghi) → bỏ dòng hỏng, giữ các dòng trước.

**Commit:** `feat(client): nhật ký thay đổi trên đĩa, ghi trước khi gửi`

### Task 1.6: Khôi phục sau rớt mạng

**Files:**
- Modify: `source/client/src/main/java/labcast/client/journal/JournalClient.java`
- Modify: `source/server/src/main/java/labcast/server/journal/JournalModule.java` — gọi `ctx.sessions().setCodeSeqLookup(store::lastSeq)`
- Test: `ResumeTest` (server thật trong test)

Luồng: `LcpClient` báo `CONNECTED` sau `RECONNECTING` → gửi `RESUME_REQ(token, lessonId, lastAckedSeq)` → nhận `RESUME_STATE(ok, serverLastSeq)` → bỏ khỏi nhật ký mọi delta `seq ≤ serverLastSeq`, gửi lại phần còn lại theo thứ tự.

**Test:** gửi delta 1–3, server nhận cả 3 nhưng ta "đánh rơi" ACK (proxy giả cắt kết nối trước khi ACK về) → nối lại → `RESUME_STATE.serverLastSeq = 3`, không gửi lại gì, code trên server đúng; trường hợp server chỉ nhận 1–2 → gửi lại 3. Proxy giả: một `ServerSocket` chuyển tiếp byte giữa app và server, có nút `cut()` đóng cả hai đầu.

**Commit:** `feat(client): gửi bù delta sau khi nối lại, không gửi trùng`

### Task 1.7: Mốc lưu và dòng thời gian code

**Files:**
- Modify: `source/server/src/main/java/labcast/server/journal/CodeStore.java` — thêm mốc lưu
- Create: `source/server/src/main/java/labcast/server/journal/CheckpointHandler.java`
- Create: `source/client/src/main/java/labcast/client/journal/LineDiff.java`
- Create: `source/client/src/main/java/labcast/client/journal/TimelinePane.java`
- Test: `CheckpointTest`, `LineDiffTest`

**Hợp đồng (bổ sung `CodeStore`):**

```java
public long checkpoint(long userId, String lessonId, String reason, String verdict);   // RUN | SUBMIT | TIMER; trả id
public List<CheckpointInfo> checkpoints(long userId, String lessonId);
public Optional<String> checkpointCode(long checkpointId);
public record CheckpointInfo(long id, long uptoSeq, String reason, String verdict, long createdAt) {}
```

- Người 3 gọi `checkpoint(…, "RUN"/"SUBMIT", verdict)` sau mỗi lần chạy thử / nộp (spec §17.3).
- Một luồng định kỳ tạo mốc `TIMER` mỗi 30 s **nếu** code đổi kể từ mốc trước.
- `current()` dựng lại từ **mốc mới nhất + các delta sau nó**, không áp từ đầu.

**Payload:** `CHECKPOINT_LIST` `str lessonId, i64 studentId` (0 = chính mình; khác 0 chỉ giáo viên được hỏi). `CHECKPOINT_FETCH` `str lessonId, i64 studentId, i64 checkpointId` (−1 = code hiện tại). `CHECKPOINT_DATA` `JSON{kind: "list", items:[{id, uptoSeq, reason, verdict, createdAt}]}` hoặc `JSON{kind: "one", id, uptoSeq, code}`.

```java
public final class LineDiff {
    public record Line(char op, String text) {}            // ' ' giữ, '-' xoá, '+' thêm
    public static List<Line> diff(String before, String after);   // LCS theo dòng
}
```

**Test:** tạo 2 mốc rồi thêm delta → `current()` đúng và chỉ áp các delta sau mốc 2 (đếm số lần gọi `TextOps.apply`); học viên hỏi `CHECKPOINT_LIST` với `studentId` của bạn khác → `ERROR(FORBIDDEN)`; `LineDiff.diff("a\nb\nc", "a\nx\nc")` → `[' a', '-b', '+x', ' c']`.

**Commit:** `feat(server): mốc lưu code, dòng thời gian và so sánh hai mốc`

### Task 1.8: Đổi máy làm tiếp

**Files:**
- Modify: `source/client/src/main/java/labcast/client/journal/JournalClient.java`
- Test: `DoiMayTest`

Khi mở một bài: gửi `CHECKPOINT_FETCH(lessonId, 0, −1)` → nạp `code` vào ô soạn, đặt `DeltaBatcher` bắt đầu từ `uptoSeq + 1`. Nhật ký trên đĩa của máy này (nếu có, từ lần trước) có delta `seq > uptoSeq` thì gửi tiếp; `seq ≤ uptoSeq` thì xoá.

**Test:** máy A gõ "abc" rồi tắt app (không đóng kết nối sạch); máy B đăng nhập cùng tài khoản → A nhận `BYE(LOGGED_IN_ELSEWHERE)`; B mở bài → ô soạn có "abc", gõ tiếp "d" → server có "abcd".

**Commit:** `feat(client): đổi sang máy khác vẫn làm tiếp đúng chỗ`

### Task 1.9: Bộ vẽ `cay`

**Files:**
- Create: `source/client/src/main/java/labcast/client/views/cay/TreeModel.java`, `TreeLayout.java`, `TreeView.java`
- Create: `runner/viz/cay.py` (`NutCay`, `danh_dau`, `xoay`) — trong `runner/` nên Người 3 duyệt PR
- Test: `TreeModelTest`, `TreeLayoutTest`, `runner/tests/test_cay.py`

- Hai nguồn dữ liệu: sự kiện `node`/`edge`/`mark`/`rotate` (cây nhị phân tìm kiếm, AVL), hoặc một list vẽ thành heap (`kieu: heap`, con của `i` là `2i+1`, `2i+2`).
- `TreeLayout`: hoành độ theo thứ tự duyệt giữa, tung độ theo độ sâu — đủ cho cây ≤ 31 nút.
- **Phía Python** (cấu trúc gắn camera, giống `viz.Mang`):

  ```python
  class NutCay:
      """Nút cây nhị phân. Gán .trai / .phai sinh sự kiện edge; tạo nút sinh node."""
      def __init__(self, khoa, ten="t"): ...           # → {"t":"node","v":ten,"id":…,"x":khoa}
      def __setattr__(self, k, v): ...                  # k ∈ {trai, phai}: bỏ cạnh cũ, thêm cạnh mới → edge
  def danh_dau(nut, trang_thai): ...                    # → mark; trang_thai ∈ dang-xet | xong | loai
  def xoay(nut, huong): ...                             # → rotate; chỉ ghi sự kiện, lời giải tự đổi con trỏ
  ```

  `id` của nút là số tăng dần theo thứ tự tạo, nên trace của hai lần chạy cùng input giống nhau. Heap không cần lớp riêng: `khung_nhin` khai báo `kieu: heap` cho một `viz.Mang` hoặc `list`.

**Test:** heap `[1,3,2,7]` → nút 7 là con trái của 3; layout cây 3 nút cân → gốc nằm giữa hai con; `rotate` phải tại gốc của cây lệch trái 3 nút → gốc mới đúng. Python: chèn 2, 1, 3 vào cây nhị phân tìm kiếm dựng bằng `NutCay` → 3 `node`, 2 `edge` đúng cha con.

**Commit:** `feat(client): thêm bộ vẽ cây và heap`

---

## Người 3 — ĐG3 đồng hồ, chấm bài, luyện tập, SQL bản đầu

### Task 3.6: Đồng hồ — đồng bộ lại, bù trôi, chỉnh dần

**Files:**
- Modify: `source/client/src/main/java/labcast/client/clock/ClockSync.java`
- Create: `source/client/src/main/java/labcast/client/clock/DriftEstimator.java`
- Test: `ClockSyncTest` (bổ sung), `DriftEstimatorTest`

**Hợp đồng:**

```java
public final class ClockSync implements Clock {
    public ClockSync(LongSupplier localMillis);               // đồng hồ máy có thể thay bằng đồng hồ giả khi test
    public void startPeriodic(LcpClient c, ScheduledExecutorService s);   // mỗi 60 s một đợt 5 mẫu
    public long serverNow();
}

final class DriftEstimator {                                   // hồi quy tuyến tính offset theo giờ máy, 10 điểm gần nhất
    void add(long localMillis, long offsetMs);
    double driftPpm();                                         // phần triệu
    long predictOffset(long localMillis);
}
```

- **Đợt đồng bộ:** 5 mẫu, chọn mẫu delay nhỏ nhất → một điểm `(local, offset)` cho `DriftEstimator`.
- **Giữa hai đợt:** `serverNow() = local + predictOffset(local) + slew(local)`.
- **Chỉnh dần:** độ lệch mới khác độ lệch đang dùng Δ. Nếu |Δ| > 1 000 ms thì nhảy ngay (vừa vào lớp, hoặc đồng hồ máy bị chỉnh tay). Ngược lại thì áp dần với tốc độ 50 ms mỗi giây, nên đồng hồ server nhìn từ app không bao giờ chạy lùi.

**Test** (đồng hồ giả):

```java
@Test
void chinhDan_khongBaoGioChayLui() {
    var local = new AtomicLong(0);
    var c = new ClockSync(local::get);
    c.addSample(0, 5000, 5000, 0);                       // offset 5000
    long truoc = c.serverNow();
    c.addSample(1000, 5900, 5900, 1000);                 // offset mới 4900, lệch −100
    for (int i = 0; i < 40; i++) {
        local.addAndGet(50);
        long bay_gio = c.serverNow();
        assertTrue(bay_gio >= truoc, "đồng hồ chạy lùi ở bước " + i);
        truoc = bay_gio;
    }
}

@Test
void lechQuaMotGiay_nhayNgay() { /* offset 5000 rồi 9000 → serverNow tăng ngay ≈ 4000 */ }

@Test
void troi_duDoanDuocGiuaHaiDot() { /* offset tăng 1 ms mỗi 10 s → sau 60 s dự đoán sai < 2 ms */ }
```

**Commit:** `feat(client): đồng hồ đồng bộ lại định kỳ, bù trôi và chỉnh dần`

### Task 3.7: Chuẩn bị bài theo lô

**Files:**
- Create: `runner/chuan_bi.py`
- Create: `source/server/src/main/java/labcast/server/lesson/LessonPreparer.java`
- Test: `runner/tests/test_chuan_bi.py`, `LessonPreparerTest`

Một lần chạy sandbox **cho cả bài** (spec §8.3) sinh ra:

```json
{"tests": [{"id": "lon-1", "input": "…", "output": "…"}, …],
 "variants": [{"kind": "PREDICT", "question": {…}, "answer": {…}}, …],
 "reference_trace": "…các dòng trace của lời giải mẫu với demo.in…"}
```

- `chuan_bi.py` nhận thư mục bài (gắn vào container ở chế độ chỉ đọc), chạy `sinh_test.py` (cỡ `lon`, seed 1…`so_test_an`), chạy `loi_giai.py` lấy output, chạy `du_doan.py` 200 lần (seed 1…200), chạy `loi_giai.py` có tracer với `demo.in`. Sinh thêm **200 test cỡ `nho` kèm output** để giai đoạn 3 tìm phản ví dụ mà không phải chạy lời giải mẫu cạnh code học viên.
- **Chỉ container chuẩn bị bài mới được gắn thư mục bài.** Container chạy code học viên **không bao giờ** được gắn thư mục bài hay thư mục đệm: nó chỉ nhận code và input qua stdin. Nếu gắn, code học viên mở được `loi_giai.py` hay output của test ẩn bằng `open(...)`. So output đúng/sai làm **bên Java**, ngoài container.
- `LessonPreparer` gọi một lần mỗi bài, lưu vào `.labcast-cache/<lessonId>/<content_hash>.json`. Khởi động lại server: bài có `content_hash` chưa đổi thì đọc đệm, không chạy lại.
- `content_hash` = SHA-256 của mọi file trong thư mục bài (sắp theo tên).

**Test:** chuẩn bị bài nổi bọt → có 10 test lớn, 200 đề biến thể, trace không rỗng; gọi lần hai → không chạy sandbox (đếm số lần gọi `Sandbox.run`); sửa `de.md` → hash đổi → chạy lại.

**Commit:** `feat(server): chuẩn bị test ẩn, đề biến thể và trace mẫu theo lô, lưu đệm theo hash`

### Task 3.8: Chấm bài và nộp

**Files:**
- Create: `runner/kiem_cam.py`
- Create: `source/server/src/main/java/labcast/server/grade/Grader.java`, `Verdict.java`
- Create: `source/server/src/main/java/labcast/server/practice/SubmitHandler.java`
- Test: `runner/tests/test_kiem_cam.py`, `GraderTest`

**Payload:** `SUBMIT_REQ` `str lessonId, i64 codeSeq`. `SUBMIT_RESULT` `JSON{verdict, score, cooldownMs, unlocked:[lessonId…], tests:[{id, verdict, timeMs}]}` — test ẩn **không** kèm input/output.

**Quy trình chấm** (spec §8.2):

1. `kiem_cam.py`: duyệt `ast`; gọi hàm có tên trong `cam_dung` (`sorted(...)`, `x.sort()`) hoặc `import` thuộc `cam_import` → `FORBIDDEN_CALL`, dừng.
2. Chạy mọi test cố định và test lớn **không** bật tracer; so output (bỏ khoảng trắng cuối dòng và dòng trống cuối).
3. Nếu bài có `luat.py`: chạy code học viên với một test cỡ `tb` có tracer (container của học viên), rồi đưa trace thu được vào `luat.py` chạy trong **container tin cậy riêng** (có gắn thư mục bài, nhận trace qua stdin, không chạy code học viên) → `TRACE_RULE` nếu vi phạm.
4. Gọi `CodeStore.checkpoint(…, "SUBMIT", verdict)` (Người 1) và `ClassEvents.publish(userId, lessonId, status)` (Người 2).

**Test:** `kiem_cam.py` bắt `sorted(a)`, `a.sort()`, `from heapq import heappush` khi bị cấm; không bắt biến tên `sorted_list`. `GraderTest` (sandbox giả trả output định sẵn): đúng hết → `OK`; sai một test ẩn → `WRONG`, trong kết quả không có input của test đó; quá giờ → `TIME_LIMIT`.

**Commit:** `feat(server): chấm bài nộp bằng test ẩn, kiểm hàm cấm và luật trace`

### Task 3.9: Thêm cấu trúc gắn camera cho Python

**Files:**
- Create: `runner/viz/ngan_xep.py` (`NganXep`, `HangDoi`), `runner/viz/bang.py` (`Bang`)
- Test: `runner/tests/test_ngan_xep.py`, `runner/tests/test_bang.py`

- `NganXep`: `push(x)`, `pop()`, `top()`, `rong()`, `len()` → sự kiện `push`, `pop`. `HangDoi`: `enq`, `deq` tương tự.
- `Bang(hang, cot, gia_tri_dau=0, ten="dp")`: `b[i][j]` đọc → `cell_r`, `b[i][j] = x` → `cell_w`. Dùng một lớp hàng trung gian để bắt được chỉ số thứ hai.

**Test:** `NganXep` push 1, push 2, pop → sự kiện `push, push, pop`, giá trị pop là 2; `Bang` gán `dp[1][2] = 5` rồi đọc → `cell_w(1,2,5)`, `cell_r(1,2)`.

**Commit:** `feat(runner): thêm ngăn xếp, hàng đợi và bảng 2 chiều gắn camera`

### Task 3.10: Bậc dự đoán, mã dùng một lần, mở bài

**Files:**
- Create: `source/server/src/main/java/labcast/server/practice/VariantService.java`, `ProgressService.java`, `PracticeModule.java`
- Test: `VariantServiceTest`, `ProgressServiceTest`

**Payload:** `PREDICT_REQ` `str lessonId`. `PREDICT_QUESTION` `JSON{nonce, lessonId, question}`. `PREDICT_ANSWER` `str nonce, str answerJson`. `PREDICT_RESULT` `JSON{correct, streak, cooldownMs, mastered}`.

- `VariantService.issue(userId, lessonId)`: lấy một đề chưa dùng của người này từ lô đã chuẩn bị, sinh `nonce` 16 byte `SecureRandom` (hex), lưu bảng `variants` với `expires_at = now + 10 phút`.
- `answer(userId, nonce, answerJson)`: nonce không có / đã dùng / hết hạn / không phải của người này → `ERROR(NONCE_INVALID)`. Đánh dấu `used_at` **trong cùng giao dịch** với việc chấm, để hai gói gửi cùng lúc không cùng được tính.
- So đáp án theo `question.kind`: `array` (so từng phần tử), `number`, `set` (không quan tâm thứ tự), `text` (bỏ khoảng trắng hai đầu).
- `ProgressService`: luật spec §8.4 — `streak`, thời gian chờ 0 → 15 → 30 → 60 → 120 s, khoá nộp sau 3 lần sai liên tiếp đến khi đúng một đề dự đoán, điểm 100 − 20 × số lần sai (thấp nhất 40), mở bài trong `mo_khoa` khi thành thạo. Bài giáo viên giao cho lớp (`LESSON_PUSH`, task 2.12) coi như đã mở với cả lớp. Quy ước điền `mo_khoa` ở [danh-muc-bai.md](danh-muc-bai.md#chuỗi-mở-bài).

**Test:** gửi đúng đáp án hai lần cùng nonce → lần hai `NONCE_INVALID`; nonce của người khác → `NONCE_INVALID`; đúng 3 đề liên tiếp → `mastered`; sai giữa chừng → `streak` về 0; nộp sai 3 lần → `SUBMIT_REQ` lần 4 bị `ERROR(LOCKED)` cho tới khi đúng một đề dự đoán.

**Commit:** `feat(server): bậc dự đoán với mã đề dùng một lần, luật thành thạo và mở bài`

### Task 3.11: Giới hạn tốc độ và hàng đợi chạy công bằng

**Files:**
- Create: `source/server/src/main/java/labcast/server/practice/RateLimitPolicy.java` (`implements FramePolicy`)
- Create: `source/server/src/main/java/labcast/server/practice/TokenBucket.java`
- Modify: `source/server/src/main/java/labcast/server/run/RunService.java`
- Test: `TokenBucketTest`, `RateLimitPolicyTest`, `RunQueueTest`

| Loại thông điệp | Thùng token |
| --- | --- |
| `RUN_REQ` | 6 lượt / phút, đợt tối đa 3 |
| `PREDICT_ANSWER` | 1 lượt / 3 s |
| `SUBMIT_REQ` | theo `cooldown_until` trong `progress` |
| Mọi thông điệp khác | 50 / s mỗi kết nối |

Quá 500 thông điệp/s trong 5 s → `BYE(TOO_MANY_MESSAGES)` và đóng kết nối. `PracticeModule.install` gọi `ctx.router().setPolicy(new RateLimitPolicy(...))`.

**Hàng đợi chạy:** mỗi người tối đa **một** lượt chạy thử đang chờ — lượt mới thay lượt cũ chưa chạy (lượt cũ nhận `RUN_RESULT{verdict:"REPLACED"}`); lượt nộp đứng trước mọi lượt chạy thử.

**Test:** `TokenBucket` (đồng hồ giả) 6/phút: 3 lượt liền được, lượt 4 bị từ chối với `retryAfterMs` ≈ 10 000; `RunQueueTest`: một người gửi 5 `RUN_REQ` liền khi worker bận → chỉ lượt cuối chạy; có một `SUBMIT` chen vào → chạy trước các `RUN`.

**Commit:** `feat(server): giới hạn tốc độ theo người và hàng đợi chạy công bằng`

### Task 3.12: Bộ máy SQL bản đầu

**Files:**
- Create: `source/server/src/main/java/labcast/server/sqlviz/SqlEngine.java`, `SqliteEngine.java`, `SqlGuard.java`, `ResultComparer.java`
- Modify: `source/server/pom.xml` — thêm `jsqlparser`
- Create: `content/sql/co-ban/loc-sinh-vien/` (bài mẫu WHERE)
- Test: `SqlGuardTest`, `SqliteEngineTest`, `ResultComparerTest`

**Hợp đồng:** đúng `SqlEngine` ở spec §7.6. Bản đầu `explainSteps` làm FROM, JOIN, WHERE (spec §7.6 bảng tách bước); các bước còn lại ở giai đoạn 3.

- `SqlGuard.check(sql, allowDml)`: JSqlParser `CCJSqlParserUtil.parseStatements` → đúng **một** câu, kiểu `Select` (hoặc `Insert/Update/Delete` nếu `allowDml`) → không thì `SQL_NOT_ALLOWED`.
- Chạy trên **bản sao** database (chép file sang thư mục tạm), `SELECT` mở chỉ đọc (`SQLiteConfig.setReadOnly(true)`), `setQueryTimeout(2)`.
- `ResultComparer.same(a, b, ordered)`: số cột bằng nhau; so như **đa tập** khi `ordered=false`; số thực so tới 1e-6; `NULL` bằng `NULL`.

**Test:** `DROP TABLE x` → bị chặn; `SELECT 1; SELECT 2` → bị chặn; câu chạy mãi (`WITH RECURSIVE r(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM r) SELECT count(*) FROM r`) → hết giờ sau ≈ 2 s; `[(1),(2)]` và `[(2),(1)]` → giống nhau khi không xét thứ tự, khác khi xét; `explainSteps` của `SELECT … FROM a JOIN b ON … WHERE …` có đủ `sql.table`, `sql.pairs`, `sql.keep`.

**Commit:** `feat(server): bộ máy SQL bản đầu — chặn câu nguy hiểm, chấm, tách bước FROM/JOIN/WHERE`

### Task 3.13: Màn các bậc học và bộ vẽ `bang-sql`

**Files:**
- Create: `source/client/src/main/java/labcast/client/practice/PracticeController.java`, `PredictPane.java`, `RunPanel.java`, `ResultPane.java`
- Create: `source/client/src/main/java/labcast/client/views/bangsql/SqlTableModel.java`, `SqlTableView.java`
- Test: `PracticeControllerTest`, `SqlTableModelTest`

- Tab theo bậc của bài (`bac` trong `lesson.yaml`); bậc chưa mở thì mờ, có ghi điều kiện mở.
- Thời gian chờ đếm ngược theo `Clock.serverNow()` (đổi giờ máy không làm hết chờ sớm).
- `SqlTableModel` nhận `sql.table`, `sql.pairs`, `sql.keep` → bảng, đường nối cặp dòng, dòng mờ đi.

**Test:** nhận `PREDICT_RESULT{cooldownMs: 15000}` → nút Gửi khoá, mở lại khi `Clock` tiến 15 s; `sql.keep` giữ 2 trên 5 dòng → 3 dòng ở trạng thái `MO`.

**Commit:** `feat(client): màn các bậc học và bộ vẽ bảng SQL`

---

## Người 2 — ĐG4, heartbeat thích nghi, sơ đồ lớp

### Task 2.11: Heartbeat thích nghi

**Files:**
- Modify: `source/server/src/main/java/labcast/server/presence/Presence.java`
- Create: `source/server/src/main/java/labcast/server/presence/RttEstimator.java`
- Test: `RttEstimatorTest`, bổ sung `PresenceTest`

**Công thức** (RFC 6298, tham khảo bản cũ `LivenessMonitor`):

```text
mẫu đầu:   SRTT = R,  RTTVAR = R/2
mẫu sau:   RTTVAR = 3/4·RTTVAR + 1/4·|SRTT − R|
           SRTT   = 7/8·SRTT   + 1/8·R
           RTO    = max(200 ms, SRTT + 4·RTTVAR)
chu kỳ:    ổn định (RTTVAR < SRTT/4 trong 5 mẫu liền) → chu kỳ × 1,5, tối đa 5 s
           mất một ACK hoặc RTTVAR > SRTT → chu kỳ về 1 s
trạng thái: không ACK sau (chu kỳ + 2·RTO) → SUSPECT; mất 3 ACK liền → DISCONNECTED
```

Có công tắc `--heartbeat co-dinh` để đo thí nghiệm 9 (chu kỳ cố định 2 s).

**Test:** chuỗi RTT 100 ms đều → chu kỳ tăng dần tới 5 s; một mẫu 900 ms → chu kỳ về 1 s; `srtt()` và `rto()` khớp công thức với số liệu tay.

**Commit:** `feat(server): heartbeat thích nghi theo SRTT/RTTVAR/RTO`

### Task 2.12: Sơ đồ lớp và giao bài

**Files:**
- Create: `source/server/src/main/java/labcast/server/classroom/ClassEvents.java`, `ClassroomState.java`, `ClassroomModule.java`
- Create: `source/client/src/main/java/labcast/client/ui/ClassMapPane.java`
- Test: `ClassroomStateTest`

**Hợp đồng:**

```java
public final class ClassEvents {                                       // spec §17.3
    public void publish(long userId, String lessonId, StudentStatus status);
    public enum StudentStatus { DANG_LAM, SAI_TEST, DA_QUA, GIO_TAY, HA_TAY }
}
```

**Payload:** `CLASS_JOIN` trống (giáo viên gửi để nhận sơ đồ). `CLASS_STATE` `JSON{students:[{userId, name, presence, lessonId, status, failedTest, pasteFlag}], stats:[{lessonId, testId, failCount}]}` — gửi `Priority.CONFLATABLE`, key `"lop"`, nên giáo viên xem chậm chỉ nhận bản mới nhất. `LESSON_PUSH` `str lessonId` (giáo viên → server → mọi học viên trong lớp).

Nguồn sự kiện: `Presence.onStateChange` (có mặt), `ClassEvents.publish` (Người 3 khi chấm, Người 1 khi giơ tay), gom lại và đẩy tối đa 5 lần/giây.

**Test:** 3 học viên, một người sai test `lon-2` → `stats` có `{testId: lon-2, failCount: 1}`; 50 sự kiện trong 100 ms → giáo viên nhận ≤ 1 `CLASS_STATE` trong khoảng đó (đếm frame đã gửi).

**Commit:** `feat(server): sơ đồ lớp thời gian thực và giao bài cho cả lớp`

### Task 2.13: ĐG4 — câu hỏi nhanh công bằng

**Files:**
- Create: `source/server/src/main/java/labcast/server/quiz/QuizService.java`, `Compensation.java`, `QuizModule.java`
- Create: `source/client/src/main/java/labcast/client/quiz/QuizController.java`, `QuizPane.java`
- Test: `CompensationTest`, `QuizServiceTest`

**Payload:**

| Thông điệp | Payload |
| --- | --- |
| `QUIZ_OPEN` (giáo viên → server) | `JSON{question, answer, durationMs}` |
| `QUIZ_OPEN` (server → học viên) | `JSON{quizId, question, opensAt, closesAt}` — **không** kèm đáp án |
| `QUIZ_ANSWER` | `i64 quizId, str answer` |
| `QUIZ_CLOSE` | `i64 quizId` |
| `QUIZ_RESULT` | giáo viên: `JSON{quizId, total, correct, late, distribution}`; học viên: `JSON{quizId, accepted, correct}` |

**Thuật toán:**

```java
final class Compensation {
    static final long CAP_MS = 150;
    static boolean accepted(long arrivedAt, long srttMs, long closesAt, boolean enabled) {
        long bu = enabled ? Math.min(Math.max(srttMs, 0) / 2, CAP_MS) : 0;
        return arrivedAt - bu <= closesAt;
    }
}
```

- `opensAt = now + 800 ms` (đủ cho `QUIZ_OPEN` tới mọi máy trước giờ hiện), gửi `Priority.CRITICAL`.
- `arrivedAt` = `ctx.receivedAt()` của luồng đọc. `srtt` lấy từ `Presence.srtt(userId)`, **không** dùng giờ do app gửi lên.
- Lưu mọi câu trả lời vào `quiz_answers` kèm `srtt_ms`, `est_sent_at`, `accepted` — dữ liệu cho thí nghiệm 8.
- `closesAt + 300 ms` → gửi `QUIZ_CLOSE` cho cả lớp, rồi `QUIZ_RESULT`.
- Công tắc `--bu-do-tre tat` để đo thí nghiệm 8.
- App: hiện câu hỏi khi `Clock.serverNow() ≥ opensAt`, khoá ô trả lời khi `≥ closesAt`.

**Test:**

```java
@Test
void denMuon80ms_rtt200_vanDuocNhan() {
    assertTrue(Compensation.accepted(10_080, 200, 10_000, true));    // bù 100
}

@Test
void buKhongVuotTran150() {
    assertFalse(Compensation.accepted(10_200, 2_000, 10_000, true)); // bù tối đa 150
}

@Test
void tatBu_denMuonLaBiLoai() {
    assertFalse(Compensation.accepted(10_001, 200, 10_000, false));
}
```

`QuizServiceTest`: học viên không nhận được đáp án trong `QUIZ_OPEN`; trả lời hai lần → chỉ tính lần đầu; trả lời sau `QUIZ_CLOSE` → không tính.

**Commit:** `feat(server): câu hỏi nhanh hẹn giờ, bù độ trễ theo SRTT có trần`

### Task 2.14: Màn danh sách bài và màn giáo viên

**Files:**
- Create: `source/client/src/main/java/labcast/client/ui/LessonListPane.java`, `TeacherHome.java`, `StudentHome.java`
- Modify: `source/client/src/main/java/labcast/client/ui/LessonScreen.java` — thêm tab của `PracticeController`

Chỉ lắp ghép: màn học viên = danh sách bài (khoá/mở) + `LessonScreen` + `QuizPane` nổi lên khi có câu hỏi + lớp phủ "đang theo giáo viên" khi có chiếu. Màn giáo viên = `ClassMapPane` + `PresenterPane` + nút Giao bài + nút Hỏi nhanh.

**Commit:** `feat(client): màn học viên và màn giáo viên`

---

## Nghiệm thu M2

Ba máy cắm dây chung switch: một máy giáo viên (server + app giáo viên), hai máy học viên.

- [ ] Giáo viên chiếu bài nổi bọt: hai máy học viên chạy khớp nhau bằng mắt thường; bấm Dừng thì cả hai dừng đúng một bước.
- [ ] Một học viên tách ra tua lại, bấm "theo giáo viên" thì về đúng chỗ.
- [ ] Giáo viên mở câu hỏi nhanh: hai máy hiện cùng lúc; kết quả về màn giáo viên.
- [ ] Giáo viên giao bài nổi bọt: học viên qua bậc dự đoán (3 đề liền) rồi nộp code; sơ đồ lớp đổi màu đúng (vàng → đỏ khi sai → xanh khi qua).
- [ ] Nộp sai liên tục: thời gian chờ tăng 15 → 30 → 60 s; khởi động lại app vẫn phải chờ.
- [ ] Rút dây một máy giữa lúc gõ, cắm lại: không mất ký tự nào.
- [ ] Đăng nhập tài khoản đó ở máy khác: máy cũ bị đăng xuất, máy mới có đúng code đang làm.
- [ ] Script gửi lại một `PREDICT_ANSWER` đã dùng: server trả `NONCE_INVALID`.
- [ ] Bài SQL lọc sinh viên: animation FROM → WHERE chạy; câu `DROP TABLE` bị từ chối.
