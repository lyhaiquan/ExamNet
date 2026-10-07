# Phân công 4 người — chia theo chức năng

Ba nguyên tắc để bốn người code song song mà không giẫm chân nhau:

1. **Mỗi người nhận trọn một nhóm chức năng, làm từ đầu tới cuối**: giao diện, lệnh protocol, xử lý ở server, bảng database, thí nghiệm.
2. **Mỗi chức năng là một package riêng, có đúng một chủ.** Chủ là người duy nhất sửa file trong package đó. Mở `package-info.java` của package nào cũng thấy ghi chủ.
3. **Chỗ hai chức năng gặp nhau được chốt bằng một hàm** (bảng "Chỗ giao nhau" bên dưới). Ai giữ phần nấy, chỉ gọi hàm của nhau.

Danh sách chức năng lấy theo `Topics.md` §4.6. Mỗi người giữ đúng **một đóng góp ★** (Novelty 15% điểm) và có phần network programming riêng để trình bày khi bảo vệ.

| Người | Nhóm chức năng | Đóng góp | Thí nghiệm | Họ tên | GitHub |
| --- | --- | --- | --- | --- | --- |
| 1 | Làm bài và không mất bài | ★ĐG1 | 1, 6, 10 | | |
| 2 · leader | Kết nối và tài khoản | ★ĐG3 | 3, 4, 5, 8 | | |
| 3 | Kỳ thi và thời gian | ★ĐG2 | 2, 7, 11 | | |
| 4 | Thông báo và giám sát | ★ĐG4 | 9 | | |

---

## Người 1 — Làm bài và không mất bài ★ĐG1

| | |
| --- | --- |
| **Chức năng** | Hiển thị câu hỏi · Cho phép trả lời · Lưu tạm bài làm · Gửi câu trả lời · Nhận câu trả lời · Lưu bài làm · Nộp bài · Nhận bài nộp · Khôi phục sau khi mất kết nối |
| **Nền chung** | Protocol EXP/1.0: `Frame`, codec, `PayloadReader/Writer` — cả nhóm dùng |
| **Package** | `common.protocol` · `server.journal` · `service.answer` · `client.answer` · `client.wal` |
| **Lệnh** | `HELLO` `HELLO_ACK` · `ANSWER_DELTA` `ANSWER_ACK` · `RESUME_REQ` `RESUME_STATE` · `SUBMIT_REQ` `SUBMIT_ACK` · `ERROR` · `BYE` |
| **Bảng** | `answers` |
| **Giao diện** | Màn làm bài (câu hỏi, chọn đáp án, trạng thái "đã lưu") |
| **Thí nghiệm** | 1 rút dây giữa lúc gửi đáp án · 6 EXP/1.0 so với JSON · 10 TCP cắt frame thành mấy mảnh |
| **Phải giải thích được** | Vì sao cần LEN và MAGIC; vì sao `read()` phải đọc lặp; vì sao kiểm LEN trước khi cấp bộ nhớ; ghi đĩa *trước* khi gửi; `seq` + ghi đè có điều kiện cho ra "gửi ít nhất một lần trên dây, ghi đúng một lần trong database" |

## Người 2 (leader) — Kết nối và tài khoản ★ĐG3

| | |
| --- | --- |
| **Chức năng** | Đăng nhập · Quản lý tài khoản và phân quyền · Quản lý danh sách thí sinh · Quản lý nhiều client đồng thời · Phát hiện client mất kết nối · Thông báo trạng thái kết nối |
| **Nền chung** | Kết nối hai phía: server nhận kết nối và chuyển lệnh; client nối tới server, tự nối lại, chuyển lệnh tới chức năng — cả nhóm cắm vào đây |
| **Package** | `server.net` · `server.session` · `server.account` · `server.liveness` · `service.account` · `client.net` · `client.account` |
| **Lệnh** | `AUTH` `AUTH_OK` `AUTH_FAIL` · `HEARTBEAT` `HEARTBEAT_ACK` · `ADMIN_CANDIDATE_IMPORT` |
| **Bảng** | `users` · `candidates` · `sessions` |
| **Giao diện** | Màn đăng nhập · thanh trạng thái kết nối · tab Thí sinh trong Admin |
| **Thí nghiệm** | 3 heartbeat thích nghi so với cố định · 4 hàng trăm máy cùng nộp · 5 NIO so với thread-per-connection · 8 một máy đọc chậm |
| **Phải giải thích được** | Vòng `accept()` và thread pool; vì sao chỉ một luồng được ghi ra mỗi socket; hàng đợi gửi đầy thì xử lý thế nào (backpressure); công thức SRTT/RTTVAR/RTO; máy trạng thái ALIVE → SUSPECT → DISCONNECTED; PBKDF2 + salt |

## Người 3 — Kỳ thi và thời gian ★ĐG2

| | |
| --- | --- |
| **Chức năng** | Quản lý kỳ thi · Quản lý ngân hàng câu hỏi · Phân phối đề / Nhận đề · Quản lý thời gian / Hiển thị thời gian còn lại · Tự động thu bài / Tự gửi khi hết giờ · Chấm điểm · Mã hoá đường truyền (TLS) |
| **Nền chung** | Kênh truyền: tùy chọn socket và TLS — server và client đều gọi vào đây; database: mở kết nối, chạy `schema.sql` |
| **Package** | `common.transport` · `server.exam` · `server.clock` · `service.db` · `service.exam` · `client.exam` · `client.clock` |
| **Lệnh** | `TIME_SYNC_REQ` `TIME_SYNC_RESP` · `EXAM_FETCH` `EXAM_DATA` · `FORCE_SUBMIT` · `ADMIN_EXAM_CREATE/OPEN/CLOSE` · `ADMIN_QUESTION_UPSERT/DELETE` · `ADMIN_RESULT_EXPORT` · `ADMIN_OK` |
| **Bảng** | `exams` · `questions` · `options` · `results` |
| **Giao diện** | Panel thời gian còn lại · tab Kỳ thi, Câu hỏi, Kết quả trong Admin |
| **Thí nghiệm** | 2 chỉnh lệch giờ máy thi ±5 phút · 7 bật/tắt `TCP_NODELAY` · 11 chi phí TLS |
| **Phải giải thích được** | Công thức offset/delay kiểu NTP và vì sao giữ mẫu RTT nhỏ nhất; Nagle là gì; TLS bắt tay và truststore; vì sao cột `is_correct` không bao giờ rời server; hai yêu cầu ở spec §16 "Hai yêu cầu dễ bị sót" |

## Người 4 — Thông báo và giám sát ★ĐG4

| | |
| --- | --- |
| **Chức năng** | Gửi thông báo / Nhận thông báo · Theo dõi trạng thái client (dashboard giám thị) · Ghi log hoạt động |
| **Nền chung** | Bench harness — công cụ cả 11 thí nghiệm đều chạy qua |
| **Package** | `server.notice` · `server.ws` · `service.audit` · `client.notice` · `bench` (khung) · trang web `server/src/main/resources/dashboard/` |
| **Lệnh** | `NOTICE` `NOTICE_NACK` · `PROCTOR_SUBSCRIBE` `PROCTOR_STATE` `PROCTOR_NOTICE_SEND` |
| **Bảng** | `audit_log` |
| **Giao diện** | Dải thông báo trên màn làm bài · dashboard giám thị trên trình duyệt |
| **Thí nghiệm** | 9 multicast so với unicast — phải chạy trong LAN thật, không chạy trên VPS |
| **Phải giải thích được** | Địa chỉ nhóm 239.x và TTL = 1; vì sao NACK đi qua TCP chứ không qua multicast; thông báo CRITICAL luôn kèm TCP; tự lùi về TCP khi không vào được nhóm; bắt tay WebSocket (`Sec-WebSocket-Accept`); vì sao mỗi thí nghiệm phải lặp nhiều lần |

Người 4 có ít chức năng hơn nhưng giữ hai phần lớn về kỹ thuật là WebSocket tự viết và bench harness, nên khối lượng vẫn cân.

---

## Chỗ giao nhau

Đây là những chỗ hai người chạm nhau. Mỗi chỗ chốt **một hàm**: bên cung cấp viết, bên dùng chỉ gọi, không ai sửa code của bên kia. Tên hàm dưới đây là đề xuất, chủ chốt lại trong PR đầu tiên của mình và báo cả nhóm nếu đổi.

| Chỗ gặp | Bên dùng | Bên cung cấp | Hàm |
| --- | --- | --- | --- |
| Mọi frame | cả nhóm | Người 1 | `Frame`, `PayloadWriter`, `PayloadReader` |
| Đăng ký xử lý lệnh ở server, kèm vai trò được phép | cả nhóm | Người 2 | `router.on(MessageType, Role…, handler)` |
| Gửi và nhận lệnh ở máy thi | cả nhóm | Người 2 | `client.send(frame)`, `client.on(MessageType, listener)` |
| Tùy chọn socket và TLS | Người 2 | Người 3 | `SocketOptions.apply(socket)`, bọc TLS trong `common.transport` |
| Nộp bài cần chấm điểm | Người 1 | Người 3 | `Grader.grade(sessionId)` |
| Chấm điểm cần đọc đáp án | Người 3 | Người 1 | `AnswerDao.answersOf(sessionId)` |
| Hết giờ hoặc dừng kỳ thi thì máy thi nộp bài | Người 3 (`client.exam` nhận `FORCE_SUBMIT`) | Người 1 | hàm nộp bài trong `client.answer` |
| Dashboard hiện trạng thái từng máy | Người 4 | Người 2 | `LivenessMonitor` báo mỗi khi một máy đổi trạng thái |
| Ghi nhật ký | cả nhóm | Người 4 | `AuditLog.record(actor, action, detail)` |

## File dùng chung (leader giữ)

Các file sau thuộc **leader**, sửa qua PR có leader duyệt:

- `common.protocol`: `MessageType`, `ErrorCode`, `Priority` — leader điền **đủ mọi lệnh theo spec §8.2 ngay trong PR đầu tiên**, sau đó gần như không ai phải sửa. Mỗi lệnh thuộc chủ ghi ở trên, không bao giờ đánh số lại.
- `common.model` (enum dùng chung), `server.app` (chỉ nối các module, mỗi module một dòng), `client.ui` (khung cửa sổ có chừa ô cho panel của từng người), `server.ops`, `ServerMain`.
- `source/pom.xml` và các `pom.xml` module, `.github/`, `deploy/`, `README.md`.

**Bảng trong `schema.sql`** thuộc chủ ghi ở đầu mỗi nhóm bảng. Đổi bảng của ai thì người đó duyệt.

## Quy tắc chống giẫm chân

1. **Chỉ sửa file trong package của mình.** Cần đổi gì ở phần người khác thì nhắn chủ, hoặc mở PR để chính chủ duyệt.
2. **PR nhỏ, merge thường xuyên.** Mỗi ngày chạy `git pull --rebase origin main` trên nhánh của mình. Độc lập không có nghĩa là mỗi người code một mình tới cuối kỳ rồi mới ghép — đó mới là cách làm hỏng dự án.
3. **Đổi protocol là việc duy nhất vượt ranh giới.** Chủ của một lệnh sở hữu định dạng payload của lệnh đó. Muốn đổi thì mở PR sửa spec §8 và sửa cả hai phía, chủ lệnh duyệt.
4. **README thuộc leader.** Trong lúc làm, mỗi người ghi kết quả vào phần riêng: `report/sections/<mục của mình>.md` và `statics/results/exp<số>_*.csv`. Cuối kỳ leader tổng hợp lên README.
5. **Test của mình không phụ thuộc code chưa xong của người khác.** Hàm xử lý nhận vào `Frame`, nên test tự tạo `Frame` là chạy được, không cần chờ codec hay kết nối thật.

## Thứ tự bắt đầu

1. **Leader mở PR khung** (1–2 ngày): điền đủ `MessageType` và `ErrorCode`, tạo chỗ đăng ký `router.on(...)` và `client.on(...)`, khung cửa sổ có chừa ô. Chỉ có khung, không có logic.
2. **Từ đó cả bốn người làm song song.** Tuần đầu không ai đụng ai: Người 1 làm codec, Người 2 làm vòng accept và kết nối, Người 3 làm database và công thức đồng hồ, Người 4 làm bắt tay WebSocket và khung harness.
3. **Ghép lên VPS theo mốc:**

| Mốc | Phase | Kiểm tra được |
| --- | --- | --- |
| M1 | 1–2 | Một máy thi đăng nhập được vào server trên VPS |
| M2 | 3–4 | Một thí sinh làm trọn bài và có điểm |
| M3 | 5–7 | Đủ 4 đóng góp ★, vẫn chạy bằng client dòng lệnh |
| M4 | 8 | Có giao diện Swing và dashboard |
| M5 | 9–10 | Xong 11 thí nghiệm và báo cáo |

## Cả nhóm phải nắm

Giảng viên có thể hỏi **bất kỳ ai về bất kỳ phần nào** (`Instruction.md` §17):

- Kiến trúc tổng thể và vì sao có bốn transport (TCP, UDP multicast, WebSocket, TLS) — spec §4.
- Khung EXP/1.0 13 byte và luồng giao tiếp từ `HELLO` tới `FORCE_SUBMIT` — spec §8.
- Đường đi của một đáp án: bấm chọn → ghi đĩa → `ANSWER_DELTA` → ghi database → `ANSWER_ACK`.
- Kết quả thí nghiệm của nhóm có ý nghĩa gì, đo như thế nào, hạn chế ở đâu.

## Chứng minh đóng góp

- Mỗi người làm trên nhánh riêng và mở PR **bằng tài khoản GitHub của chính mình**. Lịch sử PR là bằng chứng ai làm gì.
- Cột "Contribution" ở README §1.3 điền theo thực tế, khớp với lịch sử PR.
