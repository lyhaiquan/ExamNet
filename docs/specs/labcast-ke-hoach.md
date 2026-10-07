# LabCast — Kế hoạch triển khai

Đi kèm [`labcast-design.md`](labcast-design.md). Spec nói **làm gì và vì sao**, file này nói **ai làm gì, lúc nào, xong khi nào**.

- **Mốc thời gian** tính theo tuần, kể từ ngày giảng viên duyệt đề tài (gọi là T0). Có hạn nộp thật thì đổi ra ngày.
- **Tổng cộng khoảng 11 tuần** (giả định A3). Thiếu thời gian thì cắt theo thứ tự ở spec §1 R5, **không cắt ĐG1–ĐG4**.
- **Mỗi mốc có tiêu chí nghiệm thu.** Chưa đạt tiêu chí thì chưa sang giai đoạn sau, trừ khi cả nhóm đồng ý ghi lại phần nợ.
- Tên người theo spec §17: Người 1 làm bài và trợ giúp · **Người 2 leader**, kết nối và lớp học · Người 3 bài tập, chạy và chấm · Người 4 giảng và chiếu.

---

## Tổng quan

| Giai đoạn | Tuần | Mục tiêu | Mốc |
| --- | --- | --- | --- |
| 0. Đề xuất và chuẩn bị | Trước T0 → tuần 1 | Thầy duyệt; thử trước ba rủi ro lớn; dọn repo | M0 |
| 1. Xương sống | Tuần 2–3 | Một học viên xem được code của mình chạy thành animation qua mạng | M1 |
| 2. Lõi từng người | Tuần 4–6 | Mỗi đóng góp chạy được ở mức cơ bản; buổi học mini trên 3 máy | M2 |
| 3. Trợ giúp và hoàn thiện | Tuần 7–8 | Đủ 6 giai đoạn buổi học với bộ demo tối thiểu | M3 |
| 4. Thí nghiệm và nội dung | Tuần 9–10 | Đủ số liệu 8 thí nghiệm; đổ nội dung | M4 |
| 5. Đóng gói và bảo vệ | Tuần 11 | Báo cáo, bộ cài, diễn tập trong phòng máy thật | M5 |

---

## Giai đoạn 0 — Đề xuất và chuẩn bị (trước T0 → tuần 1)

### Trước khi thầy duyệt

| Việc | Người | Xong khi |
| --- | --- | --- |
| Gửi đề xuất một trang (spec §0) cho thầy, kèm sơ đồ kiến trúc §5.1 | 2 | Thầy trả lời |
| Xin đề cương hai môn DSA, SQL và hệ quản trị SQL môn học dùng | 2 | Chốt giả định A1, A2 |
| **Thử multicast trong phòng máy thật:** hai máy gửi/nhận một gói multicast. Máy **gửi** phải có cài Docker Desktop (có card vEthernet), máy nhận có card mạng ảo | 4 | Biết phòng máy có chặn multicast không, tường lửa có hỏi quyền không |
| **Thử Docker sandbox:** chạy một đoạn Python với đủ cờ ở spec §8.1 trên Windows (Docker Desktop) | 3 | Đo được thời gian khởi động một container |
| **Thử JavaFX đóng gói:** app "Hello" có một animation trượt, đóng thành `.msi` bằng `jpackage` | 2 | Cài được trên một máy không có Java |
| Đọc spec, cài JDK 17, Docker, Python 3.12, Node 22.12+ | Tất cả | — |

Ba việc thử trước chỉ là code nháp, không đưa vào repo. Mục đích là phát hiện sớm rủi ro phòng máy.

### Sau khi thầy duyệt (tuần 1)

| Việc | Người |
| --- | --- |
| Đóng PR #1 của ExamNet (không merge) | 2 |
| Đổi tên repo GitHub `ExamNet` → `LabCast` (GitHub tự chuyển hướng link cũ); đổi tên thư mục trên máy | 2 |
| Thay tài liệu ExamNet: xoá `examnet-design.md`, viết lại `README.md`, `docs/PHAN-CONG.md` theo spec §17 | 2 |
| Khung Maven mới theo spec §16: `common`, `server`, `client`, `bench`; `package-info.java` ghi chủ từng package | 2 |
| Thêm thư viện: JavaFX 21, RichTextFX, JSqlParser, SnakeYAML, Jackson; lớp `Launcher` để jar chạy được JavaFX | 2 |
| Khung `runner/` (Python, pytest, Dockerfile image `labcast-runner:py3.12`) | 3 |
| Khung `content/` với một bài mẫu rỗng và file kiểm định dạng `lesson.yaml` | 3 |
| CI: `./mvnw verify` + `pytest` + build image runner; workflow `release.yml` chạy `jpackage` trên Windows khi gắn tag | 2 |
| Cập nhật `commitlint.config.mjs`: thêm scope `runner`, `content`, `packaging` | 2 |
| Mời 3 thành viên, bật ruleset (PR + 1 review, squash), điền `CODEOWNERS` theo spec §17 | 2 |

**M0 — nghiệm thu:**

- [ ] Thầy đã duyệt đề tài.
- [ ] Ba việc thử trước có kết quả ghi lại trong `docs/`.
- [ ] `./mvnw verify` và `pytest` xanh trên CI.
- [ ] App JavaFX trống mở được từ bộ cài tạo bởi CI.

---

## Giai đoạn 1 — Xương sống (tuần 2–3)

| Người | Việc |
| --- | --- |
| 2 | `Frame`, `FrameCodec`, `PayloadWriter/Reader`, `MessageType`, `ErrorCode`. Server: vòng accept, luồng đọc/ghi riêng mỗi kết nối, hàng đợi ghi có giới hạn, `router.on(...)`. App: `client.send` / `client.on`. `HELLO`, `AUTH` (PBKDF2), bảng `users`, `sessions`. Nhập danh sách lớp từ CSV. `DISCOVER` / `DISCOVER_REPLY`. Màn đăng nhập có danh sách lớp tìm thấy |
| 4 | `common.trace` (đọc trace JSON từng dòng, keyframe). `client.player`: chạy, dừng, tua tới bước k qua keyframe, đổi tốc độ. Bộ vẽ `mang` |
| 3 | `runner/`: `viz.Mang`, tracer, `run.py` đọc công việc từ stdin, in trace ra stdout. `server.run`: chạy một container, đọc trace, gửi `TRACE_CHUNK` và `RUN_RESULT`. `server.lesson` nạp `lesson.yaml`. Bài mẫu **nổi bọt** đầy đủ thư mục. `TIME_SYNC` hai phía, giữ mẫu RTT nhỏ nhất, `Clock.serverNow()` bản đầu |
| 1 | `client.editor` (RichTextFX, tô màu Python). `CODE_DELTA` / `CODE_ACK` (chưa có nhật ký trên máy). `server.journal`: lưu delta, dựng lại code, `CodeStore.current(...)` |

**M1 — nghiệm thu (demo trên 2 máy cắm dây):**

- [ ] App học viên tự tìm thấy server, đăng nhập được.
- [ ] Mở bài nổi bọt, xem animation giảng giải do server gửi.
- [ ] Gõ code vào khung, bấm chạy thử, thấy animation từ **chính code vừa gõ**.
- [ ] Wireshark bắt được frame LCP/1.0 với `MAGIC` = `LC`.

---

## Giai đoạn 2 — Lõi từng người (tuần 4–6)

| Người | Việc | Đóng góp |
| --- | --- | --- |
| 4 | `server.cast`: phát multicast, giãn nhịp, chia khúc ≤ 1387 B. `client.cast`: tham gia nhóm **trên card mạng của kết nối TCP**, phát hiện thiếu `SEQ`. Server phát trên **card LAN** đã chọn. `CAST_CONTROL` hẹn giờ theo `Clock` của Người 3. `client.presenter` cho giáo viên. Bộ vẽ `ngan-xep`, `dslk`, `bam`. Bài mẫu **trung tố → hậu tố** | ĐG1 |
| 1 | Nhật ký thay đổi trên máy (ghi trước khi gửi, xoá khi có ACK). Tự gửi bù khi nối lại (`RESUME_*`). Bỏ trùng phía server. Mốc lưu, dòng thời gian, so sánh hai mốc, khôi phục. Đổi máy làm tiếp (`CHECKPOINT_FETCH`). Bộ vẽ `cay` | ĐG2 |
| 3 | Đồng hồ: đồng bộ lại mỗi 60 s, bù trôi, chỉnh dần. Chấm: test xem, test chấm, test lớn ẩn, kiểm hàm cấm, `luat.py`. Đề biến thể sinh sẵn theo lô, mã dùng một lần. Bậc dự đoán. Thời gian chờ, thành thạo, mở bài. Giới hạn tốc độ theo người qua `FramePolicy`. Hàng đợi chạy công bằng. `SqlEngine` bản SQLite: chạy, chấm, tách bước FROM/JOIN/WHERE. Bộ vẽ `bang-sql` | ĐG3 |
| 2 | Heartbeat thích nghi (SRTT/RTTVAR/RTO), `Presence.srtt(...)`, trạng thái có mặt, trường mở rộng của `HEARTBEAT` và `presence.onHeartbeat(...)`. Giao diện `FramePolicy` và chỗ gọi trong `router`. Chọn card LAN phía server, `DISCOVER_REPLY` quảng bá đúng địa chỉ. Sơ đồ lớp (`CLASS_STATE`, `ClassEvents.publish`). Giao bài (`LESSON_PUSH`). Câu hỏi nhanh với bù độ trễ. Lắp màn bài học: danh sách bài có khoá, các tab bậc học | ĐG4 |

**M2 — nghiệm thu (buổi học mini trên 3 máy cắm dây):**

- [ ] Giáo viên chiếu bài nổi bọt; hai máy học viên chạy khớp nhau bằng mắt thường; bấm dừng thì cả hai dừng.
- [ ] Giáo viên mở một câu hỏi nhanh; hai máy hiện cùng lúc; kết quả về màn giáo viên.
- [ ] Giáo viên giao bài; học viên qua bậc dự đoán rồi nộp code; sơ đồ lớp đổi màu đúng.
- [ ] Rút dây một máy giữa lúc gõ, cắm lại: không mất ký tự nào.
- [ ] Gửi lại một `PREDICT_ANSWER` cũ bằng script: server từ chối.

---

## Giai đoạn 3 — Trợ giúp và hoàn thiện (tuần 7–8)

| Người | Việc |
| --- | --- |
| 1 | Giơ tay và hàng chờ trợ giúp. Phản chiếu (`MIRROR_*`) có gộp thay đổi khi giáo viên chậm. Bình luận theo dòng. Phiên xem chung (`VIEW_*`). Đánh dấu "dán khối lớn". Bộ vẽ `cay-goi`, `do-thi`. Bài mẫu **N-Queens n = 4** (cùng Người 3 cho phần `luoi`) |
| 4 | `CAST_NACK` / `CAST_REPAIR`; phát lại qua multicast khi nhiều máy cùng thiếu. `CAST_KEYFRAME` cho máy vào muộn. Tự chuyển máy sang TCP khi `HEARTBEAT` báo `lastCastSeq` tụt lại. Chiếu bài học viên đã ẩn tên. Bộ vẽ `code-bien` |
| 3 | SQL đủ các bước ở spec §7.6, chấm DML. Bậc tự mô phỏng cho `mang` và `ngan-xep`. Tìm phản ví dụ nhỏ. Tracer cho chế độ chạy từng dòng. Bộ vẽ `luoi`. Bài mẫu **LCS** và **SQL GROUP BY**. Kiểm nội dung trên CI: lời giải mẫu qua hết test, trace không vượt trần |
| 2 | TLS tuỳ chọn và ghim vân tay chứng chỉ. Hoàn thiện bảng xử lý lỗi spec §12. Xuất tiến độ ra CSV. Bộ cài `.msi`, bản chạy thẳng, script mở tường lửa. `audit_log` |

**M3 — nghiệm thu (buổi học đủ 6 giai đoạn trên ≥ 4 máy cắm dây):**

- [ ] Chạy trọn kịch bản spec §6 với 4 bài mẫu và bài SQL GROUP BY.
- [ ] Giáo viên phản chiếu một học viên, bình luận; học viên sửa theo, qua bài; sơ đồ lớp chuyển xanh.
- [ ] Một máy bật tường lửa chặn multicast vẫn xem chiếu được (qua TCP).
- [ ] Một máy vào lớp giữa lúc đang chiếu bắt kịp trong ≤ 2 s.
- [ ] Bộ cài cài được trên máy phòng lab không có Java.

---

## Giai đoạn 4 — Thí nghiệm và nội dung (tuần 9–10)

### Thí nghiệm

Mỗi người chạy thí nghiệm của mình theo spec §14, ghi CSV vào `statics/results/`, lặp 3–5 lần, vẽ bằng script chung (Người 4 viết script).

| Người | Thí nghiệm | Cần chuẩn bị |
| --- | --- | --- |
| 4 | 1 băng thông chiếu · 3 mất gói và vào muộn | `ffmpeg` để đo bitrate video; 4–5 máy cắm dây; `clumsy` hoặc `tc netem` |
| 1 | 4 mất code khi sự cố · 5 độ trễ phản chiếu | Bot gõ văn bản biết trước; học viên ảo trong `bench` |
| 3 | 2 đồng hồ và độ lệch màn hình · 6 ảnh hưởng của spam · 7 chạy thử dồn tải | Cấy độ lệch, độ trôi, độ trễ giả vào app; script spam; máy server ≥ 4 nhân |
| 2 | 8 công bằng câu hỏi nhanh · (9) phát hiện máy rớt · (10) chi phí TLS | `DelayInjector` trong `bench` |

### Nội dung

Theo bảng spec §17.2: mỗi người khoảng 13 animation giảng giải và 50 bài tập. Thứ tự làm:

1. Animation ★ của mình trước.
2. Bài tập có animation.
3. Bài `luyen-tap` chỉ chấm test.

Mỗi bài phải qua kiểm nội dung trên CI. genAI được dùng để soạn đề, lời giải mẫu, bộ sinh test; **người soạn chịu trách nhiệm kiểm lại** và phải tự chạy được bài trước khi mở PR.

**M4 — nghiệm thu:**

- [ ] Đủ CSV và biểu đồ cho TN1–TN8.
- [ ] Mỗi mục tiêu O1–O6 (spec §3) có kết luận đạt hay không đạt, kèm giải thích.
- [ ] Đủ 20 animation ★; ≥ 100 bài qua kiểm nội dung (đích 200).

---

## Giai đoạn 5 — Đóng gói và bảo vệ (tuần 11)

| Việc | Người |
| --- | --- |
| Báo cáo theo `Instruction.md` §8: mỗi người viết phần đóng góp và thí nghiệm của mình; leader ghép và viết phần kiến trúc, protocol | Tất cả, ghép: 2 |
| README: cách cài, cách chạy, ảnh chụp, bảng thí nghiệm | 2 |
| Gắn tag phát hành, kiểm bộ cài và bản chạy thẳng | 2 |
| Quay video dự phòng toàn bộ kịch bản demo | 4 |
| Diễn tập bảo vệ trong phòng máy thật, mang theo một router nhỏ và dây mạng | Tất cả |

**M5 — nghiệm thu:**

- [ ] Chạy trọn kịch bản demo dưới đây không lỗi trong phòng máy thật.
- [ ] Mỗi người trả lời được các câu ở mục "Phải giải thích được".

---

## Kịch bản demo khi bảo vệ (~15 phút)

1. Mở app trên các máy: tự thấy lớp, không gõ IP. *(tìm server bằng UDP broadcast)*
2. Giáo viên chiếu bài nổi bọt, dừng ở bước 37: mọi máy cùng dừng. Mở Wireshark cho thấy gói chiếu chỉ vài chục byte. *(ĐG1)*
3. Một học viên tách ra tua lại, bấm "theo giáo viên" để về đúng chỗ. Bật một máy giữa chừng: máy đó bắt kịp ngay.
4. Chỉnh giờ một máy học viên lệch 2 phút rồi chiếu lại: máy đó vẫn chạy khớp cả lớp. *(ĐG3)*
5. Câu hỏi nhanh; một máy đang bị làm chậm 200 ms vẫn được tính đúng giờ. *(ĐG4)*
6. Giao bài; học viên nộp sai; sơ đồ lớp đỏ; thời gian chờ tăng dần. Chạy script gửi lại gói trả lời cũ và script spam: server từ chối, các máy khác không chậm. *(server trọng tài)*
7. Học viên giơ tay; giáo viên phản chiếu, xem dòng thời gian, bình luận; học viên sửa và qua bài. *(ĐG2)*
8. Rút dây máy học viên giữa lúc gõ, cắm lại: không mất chữ. Đăng nhập sang máy khác: làm tiếp đúng chỗ. *(ĐG2)*
9. Animation SQL GROUP BY và bài quay lui N-Queens.
10. Trình bày số liệu TN1–TN8.

## Phải giải thích được (mỗi người)

| Người | Câu hỏi thầy có thể hỏi |
| --- | --- |
| 1 | Vì sao ghi nhật ký **trước** khi gửi? Delta trùng hoặc thiếu thì server làm gì? "At-least-once trên dây, exactly-once về trạng thái" nghĩa là gì? Gộp thay đổi khi giáo viên chậm hoạt động ra sao, vì sao không làm server chậm? |
| 2 | Vì sao cần `LEN` và `MAGIC`, vì sao `read` phải đọc lặp? Vì sao mỗi socket chỉ một luồng được ghi? Hàng đợi ghi đầy thì xử lý thế nào? Công thức SRTT/RTTVAR/RTO? Vì sao bù độ trễ phải có trần, và vì sao không tin giờ máy học viên? |
| 3 | Đồng bộ đồng hồ kiểu NTP tính độ lệch thế nào, vì sao giữ mẫu RTT nhỏ nhất? Bù trôi và chỉnh dần để làm gì? Vì sao không chấm trên máy học viên? Mã dùng một lần chặn tấn công gửi lại gói như thế nào? Thùng token hoạt động ra sao? Sandbox chặn những gì, cờ nào chặn cái nào? Vì sao không lấy được bảng GROUP BY bằng cách cắt câu SQL? |
| 4 | Vì sao phát sự kiện thay vì phát hình? Phát hẹn giờ hoạt động thế nào? Vì sao datagram ≤ 1400 byte? Vì sao phải chọn card mạng khi phát và khi nghe multicast? NACK, keyframe, chuyển sang TCP giải quyết những trường hợp nào? |

---

## Quy tắc làm việc

Giữ quy trình của repo hiện tại (`CONTRIBUTING.md`):

- Mỗi việc một nhánh, mở PR vào `main`, cần 1 người duyệt, chỉ squash merge.
- Commit theo Conventional Commits, scope bắt buộc. Husky chặn commit sai mẫu.
- Chỉ sửa package của mình. Chạm phần người khác thì nhờ chủ phần đó duyệt.
- Đổi file chung (`MessageType`, `ErrorCode`, `pom.xml`, CI) bằng PR nhỏ riêng, leader duyệt.
- Đổi định dạng trace (spec §7.3) phải có cả Người 3 và Người 4 duyệt.

**Thứ tự bắt đầu giai đoạn 1:** leader mở PR khung protocol và `router` trước (ngày đầu tuần 2). Trong lúc chờ, người khác làm phần không cần mạng: Người 4 trình phát đọc trace từ file, Người 3 runner chạy độc lập, Người 1 ô soạn code.

**Họp nhóm:** 15 phút mỗi tuần, đi qua bảng mốc. Phần nào trễ quá một tuần thì cắt theo thứ tự spec §1 R5.

## Rủi ro và phương án

| Rủi ro | Dấu hiệu sớm | Phương án |
| --- | --- | --- |
| Phòng máy chặn multicast | Việc thử ở giai đoạn 0 thất bại | Demo bằng router của nhóm; nhánh TCP luôn chạy được |
| Máy giáo viên không có Docker | Không cài được Docker Desktop | Dùng laptop nhóm làm máy server khi demo |
| JavaFX đóng gói lỗi trên máy lab | Bộ cài không mở được | Bản chạy thẳng từ USB hoặc thư mục dùng chung |
| Nội dung quá nhiều | Tuần 10 chưa đủ 100 bài | Dừng ở số bài hiện có; bộ demo tối thiểu vẫn đủ |
| Môn SQL dùng hệ khác SQLite | Đề cương ghi SQL Server hoặc MySQL | Thêm bản cài đặt `SqlEngine` chạy trong Docker, ưu tiên sau ĐG |
| Một người trễ phần mạng | Không đạt mốc M2 | Leader hỗ trợ; tạm cắt phần nội dung của người đó |
| Thầy không chấp nhận desktop | Phản hồi đề xuất | Bàn lại trước T0, chưa có code nào phải bỏ |
