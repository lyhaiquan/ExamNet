# ExamNet — Design Spec

**Topic:** Chủ đề 3 – Ứng dụng mạng, định hướng 4.6 "Hệ thống thi qua mạng"
**Ngày:** 2026-09-17 · làm sạch cho repo nhóm 2026-10-06
**Ngôn ngữ:** Java 17 (socket thuần, không framework)
**Quy mô:** nhóm 4 sinh viên — phân công ở §15 và `docs/PHAN-CONG.md`

Tài liệu này là spec thiết kế, đồng thời là bộ khung để viết `report/report.pdf` và điền `README.md`.
Spec chỉ mô tả **thiết kế và kế hoạch**. Mọi số liệu trong báo cáo phải do nhóm tự đo trên code của nhóm.
Mọi mục ở đây ánh xạ trực tiếp sang 11 yêu cầu của `Topics.md` §4.7 và cấu trúc báo cáo của `Instruction.md` §8.

---

## 0. Nguyên tắc chi phối: mạng là trọng tâm

`Instruction.md` §1 cảnh báo thẳng:

> *"Một project chỉ có giao diện và database nhưng không thể hiện rõ bản chất của network programming sẽ không được xem là đáp ứng đầy đủ yêu cầu của bài tập lớn."*

Và thang điểm §16 đặt **Network programming & technical implementation ở 25%** — hạng mục nặng nhất. Cộng với Novelty 15% và Experiment 15%, phần liên quan trực tiếp tới mạng chiếm **55%**, gấp gần ba lần phần functionality (20%).

Do đó spec này chịu bốn ràng buộc bắt buộc:

### R1 — Mọi client đều là protocol client, không có ngoại lệ

Admin Client **không** kết nối JDBC trực tiếp tới SQLite. Nó nói EXP/1.0 qua TCP y như máy thi. Tạo kỳ thi, import thí sinh, sửa câu hỏi — tất cả đều là message trên đường truyền, đi qua cùng tầng codec, cùng thread pool, cùng cơ chế RBAC và error handling.

Hệ quả: phần CRUD không nằm ngoài phần mạng mà **trở thành** một phần của nó. Không có đường tắt nào đi vòng qua socket.

### R2 — CRUD làm mỏng có chủ ý

Các chức năng quản lý là **bắt buộc phải có** vì `Topics.md` §4.6 liệt kê, nhưng cố tình làm ở mức tối thiểu đủ dùng:

| Làm | Không làm |
|---|---|
| Import thí sinh từ CSV | Trình soạn thảo thí sinh có phân trang, lọc, sắp xếp |
| Import câu hỏi từ file text | WYSIWYG editor, chèn ảnh, LaTeX |
| Form Swing trơn, layout mặc định | Theme, icon, animation, dark mode |
| Xuất điểm ra CSV | Biểu đồ thống kê, báo cáo PDF |

`Instruction.md` §7 nói rõ thêm màn hình UI mà không có thay đổi kỹ thuật **không được tính là đóng góp**. Thời gian tiết kiệm ở đây dồn hết sang tầng mạng.

### R3 — Ngân sách công sức

| Hạng mục | Tỉ trọng công sức mục tiêu |
|---|---|
| Tầng mạng: protocol, codec, socket, concurrency, backpressure | **35%** |
| Ba đóng góp độ tin cậy (ĐG1–ĐG3) | **25%** |
| Thí nghiệm, đo đạc, phân tích số liệu | **15%** |
| CRUD + GUI + DAO | **15%** |
| Báo cáo | **10%** |

Nếu trong lúc làm thấy CRUD/GUI vượt quá 15%, đó là tín hiệu phải cắt tính năng, không phải tín hiệu xin thêm thời gian.

### R4 — Phản mục tiêu

Ba dấu hiệu cho thấy project đang trượt khỏi môn học, phải chặn ngay khi thấy:

1. Có người đang làm đẹp giao diện trong khi tầng mạng chưa xử lý xong partial read.
2. Có chức năng nào đó đi tắt qua JDBC thay vì qua protocol.
3. Báo cáo dài phần "hướng dẫn sử dụng" hơn phần "thiết kế truyền thông".

---

## 1. Problem

Các kỳ thi trên máy tính hiện nay phụ thuộc vào giả định rằng mạng phòng máy luôn ổn định, máy thí sinh không bao giờ treo, và đồng hồ trên máy thí sinh là đáng tin. Trên thực tế cả ba giả định đều sai:

- Cáp mạng lỏng, switch nghẽn, Wi-Fi rớt → thí sinh mất kết nối giữa giờ thi.
- Máy thi treo hoặc mất điện → bài làm đang giữ trong RAM của client biến mất.
- Thí sinh chỉnh giờ hệ thống → đồng hồ đếm ngược phía client không còn đáng tin.

Hệ quả là bài làm bị mất, thí sinh thiệt thòi không do lỗi của mình, và kỳ thi mất tính công bằng. Đây không phải vấn đề giao diện — đây là vấn đề về **độ tin cậy của truyền thông mạng và tính nhất quán của trạng thái phân tán**.

## 2. Objective

Xây dựng hệ thống thi trên máy theo mô hình Client/Server bằng Java socket, đảm bảo bốn mục tiêu đo được:

1. **Không mất bài làm** khi client mất kết nối hoặc bị kill giữa giờ thi.
2. **Thời gian thi do server quyết định**, không tin đồng hồ client, sai số hiển thị dưới 50 ms kể cả khi giờ máy client bị chỉnh lệch.
3. **Phát hiện máy thi mất kết nối** trong thời gian xác định, với chi phí băng thông thấp hơn heartbeat chu kỳ cố định.
4. **Phục vụ đồng thời nhiều máy thi** và chịu được đỉnh tải khi cả phòng cùng nộp bài vào phút cuối.

## 3. Scope

### In scope

- Server quản lý kỳ thi, thí sinh, tài khoản/phân quyền, ngân hàng câu hỏi.
- Ba loại client: Candidate (máy thi), Proctor (giám thị), Admin (quản trị).
- Protocol nhị phân tự thiết kế EXP/1.0 trên TCP cho máy thi.
- UDP multicast cho thông báo toàn phòng, kèm lớp tin cậy tự xây và fallback về TCP.
- WebSocket (tự implement handshake RFC 6455) cho dashboard giám thị chạy trên trình duyệt.
- Câu hỏi trắc nghiệm một đáp án và nhiều đáp án; chấm tự động ở server.
- Ghi nhật ký hoạt động tập trung.
- TLS tùy chọn qua `SSLSocket`, bật/tắt bằng config.
- Bộ harness sinh client ảo để đo hiệu năng.

### Out of scope

- Câu hỏi tự luận và chấm tự luận.
- Chống gian lận ở mức hệ điều hành (khóa máy, chặn alt-tab, giám sát màn hình).
- Chứng chỉ TLS do CA thật cấp — dùng self-signed cho môi trường demo.
- Triển khai nhiều server / cân bằng tải giữa nhiều server.
- Ứng dụng di động.

---

## 4. System Architecture

```text
 ┌──────────────────┐  ┌──────────────────┐   ┌─────────────────────────┐
 │ Candidate Client │  │ Candidate Client │   │   Admin Client (Swing)  │
 │   (Swing, MVC)   │  │       × N        │   │ - Vòng đời kỳ thi       │
 │ ┌──────────────┐ │  │                  │   │ - Danh sách thí sinh    │
 │ │ Local WAL    │ │  │                  │   │ - Ngân hàng câu hỏi     │
 │ │ ClockSync    │ │  │                  │   │ - Xem điểm              │
 │ │ ReconnectMgr │ │  │                  │   └────────────┬────────────┘
 │ └──────────────┘ │  │                  │                │
 └────────┬─────────┘  └────────┬─────────┘                │
          │                     │                          │
          └─ EXP/1.0 over TCP (persistent, length-prefixed binary) ─┘
                                │   [tùy chọn bọc SSLSocket/TLS]
                                ▼
   ┌────────────────────────────────────────────────────────────┐
   │                        Exam Server                         │
   │                                                            │
   │  Tầng mạng                          :5000 TCP / :5001 WS   │
   │  ┌──────────────────────────────────────────────────────┐  │
   │  │ Acceptor (ServerSocket → accept loop)                 │  │
   │  │ ConnectionHandler pool (ThreadPoolExecutor)           │  │
   │  │ SessionManager (token, RBAC, state machine)           │  │
   │  │ ProctorWebSocketServer (RFC 6455 handshake + codec)   │  │
   │  └──────────────────────────────────────────────────────┘  │
   │                                                            │
   │  Tầng nghiệp vụ                                            │
   │  ┌──────────────────────────────────────────────────────┐  │
   │  │ ExamDao + ExamState │ QuestionDao                    │  │
   │  │ UserDao             │ Grader                         │  │
   │  │ NoticeBroadcaster   │ AuditLog                       │  │
   │  └──────────────────────────────────────────────────────┘  │
   │                                                            │
   │  Tầng độ tin cậy  ★ bốn đóng góp của nhóm                  │
   │  ┌──────────────────────────────────────────────────────┐  │
   │  │ AnswerJournal     ★ĐG1  crash-consistent journaling   │  │
   │  │ ExamClock         ★ĐG2  server-authoritative clock    │  │
   │  │ LivenessMonitor   ★ĐG3  adaptive heartbeat            │  │
   │  │ NoticeBroadcaster ★ĐG4  reliable multicast + fallback │  │
   │  └──────────────────────────────────────────────────────┘  │
   └───────┬──────────────┬──────────────────────┬──────────────┘
           │ JDBC         │ UDP multicast        │ WebSocket push
           ▼              ▼ 239.255.42.1:5002    ▼
   ┌───────────────┐  ┌──────────────┐  ┌──────────────────────┐
   │    SQLite     │  │ Cả phòng thi │  │ Proctor Dashboard    │
   │    (DAO)      │  │ (chỉ NOTICE, │  │ (trình duyệt)        │
   └───────────────┘  │  TTL=1)      │  │ - Trạng thái từng máy│
                      └──────────────┘  │ - Tiến độ làm bài    │
                                        │ - Gửi thông báo      │
                                        └──────────────────────┘
```

Bốn transport cùng tồn tại, mỗi cái dùng đúng chỗ nó mạnh: **TCP** cho điều khiển và bài làm (cần tin cậy, có thứ tự), **UDP multicast** cho thông báo toàn phòng (một-tới-nhiều), **WebSocket** cho dashboard trên trình duyệt, **TLS** tùy chọn bọc ngoài TCP.

Ba loại client khác nhau là lý do thật sự để tồn tại cơ chế broadcast/push phía server, thay vì gắn WebSocket vào chỉ để có.

---

## 5. Server — ánh xạ 16 chức năng sang component

Bảng này trả lời trực tiếp câu hỏi §4.7.3 "Server thực hiện những chức năng gì?".

| # | Chức năng (`Topics.md` §4.6) | Component | Ghi chú |
|---|---|---|---|
| 1 | Quản lý kỳ thi | `ExamDao` + `ExamState` | State machine `DRAFT → SCHEDULED → RUNNING → CLOSED → GRADED` |
| 2 | Quản lý danh sách thí sinh | `UserDao` + handler `ADMIN_CANDIDATE_IMPORT` | Import CSV, gán thí sinh vào kỳ thi |
| 3 | Quản lý tài khoản và quyền truy cập | `SessionManager` + `RBAC` | 3 vai trò `ADMIN / PROCTOR / CANDIDATE`, kiểm quyền ở tầng handler |
| 4 | Quản lý ngân hàng câu hỏi | `QuestionDao` | CRUD, import file, phân loại theo chủ đề/độ khó |
| 5 | Phân phối đề thi | `ExamDistributor` | Xáo đề theo `seed = hash(sessionId)`, mỗi thí sinh một thứ tự khác nhau |
| 6 | Quản lý thời gian thi | `ExamClock` ★ĐG2 | Server là nguồn thời gian duy nhất |
| 7 | Theo dõi trạng thái Client | `LivenessMonitor` ★ĐG3 | `ALIVE → SUSPECT → DISCONNECTED` |
| 8 | Nhận câu trả lời | `AnswerJournal` ★ĐG1 | Nhận `ANSWER_DELTA`, khử trùng theo seq |
| 9 | Lưu bài làm | `AnswerJournal` + DAO | Ghi bền vững từng thay đổi, không đợi tới lúc nộp |
| 10 | Nhận bài nộp | `SubmissionService` | Chuyển session sang `SUBMITTED`, khóa sửa |
| 11 | Tự động thu bài khi hết giờ | `ExamClock` → `FORCE_SUBMIT` | Do server phát động, không phụ thuộc client |
| 12 | Chấm điểm | `Grader` | Chấm trắc nghiệm ngay khi nộp, **ở server** vì server là bên duy nhất giữ đáp án |
| 13 | Ghi log hoạt động | `AuditLog` | Log tập trung, append-only, có timestamp server |
| 14 | Phát hiện Client mất kết nối | `LivenessMonitor` ★ĐG3 | |
| 15 | Gửi thông báo tới Client | `NoticeBroadcaster` ★ĐG4 | UDP multicast + lớp tin cậy NACK-qua-TCP, tự thoái lui về TCP unicast (§9.4) |
| 16 | Quản lý nhiều Client đồng thời | `ThreadPoolExecutor` | Xem §9 Concurrency |

---

## 6. Client — ba loại

Trả lời §4.7.4 "Client thực hiện những chức năng gì?".

### 6.1. Candidate Client (Swing, MVC)

Đủ 11 chức năng `Topics.md` liệt kê:

| Chức năng | Thành phần |
|---|---|
| Đăng nhập vào kỳ thi | `AuthController` |
| Nhận đề thi từ Server | `ExamController` |
| Hiển thị câu hỏi | `ExamView` |
| Cho phép thí sinh trả lời | `ExamView` + `AnswerController` |
| Lưu tạm bài làm | `LocalWAL` ★ĐG1 — ghi đĩa trước khi gửi |
| Gửi câu trả lời tới Server | `AnswerController` → `ANSWER_DELTA` |
| Hiển thị thời gian còn lại | `ClockSync` ★ĐG2 — suy từ đồng hồ server |
| Nhận thông báo từ Server | `NoticeListener` |
| Nộp bài | `SubmitController` |
| Tự động gửi bài khi hết giờ | Nhận `FORCE_SUBMIT` từ server |
| Thông báo trạng thái kết nối | Vòng tự kết nối lại trong `ExamClient` — hiện băng trạng thái Online/Đang kết nối lại |

Theo MVC của giáo trình: View mở `addXxxListener()` cho Controller cắm vào. Thông điệp mạng đến trên luồng đọc socket nhưng mọi cập nhật giao diện phải bọc trong `SwingUtilities.invokeLater()`.

### 6.2. Proctor Dashboard (trình duyệt, WebSocket)

Xem trạng thái từng máy thi theo thời gian thực, tiến độ trả lời, ai đã nộp, ai đang mất kết nối; gửi thông báo tới một máy hoặc toàn phòng.

### 6.3. Admin Client (Swing)

Tạo và mở kỳ thi, quản lý danh sách thí sinh, quản lý ngân hàng câu hỏi, xem kết quả và xuất điểm.

Theo **R1**, client này **không có JDBC**. Nó mở socket tới server và gửi các message quản trị trên cùng protocol EXP/1.0:

```text
ADMIN_EXAM_CREATE / ADMIN_EXAM_OPEN / ADMIN_EXAM_CLOSE
ADMIN_CANDIDATE_IMPORT   (payload: CSV, gửi theo chunk nếu vượt frame size)
ADMIN_QUESTION_UPSERT / ADMIN_QUESTION_DELETE
ADMIN_RESULT_EXPORT
```

Mọi message đều đi qua `RBAC` và bị từ chối bằng `ERROR(FORBIDDEN)` nếu session không có vai trò `ADMIN`. `ADMIN_CANDIDATE_IMPORT` là ca thú vị về mặt mạng: file CSV có thể vượt giới hạn frame nên phải chia chunk và ghép lại ở server — một bài toán truyền file thu nhỏ nằm ngay trong protocol tự thiết kế.

---

## 7. Network Communication

Trả lời §4.7.5.

- **Kết nối:** TCP persistent, mở khi client đăng nhập, giữ suốt kỳ thi. Client chủ động kết nối lại khi rớt, mang theo session token.
- **Mô hình:** request/response cho thao tác do client khởi xướng; server push cho thông báo, cảnh báo thời gian, lệnh thu bài.
- **Đóng kết nối:** client gửi `BYE` khi thoát bình thường; server đóng socket và chuyển session sang `DISCONNECTED` khi hết lease.
- **Nguyên tắc nền:** *server là trọng tài*. Client chỉ gửi yêu cầu hành động; server kiểm tra hợp lệ rồi phát lại trạng thái mới. Server **không bao giờ gửi đáp án đúng xuống client**, nên không thể đọc đáp án từ bộ nhớ máy thi.

---

## 8. Protocol EXP/1.0 và Data Format

Trả lời §4.7.6 và §4.7.7. Đây là **protocol tự thiết kế**, không dùng HTTP.

### 8.1. Frame

```text
 0        2      3      4      5              9             13
 +--------+------+------+------+--------------+--------------+----------------+
 | MAGIC  | VER  | TYPE | FLAGS|     SEQ      |     LEN      |    PAYLOAD     |
 | 'E''X' | 1B   | 1B   | 1B   |    4B (BE)   |    4B (BE)   |   LEN bytes    |
 +--------+------+------+------+--------------+--------------+----------------+
```

- `MAGIC` — phát hiện lệch khung và từ chối client lạ ngay từ byte đầu.
- `VER` — cho phép nâng cấp protocol về sau.
- `SEQ` — số thứ tự tăng đơn điệu trong một session, nền tảng cho ĐG1.
- `LEN` — độ dài payload, giải quyết bài toán TCP là dòng byte không có ranh giới thông điệp.
- `FLAGS` — bit 0 `COMPRESSED`, bit 1 `REQUIRES_ACK`.

**Data format:** payload nhị phân, các trường độ dài thay đổi mã hóa kiểu `[len:2][utf8 bytes]`. Chọn nhị phân thay vì JSON để giảm số byte trên đường truyền và có số liệu so sánh thật ở §11.

### 8.2. Message types

| Nhóm | Types |
|---|---|
| Bắt tay | `HELLO`, `HELLO_ACK` |
| Xác thực | `AUTH`, `AUTH_OK`, `AUTH_FAIL` |
| Đồng bộ giờ | `TIME_SYNC_REQ`, `TIME_SYNC_RESP` |
| Đề thi | `EXAM_FETCH`, `EXAM_DATA` |
| Làm bài | `ANSWER_DELTA`, `ANSWER_ACK` |
| Liveness | `HEARTBEAT`, `HEARTBEAT_ACK` |
| Nộp bài | `SUBMIT_REQ`, `SUBMIT_ACK`, `FORCE_SUBMIT` |
| Khôi phục | `RESUME_REQ`, `RESUME_STATE` |
| Thông báo | `NOTICE` |
| Lỗi | `ERROR(code, message)` |
| Kết thúc | `BYE` |

### 8.3. Communication sequence

```text
Client                              Server
  |---- HELLO(ver) ------------------->|
  |<--- HELLO_ACK(serverTime) ---------|
  |---- AUTH(user, pwd) -------------->|  PBKDF2 verify → cấp session token
  |<--- AUTH_OK(token, examId, role) --|
  |---- TIME_SYNC_REQ(t1) × 5 -------->|  lấy mẫu, giữ mẫu min-RTT
  |<--- TIME_SYNC_RESP(t2, t3) --------|
  |---- EXAM_FETCH ------------------->|  xáo đề theo seed = hash(sessionId)
  |<--- EXAM_DATA(questions) ----------|  KHÔNG kèm đáp án đúng
  |                                    |
  |============ làm bài ===============|
  |---- ANSWER_DELTA(seq=n) ---------->|  WAL local ghi TRƯỚC khi gửi
  |<--- ANSWER_ACK(seq=n) -------------|  server persist, idempotent
  |<--> HEARTBEAT (chu kỳ thích nghi)  |
  |<--- NOTICE("còn 5 phút") ----------|  server push
  |                                    |
  |--------- ✂ MẤT KẾT NỐI ✂ ----------|
  |---- RESUME_REQ(token, lastSeq) --->|  kết nối lại
  |<--- RESUME_STATE(ackedSeq) --------|  client replay delta chưa được ACK
  |                                    |
  |<--- FORCE_SUBMIT ------------------|  hết giờ, server tự thu bài
  |---- SUBMIT_ACK(score) ------------>|
```

---

## 9. Concurrency

Trả lời §4.7.8.

- **Mô hình chính:** `ThreadPoolExecutor` với bounded queue. Mỗi kết nối được giao cho một task; vòng `accept()` chạy trên luồng riêng và không bao giờ bị chặn bởi xử lý nghiệp vụ.
- **So sánh:** cài cả ba mô hình — thread-per-connection, thread pool, và NIO `Selector` — để đo ở thí nghiệm 5 (§12). Giáo trình nêu rõ NIO chỉ đáng dùng ở quy mô hàng nghìn kết nối; thí nghiệm này kiểm chứng điều đó bằng số liệu của chính nhóm.
- **Đồng bộ hóa:** `synchronized` chỉ ở ba chỗ — hàng đợi, thao tác đổi trạng thái session dùng chung, và ghi ra socket của một kết nối. **Không giữ khóa trong lúc ghi ra mạng** (một client chậm sẽ kéo cả server chậm theo).
- **Cấu trúc dữ liệu:** `ConcurrentHashMap` cho bảng session, `AtomicLong` cho bộ đếm seq, `BlockingQueue` cho hàng đợi thông báo.

### 9.1. Backpressure — xử lý client chậm

Đây là vấn đề mà phần lớn bài tập lớn bỏ qua, và là chỗ lộ rõ nhất ai thực sự hiểu socket.

Khi server `write()` ra một client đang nghẽn, cửa sổ nhận TCP của client đầy dần, bộ đệm gửi của server đầy theo, và lời gọi `write()` **chặn vô hạn**. Nếu lúc đó luồng đang giữ khóa của bảng session thì toàn bộ server đứng vì một máy thi duy nhất.

Thiết kế:

```text
  Luồng nghiệp vụ ──push──> [ Outbound queue ]  ──drain──> Writer thread ──> Socket
                            (bounded, N=256)                (chỉ luồng này
                                   │                         được write)
                                   └── đầy? → áp dụng chính sách:
                                        NOTICE     → drop cái cũ nhất
                                        ANSWER_ACK → chặn có timeout
                                        FORCE_SUBMIT → không bao giờ drop
```

- Mỗi kết nối có **một luồng ghi riêng** và một hàng đợi outbound có giới hạn. Không luồng nào khác được gọi `write()` trên socket đó.
- Hàng đợi đầy là tín hiệu client không theo kịp → chuyển session sang `SUSPECT`, để `LivenessMonitor` (ĐG3) xử lý.
- Thông điệp được phân loại theo mức ưu tiên để quyết định drop hay chặn — thông báo hết giờ thì không bao giờ được phép mất.

### 9.2. Tùy chọn socket được đặt tường minh

| Tùy chọn | Giá trị | Lý do |
|---|---|---|
| `TCP_NODELAY` | `true` | Tắt Nagle. `ANSWER_DELTA` là gói nhỏ và cần độ trễ thấp; để Nagle bật thì gói bị giữ lại chờ gộp. Đo tác động ở thí nghiệm 7 |
| `SO_TIMEOUT` | 30 000 ms | Không để luồng đọc treo vĩnh viễn |
| `SO_KEEPALIVE` | `true` | Lớp phòng thủ cuối, nhưng mặc định OS là 2 giờ nên **không thể thay thế** heartbeat của ĐG3 |
| `SO_RCVBUF` / `SO_SNDBUF` | 64 KB | Đặt tường minh để kết quả đo lặp lại được giữa các máy |
| `SO_REUSEADDR` | `true` | Khởi động lại server ngay, không vướng `TIME_WAIT` |
| `SO_BACKLOG` | 128 | Chịu được cả phòng thi cùng kết nối lúc mở đề |

### 9.3. Đọc/ghi từng phần

TCP là **dòng byte, không có ranh giới thông điệp**. Một lời gọi `read()` có thể trả về nửa frame, hoặc hai frame rưỡi. Codec bắt buộc phải:

1. Đọc đủ 13 byte header bằng vòng lặp `readFully`, không tin một lời gọi `read()` duy nhất.
2. Kiểm `LEN` với ngưỡng trần **trước khi** cấp phát mảng — nếu không, một client độc hại khai `LEN = 2^31` là server hết bộ nhớ.
3. Đọc đủ `LEN` byte payload, cũng bằng vòng lặp.
4. Có unit test bơm dữ liệu vào theo từng byte một, để chứng minh codec chịu được phân mảnh tối đa.

Điểm 4 là thứ nên đưa vào báo cáo: nó chứng minh nhóm hiểu TCP là stream chứ không phải message queue.

### 9.4. Kênh multicast cho thông báo

Gửi một thông báo tới cả phòng thi là bài toán one-to-many kinh điển. Gửi bằng N lần unicast TCP thì lưu lượng trên đường truyền tăng tuyến tính theo số máy; multicast thì server chỉ phát **một** gói.

```text
                      ┌──────────────┐
                      │  Exam Server │
                      └──┬────────┬──┘
         TCP unicast     │        │   UDP multicast 239.255.42.1:5002
      (điều khiển, bài làm)       │   (chỉ NOTICE)
              ┌─────────┴──┐     │
              ▼            ▼     ▼──────────┬──────────┐
         ┌────────┐  ┌────────┐  ┌────────┐ ┌────────┐
         │Client 1│  │Client 2│  │Client 3│ │Client N│
         └────────┘  └────────┘  └────────┘ └────────┘
```

Địa chỉ nhóm nằm trong dải administratively scoped `239.0.0.0/8`, TTL = 1 để gói không thoát khỏi phòng thi.

**Vấn đề:** UDP không đảm bảo tới nơi, và giáo trình mục 5.1 viết sai chỗ này — UDP **không có** cơ chế truyền lại. Thông báo "còn 5 phút" mà mất thì thí sinh thiệt thật.

**Lớp tin cậy tự xây (★ĐG4):**

1. Mỗi `NOTICE` mang `notice_seq` tăng đơn điệu theo kỳ thi.
2. Client phát hiện lỗ hổng khi thấy seq nhảy cóc (nhận được `n+2` mà chưa có `n+1`).
3. Client gửi **NACK qua kênh TCP sẵn có** — không NACK qua multicast, tránh bão phản hồi khi cả phòng cùng mất một gói.
4. Server gửi bù gói thiếu qua TCP unicast cho đúng client đó.
5. Thông báo mức `CRITICAL` (`FORCE_SUBMIT`, cảnh báo hết giờ) **luôn đi kèm TCP unicast**, multicast chỉ là đường nhanh. Không bao giờ đánh cược thứ quan trọng vào UDP.
6. Nếu socket multicast không join được nhóm (mạng chặn, máy ảo, khác subnet), client **tự động chạy hoàn toàn bằng TCP** và ghi log. Hệ thống không bao giờ phụ thuộc vào multicast để hoạt động đúng.

Điểm 5 và 6 là lý do thiết kế này an toàn để demo: multicast hỏng thì hệ thống chỉ chậm hơn chứ không sai.

## 10. Error Handling

Trả lời §4.7.9. Nguyên tắc: **một client gửi dữ liệu sai không được phép làm chết server**.

| Tình huống | Cách xử lý |
|---|---|
| Frame sai `MAGIC` / `VER` lạ | Trả `ERROR(BAD_FRAME)`, đóng kết nối, ghi audit log |
| `LEN` vượt ngưỡng cho phép | Từ chối ngay, không cấp phát bộ nhớ theo `LEN` mà client khai báo |
| Payload hỏng / thiếu byte | Bắt exception ở tầng codec, trả `ERROR(MALFORMED)`, giữ kết nối |
| Timeout đọc | `setSoTimeout()`; quá hạn → chuyển session sang `SUSPECT` |
| Client mất kết nối | `LivenessMonitor` phát hiện, giữ session còn sống để chờ resume |
| Duplicate request | Khử trùng theo `(session_id, question_id, seq)` — idempotent |
| Invalid state | `ExamState` + `ExamClock` từ chối, ví dụ nộp bài khi kỳ thi `CLOSED` → `ERROR(INVALID_STATE)` |
| Sai quyền | `RBAC` chặn ở handler, trả `ERROR(FORBIDDEN)` |
| Lỗi database | Rollback transaction, trả `ERROR(SERVER_ERROR)`, ghi log, kết nối vẫn sống |
| Exception không lường trước | `try/catch` bao quanh mỗi task trong pool; một task chết không kéo theo pool |

---

## 11. Novelty and Contributions

Trả lời §4.7.10. Cả bốn đóng góp đều nằm trong danh sách hợp lệ của `Instruction.md` §7 (cơ chế concurrency, fault tolerance, reliability, cơ chế adaptive), và đều có số liệu chứng minh.

### ĐG1 — Crash-consistent answer journaling + resume

**Baseline:** client giữ bài làm trong RAM và nộp một lần vào cuối giờ. Mất mạng hoặc treo máy là mất bài.

**Đề xuất:** mỗi thay đổi đáp án được ghi vào write-ahead log trên đĩa client **trước khi** gửi đi, rồi gửi `ANSWER_DELTA(seq)`. Server ghi bền vững theo khóa `(session_id, question_id, seq)` nên xử lý trùng lặp là idempotent. Khi kết nối lại, hai bên đối chiếu seq và client phát lại phần chưa được ACK. Kết quả là ngữ nghĩa at-least-once trên đường truyền nhưng exactly-once về mặt trạng thái.

**Bằng chứng:** thí nghiệm 1 (§12).

### ĐG2 — Server-authoritative clock với bù trôi

**Baseline:** client đếm ngược bằng `System.currentTimeMillis()` của chính nó. Chỉnh giờ máy là gian lận được; máy lag thì thí sinh chịu thiệt.

**Đề xuất:** ước lượng offset theo kiểu NTP ngay trên kết nối sẵn có:

```text
offset = ((t2 − t1) + (t3 − t4)) / 2
delay  = (t4 − t1) − (t3 − t2)
```

Lấy nhiều mẫu, giữ mẫu có `delay` nhỏ nhất vì mẫu đó chịu ít nhiễu hàng đợi nhất. Client hiển thị thời gian còn lại suy ra từ đồng hồ server. Deadline và lệnh thu bài do server quyết định hoàn toàn.

**Bằng chứng:** thí nghiệm 2 (§12).

### ĐG3 — Adaptive heartbeat và phát hiện mất kết nối

**Baseline:** heartbeat chu kỳ cố định, hoặc dựa vào TCP keepalive mà mặc định của hệ điều hành là 2 giờ — coi như không phát hiện được trong phạm vi một kỳ thi.

**Đề xuất:** chu kỳ heartbeat tự điều chỉnh theo RTT đo được, dùng công thức kiểu RTO của TCP:

```text
SRTT   ← (1 − α)·SRTT + α·RTT          α = 1/8
RTTVAR ← (1 − β)·RTTVAR + β·|SRTT − RTT|   β = 1/4
interval ← clamp(SRTT + 4·RTTVAR, MIN, MAX)
```

Máy mạng ổn định gửi thưa nên tốn ít băng thông; máy chập chờn bị dò dày hơn nên phát hiện nhanh hơn. Server chạy state machine `ALIVE → SUSPECT → DISCONNECTED`.

**Bằng chứng:** thí nghiệm 3 (§12).

### ĐG4 — Reliable multicast notification với TCP fallback

**Baseline:** gửi thông báo tới N máy thi bằng N lần unicast TCP. Lưu lượng trên đường truyền tăng tuyến tính theo N, và ở phòng 300 máy thì một thông báo trở thành 300 lần ghi socket.

**Đề xuất:** phát `NOTICE` một lần qua UDP multicast, kèm lớp tin cậy tự xây ở §9.4 — đánh seq, client phát hiện lỗ hổng, NACK **qua kênh TCP** (tránh bão phản hồi), server gửi bù unicast. Thông báo mức `CRITICAL` luôn đi kèm TCP nên không bao giờ đánh cược vào UDP. Không join được nhóm multicast thì tự thoái lui về TCP hoàn toàn.

Đóng góp nằm ở chỗ **kết hợp hai transport có đặc tính ngược nhau**: lấy hiệu quả băng thông của UDP multicast nhưng mượn kênh TCP sẵn có làm đường phản hồi và đường sửa lỗi, nên không phải xây lại toàn bộ cơ chế tin cậy trên UDP.

**Bằng chứng:** thí nghiệm 9 (§12).

---

## 12. Evaluation

Trả lời §4.7.11. Toàn bộ số liệu phải đo thật; `Instruction.md` §8.12 cấm tạo số liệu giả.

| # | Thí nghiệm | Biến độc lập | Metric | Chứng minh |
|---|---|---|---|---|
| 1 | Fault injection | Ngắt mạng / `kill -9` client tại thời điểm ngẫu nhiên | Tỉ lệ đáp án bị mất, thời gian resume, số delta trùng bị khử | ĐG1 |
| 2 | Clock attack | Chỉnh giờ máy client lệch −5, −1, 0, +1, +5 phút | Sai số thời gian hiển thị (ms) | ĐG2 |
| 3 | Liveness | Fixed 1s/5s/10s vs adaptive, ở 20/100/300 client | Thời gian phát hiện disconnect vs overhead (msg/s) | ĐG3 |
| 4 | Đỉnh tải nộp bài | 300 client cùng nộp trong 10 giây | Response time p50/p95/p99, error rate, throughput | Concurrency |
| 5 | Mô hình concurrency | thread-per-conn vs thread pool vs NIO, ở 50/200/1000 client | Throughput, bộ nhớ, số luồng, CPU | Concurrency |
| 6 | Protocol | EXP nhị phân vs JSON cùng nội dung | Bytes trên đường truyền, thời gian encode/decode | Data format |
| 7 | Nagle / `TCP_NODELAY` | Bật vs tắt Nagle, với `ANSWER_DELTA` gói nhỏ | RTT p50/p95 của `ANSWER_DELTA → ANSWER_ACK` | Socket tuning §9.2 |
| 8 | Backpressure | Cố tình cho 1 client đọc chậm (`sleep` trong vòng đọc) | Thời gian phản hồi của **các client còn lại**, độ dài outbound queue | §9.1 — chứng minh một máy chậm không kéo cả phòng |
| 9 | Multicast vs unicast | N = 10, 50, 100, 300 client nhận cùng một `NOTICE` | Tổng bytes trên đường truyền, thời gian phát xong cho toàn phòng, tỉ lệ mất gói UDP, số NACK | ★ĐG4 — kỳ vọng unicast O(N) vs multicast O(1) |
| 10 | Phân mảnh TCP | Bơm dữ liệu từng byte một vào codec; đo một frame lớn bị TCP chia mấy mảnh | Tỉ lệ decode đúng (kỳ vọng 100%), số lần `read()` cho một frame | §9.3 — TCP là stream, không phải message queue |
| 11 | Chi phí TLS | TCP trần vs TLS, cùng tải | Thời gian bắt tay, RTT p50/p95, throughput | Cái giá của mã hoá đường truyền |

Tên file kết quả theo mẫu `statics/results/exp<số>_<tên>.csv`, ví dụ `exp9_multicast.csv`.
Mỗi cấu hình phải lặp nhiều lần và báo cáo trung bình ± độ lệch chuẩn — một lần đo không phân biệt được "hệ thống nhanh" với "lúc đó máy rảnh".

Client ảo dùng để đo là harness CLI riêng trong `source/bench/`, không phải GUI.

Thí nghiệm 7, 8, 10 là loại số liệu chỉ có được khi tự viết tầng socket. Nếu dùng Spring Boot hay thư viện có sẵn thì không đo được, vì framework đã quyết hộ tất cả những lựa chọn này.

---

## 13. Database schema

Thiết kế theo bốn bước của giáo trình; mọi câu SQL có dữ liệu người dùng đi qua `PreparedStatement`.

```text
users(id, username, password_hash, salt, role, created_at)
exams(id, title, state, duration_sec, scheduled_start, created_by)
questions(id, exam_id, type, content, points, topic, difficulty)
options(id, question_id, content, is_correct)          ← đáp án đúng chỉ nằm ở server
candidates(id, user_id, exam_id, seat_no)
sessions(id, candidate_id, exam_id, token, state, started_at, last_seq, clock_offset_ms)
answers(session_id, question_id, seq, option_ids, updated_at)   PK(session_id, question_id)
results(session_id, score, max_score, graded_at)
audit_log(id, ts, actor, action, detail)
```

Quan hệ `questions`–`options` là 1-n nên khóa ngoại nằm ở phía `options`. Bảng `answers` dùng khóa chính tổ hợp `(session_id, question_id)` với cột `seq` để khử trùng — đây chính là cơ chế idempotent của ĐG1.

---

## 14. Cấu trúc thư mục

Theo `Instruction.md` §12. Cây dưới đây khớp với khung đã dựng sẵn trong repo; ai thêm hay đổi thư mục thì sửa cả mục này lẫn README.

```text
ExamNet/
├── README.md, Instruction.md, Topics.md, CONTRIBUTING.md
├── docs/
│   ├── specs/examnet-design.md       spec này
│   ├── PHAN-CONG.md                  chia việc 4 người
│   └── deploy/VPS-SETUP.md           dựng VPS + secrets cho CI/CD
├── report/report.pdf                 (phase 10)
├── statics/
│   ├── architecture.png, protocol.png   ← phải xuất ảnh thật, không chỉ ASCII
│   ├── dataset/
│   └── results/                      CSV của 11 thí nghiệm
├── deploy/  Dockerfile, compose.yaml
└── source/                           Maven multi-module, chạy bằng ./mvnw
    ├── common/   protocol/ (frame, codec, MessageType, ErrorCode), model/, tls/
    ├── server/   net/, session/, app/, clock/, liveness/, journal/, notice/, ws/, ops/
    ├── service/  dao/, db/, grade/, auth/, model/ + resources/schema.sql
    ├── client/   (ExamClient), wal/, clock/, notice/, ui/, admin/
    └── bench/    harness sinh client ảo
```

Phụ thuộc giữa các module: `client` và `bench` chỉ phụ thuộc `common`; `server` phụ thuộc `common` và `service`. Máy thi không chứa code database hay code chấm điểm.

---

## 15. Phân công 4 người

Mỗi người giữ **đúng một** đóng góp ★ (phần Novelty), cộng một mảng mạng nền và các thí nghiệm đi kèm. Chi tiết package và thông điệp protocol của từng người ở `docs/PHAN-CONG.md`.

| Người | Đóng góp ★ | Mạng nền | Thí nghiệm |
|---|---|---|---|
| 1 | ĐG1 — `AnswerJournal` + `LocalWal` + resume | Protocol EXP/1.0, frame, codec | 1, 6, 10 |
| 2 | ĐG3 — `LivenessMonitor` | Server core: accept loop, thread pool, `ClientConnection`, backpressure | 3, 4, 5, 8 |
| 3 | ĐG2 — `ExamClock` + `ClockSync` | Tùy chọn socket, TLS, DB, kiểm quyền, chấm điểm | 2, 7, 11 |
| 4 | ĐG4 — `NoticeBroadcaster` + bên nhận multicast | WebSocket dashboard, bench harness | 9 |

**GUI chia đều, không giao cho một người.** Mỗi thành viên tự làm phần giao diện cho tính năng mình phụ trách (phase 8). Lý do: gom hết GUI vào một người thì người đó thành nút thắt ở cuối kỳ, đúng lúc cần chạy thí nghiệm — và người đó cũng là người duy nhất không có đóng góp kỹ thuật để trình bày khi bảo vệ, trong khi `Instruction.md` §16 yêu cầu mỗi sinh viên chứng minh được phần đóng góp thực tế của mình.

Mọi thành viên phải đọc hiểu được toàn bộ protocol và kiến trúc — `Instruction.md` §2 và §17 nói rõ giảng viên có thể hỏi bất kỳ ai giải thích bất kỳ phần nào.

---

## 16. Kế hoạch triển khai

Thứ tự dưới đây cố ý đẩy **toàn bộ tầng mạng lên trước GUI**. Giao diện là phase 8, gần cuối. Nếu hết thời gian thì thứ bị cắt là giao diện, không phải phần mạng.

| Phase | Nội dung | Kết quả kiểm chứng được |
|---|---|---|
| 0 | Khởi tạo Maven, schema SQLite, CI/CD, luật commit | **Đã dựng sẵn trong repo.** `./mvnw verify` xanh; merge vào main thì VPS trả về đúng mã commit ở `:8080/version` |
| 1 | `common/` — frame codec EXP/1.0, **test phân mảnh từng byte** (§9.3) | Bơm 1 byte/lần vẫn decode đúng 100% |
| 2 | Server tối thiểu: accept loop, HELLO, AUTH, thread pool, **socket options** (§9.2), seed dữ liệu mẫu. Thay `ServerMain` mẫu nhưng giữ `StatusEndpoint` | Client CLI đăng nhập được; `netstat` xác nhận tùy chọn socket |
| 3 | **Backpressure**: writer thread + outbound queue có giới hạn (§9.1) | Client chậm không làm chậm các client khác |
| 4 | Đề thi, làm bài, nộp bài, `Grader` | Một thí sinh làm trọn bài và có điểm; bảng điểm liệt kê đủ thí sinh, kể cả người chưa nộp |
| 5 | ★ĐG1 journaling + resume | Kill client giữa chừng, bài làm còn nguyên |
| 6 | ★ĐG2 clock sync · ★ĐG3 adaptive heartbeat | Chỉnh giờ ±5 phút vẫn đúng; rút dây mạng phát hiện đúng hạn; máy mất mạng lúc hết giờ vẫn được chấm điểm |
| 7 | ★ĐG4 multicast + NACK-qua-TCP + fallback (§9.4) | Chặn multicast bằng firewall → hệ thống vẫn chạy đúng qua TCP |
| 8 | GUI: Candidate Swing, Admin Swing, Proctor WebSocket dashboard | GUI nối server thật, dashboard từ chối vai trò không đủ quyền |
| 9 | Bench harness + chạy **11 thí nghiệm** (§12) | Số liệu trong `statics/results/`, mỗi cấu hình lặp nhiều lần |
| 10 | TLS tùy chọn, README, xuất PNG, viết báo cáo | `--tls` chạy được trên cả cổng 5000 lẫn 5001; báo cáo qua Compilatio |

Mốc quan trọng nhất là **hết phase 7**: tới đó hệ thống đã có đủ toàn bộ nội dung kỹ thuật để chấm — cả bốn đóng góp, cả bốn transport — mà vẫn chỉ chạy bằng client CLI, chưa cần một cửa sổ đồ họa nào.

### Hai yêu cầu dễ bị sót

Cả hai đều xuất phát từ nguyên tắc *server là trọng tài* (§7):

1. **Dừng kỳ thi phải xảy ra ở server, và đi bằng TCP.** Khi Admin đóng kỳ thi giữa chừng (`ADMIN_EXAM_CLOSE`) hoặc khi hết giờ, server (a) đổi trạng thái kỳ thi, (b) từ chối mọi `ANSWER_DELTA` tới sau đó dựa trên **trạng thái** kỳ thi chứ không chỉ dựa trên giờ, (c) gửi `FORCE_SUBMIT` mức CRITICAL qua TCP tới từng máy. Multicast chỉ được dùng thêm để hiện dòng thông báo; bên nhận multicast không bao giờ thực hiện lệnh, vì nhóm multicast không có xác thực (§17).
2. **Chấm điểm không được phụ thuộc vào việc máy thi còn sống.** Lúc kỳ thi đóng, server tự chấm mọi phiên chưa nộp từ dữ liệu đã có trong bảng `answers` — đó chính là thứ ĐG1 bảo vệ. Bảng điểm xuất ra phải đi từ danh sách thí sinh (`candidates`) để người chưa có kết quả vẫn hiện ra, không biến mất.

---

## 17. Giới hạn đã biết

1. TLS dùng chứng chỉ self-signed, chỉ phù hợp môi trường demo trong phòng thi nội bộ. Repo công khai nên **khoá riêng không bao giờ được commit** — mỗi máy tự sinh keystore theo hướng dẫn trong README.
2. Chỉ có một server; server chết là cả kỳ thi dừng. Fault tolerance ở đây bảo vệ trước lỗi client và lỗi mạng, không bảo vệ trước lỗi server.
3. Không chống được gian lận ở mức hệ điều hành.
4. `ExamClock` chống được việc chỉnh giờ máy client, nhưng không chống được client bị sửa mã nguồn để bỏ qua deadline — dù vậy server vẫn từ chối mọi `ANSWER_DELTA` gửi sau deadline, nên tác hại bị chặn ở server.
5. SQLite phù hợp quy mô một phòng thi; quy mô lớn hơn cần đổi sang PostgreSQL.
6. Nhóm multicast không có xác thực: bất cứ ai trong cùng LAN cũng gửi được gói giả. Đó là lý do thông báo mức CRITICAL luôn kèm TCP, và TCP mới là đường đáng tin.
7. UDP multicast chỉ hoạt động trong cùng một phân đoạn mạng LAN — không đi qua router giữa các subnet nếu không bật IGMP/PIM, và thường bị chặn trên Wi-Fi doanh nghiệp hoặc trong máy ảo có NIC dạng NAT. Thiết kế đã lường trước bằng cơ chế thoái lui về TCP ở §9.4, nên đây là giới hạn về **hiệu quả băng thông**, không phải về tính đúng đắn.
8. Bản chạy trên VPS (Internet) không có multicast vì lý do ở mục 7 — mọi thông báo đi bằng TCP. Thí nghiệm 9 phải chạy trong LAN thật (phòng máy hoặc laptop của nhóm nối chung switch), không chạy trên VPS.
