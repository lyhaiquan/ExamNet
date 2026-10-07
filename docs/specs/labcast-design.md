# LabCast — Design Spec

**Đề tài:** Trực quan hoá học liệu DSA và SQL cho buổi thực hành trong phòng máy — Chủ đề 3 "Ứng dụng mạng"
**Ngày:** 2026-10-07
**Trạng thái:** bản đề xuất, **chờ giảng viên xác nhận** theo `Topics.md` §7. Chưa triển khai.
**Ngôn ngữ:** Java 17 (server và app desktop JavaFX) · Python 3.12 (bài làm của học viên, chạy trong sandbox)
**Quy mô:** nhóm 4 sinh viên. Phân công ở §17, kế hoạch triển khai ở [`labcast-ke-hoach.md`](labcast-ke-hoach.md).
**Tên:** "LabCast" là tên tạm: *Lab* là phòng máy, *Cast* là phát cho cả lớp.

Spec mô tả **thiết kế**. Mọi con số ở §3 là **mục tiêu cần kiểm chứng**, không phải kết quả. Số liệu trong báo cáo phải do nhóm tự đo. Những điểm chưa chắc chắn được ghi thành giả định ở §18.

---

## 0. Đề xuất một trang (theo `Topics.md` §7)

| Mục | Nội dung |
| --- | --- |
| **Project Title** | LabCast — phòng thực hành DSA và SQL có animation, đồng bộ qua mạng LAN |
| **Problem** | Trong buổi thực hành ở phòng máy, sinh viên khó hình dung thuật toán và câu truy vấn. Giáo viên không biết ai đang kẹt. Phần mềm chiếu màn hình truyền video nên tốn băng thông và không tương tác được. Máy phòng lab hay treo hoặc khởi động lại làm mất code. Luyện tập trên máy dễ bị học đối phó: đoán mò, nộp liên tục, chép đáp án |
| **Objective** | Hệ thống client/server cho buổi thực hành. Giáo viên giảng bằng animation phát đồng bộ tới mọi máy. Học viên luyện tập theo bậc, xem code của chính mình chạy thành animation, được chấm trên server. Giáo viên theo dõi cả lớp và trợ giúp trực tiếp. Code không mất khi mạng hay máy gặp sự cố |
| **Proposed Architecture** | Một server Java chạy trên máy giáo viên, kèm Docker để chạy code an toàn. App desktop JavaFX trên máy giáo viên và từng máy học viên. Tất cả cắm dây chung một switch |
| **Network Communication** | TCP với protocol tự thiết kế **LCP/1.0** cho phiên làm việc. UDP multicast để chiếu animation cho cả phòng. UDP broadcast để app tự tìm server. TLS tuỳ chọn |
| **Main Functions** | Chiếu animation hẹn giờ · câu hỏi nhanh tính giờ · luyện tập 4 bậc (xem, dự đoán, tự mô phỏng, viết code) · chạy code thành animation · chấm bằng test ẩn · sơ đồ lớp thời gian thực · giơ tay, phản chiếu code, bình luận · dòng thời gian code, đổi máy làm tiếp |
| **Technology** | Java 17 (`java.net`, `javax.net.ssl`, `java.util.concurrent`), JavaFX 21, RichTextFX, SQLite (`sqlite-jdbc`), JSqlParser, Python 3.12 trong Docker, Maven, GitHub Actions |
| **Expected Novelty** | ĐG1 chiếu animation bằng *sự kiện* qua multicast tin cậy, phát hẹn giờ theo đồng hồ đồng bộ · ĐG2 dòng code tin cậy theo thời gian thực: không mất, đổi máy làm tiếp, phản chiếu có gộp thay đổi · ĐG3 đồng bộ đồng hồ kiểu NTP có bù trôi và chỉnh dần, nền cho mọi thứ "hẹn giờ" · ĐG4 câu hỏi nhanh công bằng nhờ bù độ trễ có trần |
| **Evaluation Plan** | 8 thí nghiệm bắt buộc và 2 tuỳ chọn (§14): băng thông chiếu so với chiếu màn hình video, độ lệch giữa các màn hình, mất gói và vào lớp muộn, mất code khi sự cố, độ trễ phản chiếu, ảnh hưởng của spam, chạy thử dồn tải, công bằng của câu hỏi nhanh |

---

## 1. Nguyên tắc chi phối

### R1 — Mạng là lõi, animation là bộ mặt

`Topics.md` §5.7 ghi: *"Thay đổi giao diện đơn thuần không được xem là đóng góp kỹ thuật chính."* §8 nói thêm rằng project vừa phải nhưng thể hiện tốt network programming có giá trị hơn project lớn mà mạng chỉ phụ trợ.

Vì vậy animation, bộ vẽ và số lượng bài học **không** được tính là đóng góp. Chúng là nội dung để phần mạng có việc thật để làm. Bốn đóng góp ĐG1–ĐG4 đều là cơ chế mạng.

### R2 — Phép thử REST

Một tính năng chỉ được tính là phần mạng nếu thay bằng REST API thì hỏng hẳn hoặc kém rõ rệt. Điều đó xảy ra khi tính năng có ít nhất một đặc điểm: **(a)** server phải tự đẩy dữ liệu, **(b)** một nguồn gửi nhiều máy nhận, **(c)** dòng dữ liệu liên tục có thứ tự, **(d)** phiên giữ trạng thái lâu, **(e)** nhạy với độ trễ.

| Tính năng | Đặc điểm | Ghi chú |
| --- | --- | --- |
| Chiếu animation cho cả phòng | a, b, e | ĐG1 |
| Dòng code, đổi máy, phản chiếu, phiên xem chung | a, c, d | ĐG2 |
| Đồng bộ đồng hồ giữa các máy | e | ĐG3 — nền cho chiếu hẹn giờ và câu hỏi nhanh |
| Câu hỏi nhanh tính giờ | a, e | ĐG4 |
| Đề dùng một lần, giới hạn tốc độ, hàng đợi chạy công bằng | — | Hạ tầng bảo mật. REST cũng làm được, **không tính là đóng góp mạng** |
| Sơ đồ lớp thời gian thực | a | Hạ tầng |
| Tự tìm server | — | Chưa biết địa chỉ server thì không gọi HTTP được |
| Nộp bài, xem kết quả chấm | — | Hỏi–đáp, REST là đủ. Đi chung kết nối TCP cho thống nhất, **không tính là đóng góp mạng** |

### R3 — Không tin máy học viên

Máy học viên do học viên kiểm soát: sửa được app, gửi được gói tin tự chế. Vì vậy:

- Đáp án, test ẩn, trạng thái mở khoá bài và điểm **chỉ nằm trên server**.
- Client chỉ hiển thị và gửi *hành động* (câu trả lời, code, lệnh chạy). Mọi quyết định "đúng", "qua", "mở bài" do server đưa ra.
- Chạy thử và xem animation không giới hạn lượt, vì không tính điểm. Mọi thứ tính điểm đều qua luật ở §8.3–§8.4.

### R4 — Nội dung là dữ liệu, không phải code

Thêm một bài là thêm một thư mục trong `content/` (§7.1), không sửa code Java. CI tự kiểm từng bài: lời giải mẫu qua hết test, trace không vượt trần, file cấu hình hợp lệ.

### R5 — Ngân sách công sức và thứ tự cắt

| Hạng mục | Tỉ trọng công sức mục tiêu |
| --- | --- |
| Tầng mạng nền: protocol, kết nối, backpressure, tìm server, TLS | 20% |
| Bốn đóng góp ĐG1–ĐG4 | 25% |
| Thí nghiệm, đo đạc, phân tích | 15% |
| Bộ máy animation và chấm: runner, trình phát, bộ vẽ, SQL | 20% |
| Nội dung: animation giảng giải và bài tập | 10% |
| Báo cáo | 10% |

**Bộ demo tối thiểu để bảo vệ:** 4 bài mẫu (nổi bọt, trung tố → hậu tố, N-Queens n = 4, LCS), 1 bài SQL GROUP BY, cộng 30–50 bài tập.

**Thứ tự cắt khi thiếu thời gian:**

1. Số bài tập: 200 → 100 → 50.
2. Animation giảng giải ngoài 20 bài ưu tiên (★ ở §7.7).
3. Bậc "tự mô phỏng".
4. Tìm phản ví dụ nhỏ.
5. TLS.
6. SQL nâng cao: truy vấn con tương quan, phép tập hợp.

**Không bao giờ cắt:** ĐG1–ĐG4, protocol LCP/1.0, sandbox an toàn, nguyên tắc R3.

**Dấu hiệu đang trượt khỏi môn học**, phải chặn ngay:

1. Có người làm đẹp giao diện trong khi đóng góp của mình chưa chạy được.
2. Số bài tăng trong khi chưa có thí nghiệm nào chạy.
3. Báo cáo dài phần nội dung bài học hơn phần thiết kế truyền thông.

---

## 2. Problem

Một buổi thực hành DSA hay SQL ở phòng máy gặp năm vấn đề:

1. **Khó hình dung.** Thuật toán đệ quy, quay lui, quy hoạch động và thứ tự thực thi câu SQL rất khó tưởng tượng chỉ qua code. Animation có sẵn trên mạng là video dựng sẵn, không chạy được code của chính sinh viên.
2. **Giáo viên không biết ai đang kẹt** cho tới khi đi hết một vòng phòng.
3. **Chiếu màn hình truyền video.** Phần mềm phòng máy gửi hình ảnh màn hình giáo viên tới từng máy: tốn băng thông, chữ mờ khi co giãn, học viên không tua lại hay tự xem chậm được.
4. **Máy phòng lab không đáng tin.** Máy treo, khởi động lại, phần mềm đóng băng ổ cứng xoá dữ liệu, cáp mạng lỏng. Code đang làm dở biến mất.
5. **Học đối phó.** Luyện tập có điểm thì sinh viên đoán mò trắc nghiệm, nộp liên tục đến khi đúng, chép đáp án của bạn, hoặc sửa app để tự báo "đã qua".

Vấn đề 1 và 2 là lý do có hệ thống. Vấn đề 3, 4, 5 là **bài toán mạng**: đồng bộ trạng thái thời gian thực giữa hàng chục máy trong một mạng LAN, giữ dữ liệu toàn vẹn khi kết nối và máy gặp sự cố, và giữ công bằng khi mỗi máy có độ trễ khác nhau và không tin được máy nào.

### 2.1. Hệ thống tương tự

| Hệ thống | Làm được | Còn thiếu |
| --- | --- | --- |
| NetSupport School, Veyon | Chiếu màn hình giáo viên, xem màn hình học viên | Truyền video: tốn băng thông, không tương tác, không hiểu nội dung bài học |
| VisuAlgo, Python Tutor | Animation thuật toán, chạy từng dòng | Chạy trên một máy, không có lớp học, không chấm khắt khe |
| Online judge (CodePTIT…) | Chấm code bằng test | Không có animation, không giảng dạy thời gian thực |

LabCast kết hợp cả ba vai trò cho một buổi học. Phần mới về kỹ thuật nằm ở cách truyền: phát **sự kiện** thay vì hình ảnh, hẹn giờ theo một đồng hồ chung có bù trôi, dòng code không mất, và công bằng khi mỗi máy có độ trễ khác nhau.

---

## 3. Objective

Mục tiêu đo được, kiểm chứng ở §14:

| # | Mục tiêu | Thí nghiệm |
| --- | --- | --- |
| O1 | Chiếu animation: độ lệch giữa các màn hình ≤ 50 ms (p95) trên mạng dây, kể cả khi đồng hồ các máy lệch nhau ±2 s. Băng thông thấp hơn chiếu màn hình video ít nhất 100 lần | TN1, TN2 |
| O2 | Chiếu chịu được mất gói: mất 5% gói multicast, mọi máy vẫn nhận đủ. Máy vào lớp muộn theo kịp trong ≤ 2 s | TN3 |
| O3 | Không mất code: mất 0 ký tự khi rớt mạng; khi tắt app đột ngột chỉ mất phần chưa được xác nhận (< 1 s gõ); đổi máy làm tiếp trong < 5 s sau khi đăng nhập | TN4 |
| O4 | Phản chiếu code: từ lúc học viên gõ tới lúc màn hình giáo viên hiện ≤ 200 ms (p95) khi 40 học viên cùng gõ. Giáo viên xem chậm không làm server chậm | TN5 |
| O5 | Trọng tài: một máy gửi 1000 thông điệp/giây không làm độ trễ p95 của các máy khác tăng quá 20%. 40 lượt chạy thử dồn trong 10 s đều có kết quả | TN6, TN7 |
| O6 | Công bằng: khi bật bù độ trễ, chênh lệch tỉ lệ bị tính trễ giữa nhóm mạng chậm thêm 200 ms và nhóm bình thường ≤ 2 điểm phần trăm | TN8 |

## 4. Scope

### In scope

- Server Java chạy trên máy giáo viên. Một server phục vụ một lớp, tối đa 50 máy học viên.
- Một app desktop JavaFX, hai chế độ theo vai trò: **giáo viên** và **học viên**.
- Protocol nhị phân tự thiết kế LCP/1.0 trên TCP. UDP multicast có lớp tin cậy tự xây, tự lùi về TCP. UDP broadcast để tìm server. TLS tuỳ chọn.
- Hai môn: **DSA** và **SQL**, mỗi môn khoảng 100 bài. Animation giảng giải cho khoảng 40 thuật toán DSA và 13 khái niệm SQL (§7.7).
- Bài làm DSA viết bằng **Python 3.12**, chạy trong Docker sandbox trên máy server.
- Bài làm SQL chạy trên bộ máy SQL phía server (§7.6).
- Bộ chạy client ảo để đo hiệu năng.

### Out of scope

- Học từ xa qua Internet. Chỉ dùng trong phòng máy, mạng dây.
- Client chạy trên trình duyệt hay điện thoại.
- Hai người cùng sửa một file code (kiểu Google Docs). Giáo viên chỉ bình luận.
- Chiếu màn hình dạng video.
- Ngôn ngữ bài làm khác Python.
- Chấm thi chính thức, coi thi, xếp hạng. Đây là hệ thống **dạy và luyện tập**.
- Nhiều server cho một lớp, cân bằng tải giữa các server.
- Trực quan hoá giao dịch SQL đồng thời.

---

## 5. Kiến trúc

### 5.1. Triển khai trong phòng máy

```text
                    Phòng máy — mạng dây, chung một switch

  Máy giáo viên                                   Máy học viên × N (N ≤ 50)
  ┌───────────────────────────────────┐          ┌──────────────────────┐
  │ LabCast Server (Java) ── SQLite    │  TCP     │ App LabCast          │
  │      │                             │  7000    │ (JavaFX, chế độ      │
  │      ├── Docker: sandbox Python    │◄────────►│  học viên)           │
  │      │                             │          │                      │
  │      │  UDP multicast              │  ──────► │ nhận chiếu           │
  │      │  239.255.70.1:7001          │          │                      │
  │      │  UDP broadcast 7002         │ ◄──────  │ tìm server           │
  │      │                             │          └──────────────────────┘
  │ App LabCast (chế độ giáo viên) ────┤ TCP localhost:7000
  └───────────────────────────────────┘
```

- Giáo viên mở server rồi mở app ở chế độ giáo viên. App giáo viên cũng nói LCP/1.0 qua TCP như mọi máy học viên, **không** đọc thẳng database.
- App học viên tự tìm server bằng UDP broadcast, rồi giữ một kết nối TCP suốt buổi và nghe nhóm multicast.
- Code học viên chỉ chạy trong Docker trên máy server. Máy học viên không cần cài Python hay Java (bộ cài kèm sẵn Java và JavaFX, §16).

### 5.2. Thành phần server

| Package `labcast.server.*` | Việc | Chủ |
| --- | --- | --- |
| `net` | Vòng accept, đọc/ghi frame, hàng đợi ghi riêng cho từng kết nối | Người 2 |
| `session`, `account` | Đăng nhập, phân quyền, phiên, xử lý đăng nhập ở nơi khác | Người 2 |
| `presence` | Heartbeat thích nghi, RTT từng máy, trạng thái có mặt | Người 2 |
| `discovery` | Trả lời `DISCOVER` qua UDP | Người 2 |
| `classroom` | Sơ đồ lớp, giao bài, danh sách lớp, xuất tiến độ | Người 2 |
| `quiz` | Câu hỏi nhanh, bù độ trễ (ĐG4) | Người 2 |
| `store`, `ops` | Mở SQLite, chạy `schema.sql`; `/version` cho bản chạy trên VPS của nhóm | Người 2 |
| `cast` | Phát multicast, nhận NACK, sửa gói, keyframe, chuyển máy sang TCP (ĐG1) | Người 4 |
| `sync` | Trả lời `TIME_SYNC` (ĐG3) | Người 3 |
| `lesson` | Nạp và kiểm thư mục bài học, sinh sẵn đề biến thể và test ẩn | Người 3 |
| `practice` | Bậc học, thành thạo, mã dùng một lần, giới hạn tốc độ | Người 3 |
| `run` | Hàng đợi chạy, Docker sandbox, đọc trace | Người 3 |
| `grade` | Chấm test, luật trace, tìm phản ví dụ | Người 3 |
| `sqlviz` | `SqlEngine`, tách bước câu SQL, chấm SQL | Người 3 |
| `journal` | Nhận `CODE_DELTA`, dựng lại code, mốc lưu, khôi phục (ĐG2) | Người 1 |
| `mirror` | Giơ tay, phản chiếu, bình luận, phiên xem chung (ĐG2) | Người 1 |

### 5.3. Thành phần app desktop

| Package `labcast.client.*` | Việc | Chủ |
| --- | --- | --- |
| `app` | `Launcher`, khởi động JavaFX | Người 2 |
| `net` | Kết nối TCP, tự kết nối lại, `client.send` / `client.on` | Người 2 |
| `ui` | Lắp các màn hình: đăng nhập, danh sách bài, màn bài học, sơ đồ lớp | Người 2 |
| `quiz` | Hiện câu hỏi đúng giờ, khoá ô trả lời khi hết giờ | Người 2 |
| `clock` | Đồng bộ đồng hồ với server, bù trôi, `Clock.serverNow()` (ĐG3) | Người 3 |
| `cast` | Nghe multicast trên đúng card mạng, phát hiện thiếu gói, gửi NACK | Người 4 |
| `player` | Trình phát trace: chạy, dừng, tua, tốc độ, phát hẹn giờ | Người 4 |
| `presenter` | Bảng điều khiển chiếu của giáo viên | Người 4 |
| `views.*` | Mười bộ vẽ (§7.4), mỗi bộ một chủ | Theo §7.4 |
| `editor` | Ô soạn code Python có tô màu cú pháp | Người 1 |
| `journal` | Nhật ký thay đổi trên máy, gửi bù, dòng thời gian code | Người 1 |
| `mirror` | Giơ tay; phía giáo viên: hàng chờ trợ giúp, khung phản chiếu, bình luận | Người 1 |
| `practice` | Màn các bậc học, kết quả chấm, thời gian chờ | Người 3 |

---

## 6. Một buổi học

| Giai đoạn | Trên lớp | Thông điệp chính | Chủ |
| --- | --- | --- | --- |
| 1. Giảng | Giáo viên chạy animation. Mọi máy thấy cùng một bước, cùng lúc. Giáo viên dừng thì cả lớp dừng. Học viên có thể tách ra tự tua rồi bấm "theo giáo viên" | `CAST_CONTROL`, `CAST_DATA`, `CAST_KEYFRAME`, `CAST_NACK`, `CAST_REPAIR` | Người 4 |
| 2. Hỏi nhanh | "Cặp nào đổi chỗ tiếp?" — 10 giây, kết quả hiện ngay | `QUIZ_OPEN`, `QUIZ_ANSWER`, `QUIZ_CLOSE`, `QUIZ_RESULT` | Người 2 |
| 3. Làm bài | Giáo viên giao bài. Mỗi học viên nhận đề biến thể riêng, đi qua các bậc, chạy thử để xem code của mình thành animation, rồi nộp | `LESSON_PUSH`, `PREDICT_*`, `SIM_*`, `CODE_DELTA`, `RUN_REQ`, `TRACE_CHUNK`, `SUBMIT_REQ` | Người 3, Người 1 |
| 4. Theo dõi | Sơ đồ lớp: 🟢 đã qua · 🟡 đang làm · 🔴 sai test · ✋ giơ tay · ⚪ mất kết nối. Thống kê kiểu "8 bạn sai test mảng rỗng" | `CLASS_STATE` | Người 2 |
| 5. Trợ giúp | Giáo viên bấm vào bạn đang giơ tay: xem code đang gõ, xem dòng thời gian, cùng xem animation, bình luận vào dòng code | `HAND_RAISE`, `MIRROR_*`, `COMMENT_*`, `VIEW_*`, `CHECKPOINT_*` | Người 1 |
| 6. Chữa chung | Giáo viên chiếu một bài sai đã ẩn tên lên cả lớp | Như giai đoạn 1 | Người 4 |

---

## 7. Bài học, bài tập và animation

### 7.1. Thư mục bài học

```text
content/
├── dsa/
│   └── sap-xep/
│       └── noi-bot/
│           ├── lesson.yaml     # cấu hình bài
│           ├── de.md           # đề bài
│           ├── loi_giai.py     # lời giải mẫu, có viz.say / viz.key cho animation giảng giải
│           ├── khung.py        # khung code đưa cho học viên (tên biến đã đặt sẵn)
│           ├── sinh_test.py    # sinh test ngẫu nhiên theo cỡ: nho | tb | lon
│           ├── du_doan.py      # (tuỳ chọn) sinh câu hỏi bậc dự đoán
│           ├── luat.py         # (tuỳ chọn) luật soi trace
│           ├── demo.in         # input nhỏ cho animation giảng giải
│           └── tests/          # test cố định: nho-1.in, nho-1.out, …
└── sql/
    └── gom-nhom/
        └── diem-trung-binh-lop/
            ├── lesson.yaml
            ├── de.md
            ├── dap_an.sql      # câu đáp án
            ├── schema.sql      # lược đồ
            ├── du_lieu_mau.sql # dữ liệu học viên nhìn thấy
            ├── sinh_du_lieu.py # sinh database ẩn: có NULL, dòng trùng, bảng rỗng
            └── du_doan.py      # (tuỳ chọn)
```

`lesson.yaml` của một bài DSA:

```yaml
id: dsa.sap-xep.noi-bot
mon: dsa
chuong: sap-xep
ten: Sắp xếp nổi bọt
loai: giang-giai          # giang-giai | bai-tap | luyen-tap (luyện tập = chỉ chấm test)
bac: [xem, du-doan, mo-phong, code]
khung_nhin:               # biến nào vẽ bằng bộ vẽ nào
  - bien: a
    kieu: mang
cay_goi: []               # tên hàm cần dựng cây lời gọi, ví dụ [dat_hau]
cam_dung: [sorted, sort]  # hàm cấm gọi
cam_import: []
gioi_han: { thoi_gian_ms: 1000, bo_nho_mb: 256 }
test_xem: [nho-1, nho-2, tb-1]   # học viên thấy input, xem được animation
test_cham: tat-ca                # gồm test lớn ẩn, chỉ trả đúng/sai
so_test_an: 10                   # số test lớn sinh thêm bằng sinh_test.py
mo_khoa: [dsa.sap-xep.chen]
```

Bài SQL có thêm `thu_tu: khong` (so kết quả không quan tâm thứ tự dòng) hoặc `thu_tu: co`, và `loai_cau: select | dml`.

### 7.2. Bốn bậc của một bài

| Bậc | Học viên làm gì | Điều kiện qua | Có ở |
| --- | --- | --- | --- |
| **Xem** | Xem animation giảng giải, tua tới lui | Không chặn | Bài `giang-giai` |
| **Dự đoán** | Server đưa đề biến thể: mảng ngẫu nhiên, hai chuỗi ngẫu nhiên, dữ liệu bảng ngẫu nhiên. Học viên **gõ** đáp án: cả mảng sau lượt 1, giá trị `dp[3][4]`, các dòng còn lại sau WHERE… Không có trắc nghiệm A/B/C/D | Đúng 3 đề liên tiếp | Bài có `du_doan.py` |
| **Tự mô phỏng** | Tự thực hiện thuật toán từng bước: chọn cặp đổi chỗ, chọn đẩy hay lấy ngăn xếp. Server kiểm từng bước | Làm trọn một đề không sai bước nào | Chỉ bộ vẽ `mang` và `ngan-xep` ở bản đầu |
| **Viết code** | Viết code từ khung, chạy thử, nộp | Qua mọi test chấm và luật trace | Mọi bài |

Bài `luyen-tap` chỉ có bậc viết code, không có animation giảng giải. Mọi bài vẫn có chế độ **chạy từng dòng** (bộ vẽ `code-bien`).

### 7.3. Trace — định dạng chung

Trace là chuỗi sự kiện mô tả một lần chạy. Cùng một định dạng dùng cho animation giảng giải (sinh sẵn từ lời giải mẫu), cho code học viên (sinh khi chạy thử) và cho SQL (do `sqlviz` sinh). Mỗi sự kiện là một đối tượng JSON trên một dòng:

```text
{"t":"hdr","lesson":"dsa.sap-xep.noi-bot","views":[{"v":"a","kieu":"mang"}]}
{"t":"line","n":4}
{"t":"read","v":"a","i":0}
{"t":"read","v":"a","i":1}
{"t":"set","v":"a","i":0,"x":1}
{"t":"set","v":"a","i":1,"x":5}
{"t":"say","s":"5 > 1 nên đổi chỗ"}
{"t":"key","s":"Hết lượt 1: 9 đã nổi lên cuối"}
{"t":"snap","n":200,"state":{"a":[1,4,2,5,9]}}
{"t":"end","truncated":false}
```

| Nhóm | Sự kiện |
| --- | --- |
| Chung | `hdr`, `line`, `vars` (biến cục bộ đổi giá trị), `out` (dòng in ra), `say`, `key`, `snap`, `end` |
| Mảng, chuỗi, bit | `read`, `set`, `ptr` (con trỏ i, j…) |
| Ngăn xếp, hàng đợi, DSLK, băm | `push`, `pop`, `enq`, `deq`, `link`, `unlink`, `bucket` |
| Cây, đồ thị | `node`, `edge`, `mark` (đổi màu), `rotate` |
| Lời gọi | `call` (hàm, tham số, id, id cha), `ret` (id, giá trị) |
| Bảng 2 chiều | `cell_r`, `cell_w` |
| SQL | `sql.table`, `sql.pairs`, `sql.keep`, `sql.group`, `sql.agg`, `sql.having`, `sql.project`, `sql.distinct`, `sql.order`, `sql.limit`, `sql.sub`, `sql.diff` |

- **`snap` (keyframe)**: trạng thái đầy đủ của mọi bộ vẽ, sinh mỗi 200 sự kiện. Muốn tua tới bước k, trình phát lấy keyframe gần nhất trước k rồi áp tiếp tối đa 199 sự kiện.
- **Trần**: 10 000 sự kiện và 2 MB mỗi trace. Vượt trần thì dừng ghi và đặt `truncated: true`. App báo "code có thể lặp vô hạn hoặc chậm hơn mức cần thiết".
- **Chủ của định dạng:** Người 3 sinh trace (Python và `sqlviz`), Người 4 đọc trace (`common.trace`, `player`). Đổi định dạng phải có cả hai duyệt.

### 7.4. Bộ vẽ

| Bộ vẽ (`kieu`) | Vẽ gì | Chủ |
| --- | --- | --- |
| `mang` | Cột hoặc ô, có con trỏ. Chế độ phụ: chuỗi có con trỏ (KMP), dải bit | Người 4 |
| `ngan-xep` | Ngăn xếp đứng, hàng đợi nằm ngang | Người 4 |
| `dslk` | Danh sách liên kết | Người 4 |
| `bam` | Bảng băm: dây chuyền, địa chỉ mở | Người 4 |
| `cay` | Cây nhị phân, AVL, heap (`heap` = list vẽ thành cây) | Người 1 |
| `cay-goi` | Cây lời gọi đệ quy, nhánh quay lui, nhánh bị cắt | Người 1 |
| `do-thi` | Đỉnh, cạnh, trọng số, nhãn | Người 1 |
| `luoi` | Bảng 2 chiều có mũi tên phụ thuộc: quy hoạch động, Floyd, bàn cờ | Người 3 |
| `bang-sql` | Bảng dữ liệu cho animation SQL | Người 3 |
| `code-bien` | Code với dòng đang chạy được tô sáng, bảng biến | Người 4 |

Giao diện chung do Người 4 định nghĩa trong `client.player`:

```java
public interface View {
    String kind();                          // "mang", "cay", …
    javafx.scene.Node node();
    void reset(JsonNode snapshot);          // dựng lại từ keyframe, không animation
    void apply(TraceEvent e, boolean animate, Duration d);
}
```

**Quy ước màu dùng chung cho mọi bộ vẽ:** vàng là đang xét · xanh lá là đã xong hoặc đúng chỗ · xanh dương là vừa ghi · đỏ là quay lui hoặc bị loại · xám là bị cắt hoặc bỏ qua. Màn hình luôn có chú thích màu.

### 7.5. Ghi trace cho bài DSA

Thư viện `viz` (trong `runner/`) cho ba cách ghi, mức chi tiết giảm dần:

1. **Cấu trúc "gắn camera"** — `viz.Mang`, `viz.Bang`. Ghi cả lần đọc lẫn lần ghi, nên thấy được phép so sánh và mũi tên phụ thuộc. Hai lệnh `set` liên tiếp đổi giá trị cho nhau thì trình phát vẽ thành một lần đổi chỗ. Dùng cho bài cơ bản và quy hoạch động; `khung.py` đã khai báo sẵn.
2. **Chụp biến theo khai báo** — học viên dùng `list`, `dict`, `deque` bình thường. Sau mỗi dòng, tracer chụp các biến khai báo trong `khung_nhin`, so với lần chụp trước rồi sinh `set`, `push`, `pop`… Chỉ thấy được lần ghi. Dùng cho bài khó (ví dụ Dijkstra: `adj` → `do-thi`, `dist` → `mang`, `pq` → `heap`).
3. **`sys.settrace`** — sinh `line` và `vars` cho chế độ chạy từng dòng; sinh `call` và `ret` cho các hàm khai báo trong `cay_goi`. Không cần sửa code học viên.

Lời giải mẫu gọi thêm `viz.say(...)` (thuyết minh) và `viz.key(...)` (bước quan trọng; trình phát có nút "tới bước quan trọng tiếp theo").

**Animation chỉ chạy với test nhỏ và trung bình:** 5–10 phần tử (đồ thị 5–8 đỉnh) cho xem kỹ; 20–50 phần tử (đồ thị 10–20 đỉnh) cho xem nhanh. Test lớn chạy **không bật tracer**, chỉ để chấm.

### 7.6. Bộ máy SQL

Bộ máy SQL nằm sau một giao diện, để đổi hệ quản trị mà không đổi phần còn lại:

```java
public interface SqlEngine {
    QueryResult run(DbHandle db, String sql, Duration timeout);
    List<TraceEvent> explainSteps(DbHandle db, String sql);   // cho animation
    DbState snapshot(DbHandle db);                              // chấm câu DML
}
```

Bản đầu cài bằng **SQLite** (`sqlite-jdbc` 3.45, hỗ trợ RIGHT/FULL JOIN và hàm cửa sổ). Môn học dùng hệ khác thì thêm một bản cài đặt chạy hệ đó trong Docker (giả định A2, §18).

**An toàn:**

- JSqlParser phân tích câu lệnh. Chỉ nhận **đúng một** câu, thuộc danh sách cho phép: `SELECT`, hoặc thêm `INSERT`/`UPDATE`/`DELETE` nếu bài là `dml`. `ATTACH`, `PRAGMA`, tạo/xoá bảng… đều bị từ chối.
- Mỗi lượt chạy dùng **bản sao** của database. Câu `SELECT` mở ở chế độ chỉ đọc.
- `Statement.setQueryTimeout(2)` chặn câu chạy mãi.

**Tách bước cho animation**, theo thứ tự thực thi logic:

| Bước | Cách lấy dữ liệu | Sự kiện |
| --- | --- | --- |
| FROM | Đọc từng bảng gốc kèm `rowid` | `sql.table` |
| JOIN (từng phép) | `SELECT t1.rowid, t2.rowid FROM t1 JOIN t2 ON …`. Với OUTER JOIN, `rowid` rỗng là dòng không có cặp | `sql.pairs` |
| WHERE | Chạy phần FROM…WHERE, lấy tập `rowid` còn lại | `sql.keep` |
| GROUP BY | Chạy phần trước GROUP BY kèm biểu thức khoá nhóm, **tự gom nhóm bằng Java** để biết dòng nào thuộc nhóm nào | `sql.group` |
| Hàm gộp | Chạy câu có GROUP BY thật, lấy giá trị COUNT/SUM/AVG… của từng nhóm | `sql.agg` |
| HAVING | `SELECT khoá, (biểu thức HAVING) FROM … GROUP BY …` → nhóm qua/không qua | `sql.having` |
| SELECT, DISTINCT | Câu đầy đủ bỏ ORDER BY và LIMIT | `sql.project`, `sql.distinct` |
| ORDER BY, LIMIT | Câu đầy đủ, ghi lại hoán vị dòng và số dòng bị cắt | `sql.order`, `sql.limit` |
| Truy vấn con | Chạy câu con trước, hiện thành bảng tạm. Câu con tương quan (EXISTS) chạy lại cho từng dòng ngoài, chỉ với bảng nhỏ | `sql.sub` |
| INSERT/UPDATE/DELETE | Chụp bảng trước và sau, so theo `rowid` | `sql.diff` |

Kỹ thuật `rowid` chỉ áp dụng cho **bảng gốc**. Bản đầu hỗ trợ: một câu SELECT với JOIN các loại, WHERE, GROUP BY, HAVING, DISTINCT, ORDER BY, LIMIT, truy vấn con trong WHERE (IN, EXISTS, trả về một giá trị), UNION/INTERSECT/EXCEPT, CASE, NULL; và một câu DML. **Không** hỗ trợ animation chi tiết cho CTE (`WITH`), bảng dẫn xuất trong FROM, hàm cửa sổ: các câu này vẫn chạy và chấm được, animation chỉ hiện kết quả cuối.

**Chấm SQL:** chạy câu học viên và câu đáp án trên database mẫu và **3 database ẩn** sinh từ `sinh_du_lieu.py` (có NULL, dòng trùng, bảng rỗng). So hai tập kết quả như **đa tập** (không quan tâm thứ tự dòng) trừ khi bài đặt `thu_tu: co`; số thực so tới 1e-6; số cột phải khớp, tên cột không cần. Bài DML so **toàn bộ trạng thái database** sau khi chạy.

### 7.7. Danh sách animation giảng giải dự kiến

Danh sách theo một học phần thông thường; sẽ chỉnh theo đề cương thật (giả định A1). ★ là 20 bài ưu tiên, làm trước. Cột "Người" là người soạn nội dung (§17.2); bộ vẽ do chủ bộ vẽ làm.

**DSA — 40**

| Chương | Thuật toán | Bộ vẽ | Người |
| --- | --- | --- | --- |
| Sắp xếp, tìm kiếm (9) | Nổi bọt ★, chọn, chèn, trộn, nhanh, vun đống, đếm; tìm tuần tự, nhị phân | `mang`, `cay` | 4 |
| Ngăn xếp, hàng đợi (4) | Kiểm tra ngoặc ★, trung tố → hậu tố ★, tính biểu thức hậu tố ★, hàng đợi vòng ★ | `ngan-xep`, `mang` | 2 |
| Danh sách liên kết (2) | Chèn và xoá, đảo ngược | `dslk` | 4 |
| Đệ quy, quay lui (6) | Cây lời gọi Fibonacci ★, hoán vị ★, tổ hợp ★, N-Queens ★, tổng tập con ★, Sudoku 4×4 ★ | `cay-goi`, `luoi` | 2 |
| Quy hoạch động (6) | Fibonacci có ghi nhớ ★, cái túi ★, LCS ★, LIS ★, đổi tiền ★, đường đi trên lưới ★ | `luoi`, `cay-goi` | 3 |
| Cây (4) | Cây nhị phân tìm kiếm, 4 kiểu duyệt, heap, AVL | `cay` | 1 |
| Đồ thị (7) | BFS ★, DFS ★, Dijkstra ★, Prim, Kruskal, sắp xếp tô-pô, Floyd | `do-thi`, `ngan-xep`, `luoi` | 1 |
| Băm (2) | Băm dây chuyền, băm địa chỉ mở | `bam` | 4 |

**SQL — 13**

| # | Khái niệm | Animation thể hiện | Người |
| --- | --- | --- | --- |
| 1 | SELECT, WHERE | Dòng không thoả mờ dần rồi biến mất | 3 |
| 2 | ORDER BY, LIMIT | Dòng trượt về đúng thứ tự, phần thừa bị cắt | 3 |
| 3 | DISTINCT | Dòng trùng chồng lên nhau rồi gộp lại | 3 |
| 4 | INNER JOIN | Đường nối các cặp dòng khớp | 1 |
| 5 | LEFT / RIGHT / FULL JOIN | Dòng không có cặp vẫn giữ, phần thiếu điền NULL | 1 |
| 6 | Tự nối, CROSS JOIN | Bảng nối với chính nó; mọi dòng ghép mọi dòng | 1 |
| 7 | GROUP BY và hàm gộp | Tô màu theo nhóm, dồn dòng, tính COUNT/SUM/AVG | 3 |
| 8 | HAVING | Nhóm không đạt bị gạch bỏ | 3 |
| 9 | Truy vấn con IN, trả về một giá trị | Câu con chạy trước thành bảng tạm | 2 |
| 10 | EXISTS, truy vấn con tương quan | Câu con chạy lại cho từng dòng | 2 |
| 11 | UNION, INTERSECT, EXCEPT | Hai tập dòng chồng lên nhau, giữ hợp/giao/hiệu | 2 |
| 12 | NULL, CASE | So sánh với NULL ra "không biết"; CASE rẽ nhánh từng dòng | 3 |
| 13 | INSERT, UPDATE, DELETE | Bảng trước và sau, dòng bị đổi sáng lên | 3 |

---

## 8. Chạy, chấm và luyện tập khắt khe

### 8.1. Sandbox

Mỗi lượt chạy là một container dùng một lần:

```text
docker run --rm -i --network none --memory 256m --memory-swap 256m --cpus 1
           --pids-limit 64 --read-only --tmpfs /tmp:size=16m
           --user 65534:65534 --cap-drop ALL --security-opt no-new-privileges
           labcast-runner:py3.12 python /runner/run.py
```

- Java gửi công việc (code, input, chế độ ghi trace) qua stdin dạng JSON, đọc trace và kết quả qua stdout dạng JSON từng dòng, đọc stderr để lấy lỗi Python.
- Thời gian bị giới hạn ở hai lớp: `RLIMIT_CPU` bên trong container và `docker kill` từ phía Java khi quá hạn.
- Máy server phải có Docker (giả định A5). **Chế độ không sandbox** — chạy `python` bằng tiến trình con có timeout — chỉ để phát triển trên máy cá nhân, server từ chối bật chế độ này khi đang có lớp học.

### 8.2. Chạy thử và nộp bài

```text
RUN_REQ(codeSeq) ──► hàng đợi chạy ──► container ──► TRACE_CHUNK… ──► RUN_RESULT
SUBMIT_REQ(codeSeq) ─► hàng đợi ưu tiên cao ─► chạy toàn bộ test chấm ─► SUBMIT_RESULT
```

- Lệnh chạy chỉ gửi `codeSeq`: server đã có code qua dòng `CODE_DELTA` (ĐG2), nên chạy **đúng phiên bản code đã được xác nhận**, không gửi lại code.
- **Chạy thử** dùng `test_xem`, bật tracer, gửi trace về để xem animation. Không giới hạn lượt, không tính điểm.
- **Nộp** chạy mọi test chấm: test cố định, test lớn ẩn (không tracer), rồi chạy một test trung bình có tracer để kiểm `luat.py`. Kiểm hàm cấm bằng `ast` trước khi chạy.
- Kết quả nộp chỉ trả **đúng/sai từng test**, thời gian, bộ nhớ. Không trả input hay output của test ẩn.

### 8.3. Đề biến thể và mã dùng một lần

- Khi nạp bài, server sinh sẵn 200 đề biến thể cho bậc dự đoán và bậc tự mô phỏng (chạy `du_doan.py` trong sandbox), kèm đáp án. Đáp án chỉ ở server.
- Đề biến thể, output của test ẩn và trace lời giải mẫu của một bài được sinh **trong một lần chạy sandbox duy nhất** cho bài đó, rồi lưu đệm theo `content_hash`. Lần khởi động sau chỉ sinh lại bài có nội dung đổi, nên không phải khởi động hàng nghìn container.
- Mỗi lần học viên xin đề, server cấp một đề chưa dùng kèm **mã đề (nonce) 128 bit, hết hạn sau 10 phút, dùng một lần**. Câu trả lời phải kèm mã đề.
- Gửi lại câu trả lời cũ (kể cả gói tin bắt được của lần trước) bị từ chối vì mã đề đã dùng.
- Đề tự mô phỏng kiểm từng bước bằng trace của **lời giải mẫu** chạy trên đúng input của đề đó. Bước học viên chọn phải khớp bước kế tiếp trong trace.

### 8.4. Luật chống học đối phó

| Luật | Mặc định |
| --- | --- |
| Thời gian chờ sau mỗi lần nộp sai | 0 → 15 s → 30 s → 60 s → 120 s (giữ 120 s) |
| Sai bậc dự đoán | Chờ 5 s, đề mới; đếm "đúng liên tiếp" về 0 |
| Nộp sai 3 lần liên tiếp | Khoá nộp đến khi làm đúng một đề dự đoán mới |
| Điểm của bài | 100, trừ 20 mỗi lần nộp sai, thấp nhất 40 |
| Thành thạo | Dự đoán đúng 3 đề liên tiếp; tự mô phỏng trọn 1 đề; code qua mọi test chấm |
| Mở bài sau | Khi bài hiện tại đạt thành thạo |
| Dán khối lớn | Một thay đổi chèn > 200 ký tự → đánh dấu trên sơ đồ lớp để giáo viên xem; không tự phạt |

Thời gian chờ và trạng thái khoá lưu trong bảng `progress`, nên khởi động lại app hay đổi máy cũng không thoát được.

### 8.5. Tìm phản ví dụ nhỏ

Sau 3 lần nộp sai, học viên được mở nút "tìm trường hợp nhỏ" (trừ 10 điểm). Server sinh test cỡ `nho` bằng `sinh_test.py`, chạy code học viên và lời giải mẫu, tối đa 200 lần hoặc 5 giây, dừng ở test **nhỏ nhất** cho kết quả khác nhau. Sau đó chạy test này có tracer để học viên xem animation chỗ sai.

---

## 9. Network Communication

Trả lời `Topics.md` §4.7.5 và §5.4.

| Kênh | Cổng | Dùng cho | Chủ |
| --- | --- | --- | --- |
| TCP, LCP/1.0 | 7000 | Phiên làm việc: đăng nhập, bài học, code, chạy, nộp, câu hỏi nhanh, trợ giúp, sửa gói multicast | Người 2 (nền), mỗi người phần thông điệp của mình |
| UDP multicast | 239.255.70.1:7001, TTL = 1 | Chiếu animation: điều khiển, dữ liệu trace, keyframe | Người 4 |
| UDP broadcast | 7002 | `DISCOVER` / `DISCOVER_REPLY` | Người 2 |
| TLS trên TCP | 7000 (bật bằng cấu hình, cả lớp cùng chế độ) | Mã hoá phiên làm việc | Người 2 |
| HTTP | 8080 | `/version` — chỉ bật khi chạy server trên VPS để nhóm thử TCP từ xa | Người 2 |

- **Thiết lập kết nối:** app gửi `DISCOVER` tới địa chỉ broadcast của từng card mạng; server trả tên lớp, cổng TCP, chế độ TLS và vân tay chứng chỉ. Không tìm được thì cho nhập IP.
- **Chọn card mạng cho multicast phía app:** sau khi kết nối TCP, app lấy `socket.getLocalAddress()`, tìm `NetworkInterface` chứa địa chỉ đó và tham gia nhóm multicast **trên đúng card đó**. Cách này tránh lỗi nghe nhầm card mạng ảo (VirtualBox, VMware, Hyper-V).
- **Chọn card mạng phía server:** máy server có Docker Desktop nên có thêm card ảo (vEthernet của WSL2). Server chọn **card LAN** theo cấu hình; mặc định là card có IPv4 riêng và không phải card ảo, hiện trên màn giáo viên để kiểm. Server phát multicast bằng `setNetworkInterface(card LAN)` và `DISCOVER_REPLY` quảng bá **địa chỉ của card LAN**, không phải địa chỉ card ảo.
- **Một kết nối TCP suốt buổi** cho mỗi app. Rớt thì tự kết nối lại và khôi phục (§10.4e).

---

## 10. Protocol LCP/1.0 và Data Format

Trả lời §4.7.6 và §4.7.7. **Protocol tự thiết kế**, không dùng HTTP.

### 10.1. Frame

```text
 0        2      3      4      5              9             13
 +--------+------+------+------+--------------+--------------+----------------+
 | MAGIC  | VER  | TYPE | FLAGS|     SEQ      |     LEN      |    PAYLOAD     |
 | 'L''C' | 1B   | 1B   | 1B   |    4B (BE)   |    4B (BE)   |   LEN bytes    |
 +--------+------+------+------+--------------+--------------+----------------+
```

- `MAGIC` phát hiện lệch khung và từ chối client lạ ngay từ byte đầu.
- `VER` cho phép nâng cấp protocol.
- `SEQ` là số thứ tự tăng đơn điệu theo chiều gửi của một kết nối. Trên multicast, `SEQ` là số thứ tự của luồng chiếu.
- `LEN` là độ dài payload, tối đa 8 MB. **Kiểm `LEN` trước khi cấp bộ nhớ.** Đọc bằng vòng `readFully`, vì TCP là dòng byte không có ranh giới thông điệp.
- `FLAGS`: bit 0 `COMPRESSED` (Deflate), bit 1 `JSON`, bit 2 `REQUIRES_ACK`, bit 3 `VIA_TCP` (gói chiếu được chuyển qua TCP cho máy không nhận được multicast).

Frame UDP dùng đúng định dạng này, mỗi datagram một frame, đọc bằng `Frame.fromBytes`.

### 10.2. Data format

- **Thông điệp nhỏ, tần suất cao** (`HEARTBEAT`, `TIME_SYNC_*`, `CODE_DELTA`, `CODE_ACK`, `CAST_CONTROL`, `CAST_KEYFRAME`, `CAST_NACK`, `MIRROR_DELTA`, `QUIZ_ANSWER`, `VIEW_CONTROL`): payload **nhị phân** qua `PayloadWriter`/`PayloadReader`; chuỗi mã hoá `[len:2][utf8]`, số nguyên big-endian.
- **Dữ liệu cấu trúc lớn, tần suất thấp** (`LESSON_DATA`, `TRACE_CHUNK`, `CAST_DATA`, `RUN_RESULT`, `SUBMIT_RESULT`, `CLASS_STATE`): payload **JSON UTF-8** (`FLAGS.JSON`), nén Deflate khi > 1 KB.
- **Datagram multicast** không vượt 1400 byte (13 byte header + tối đa 1387 byte payload), để không bị phân mảnh IP. Trace lớn hơn được chia khúc trong `CAST_DATA`.

### 10.3. Message types

| Nhóm | Types | Chủ |
| --- | --- | --- |
| Tìm server (UDP) | `DISCOVER`, `DISCOVER_REPLY` | 2 |
| Bắt tay | `HELLO`, `HELLO_ACK` | 2 |
| Xác thực | `AUTH`, `AUTH_OK`, `AUTH_FAIL` | 2 |
| Đồng bộ giờ | `TIME_SYNC_REQ`, `TIME_SYNC_RESP` | 3 |
| Liveness | `HEARTBEAT`, `HEARTBEAT_ACK` | 2 |
| Khôi phục | `RESUME_REQ`, `RESUME_STATE` | 2 (phiên), 1 (code) |
| Lớp học | `CLASS_JOIN`, `CLASS_STATE`, `LESSON_PUSH` | 2 |
| Bài học | `LESSON_LIST`, `LESSON_FETCH`, `LESSON_DATA` | 3 |
| Bậc dự đoán | `PREDICT_REQ`, `PREDICT_QUESTION`, `PREDICT_ANSWER`, `PREDICT_RESULT` | 3 |
| Bậc tự mô phỏng | `SIM_START`, `SIM_STEP`, `SIM_RESULT` | 3 |
| Dòng code | `CODE_DELTA`, `CODE_ACK`, `CHECKPOINT_LIST`, `CHECKPOINT_FETCH`, `CHECKPOINT_DATA` | 1 |
| Chạy, nộp | `RUN_REQ`, `RUN_QUEUED`, `TRACE_CHUNK`, `RUN_RESULT`, `SUBMIT_REQ`, `SUBMIT_RESULT`, `HINT_REQ`, `HINT_DATA` | 3 |
| Chiếu | `CAST_CONTROL`, `CAST_DATA`, `CAST_KEYFRAME`, `CAST_NACK`, `CAST_REPAIR` | 4 |
| Câu hỏi nhanh | `QUIZ_OPEN`, `QUIZ_ANSWER`, `QUIZ_CLOSE`, `QUIZ_RESULT` | 2 |
| Trợ giúp | `HAND_RAISE`, `HAND_LOWER`, `MIRROR_SUBSCRIBE`, `MIRROR_UNSUBSCRIBE`, `MIRROR_SNAPSHOT`, `MIRROR_DELTA`, `COMMENT_ADD`, `COMMENT`, `VIEW_JOIN`, `VIEW_CONTROL`, `VIEW_STATE` | 1 |
| Quản trị lớp | `ADMIN_ROSTER_IMPORT`, `ADMIN_PROGRESS_EXPORT`, `ADMIN_OK` | 2 |
| Lỗi | `ERROR(code, message, retryAfterMs)` | 2 |
| Kết thúc | `BYE(reason)` | 2 |

| Mã | Giá trị |
| --- | --- |
| `ErrorCode` | `BAD_FRAME`, `AUTH_REQUIRED`, `FORBIDDEN` (sai vai trò), `LOCKED` (bài chưa mở), `COOLDOWN`, `RATE_LIMITED`, `NONCE_INVALID`, `SQL_NOT_ALLOWED`, `SANDBOX_DOWN`, `INTERNAL` |
| Kết quả chấm | `OK`, `WRONG`, `TIME_LIMIT`, `MEMORY_LIMIT`, `RUNTIME_ERROR`, `FORBIDDEN_CALL`, `TRACE_RULE` |
| Lý do `BYE` | `LOGGED_IN_ELSEWHERE`, `SERVER_SHUTDOWN`, `CLIENT_EXIT`, `TOO_MANY_MESSAGES` |

Danh sách `MessageType` và `ErrorCode` là **file chung do leader (Người 2) giữ**; thêm loại mới bằng PR nhỏ riêng.

### 10.4. Communication sequence

**a. Vào lớp**

```text
App học viên                                  Server
  |== DISCOVER (UDP broadcast :7002) ========>|
  |<= DISCOVER_REPLY(lớp, cổng, TLS, vân tay) |
  |---- HELLO(ver) -------------------------->|
  |<--- HELLO_ACK(serverTime) ----------------|
  |---- AUTH(user, pwd) --------------------->|  PBKDF2; đăng nhập nơi khác → BYE cho phiên cũ
  |<--- AUTH_OK(token, role, classId) --------|
  |---- TIME_SYNC_REQ(t1) × 5 --------------->|  giữ mẫu RTT nhỏ nhất
  |<--- TIME_SYNC_RESP(t2, t3) ---------------|
  |---- CLASS_JOIN -------------------------->|  → CLASS_STATE (đẩy cho giáo viên)
  |     tham gia 239.255.70.1 trên card của kết nối TCP
  |---- LESSON_LIST ------------------------->|
  |<--- LESSON_DATA(danh sách, trạng thái khoá)
```

**b. Làm bài**

```text
  |---- LESSON_FETCH(id) -------------------->|
  |<--- LESSON_DATA(đề, khung, trace giảng giải)
  |---- PREDICT_REQ ------------------------->|
  |<--- PREDICT_QUESTION(nonce, đề) ----------|
  |---- PREDICT_ANSWER(nonce, đáp án) ------->|  nonce dùng một lần
  |<--- PREDICT_RESULT(đúng?, chờ ms) --------|
  |---- SIM_START --------------------------->|
  |<--- SIM_STEP(nonce, trạng thái) ----------|  hai chiều, mỗi bước một lần
  |---- SIM_STEP(nonce, thao tác) ----------->|
  |<--- SIM_RESULT(qua? / sai ở bước k) ------|
  |---- CODE_DELTA(seq = n) ----------------->|  gộp phím gõ mỗi 100 ms, ghi nhật ký trước khi gửi
  |<--- CODE_ACK(lastSeq = n) ----------------|
  |---- RUN_REQ(codeSeq) -------------------->|
  |<--- RUN_QUEUED(vị trí) -------------------|
  |<--- TRACE_CHUNK × k ----------------------|  animation chạy dần khi trace về
  |<--- RUN_RESULT(test xem: đúng/sai) -------|
  |---- SUBMIT_REQ(codeSeq) ----------------->|
  |<--- SUBMIT_RESULT(từng test, điểm, chờ ms, mở bài?)
  |---- HINT_REQ ---------------------------->|  sau 3 lần nộp sai
  |<--- HINT_DATA(test nhỏ, trace) -----------|
```

**c. Chiếu và câu hỏi nhanh**

```text
App giáo viên        Server                                 Các máy học viên
  |-CAST_CONTROL----->|  PLAY bước 37, tốc độ 1×                  |
  |  (TCP)            |== CAST_DATA × k (multicast) ==============>|  trace chia khúc ≤ 1387 B
  |                   |== CAST_CONTROL(PLAY, 37, at = now+300 ms) =>|  phát đúng giờ server
  |                   |== CAST_KEYFRAME mỗi 2 s ===================>|  máy vào muộn bắt kịp
  |                   |<-------- CAST_NACK(từ, đến) (TCP) ---------|  phát hiện thiếu SEQ
  |                   |--------- CAST_REPAIR (TCP) --------------->|
  |-QUIZ_OPEN-------->|--------- QUIZ_OPEN(đề, opensAt, closesAt) ->|  gửi trước ≥ 500 ms, TCP
  |                   |<-------- QUIZ_ANSWER(đáp án) --------------|  bù độ trễ (ĐG4)
  |                   |--------- QUIZ_CLOSE ---------------------->|
  |<-QUIZ_RESULT------|--------- QUIZ_RESULT --------------------->|
```

**d. Trợ giúp**

```text
App học viên              Server                       App giáo viên
  |-- HAND_RAISE --------->|-- CLASS_STATE(✋) --------->|
  |                        |<-- MIRROR_SUBSCRIBE(hv) ----|
  |                        |-- MIRROR_SNAPSHOT --------->|  code hiện tại + seq
  |-- CODE_DELTA --------->|-- MIRROR_DELTA ------------>|  gộp thành SNAPSHOT nếu giáo viên chậm
  |                        |<-- CHECKPOINT_LIST ---------|
  |                        |-- CHECKPOINT_DATA --------->|  dòng thời gian code
  |                        |<-- COMMENT_ADD(dòng 5, nd) -|
  |<-- COMMENT ------------|                             |
  |-- VIEW_JOIN(runId) --->|<-- VIEW_JOIN(runId) --------|  phiên xem chung
  |-- VIEW_CONTROL ------->|-- VIEW_STATE -------------->|  server xếp thứ tự lệnh
  |<-- VIEW_STATE ---------|                             |
  |-- HAND_LOWER --------->|<-- MIRROR_UNSUBSCRIBE ------|
```

**e. Rớt mạng và đổi máy**

```text
  |--- ✂ mất kết nối: app vẫn ghi nhật ký, gõ tiếp bình thường ---|
  |---- HELLO, RESUME_REQ(token, lessonId, lastAckedSeq) -------->|
  |<--- RESUME_STATE(serverLastSeq) ------------------------------|
  |---- CODE_DELTA(seq > serverLastSeq) × k --------------------->|  gửi bù, server bỏ trùng

  Đổi sang máy khác:
  |---- AUTH (máy mới) ------------------------------------------>|  → BYE(LOGGED_IN_ELSEWHERE) cho máy cũ
  |---- CHECKPOINT_FETCH(lessonId, latest) ---------------------->|
  |<--- CHECKPOINT_DATA(code hiện tại, seq) ---------------------|  làm tiếp từ seq + 1
```

---

## 11. Concurrency

Trả lời §4.7.8.

**Server:**

- Một luồng `accept`. Mỗi kết nối có **một luồng đọc** và **một luồng ghi** riêng. Quy mô ≤ 50 máy nên mô hình luồng chặn là đủ và dễ đọc; không dùng NIO.
- Xử lý nghiệp vụ trên một thread pool cố định (`2 × số nhân`). Luồng đọc không bao giờ làm việc nặng.
- Hàng đợi chạy code: tối đa `số nhân − 1` container cùng lúc. Mỗi học viên có **tối đa một lượt chạy thử đang chờ**: lượt mới thay lượt cũ chưa chạy. Lượt nộp xếp hàng ưu tiên cao hơn chạy thử.
- Luồng phát multicast: một luồng, **giãn nhịp** khi gửi `CAST_DATA` (tối đa 2 MB/s), vì phát dồn sẽ tràn bộ đệm nhận của máy học viên và gây mất gói.
- `ConcurrentHashMap` cho bảng phiên; `AtomicLong` cho bộ đếm; không giữ khoá khi ghi ra mạng.

**Backpressure — hàng đợi ghi có ưu tiên** (mỗi kết nối 256 phần tử):

| Lớp | Thông điệp | Khi hàng đợi đầy |
| --- | --- | --- |
| CRITICAL | `AUTH_*`, `QUIZ_OPEN`, `QUIZ_CLOSE`, `SUBMIT_RESULT`, `ERROR`, `BYE` | Chen lên đầu, không bao giờ bỏ |
| NORMAL | Phần lớn thông điệp | Chặn tối đa 5 s; quá thì đánh dấu kết nối `SUSPECT` |
| CONFLATABLE | `MIRROR_DELTA`, `CLASS_STATE`, `VIEW_STATE` | Bỏ các bản cũ đang chờ, thay bằng **một bản chụp mới nhất** |
| DROPPABLE | Gói chiếu chuyển qua TCP (trừ keyframe) | Bỏ bản cũ nhất; keyframe kế tiếp sẽ sửa trạng thái |

**App:**

- Chỉ luồng JavaFX được sửa giao diện. Luồng đọc mạng và luồng nghe multicast chuyển dữ liệu sang bằng `Platform.runLater`.
- Trình phát chạy bằng `AnimationTimer` trên luồng JavaFX, tính bước hiện tại từ `Clock.serverNow()` khi đang theo giáo viên.

**Tuỳ chọn socket đặt tường minh:**

| Tuỳ chọn | Giá trị | Lý do |
| --- | --- | --- |
| `TCP_NODELAY` | `true` | `CODE_DELTA`, `CAST_CONTROL`, `QUIZ_ANSWER` là gói nhỏ cần độ trễ thấp |
| `SO_TIMEOUT` | 3 × chu kỳ heartbeat lớn nhất | Luồng đọc không treo vĩnh viễn |
| `SO_KEEPALIVE` | `true` | Lớp phòng thủ cuối; không thay được heartbeat |
| Multicast `TTL` | 1 | Không ra khỏi mạng phòng máy |
| Multicast `SO_RCVBUF` | 1 MB | Chịu được đợt `CAST_DATA` |
| `IP_MULTICAST_LOOP` | `true` | Chạy nhiều máy ảo trên một máy khi đo |
| `SO_REUSEADDR` (UDP) | `true` | Nhiều tiến trình cùng nghe một cổng khi đo |

---

## 12. Error Handling

Trả lời §4.7.9.

| Tình huống | Xử lý |
| --- | --- |
| TCP trả về một phần frame | `readFully` đọc lặp đến đủ `LEN` byte |
| `MAGIC` sai, `LEN` > 8 MB, `TYPE` lạ | `ERROR(BAD_FRAME)` rồi đóng kết nối, ghi `audit_log` |
| Không có dữ liệu quá `SO_TIMEOUT` | Heartbeat đã đánh dấu `SUSPECT`; quá RTO thì `DISCONNECTED` |
| Rớt mạng khi đang gõ | Nhật ký trên máy giữ thay đổi; nối lại thì `RESUME_REQ` và gửi bù |
| `CODE_DELTA` đến không liền mạch (thiếu seq) | Không áp dụng; `CODE_ACK` trả `lastSeq` liền mạch cuối, client gửi lại từ đó |
| `CODE_DELTA` trùng | Bỏ qua (khoá chính `user, lesson, seq`) và vẫn ACK |
| Mất gói multicast | Phát hiện lỗ hổng `SEQ` → `CAST_NACK` qua TCP → `CAST_REPAIR`. Quá 20% lớp cùng NACK một gói trong 50 ms → phát lại gói đó qua multicast |
| Không nhận được multicast (tường lửa, switch chặn) | `HEARTBEAT` mang `lastCastSeq`; server thấy máy không theo kịp trong 3 chu kỳ keyframe → chuyển máy đó sang nhận qua TCP (`FLAGS.VIA_TCP`) |
| Máy vào lớp muộn | Đợi `CAST_KEYFRAME` (≤ 2 s); thiếu dữ liệu trace thì xin qua `CAST_NACK` |
| Máy xem chậm | Lớp CONFLATABLE thay thông điệp cũ bằng bản chụp |
| Đăng nhập ở hai máy | Giữ phiên mới, gửi `BYE(LOGGED_IN_ELSEWHERE)` cho phiên cũ: mỗi bài chỉ có một nơi ghi |
| Code lặp vô hạn, quá RAM | `RLIMIT_CPU`, giới hạn bộ nhớ của container, `docker kill`; trả `TIME_LIMIT` / `MEMORY_LIMIT` |
| Docker không chạy | Server không mở lớp, báo rõ cho giáo viên |
| Câu SQL cấm hoặc nhiều câu | Từ chối trước khi chạy, `ERROR(SQL_NOT_ALLOWED)` |
| Câu SQL chạy mãi | `setQueryTimeout(2)` |
| Mã đề đã dùng hoặc hết hạn | `ERROR(NONCE_INVALID)` |
| Gửi quá tần suất | `ERROR(RATE_LIMITED, retryAfterMs)`; quá 500 thông điệp/s trong 5 s thì đóng kết nối |
| Bài học lỗi khi nạp | Bỏ qua bài đó, ghi log; CI đã chặn từ trước (R4) |
| Server khởi động lại giữa buổi | Phiên, tiến độ, code nằm trong SQLite; app tự nối lại và khôi phục. Trạng thái chiếu đặt lại, giáo viên bấm phát lại |

---

## 13. Novelty and Contributions

Mỗi đóng góp có một chủ (§17) và ít nhất một thí nghiệm (§14).

### ĐG1 — Chiếu animation bằng sự kiện, phát hẹn giờ qua multicast tin cậy (Người 4)

- **Phát sự kiện, không phát hình.** Server phát lệnh điều khiển và dữ liệu trace; mỗi máy tự vẽ. Băng thông độc lập với độ phân giải màn hình, chữ luôn nét, học viên tách ra tự tua được.
- **Phát hẹn giờ.** `CAST_CONTROL` mang thời điểm `at` theo **đồng hồ server** (thường là `now + 300 ms`). Mỗi máy đổi `at` sang giờ máy mình bằng `Clock` của ĐG3. Mọi máy bắt đầu cùng lúc bất kể gói đến sớm hay muộn.
- **Tin cậy trên UDP.** Số thứ tự liên tục; thiếu thì NACK qua TCP; nhiều máy cùng thiếu thì phát lại qua multicast; `CAST_KEYFRAME` mỗi 2 s cho máy vào muộn; máy không nhận được multicast tự chuyển sang TCP.
- **Đo:** TN1, TN3. Độ lệch giữa các màn hình (TN2) đo chung với ĐG3.

### ĐG2 — Dòng code tin cậy theo thời gian thực (Người 1)

- Phím gõ được gộp mỗi 100 ms thành `CODE_DELTA(seq, vị trí, số ký tự xoá, chuỗi chèn)`, **ghi vào nhật ký trên máy trước khi gửi**, xoá khỏi nhật ký khi có `CODE_ACK`.
- Server lưu với khoá `(user, lesson, seq)` và chỉ áp dụng delta liền mạch: gửi lại bao nhiêu lần cũng ra cùng một trạng thái — *at-least-once trên dây, exactly-once về trạng thái*.
- **Mốc lưu** khi chạy thử, khi nộp và mỗi 30 s: dòng thời gian code, khôi phục, so sánh hai mốc. Đổi máy chỉ cần lấy mốc mới nhất cộng các delta sau nó.
- **Một dòng, hai nơi nhận.** Cùng dòng delta đó được chuyển tiếp tới giáo viên đang phản chiếu. Giáo viên xem chậm thì các delta đang chờ được **gộp** thành một `MIRROR_SNAPSHOT` (lớp CONFLATABLE), nên server không bao giờ bị kéo chậm.
- **Phiên xem chung:** giáo viên và học viên cùng xem một trace; lệnh điều khiển từ hai phía được server đánh số và phát lại cho cả hai theo cùng một thứ tự.
- **Đo:** TN4, TN5.

### ĐG3 — Đồng bộ đồng hồ kiểu NTP, có bù trôi và chỉnh dần (Người 3)

Mọi thứ "hẹn giờ" trong hệ thống — chiếu animation (ĐG1), hiện và đóng câu hỏi nhanh (ĐG4) — dựa vào một đồng hồ chung. Đồng hồ máy học viên thì không tin được: lệch vài giây, có khi bị chỉnh tay.

- **Đo độ lệch.** Mỗi lần đồng bộ gửi 5 cặp `TIME_SYNC_REQ(t1)` / `TIME_SYNC_RESP(t2, t3)`, ghi `t4` khi nhận. Độ lệch `θ = ((t2 − t1) + (t3 − t4)) / 2`, độ trễ `δ = (t4 − t1) − (t3 − t2)`. **Giữ mẫu có `δ` nhỏ nhất**, vì mẫu đó ít bị hàng đợi mạng làm méo nhất.
- **Bù trôi.** Đồng bộ lại mỗi 60 s. Từ các độ lệch gần nhất, ước lượng tốc độ trôi của đồng hồ máy (hồi quy tuyến tính), để giữa hai lần đồng bộ vẫn dự đoán được độ lệch.
- **Chỉnh dần.** Độ lệch mới được áp dần trong vài trăm mili giây, không nhảy một lần, để animation đang chạy không giật lùi hay nhảy cóc.
- **Một giao diện cho mọi người dùng:** `Clock.serverNow()`.
- **Đo:** TN2.

### ĐG4 — Câu hỏi nhanh công bằng nhờ bù độ trễ (Người 2)

- `QUIZ_OPEN` gửi qua TCP trước ít nhất 500 ms, mang `opensAt` và `closesAt` theo giờ server. Mọi máy hiện câu hỏi **cùng lúc** nhờ `Clock` của ĐG3.
- Server ước lượng thời điểm gửi của câu trả lời: `tGửi = tĐến − min(SRTT/2, 150 ms)`, với SRTT do **server tự đo** qua heartbeat thích nghi (SRTT/RTTVAR/RTO). Nhận nếu `tGửi ≤ closesAt`. Không tin giờ do máy học viên gửi lên.
- **Trần 150 ms** giới hạn lợi ích của máy cố tình làm chậm heartbeat để được thêm giờ.
- **Đo:** TN8.

### Hạ tầng (không tính là đóng góp chính)

Protocol LCP/1.0, hàng đợi ghi có ưu tiên, heartbeat thích nghi, tìm server bằng UDP broadcast, TLS tuỳ chọn, sandbox, bộ máy SQL, trình phát và các bộ vẽ.

**Server trọng tài cho luyện tập** (Người 3) cũng là hạ tầng, thuộc phần bảo mật, vì REST làm được tương tự:

- Đề biến thể có mã dùng một lần (§8.3): chép đáp án hay gửi lại gói tin cũ đều vô dụng.
- Giới hạn tốc độ theo người, kiểm **trước khi** thông điệp tới nghiệp vụ: thùng token cho từng loại thông điệp (ví dụ `RUN_REQ` 6 lượt/phút, `PREDICT_ANSWER` 1 lượt/3 s), cộng thời gian chờ tăng dần khi nộp sai (§8.4).
- Hàng đợi chạy công bằng: mỗi người tối đa một lượt chạy thử đang chờ; lượt nộp ưu tiên hơn.
- Đo ở TN6, TN7.

---

## 14. Evaluation

Trả lời §4.7.11 và `Instruction.md`. Mọi thí nghiệm ghi kết quả ra CSV trong `statics/results/`, lặp 3–5 lần, vẽ biểu đồ bằng một script chung.

**Cách tạo điều kiện mạng:** thêm độ trễ và mất gói bằng `tc netem` trên Linux, `clumsy` trên Windows, hoặc bằng lớp `DelayInjector` trong bộ chạy client ảo khi cần điều khiển riêng từng máy ảo. Quan sát gói tin bằng Wireshark.

| TN | Đo gì | Cách làm | Chủ |
| --- | --- | --- | --- |
| **1** | Băng thông chiếu | Đếm byte rời card mạng server khi chiếu cùng một animation 60 s, với: (a) multicast sự kiện, (b) TCP gửi riêng N máy, N = 5…50 (số byte xác định được, không cần đủ máy thật), (c) video màn hình: quay màn hình animation rồi mã hoá H.264 bằng `ffmpeg` (1080p, 15 fps) để lấy bitrate thật | 4 |
| **2** | Đồng hồ và độ lệch giữa các màn hình | Chạy N app trên **cùng một máy** (chung đồng hồ thật), cấy độ lệch đồng hồ giả ±2 s, độ trôi giả và độ trễ 0–200 ms (jitter 0–50 ms) vào từng app. (a) So độ lệch ước lượng với độ lệch đã cấy: chọn mẫu RTT nhỏ nhất so với lấy trung bình; có và không bù trôi. (b) Mỗi app ghi thời điểm vẽ bước k theo đồng hồ thật của máy: so đồng bộ + hẹn giờ với phát ngay khi nhận | 3 |
| **3** | Mất gói và vào lớp muộn | 4–5 máy thật cắm dây; cấy mất gói 1%, 5%, 10%. Đo số NACK, thời gian sửa, tỉ lệ máy nhận đủ; thời gian máy vào muộn bắt kịp | 4 |
| **4** | Mất code khi sự cố | Bot gõ một đoạn văn bản biết trước. Rút dây giữa chừng; tắt app bằng `taskkill /F`; đăng nhập máy khác. So code cuối trên server với văn bản gốc; đo thời gian khôi phục | 1 |
| **5** | Độ trễ phản chiếu | 10/20/40 học viên ảo cùng gõ 5 ký tự/s; giáo viên phản chiếu một bạn. Đo p50/p95 từ lúc gõ tới lúc hiện. Thêm giáo viên đọc chậm: có và không gộp thay đổi — so độ dài hàng đợi và độ trễ của các máy khác | 1 |
| **6** | Ảnh hưởng của spam | Một máy gửi 1000 thông điệp/s; 20 máy ảo khác làm bài bình thường. Đo độ trễ p95 của 20 máy đó khi bật và tắt giới hạn tốc độ | 3 |
| **7** | Chạy thử dồn tải | 40 lượt chạy thử trong 10 s; 1/2/4 container cùng lúc. Đo thời gian chờ, thời gian chạy, thông lượng, mức bận của worker (theo danh sách "Online Judge" của `Instruction.md`) | 3 |
| **8** | Công bằng câu hỏi nhanh | 20 máy ảo, một nửa bị thêm 200 ms. Bot trả lời ở thời điểm ngẫu nhiên trong 2 s cuối (theo giờ thật). So tỉ lệ bị tính trễ giữa hai nhóm, có và không bù. Thêm một máy cố tình làm chậm heartbeat: đo lượng thời gian được lợi | 2 |
| 9 *(tuỳ chọn)* | Phát hiện máy rớt | Heartbeat thích nghi so với chu kỳ cố định: thời gian phát hiện và số byte heartbeat | 2 |
| 10 *(tuỳ chọn)* | Chi phí TLS | Thời gian bắt tay, độ trễ và CPU khi bật/tắt TLS | 2 |

---

## 15. Database schema

SQLite trên máy server, `journal_mode=WAL`. Mọi câu có dữ liệu người dùng đi qua `PreparedStatement`. Chủ của từng nhóm bảng ghi ở cột cuối.

| Bảng | Cột chính | Chủ |
| --- | --- | --- |
| `users` | `id`, `username`, `full_name`, `role` (TEACHER/STUDENT), `pwd_hash`, `salt`, `created_at` | 2 |
| `sessions` | `token`, `user_id`, `created_at`, `last_seen`, `device` | 2 |
| `classes` | `id`, `name`, `teacher_id`, `created_at` | 2 |
| `enrollments` | `class_id`, `user_id` | 2 |
| `quizzes` | `id`, `class_id`, `lesson_id`, `question_json`, `answer_json`, `opens_at`, `closes_at` | 2 |
| `quiz_answers` | `quiz_id`, `user_id`, `answer_json`, `arrived_at`, `est_sent_at`, `srtt_ms`, `accepted`, `correct` | 2 |
| `audit_log` | `id`, `ts`, `actor`, `action`, `detail` | 2 |
| `lessons` | `id`, `mon`, `chuong`, `ten`, `loai`, `path`, `content_hash` | 3 |
| `progress` | `user_id`, `lesson_id`, `tier`, `status`, `streak`, `wrong_submits`, `score`, `cooldown_until`, `updated_at` | 3 |
| `variants` | `nonce`, `user_id`, `lesson_id`, `kind` (PREDICT/SIM), `question_json`, `answer_json`, `issued_at`, `expires_at`, `used_at` | 3 |
| `submissions` | `id`, `user_id`, `lesson_id`, `code_seq`, `kind` (RUN/SUBMIT/HINT), `verdict`, `detail_json`, `score`, `created_at` | 3 |
| `code_deltas` | `user_id`, `lesson_id`, `seq`, `pos`, `del_len`, `ins_text`, `client_ts`, `server_ts` — khoá chính `(user_id, lesson_id, seq)` | 1 |
| `checkpoints` | `id`, `user_id`, `lesson_id`, `upto_seq`, `code`, `reason` (RUN/SUBMIT/TIMER), `verdict`, `created_at` | 1 |
| `hand_raises` | `id`, `user_id`, `lesson_id`, `raised_at`, `handled_by`, `handled_at` | 1 |
| `comments` | `id`, `student_id`, `lesson_id`, `line`, `author_id`, `text`, `created_at` | 1 |

Trạng thái chiếu không lưu xuống database: nó chỉ sống trong một buổi.

---

## 16. Cấu trúc thư mục

Giữ hạ tầng của repo hiện tại (Maven Wrapper, husky, commitlint, CI, Docker cho VPS). Đổi tên repo và thay tài liệu sau khi giảng viên duyệt đề tài (kế hoạch, giai đoạn 0).

```text
LabCast/
├── source/                         Maven, Java 17
│   ├── common/   protocol (Frame, FrameCodec, MessageType, ErrorCode, Payload*)
│   │             trace (TraceEvent, TraceReader) · transport (SocketOptions, Tls)
│   ├── server/   net · session · account · presence · discovery · classroom · quiz
│   │             store · ops · sync · cast · lesson · practice · run · grade · sqlviz
│   │             journal · mirror   + resources/schema.sql
│   ├── client/   app · net · ui · quiz · clock · cast · player · presenter
│   │             views/{mang, nganxep, dslk, bam, cay, caygoi, dothi, luoi, bangsql, codebien}
│   │             editor · journal · mirror · practice
│   └── bench/    học viên ảo, DelayInjector, chạy thí nghiệm
├── runner/                         Python 3.12
│   ├── viz/      Mang, Bang, say, key, chụp biến
│   ├── tracer.py · run.py · kiem_cam.py (kiểm hàm cấm bằng ast)
│   ├── tests/    pytest
│   └── Dockerfile                  image labcast-runner:py3.12
├── content/      dsa/… · sql/…     (§7.1)
├── deploy/       Dockerfile, compose.yaml — server chạy trên VPS để nhóm thử TCP
├── packaging/    cấu hình jpackage, script mở tường lửa cho phòng máy
├── docs/         specs/ · PHAN-CONG.md · deploy/
├── report/ · statics/results/ · statics/dataset/
```

**Đóng gói:** `jpackage` tạo bộ cài `.msi` và bản chạy thẳng không cần cài (cho phòng máy có phần mềm đóng băng ổ cứng), đều **kèm sẵn Java và JavaFX**. GitHub Actions build trên máy Windows khi gắn tag và đưa lên GitHub Releases. Script `packaging/mo-tuong-lua.bat` (chạy bằng quyền admin) thêm luật tường lửa cho cổng 7000–7002.

**Thư viện:** `sqlite-jdbc`, JavaFX 21 (`javafx-controls`), RichTextFX, JSqlParser, SnakeYAML (đọc `lesson.yaml`), Jackson (JSON), JUnit 5. Phần mạng chỉ dùng thư viện chuẩn của Java.

---

## 17. Phân công 4 người

Chia **theo chức năng**: mỗi package một chủ, như cách làm cũ của nhóm. Mỗi người giữ một đóng góp mạng, một nhóm bộ vẽ và một phần nội dung.

### 17.1. Phần kỹ thuật

| | Người 1 | Người 2 (leader) | Người 3 | Người 4 |
| --- | --- | --- | --- | --- |
| **Mảng** | Làm bài và trợ giúp | Kết nối, lớp học, câu hỏi nhanh | Bài tập, chạy và chấm | Giảng và chiếu |
| **Đóng góp** | ĐG2 | ĐG4 | ĐG3 | ĐG1 |
| **Server** | `journal`, `mirror` | `net`, `session`, `account`, `presence`, `discovery`, `classroom`, `quiz`, `store`, `ops` | `sync`, `lesson`, `practice`, `run`, `grade`, `sqlviz` | `cast` |
| **App** | `editor`, `journal`, `mirror` | `app`, `net`, `ui`, `quiz` | `clock`, `practice` | `cast`, `player`, `presenter` |
| **Chung** | | `common.protocol`, `common.transport`, file chung | `runner/`, định dạng `content/` | `common.trace` |
| **Bộ vẽ** | `cay`, `cay-goi`, `do-thi` | — | `luoi`, `bang-sql` | `mang`, `ngan-xep`, `dslk`, `bam`, `code-bien` |
| **Thí nghiệm** | 4, 5 | 8, (9), (10) | 2, 6, 7 | 1, 3 |
| **Bảng DB** | `code_deltas`, `checkpoints`, `hand_raises`, `comments` | `users`, `sessions`, `classes`, `enrollments`, `quizzes`, `quiz_answers`, `audit_log` | `lessons`, `progress`, `variants`, `submissions` | — |

Leader còn giữ: `MessageType`, `ErrorCode`, các `pom.xml`, CI, đóng gói, README.

### 17.2. Phần nội dung (làm ở giai đoạn 4)

| | Animation giảng giải | Bài tập DSA | Bài tập SQL |
| --- | --- | --- | --- |
| Người 1 | Cây (4), đồ thị (7), SQL JOIN (3) — **14** | 30: cây, đồ thị | 20: JOIN |
| Người 2 | Ngăn xếp/hàng đợi (4), đệ quy/quay lui (6), SQL truy vấn con và tập hợp (3) — **13** | 30: đệ quy, quay lui, ngăn xếp, hàng đợi | 20: truy vấn con, phép tập hợp |
| Người 3 | Quy hoạch động (6), SQL cơ bản, gộp nhóm, NULL/CASE, DML (7) — **13** | 20: quy hoạch động | 30: cơ bản, gộp nhóm, DML |
| Người 4 | Sắp xếp/tìm kiếm (9), DSLK (2), băm (2) — **13** | 20: sắp xếp, tìm kiếm, DSLK, băm | 30: bài tổng hợp nhiều mệnh đề (loại `luyen-tap`) |
| **Tổng** | **53** | **100** | **100** |

Riêng 4 bài mẫu của bộ demo tối thiểu (§1 R5) do **chủ bộ vẽ** làm ngay trong giai đoạn 1–3 để kiểm bộ máy.

### 17.3. Chỗ giao nhau

| Giao diện | Người cung cấp | Người dùng |
| --- | --- | --- |
| `Frame`, `PayloadWriter/Reader`, `MessageType` | 2 | Tất cả |
| Server: `router.on(MessageType, Role…, handler)` | 2 | Tất cả |
| App: `client.send(...)`, `client.on(MessageType, listener)` | 2 | Tất cả |
| `Presence.srtt(userId)` | 2 | 2 (ĐG4) |
| `FramePolicy.check(session, type)` — `router` gọi **trước** khi chuyển thông điệp cho nghiệp vụ; trả cho qua, `RATE_LIMITED` hoặc `COOLDOWN` | 2 (giao diện, chỗ gọi) | 3 (cài đặt luật giới hạn tốc độ) |
| Trường mở rộng của `HEARTBEAT`: app `heartbeat.addField("lastCastSeq", …)`; server `presence.onHeartbeat(listener)` | 2 | 4 (phát hiện máy không nhận được multicast) |
| `ClassEvents.publish(userId, lessonId, status)` → sơ đồ lớp | 2 | 1, 3 |
| `Clock.serverNow()` | 3 | 2 (câu hỏi nhanh), 4 (phát hẹn giờ) |
| Định dạng trace (§7.3), `TraceEvent`, `TraceReader` | 3 (sinh), 4 (đọc) | 1, 3, 4 (bộ vẽ) |
| `View` (§7.4) | 4 | 1, 3 |
| `CodeStore.current(userId, lessonId)` → code tại `codeSeq` | 1 | 3 (chạy, nộp) |
| `RunService.run(request)` → trace + kết quả | 3 | 1 (phiên xem chung), 4 (chiếu bài học viên) |
| `SqlEngine` (§7.6) | 3 | 3 |

---

## 18. Giả định cần xác nhận

| # | Giả định | Nếu khác thì |
| --- | --- | --- |
| A1 | Danh sách animation ở §7.7 theo một học phần DSA và SQL thông thường | Chỉnh theo đề cương thật của hai môn |
| A2 | Môn SQL dùng cú pháp chuẩn, chạy được trên SQLite | Thêm bản cài đặt `SqlEngine` cho hệ quản trị của môn (SQL Server, MySQL, PostgreSQL) chạy trong Docker; kỹ thuật `rowid` đổi sang cột định danh tương đương |
| A3 | Có khoảng 11 tuần kể từ khi giảng viên duyệt | Cắt theo thứ tự ở §1 R5 |
| A4 | Phòng máy chạy Windows, mạng dây chung switch, ≤ 50 máy; học viên không có quyền admin | Wi-Fi: multicast kém, dựa nhiều hơn vào nhánh TCP |
| A5 | Máy chạy server có Docker (Windows: Docker Desktop + WSL2), ≥ 4 nhân, ≥ 8 GB RAM | Dùng laptop của nhóm làm máy server khi demo |
| A6 | Học viên viết bài DSA bằng Python 3.12 | Ngôn ngữ khác cần runner và tracer mới |
| A7 | Giảng viên chấp nhận app desktop | — |
| A8 | Tên "LabCast" | Đổi tên không ảnh hưởng thiết kế |

## 19. Giới hạn đã biết

1. Multicast chỉ chạy trong một mạng LAN; không dùng được qua Internet hay trên VPS.
2. Một server cho một lớp; server hỏng thì buổi học dừng tới khi khởi động lại.
3. Độ lệch giữa các màn hình không nhỏ hơn một khung hình JavaFX (~16,7 ms ở 60 fps).
4. Tracer làm code chậm đi nhiều lần; vì vậy chỉ bật cho test nhỏ và trung bình.
5. Kiểm hàm cấm bằng `ast` có thể bị lách (ví dụ `getattr`); luật soi trace là lớp kiểm thứ hai.
6. Dấu hiệu "dán khối lớn" chỉ là gợi ý cho giáo viên, không phải bằng chứng gian lận.
7. `QUIZ_OPEN` tới máy trước `opensAt` khoảng 0,5 s; một app bị sửa có thể đọc câu hỏi sớm chừng đó.
8. Animation SQL chi tiết chỉ áp dụng cho phạm vi ở §7.6.
9. Sandbox phụ thuộc Docker trên máy server.
