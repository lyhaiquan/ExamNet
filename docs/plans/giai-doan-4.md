# Giai đoạn 4 — Thí nghiệm và nội dung (tuần 9–10)

**Mục tiêu:** có số liệu thật cho 8 thí nghiệm bắt buộc (spec §14), mỗi mục tiêu O1–O6 (spec §3) có kết luận đạt hay không; đổ nội dung tới ≥ 100 bài (đích 200).

**Thứ tự trong hai tuần:** ngày 1–2 dựng khung đo (task 4.0); sau đó mỗi người chạy thí nghiệm của mình **buổi sáng** và soạn nội dung **buổi chiều**. Thí nghiệm cần máy thật (TN3) hẹn cả nhóm làm chung một buổi trong phòng máy.

Quy ước chung: xem [README.md](README.md).

---

## Quy tắc chung cho mọi thí nghiệm

- **Một lớp một thí nghiệm** trong `source/bench/src/main/java/labcast/bench/exp/`, tên `ExpN<TenNgan>.java`, chạy được bằng:

  ```bash
  cd source && ./mvnw -q package -DskipTests
  java -cp bench/target/labcast-bench.jar labcast.bench.exp.Exp1BangThong --lap 5
  ```

- **Kết quả** ghi CSV vào `statics/results/expN_<ten>.csv`, mỗi lần lặp một nhóm dòng, cột đầu luôn là `lan` (lần lặp) và các tham số của cấu hình. **Không sửa tay CSV.**
- **Lặp 3–5 lần** mỗi cấu hình; báo cáo trung bình ± độ lệch chuẩn, và p50/p95 cho độ trễ.
- **Ghi môi trường đo** vào đầu CSV dạng dòng chú thích `# cpu=…, ram=…, os=…, mang=…, ngay=…`.
- **Vẽ biểu đồ** bằng một script chung `statics/ve_bieu_do.py` (Người 4 viết), ra `statics/results/expN_<ten>.png`.
- Mỗi thí nghiệm có một mục ngắn trong `report/` (giai đoạn 5): đo gì, vì sao chọn chỉ số đó, kết quả, ý nghĩa, hạn chế (`Instruction.md` §17 — thầy sẽ hỏi đúng các câu này).

---

## Task 4.0: Khung đo (Người 4, ngày 1–2)

**Files:**
- Create: `source/bench/src/main/java/labcast/bench/VirtualStudent.java`
- Create: `source/bench/src/main/java/labcast/bench/VirtualTeacher.java`
- Create: `source/bench/src/main/java/labcast/bench/DelayProxy.java`
- Create: `source/bench/src/main/java/labcast/bench/DelayInjector.java`
- Create: `source/bench/src/main/java/labcast/bench/Csv.java`, `Stats.java`, `ExpArgs.java`
- Create: `statics/ve_bieu_do.py`
- Modify: `source/bench/pom.xml` — phụ thuộc `labcast-client` và `labcast-server` (để dựng server và app ảo trong cùng tiến trình); thêm shade plugin, `finalName` `labcast-bench`
- Test: `DelayProxyTest`, `StatsTest`

**Hợp đồng:**

```java
/** Học viên ảo: dùng đúng LcpClient, ClockSync, CastReceiver, JournalClient của app thật, không có giao diện. */
public final class VirtualStudent implements AutoCloseable {
    public static VirtualStudent connect(String host, int port, String user, String pass, Options o) throws Exception;
    public record Options(long clockOffsetMs, double driftPpm, boolean joinCast) {}
    public LcpClient client(); ClockSync clock(); Player player();
    public void typeText(String text, int charsPerSecond);     // gõ qua DeltaBatcher như người thật
}

/** Proxy TCP chen giữa app và server: thêm độ trễ, jitter, mất kết nối theo lệnh. */
public final class DelayProxy implements AutoCloseable {
    public DelayProxy(int listenPort, String targetHost, int targetPort);
    public void setDelay(long ms, long jitterMs);               // mỗi chiều
    public void cut();                                          // đóng mọi kết nối đang đi qua (giả lập rút dây)
    public void block(boolean on);                              // từ chối kết nối mới (giả lập dây còn rút)
    public int port();
}

/** Thêm độ trễ / mất gói phía NHẬN multicast của học viên ảo (vì nhiều app chung một máy). */
public final class DelayInjector {
    public static Consumer<Frame> wrap(Consumer<Frame> deliver, long delayMs, long jitterMs, double lossRate, Random r);
}

public final class Stats {
    public static double mean(long[] xs); double stddev(long[] xs); long percentile(long[] xs, double p);
}
```

`statics/ve_bieu_do.py` đọc mọi `statics/results/exp*.csv`, mỗi file một hình theo cấu hình khai báo ở đầu script (cột trục x, cột trục y, cột nhóm). Cần `python -m pip install matplotlib`.

**Test:** `DelayProxy` đặt trễ 100 ms → một lượt hỏi–đáp qua proxy mất ≥ 200 ms; `cut()` → hai đầu nhận `-1`; `Stats.percentile([1..100], 95) = 95`.

**Commit:** `test(bench): khung đo — học viên ảo, proxy cấy độ trễ, ghi CSV, vẽ biểu đồ`

---

## Task 4.1 — TN1: Băng thông chiếu (Người 4)

**Chứng minh:** O1 (phần băng thông), ĐG1. **File:** `Exp1BangThong.java` → `exp1_bang_thong.csv`.

| Cấu hình | Cách đo |
| --- | --- |
| (a) Multicast sự kiện | Bộ đếm byte trong `CastSender` (tổng độ dài datagram đã gửi) |
| (b) TCP gửi riêng N máy | Ép mọi học viên ảo vào chế độ TCP (`TcpFallback` bật cho tất cả), cộng byte của mọi `Connection` cho gói chiếu |
| (c) Video màn hình | Quay màn hình app giáo viên chạy cùng animation 60 s, mã hoá H.264 bằng `ffmpeg` |

- Cùng một animation: nổi bọt 30 phần tử, tốc độ 1×, 60 s; N = 5, 10, 20, 30, 40, 50 học viên ảo.
- (c) quay bằng `ffmpeg -f gdigrab -framerate 15 -i title="LabCast" -t 60 man_hinh.mkv` rồi `ffmpeg -i man_hinh.mkv -c:v libx264 -crf 28 -s 1920x1080 man_hinh.mp4`; bitrate lấy bằng `ffprobe -v error -show_entries format=bit_rate man_hinh.mp4`. Băng thông chiếu màn hình cho N máy = bitrate × N (phần mềm phòng máy gửi riêng từng máy).

**Cột CSV:** `lan, cach, so_may, thoi_gian_s, tong_byte, kbps`.

**Biểu đồ:** trục x số máy, trục y kbps (thang log), ba đường (a), (b), (c).

**Kết luận cần có:** (a) gần như không đổi theo N; (b) tăng tuyến tính theo N; tỉ lệ (c)/(a) ở N = 40 — mục tiêu ≥ 100 lần.

**Commit:** `test(bench): thí nghiệm 1 — băng thông chiếu multicast, TCP và video`

## Task 4.2 — TN2: Đồng hồ và độ lệch giữa các màn hình (Người 3)

**Chứng minh:** O1 (phần độ lệch), ĐG3. **File:** `Exp2DongHo.java` → `exp2a_sai_so_dong_ho.csv`, `exp2b_lech_man_hinh.csv`.

Chạy N = 10 học viên ảo **trong cùng một tiến trình** (chung đồng hồ thật của máy, nên đo được độ lệch chính xác). Mỗi học viên được cấy:
- độ lệch đồng hồ ngẫu nhiên trong ±2 000 ms (`Options.clockOffsetMs`);
- độ trôi ngẫu nhiên trong ±50 ppm (`Options.driftPpm`);
- độ trễ mạng 0, 50, 100, 200 ms, jitter 0–50 ms qua `DelayProxy` (TCP) và `DelayInjector` (multicast).

**(a) Sai số đồng hồ:** mỗi giây ghi `|ước lượng offset − offset thật|` của từng học viên trong 10 phút. So 3 cách: chọn mẫu RTT nhỏ nhất + bù trôi (đủ ĐG3); chọn mẫu RTT nhỏ nhất, không bù trôi; lấy trung bình 5 mẫu.

**(b) Độ lệch màn hình:** giáo viên ảo chiếu, `PLAY` với `at = now + 300 ms`. Mỗi học viên ghi `System.nanoTime()` lúc `Player.position()` lần đầu bằng k, với k = 10, 20, …, 100. Độ lệch của bước k = max − min trên N học viên. So: phát hẹn giờ theo `Clock`; phát ngay khi nhận gói.

**Cột CSV:** (a) `lan, cach, tre_ms, jitter_ms, giay, sai_so_ms`; (b) `lan, cach, tre_ms, jitter_ms, buoc, lech_ms`.

**Kết luận cần có:** p95 độ lệch màn hình ≤ 50 ms với phát hẹn giờ (O1); phát ngay khi nhận thì lệch xấp xỉ jitter; nhắc giới hạn một khung hình JavaFX (~16,7 ms, spec §19).

**Commit:** `test(bench): thí nghiệm 2 — sai số đồng hồ và độ lệch giữa các màn hình`

## Task 4.3 — TN3: Mất gói và vào lớp muộn (Người 4, cả nhóm làm chung một buổi)

**Chứng minh:** O2, ĐG1. **File:** `Exp3MatGoi.java` (chạy trên mỗi máy học viên thật, ghi log) → `exp3_mat_goi.csv`.

- 4–5 laptop cắm dây chung một switch. Một máy chạy server; các máy còn lại chạy `Exp3MatGoi` ở chế độ học viên.
- Cấy mất gói **phía nhận** trên từng máy học viên: Windows dùng `clumsy` (lọc `udp.DstPort == 7001`, *Drop* 1%, 5%, 10%), Linux dùng `sudo tc qdisc add dev <card> root netem loss 5%`.
- Giáo viên chiếu một trace 200 KB (khoảng 160 khúc `CAST_DATA`) rồi phát 60 s.
- Mỗi máy ghi: số gói multicast nhận được, số gói thiếu, số NACK đã gửi, thời gian từ lúc phát hiện thiếu tới lúc có đủ, có nhận đủ trace hay không.
- **Vào muộn:** khởi động một học viên ở giây thứ 20, 40 của buổi chiếu; đo thời gian tới khi `CastSession.following()` và vị trí khớp lớp.
- Thêm một lượt: máy học viên bật tường lửa chặn UDP 7001 → đo thời gian tới khi được chuyển sang TCP.

**Cột CSV:** `lan, may, ti_le_mat, goi_nhan, goi_thieu, so_nack, sua_p50_ms, sua_p95_ms, du_trace, vao_muon_ms, chuyen_tcp_ms`.

**Kết luận cần có:** với 5% mất gói mọi máy nhận đủ (O2); thời gian vào muộn ≤ 2 s; số lần phát lại multicast nhờ gộp NACK.

**Commit:** `test(bench): thí nghiệm 3 — mất gói multicast, NACK và vào lớp muộn`

## Task 4.4 — TN4: Mất code khi sự cố (Người 1)

**Chứng minh:** O3, ĐG2. **File:** `Exp4MatCode.java` → `exp4_mat_code.csv`.

Bot gõ một đoạn văn bản 2 000 ký tự biết trước, 5 ký tự/s, qua `VirtualStudent.typeText` (đi đúng đường `DeltaBatcher` → `LocalJournal` → gửi). Ba kịch bản, mỗi kịch bản 20 lần, sự cố xảy ra ở thời điểm ngẫu nhiên:

| Kịch bản | Cách tạo |
| --- | --- |
| Rút dây | `DelayProxy.cut()` + `block(true)` 10 s rồi `block(false)` |
| Tắt app đột ngột | Bot chạy **tiến trình riêng** (`java -cp … VirtualStudentMain`), bị `taskkill /F` (Linux `kill -9`); khởi động lại tiến trình bot trên cùng thư mục nhật ký |
| Đổi máy | Tắt bot như trên, bot thứ hai đăng nhập cùng tài khoản với **thư mục nhật ký trống** |

Đo: số ký tự mất = độ dài văn bản bot đã gõ trước sự cố − độ dài phần khớp trên server sau khôi phục; thời gian khôi phục (từ lúc có lại mạng / bật lại app tới khi `CODE_ACK` đuổi kịp).

**Cột CSV:** `lan, kich_ban, ky_tu_da_go, ky_tu_mat, khoi_phuc_ms`.

**Kết luận cần có:** rút dây → mất 0; tắt app → mất 0 (nhật ký trên đĩa); đổi máy → mất đúng phần chưa ACK lúc tắt (< 1 s gõ, tức ≤ 5 ký tự); thời gian khôi phục < 5 s (O3).

**Commit:** `test(bench): thí nghiệm 4 — mất code khi rút dây, tắt app, đổi máy`

## Task 4.5 — TN5: Độ trễ phản chiếu (Người 1)

**Chứng minh:** O4, ĐG2. **File:** `Exp5PhanChieu.java` → `exp5_phan_chieu.csv`.

- 10, 20, 40 học viên ảo cùng gõ 5 ký tự/s; giáo viên ảo phản chiếu một học viên.
- Độ trễ = lúc giáo viên áp xong `MIRROR_DELTA` − `clientTs` của delta đó (cùng tiến trình nên chung đồng hồ).
- **Giáo viên đọc chậm:** giáo viên ảo ngủ 200 ms sau mỗi frame đọc được. So server bật gộp thay đổi và tắt gộp (`--khong-gop`): ghi độ dài hàng đợi của kết nối giáo viên mỗi 100 ms và độ trễ `CODE_ACK` của các học viên **khác**.

**Cột CSV:** `lan, so_hoc_vien, gop, p50_ms, p95_ms, hang_doi_max, ack_khac_p95_ms`.

**Kết luận cần có:** p95 ≤ 200 ms với 40 học viên (O4); tắt gộp thì hàng đợi tăng không giới hạn tới khi đầy, bật gộp thì ≤ 33; độ trễ của học viên khác không đổi.

**Commit:** `test(bench): thí nghiệm 5 — độ trễ phản chiếu và gộp thay đổi`

## Task 4.6 — TN6: Ảnh hưởng của spam (Người 3)

**Chứng minh:** O5 (phần spam). **File:** `Exp6Spam.java` → `exp6_spam.csv`.

- 20 học viên ảo mỗi giây gửi một `LESSON_LIST` và đo thời gian tới `LESSON_DATA`.
- 1 máy spam gửi `PREDICT_ANSWER` với nonce rác, 1 000 thông điệp/s, trong 60 s.
- So server bật và tắt giới hạn tốc độ (`--khong-gioi-han`).

**Cột CSV:** `lan, gioi_han, p50_ms, p95_ms, so_bi_tu_choi, may_spam_bi_dong`.

**Kết luận cần có:** bật giới hạn thì p95 của 20 máy tăng ≤ 20% so với không có spam (O5); máy spam bị đóng kết nối sau 5 s.

**Commit:** `test(bench): thí nghiệm 6 — một máy spam không làm chậm cả lớp`

## Task 4.7 — TN7: Chạy thử dồn tải (Người 3)

**Chứng minh:** O5 (phần chạy thử); khớp danh sách "Online Judge" của `Instruction.md` §9. **File:** `Exp7DonTai.java` → `exp7_don_tai.csv`.

- 40 học viên ảo gửi `RUN_REQ` rải đều trong 10 s, mỗi người một lần, code nổi bọt đúng, test `tb-1` có tracer.
- Số container chạy cùng lúc 1, 2, 4 (`--so-worker`). Nếu kịp làm container khởi động sẵn thì thêm cấu hình "khởi động sẵn" so với "mỗi lượt một container mới".
- Ghi cho từng lượt: thời gian chờ trong hàng đợi, thời gian chạy, tổng thời gian; cho cả đợt: thông lượng (lượt/phút), mức bận của worker (% thời gian có việc).

**Cột CSV:** `lan, so_worker, khoi_dong_san, cho_p50_ms, cho_p95_ms, chay_p50_ms, tong_p95_ms, luot_moi_phut, ban_pct`.

**Kết luận cần có:** cả 40 lượt đều có kết quả; thời gian chờ giảm theo số worker; chi phí khởi động container chiếm bao nhiêu phần trăm.

**Commit:** `test(bench): thí nghiệm 7 — 40 lượt chạy thử dồn trong 10 giây`

## Task 4.8 — TN8: Công bằng câu hỏi nhanh (Người 2)

**Chứng minh:** O6, ĐG4. **File:** `Exp8CongBang.java` → `exp8_cong_bang.csv`.

- 20 học viên ảo; 10 máy đi qua `DelayProxy` thêm 200 ms mỗi chiều, 10 máy không.
- 30 câu hỏi, mỗi câu 10 s. Mỗi bot chọn một thời điểm **thật** ngẫu nhiên đều trong 2 s cuối trước `closesAt` (theo đồng hồ của máy đo, chung cho mọi bot) để bấm gửi.
- So server bật và tắt bù độ trễ (`--bu-do-tre tat`). Dữ liệu lấy từ bảng `quiz_answers`.
- **Máy gian lận:** một bot cố tình trả `HEARTBEAT_ACK` chậm 1 s để `SRTT` bị đội lên; đo nó được lợi thêm bao nhiêu mili giây.

**Cột CSV:** `lan, bu_do_tre, nhom (cham|thuong|gian_lan), so_tra_loi, so_bi_tinh_tre, ti_le_tre, loi_them_ms`.

**Kết luận cần có:** tắt bù thì nhóm chậm bị tính trễ nhiều hơn rõ; bật bù thì chênh lệch tỉ lệ bị tính trễ giữa hai nhóm ≤ 2 điểm phần trăm (O6); máy gian lận lợi tối đa 150 ms nhờ trần.

**Commit:** `test(bench): thí nghiệm 8 — công bằng của câu hỏi nhanh khi bù độ trễ`

## Task 4.9 — TN9 (tuỳ chọn): Phát hiện máy rớt (Người 2)

**File:** `Exp9Heartbeat.java` → `exp9_heartbeat.csv`. 20 học viên ảo qua `DelayProxy`; ở thời điểm ngẫu nhiên `cut()` + `block(true)` một máy. So heartbeat thích nghi với cố định 2 s (`--heartbeat co-dinh`): thời gian tới `SUSPECT`, tới `DISCONNECTED`, số byte heartbeat mỗi phút khi mạng ổn định. **Cột:** `lan, che_do, toi_suspect_ms, toi_disconnected_ms, byte_moi_phut`.

**Commit:** `test(bench): thí nghiệm 9 — heartbeat thích nghi và cố định`

## Task 4.10 — TN10 (tuỳ chọn): Chi phí TLS (Người 2)

**File:** `Exp10Tls.java` → `exp10_tls.csv`. 1 000 lần kết nối mới (đo thời gian bắt tay tới `HELLO_ACK`); 10 000 lượt hỏi–đáp `LESSON_LIST` trên một kết nối (đo độ trễ); CPU của server đo bằng `ThreadMXBean`. So bật và tắt TLS. **Cột:** `lan, tls, bat_tay_p50_ms, hoi_dap_p50_ms, hoi_dap_p95_ms, cpu_ms`.

**Commit:** `test(bench): thí nghiệm 10 — chi phí của TLS`

---

## Nội dung bài học

### Chia việc

Theo spec §17.2. Mỗi người khoảng 13 animation giảng giải và 50 bài tập:

| Người | Animation giảng giải | DSA | SQL |
| --- | --- | --- | --- |
| 1 | Cây (4), đồ thị (7), SQL JOIN (3) | 30: cây, đồ thị | 20: JOIN |
| 2 | Ngăn xếp/hàng đợi (4), đệ quy/quay lui (6), SQL truy vấn con và tập hợp (3) | 30: đệ quy, quay lui, ngăn xếp, hàng đợi | 20: truy vấn con, tập hợp |
| 3 | Quy hoạch động (6), SQL cơ bản, gộp nhóm, NULL/CASE, DML (7) | 20: quy hoạch động | 30: cơ bản, gộp nhóm, DML |
| 4 | Sắp xếp/tìm kiếm (9), DSLK (2), băm (2) | 20: sắp xếp, tìm kiếm, DSLK, băm | 30: tổng hợp nhiều mệnh đề |

Khi có đề cương thật của hai môn (giả định A1), chia lại theo chương của đề cương, giữ nguyên mỗi người ≈ 50 bài.

### Thứ tự làm

1. Animation ★ của mình (spec §7.7).
2. Bài tập có animation (bài `bai-tap`).
3. Bài chỉ chấm test (bài `luyen-tap`).

Mỗi PR gói **một chương** (5–15 bài), tiêu đề kiểu `feat(content): thêm 12 bài chương quy hoạch động`.

### Quy trình soạn một bài

1. Chép `content/_mau/` sang `content/<môn>/<chương>/<bài>/`.
2. Viết `de.md`: yêu cầu, định dạng input/output, giới hạn, một ví dụ nhỏ.
3. Viết `loi_giai.py`. Bài có animation: dùng cấu trúc `viz` hoặc khai báo `khung_nhin`; thêm `viz.say(...)` ngắn (≤ 12 từ) ở chỗ đáng giải thích và `viz.key(...)` ở mỗi mốc (hết một lượt, tìm thấy lời giải, bắt đầu truy vết).
4. Viết `khung.py`: giống lời giải nhưng **bỏ thân thuật toán**, giữ nguyên tên biến đã khai báo trong `khung_nhin`.
5. Viết `sinh_test.py` đủ ba cỡ `nho`, `tb`, `lon`; thêm `tests/` cho các trường hợp biên: rỗng, một phần tử, đã sắp, sắp ngược, toàn phần tử trùng, giá trị âm, giá trị lớn nhất.
6. Bài có bậc dự đoán: viết `du_doan.py`, đáp án **không** đoán mò được (gõ cả mảng, gõ số, chọn tập dòng — không trắc nghiệm).
7. Bài có luật hành vi: viết `luat.py` (ví dụ nổi bọt chỉ được đổi chỗ hai phần tử kề nhau).
8. Chạy kiểm nội dung trên máy mình: `java -cp source/server/target/labcast-server.jar labcast.server.lesson.ContentCheck content`.
9. Mở app, **tự làm bài như học viên**: đọc đề, làm bậc dự đoán, nộp một bài sai và một bài đúng.

### Danh sách kiểm một bài (người duyệt PR đánh dấu)

- [ ] Đề không mơ hồ; ví dụ trong đề chạy đúng với lời giải mẫu.
- [ ] Có test biên; có test `lon` đủ để code O(n²) quá giờ nếu đề đòi O(n log n).
- [ ] Animation với `demo.in` ≤ 10 phần tử (đồ thị ≤ 8 đỉnh); không `truncated`.
- [ ] Màu và thuyết minh theo quy ước spec §7.4.
- [ ] Bài SQL có ít nhất một database ẩn chứa NULL, dòng trùng và một bảng rỗng.
- [ ] genAI đã được dùng ở đâu thì người soạn đã tự kiểm lại chỗ đó.

---

## Nghiệm thu M4

- [ ] Đủ CSV và biểu đồ cho TN1–TN8 trong `statics/results/`.
- [ ] Mỗi mục tiêu O1–O6 có một đoạn kết luận "đạt / không đạt + vì sao" (nháp cho báo cáo §8.12–§8.13).
- [ ] Đủ 20 animation ★.
- [ ] ≥ 100 bài qua kiểm nội dung trên CI (đích 200).
- [ ] Mỗi người tự chạy lại được thí nghiệm của mình từ đầu theo đúng lệnh ghi trong task.
