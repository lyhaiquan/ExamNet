# Giai đoạn 5 — Đóng gói và bảo vệ (tuần 11)

**Mục tiêu:** báo cáo hoàn chỉnh, Compilatio ≤ 20%; bản phát hành chạy được trên máy sạch; nộp source theo hướng dẫn của thầy; cả nhóm diễn tập trọn kịch bản demo trong phòng máy thật và trả lời được câu hỏi về phần của mình.

**Đầu vào:** hệ thống đạt M3, số liệu và biểu đồ của M4.

---

## Task 5.1: Báo cáo

**File:** `report/report.pdf` (soạn bằng Word hoặc Google Docs rồi xuất PDF; nguồn soạn để cùng thư mục).

Cấu trúc theo `Instruction.md` §8. Mỗi mục có người viết chính và nguồn lấy ý:

| Mục báo cáo | Lấy từ | Người viết |
| --- | --- | --- |
| 8.1 Title, 8.2 Abstract, 8.3 Keywords | spec §0 | 2 |
| 8.4 Introduction | spec §2 | 2 |
| 8.5 Background / Related Work | spec §2.1; thêm tài liệu về NTP (RFC 5905), RFC 6298, multicast tin cậy, phần mềm phòng máy | 4 (multicast), 3 (đồng hồ), 1 (dòng code), 2 (còn lại) |
| 8.6 Problem Definition | spec §2, §3 | 2 |
| 8.7 Proposed Approach | spec §1, §13 | Mỗi người một đoạn về ĐG của mình |
| 8.8 System Architecture | spec §5, `statics/architecture.png` | 2 |
| 8.9 Network Communication Design | spec §9, §10, §11, §12; sơ đồ tuần tự | 2 (khung, protocol), mỗi người phần thông điệp của mình |
| 8.10 Implementation | code thật: lớp chính, luồng, hàng đợi | Mỗi người phần của mình |
| 8.11 Experimental Setup | spec §14, `docs/plans/giai-doan-4.md` | Người phụ trách từng thí nghiệm |
| 8.12 Results | `statics/results/*.png`, bảng trung bình ± độ lệch chuẩn | Người phụ trách từng thí nghiệm |
| 8.13 Discussion | kết luận O1–O6 ở M4 | 2 ghép, mọi người góp |
| 8.14 Novelty and Contributions | spec §13; bảng "cách làm thông thường so với LabCast" ở README §6 | Mỗi người một ĐG |
| 8.15 Limitations | spec §19, cập nhật theo hệ thống thật | 2 |
| 8.16 Future Work, 8.17 Conclusion | | 2 |
| 8.18 References | | Mọi người, leader gom |
| 8.19 Appendix | bảng mã thông điệp, payload, lược đồ database | 2 |

**Hình cần vẽ** (để trong `statics/`, xuất PNG):

- `architecture.png` — sơ đồ phòng máy và các thành phần (spec §5.1, §5.2, §5.3).
- `sequence-vao-lop.png`, `sequence-lam-bai.png`, `sequence-chieu.png`, `sequence-tro-giup.png`, `sequence-khoi-phuc.png` — năm sơ đồ tuần tự ở spec §10.4.
- `frame.png` — khung 13 byte của LCP/1.0.
- Mỗi ĐG một hình minh hoạ cơ chế: hẹn giờ và NACK (ĐG1), nhật ký và gộp thay đổi (ĐG2), đo độ lệch kiểu NTP (ĐG3), bù độ trễ có trần (ĐG4).

**Quy tắc viết** (`Instruction.md` §10, §11):

- Không chép nguyên văn từ Internet hay báo cáo nhóm khác; dùng ý của ai thì trích dẫn.
- Số liệu chỉ lấy từ CSV của nhóm, ghi rõ cấu hình đo.
- Phần nào có dùng genAI hỗ trợ thì người viết phần đó phải tự đọc lại, tự giải thích được.
- Chạy Compilatio trước hạn ít nhất 3 ngày để còn thời gian sửa; mục tiêu ≤ 20%.

**Xong khi:** PDF đủ 19 mục, mọi hình có chú thích, mọi số liệu có nguồn CSV, Compilatio ≤ 20%.

**Commit:** `docs(report): hoàn thiện báo cáo và hình minh hoạ`

---

## Task 5.2: README hoàn chỉnh (Người 2)

Điền mọi mục còn ghi *(giai đoạn N)* trong `README.md`:

- §1.3 bảng nhóm: mã sinh viên, họ tên, email, phần phụ trách, tỉ lệ đóng góp (khớp lịch sử PR).
- §1.4 tên giảng viên.
- §3 chèn `statics/architecture.png`.
- §4 tóm tắt protocol, khung frame, bảng thông điệp chính, một sơ đồ tuần tự.
- §10 mọi tham số dòng lệnh và biến môi trường của server.
- §12.2 cách chạy app (bộ cài, bản chạy thẳng, chạy từ jar).
- §14 bảng kết quả thí nghiệm (Baseline so với LabCast), mỗi dòng trỏ tới biểu đồ.
- §15–§17 thảo luận, giới hạn, hướng phát triển (rút gọn từ báo cáo).

**Kiểm:** một bạn **không** viết README làm theo từ đầu trên máy sạch, từ `git clone` tới chạy được server và app, không cần hỏi ai.

**Commit:** `docs(repo): hoàn thiện README cho bản nộp`

---

## Task 5.3: Bản phát hành (Người 2)

- [ ] Gắn tag `v1.0.0` trên `main`. Workflow `release.yml` (giai đoạn 0) tạo `.msi` và `LabCast-1.0.0-chay-thang.zip`.
- [ ] Thêm vào release file image runner để máy giáo viên không cần build hay mạng Internet:

  ```bash
  docker build -t labcast-runner:py3.12 runner
  docker save labcast-runner:py3.12 | gzip > labcast-runner-py3.12.tar.gz
  gh release upload v1.0.0 labcast-runner-py3.12.tar.gz labcast-server.jar
  ```

  Trên máy giáo viên: `docker load < labcast-runner-py3.12.tar.gz`.
- [ ] Cài thử trên một máy Windows sạch (không Java, không Python): app chạy, tìm thấy server, đăng nhập, xem chiếu.

---

## Task 5.4: Nộp source (Người 2)

`Instruction.md` §14: nộp qua Google Drive theo link thầy thông báo.

- [ ] Tạo gói nộp từ đúng commit đã gắn tag (không kèm file rác trên máy):

  ```bash
  git archive --format=zip --prefix=LabCast/ -o LabCast-v1.0.0-source.zip v1.0.0
  ```

- [ ] Kèm vào thư mục Drive: `LabCast-v1.0.0-source.zip`, `report.pdf`, bộ cài `.msi`, bản chạy thẳng `.zip`, `labcast-runner-py3.12.tar.gz`, video dự phòng (task 5.5).
- [ ] Giải nén gói source trên máy khác, làm theo README tới `./mvnw verify` xanh.
- [ ] Kiểm link Drive mở được bằng tài khoản không thuộc nhóm.
- [ ] Không có khoá, `.p12`, `.db` trong gói (`unzip -l LabCast-v1.0.0-source.zip | grep -E "\.p12|\.db|deploy"` không ra gì).

---

## Task 5.5: Video dự phòng (Người 4)

- [ ] Quay toàn bộ kịch bản demo (mục dưới) trên ≥ 4 máy thật, bằng OBS: một khung quay màn hình giáo viên, một khung quay điện thoại hướng vào các màn hình học viên (để thấy chúng chạy khớp nhau).
- [ ] Chèn phụ đề ngắn cho từng bước; dài ≤ 15 phút.
- [ ] Dùng khi phòng bảo vệ có sự cố mạng hay thiết bị.

---

## Task 5.6: Diễn tập bảo vệ (cả nhóm)

**Mang theo:** router hoặc switch nhỏ, 5 dây mạng, ≥ 4 laptop đã cài app và sạc đầy, laptop server có Docker và image runner đã `docker load`, USB chứa bản chạy thẳng, video dự phòng.

**Kịch bản ~15 phút** (chi tiết ở `docs/specs/labcast-ke-hoach.md` mục "Kịch bản demo khi bảo vệ"):

| Phút | Phần | Người trình bày |
| --- | --- | --- |
| 0–2 | Bài toán, kiến trúc, protocol LCP/1.0 | 2 |
| 2–5 | Tìm server; chiếu animation, dừng ở bước 37; Wireshark gói chiếu (ĐG1) | 4 |
| 5–6 | Chỉnh giờ một máy lệch 2 phút, vẫn chạy khớp (ĐG3) | 3 |
| 6–7 | Câu hỏi nhanh với máy bị làm chậm 200 ms (ĐG4) | 2 |
| 7–9 | Giao bài, nộp sai, thời gian chờ, script gửi lại gói cũ và script spam bị chặn | 3 |
| 9–11 | Giơ tay, phản chiếu, bình luận, rút dây và đổi máy (ĐG2) | 1 |
| 11–12 | Animation SQL GROUP BY, N-Queens | 3, 1 |
| 12–15 | Số liệu TN1–TN8 | Mỗi người thí nghiệm của mình |

**Luyện hỏi đáp:** mỗi người trả lời được các câu ở mục "Phải giải thích được" (`docs/specs/labcast-ke-hoach.md`) và các yêu cầu ở `Instruction.md` §11, §17:

- Giải thích một lớp, một hàm bất kỳ trong package của mình.
- Vẽ lại luồng thông điệp của tính năng mình làm.
- Chỉ ra luồng nào chạy ở đâu, khoá nào giữ ở đâu.
- **Sửa một lỗi nhỏ ngay tại chỗ:** mỗi người tự tạo một lỗi trong phần của mình (ví dụ đổi `<=` thành `<` ở bù độ trễ), nhờ bạn khác tìm và sửa trong 5 phút, rồi đổi vai.
- Giải thích thí nghiệm: đo thế nào, vì sao chọn chỉ số đó, kết quả nghĩa là gì, hạn chế.

**Xong khi:** chạy trọn kịch bản hai lần liền trong phòng máy không lỗi, mỗi lần ≤ 15 phút.

---

## Task 5.7: Rà danh sách cuối (`Instruction.md` §18)

| Mục | Ở đâu |
| --- | --- |
| Topic hợp lệ, đã được duyệt | M0 |
| Problem, Objective | báo cáo 8.6, spec §2–§3 |
| Architecture diagram | `statics/architecture.png` |
| Network communication, protocol/message format | báo cáo 8.9, spec §9–§10 |
| Concurrency | báo cáo 8.9–8.10, spec §11 |
| Error handling | spec §12, `ErrorHandlingTest` |
| Project chạy được | task 5.3, 5.4 |
| README đầy đủ | task 5.2 |
| Test cases | `./mvnw verify`, `pytest`, kiểm nội dung trên CI |
| Experiment, số liệu thực tế | `statics/results/` |
| Discussion, Novelty, Limitations, References | báo cáo 8.13–8.18 |
| Compilatio ≤ 20% | task 5.1 |
| Source code đầy đủ, link Drive hoạt động | task 5.4 |
| Mọi thành viên hiểu project | task 5.6 |

---

## Nghiệm thu M5

- [ ] Chạy trọn kịch bản demo không lỗi trong phòng máy thật, hai lần liền.
- [ ] Báo cáo nộp được, Compilatio ≤ 20%.
- [ ] Gói nộp trên Drive tải về, cài và chạy được trên máy sạch.
- [ ] Mỗi người trả lời được các câu ở mục "Phải giải thích được".
