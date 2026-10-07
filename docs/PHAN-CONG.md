# Phân công 4 người

Chia **theo chức năng**: mỗi package một chủ, mỗi người giữ **một đóng góp mạng ★**, một nhóm bộ vẽ và một phần nội dung. Bản đầy đủ ở spec [§17](specs/labcast-design.md) — file này là bản tóm tắt để tra nhanh; hai chỗ lệch nhau thì spec đúng.

Điền tên và tài khoản GitHub vào bảng dưới, rồi chép sang README §1.3.

| Người | Họ tên | GitHub | Mảng | Đóng góp ★ | Thí nghiệm |
| --- | --- | --- | --- | --- | --- |
| 1 | | | Làm bài và trợ giúp | ĐG2 — dòng code tin cậy, phản chiếu | 4, 5 |
| 2 (leader) | | | Kết nối, lớp học, câu hỏi nhanh | ĐG4 — câu hỏi nhanh công bằng | 8, (9), (10) |
| 3 | | | Bài tập, chạy và chấm | ĐG3 — đồng bộ đồng hồ | 2, 6, 7 |
| 4 | | | Giảng và chiếu | ĐG1 — chiếu animation qua multicast | 1, 3 |

---

## Người 1 — Làm bài và trợ giúp

| | |
| --- | --- |
| Package | `server.journal`, `server.mirror`, `client.editor`, `client.journal`, `client.mirror`, `client.views.{cay, caygoi, dothi}` |
| Thông điệp | `CODE_DELTA`/`CODE_ACK`, khôi phục code, `CHECKPOINT_*`, `HAND_*`, `MIRROR_*`, `COMMENT_*`, `VIEW_*` |
| Bảng DB | `code_deltas`, `checkpoints`, `hand_raises`, `comments` |
| Nội dung | Cây, đồ thị, SQL JOIN — 14 animation; 30 bài DSA, 20 bài SQL |
| Phải giải thích được | Vì sao ghi nhật ký **trước** khi gửi; delta trùng hoặc thiếu thì server làm gì; "at-least-once trên dây, exactly-once về trạng thái"; gộp thay đổi khi giáo viên xem chậm |

## Người 2 (leader) — Kết nối, lớp học, câu hỏi nhanh

| | |
| --- | --- |
| Package | `common.protocol`, `common.transport`, `server` (điểm vào), `server.{net, session, account, presence, discovery, classroom, quiz, store, ops}`, `client.{app, net, ui, quiz}` |
| Thông điệp | `HELLO`, `AUTH_*`, `HEARTBEAT`, khôi phục phiên, `DISCOVER*`, `CLASS_*`, `LESSON_PUSH`, `QUIZ_*`, `ADMIN_*`, `ERROR`, `BYE` |
| Bảng DB | `users`, `sessions`, `classes`, `enrollments`, `quizzes`, `quiz_answers`, `audit_log` |
| File chung | `MessageType`, `ErrorCode`, các `pom.xml`, `.github/`, `deploy/`, `packaging/`, `README.md` |
| Nội dung | Ngăn xếp/hàng đợi, đệ quy/quay lui, SQL truy vấn con và phép tập hợp — 13 animation; 30 bài DSA, 20 bài SQL |
| Phải giải thích được | Vì sao cần `LEN` và `MAGIC`; vì sao `read` phải đọc lặp; mỗi socket một luồng ghi; hàng đợi ghi đầy thì làm gì; SRTT/RTTVAR/RTO; vì sao bù độ trễ phải có trần và không tin giờ máy học viên |

## Người 3 — Bài tập, chạy và chấm

| | |
| --- | --- |
| Package | `server.{sync, lesson, practice, run, grade, sqlviz}`, `client.{clock, practice}`, `client.views.{luoi, bangsql}`, `runner/`, định dạng `content/` |
| Thông điệp | `TIME_SYNC_*`, `LESSON_LIST`/`FETCH`/`DATA`, `PREDICT_*`, `SIM_*`, `RUN_*`, `TRACE_CHUNK`, `SUBMIT_*`, `HINT_*` |
| Bảng DB | `lessons`, `progress`, `variants`, `submissions` |
| Nội dung | Quy hoạch động, SQL cơ bản, gộp nhóm, NULL/CASE, DML — 13 animation; 20 bài DSA, 30 bài SQL |
| Phải giải thích được | Đồng bộ đồng hồ kiểu NTP, vì sao giữ mẫu RTT nhỏ nhất, bù trôi và chỉnh dần; vì sao không chấm trên máy học viên; mã dùng một lần; thùng token; sandbox chặn những gì; vì sao không lấy được bảng GROUP BY bằng cách cắt câu SQL |

## Người 4 — Giảng và chiếu

| | |
| --- | --- |
| Package | `common.trace`, `server.cast`, `client.{cast, player, presenter}`, `client.views.{mang, nganxep, dslk, bam, codebien}`, khung `bench` |
| Thông điệp | `CAST_CONTROL`, `CAST_DATA`, `CAST_KEYFRAME`, `CAST_NACK`, `CAST_REPAIR` |
| Bảng DB | — (trạng thái chiếu chỉ sống trong một buổi) |
| Nội dung | Sắp xếp/tìm kiếm, danh sách liên kết, băm — 13 animation; 20 bài DSA, 30 bài SQL tổng hợp |
| Phải giải thích được | Vì sao phát sự kiện thay vì phát hình; phát hẹn giờ; vì sao datagram ≤ 1400 byte; vì sao phải chọn card mạng khi phát và khi nghe multicast; NACK, keyframe, chuyển sang TCP |

---

## Chỗ giao nhau

Gọi phần của người khác **qua các giao diện đã chốt** ở spec [§17.3](specs/labcast-design.md), không sửa thẳng vào code của họ. Quan trọng nhất:

- `router.on(MessageType, Role…, handler)`, `client.send(...)`, `client.on(...)` — Người 2 cung cấp cho tất cả.
- `FramePolicy.check(session, type)` — Người 2 gọi, Người 3 cài luật giới hạn tốc độ.
- `Clock.serverNow()` — Người 3 cung cấp cho Người 2 (câu hỏi nhanh) và Người 4 (phát hẹn giờ).
- Định dạng trace và `View` — Người 3 sinh trace, Người 4 đọc trace và định nghĩa `View`; Người 1, 3 viết bộ vẽ.
- `CodeStore.current(...)` — Người 1 cung cấp cho Người 3 (chạy đúng phiên bản code đã xác nhận).
- `RunService.run(...)` — Người 3 cung cấp cho Người 1 (phiên xem chung) và Người 4 (chiếu bài học viên).

## Quy tắc chống giẫm chân

1. Chỉ sửa package của mình. Cần đổi phần người khác thì nhắn chủ hoặc mở PR để chủ duyệt.
2. File chung (`MessageType`, `ErrorCode`, `pom.xml`, CI) sửa bằng **PR nhỏ riêng**, leader duyệt.
3. Đổi định dạng trace phải có **cả Người 3 và Người 4** duyệt.
4. Mỗi ngày `git pull --rebase origin main` trên nhánh của mình.

## Thứ tự bắt đầu

Leader mở PR khung protocol và `router` trước (đầu giai đoạn 1). Trong lúc chờ, người khác làm phần không cần mạng: Người 4 trình phát đọc trace từ file, Người 3 runner chạy độc lập, Người 1 ô soạn code. Lịch đầy đủ: [`specs/labcast-ke-hoach.md`](specs/labcast-ke-hoach.md).
