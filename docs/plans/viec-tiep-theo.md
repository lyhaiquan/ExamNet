# Việc tiếp theo

Cập nhật 26/10/2026. File này nói **làm gì tiếp, theo thứ tự nào**; chi tiết từng task ở [giai-doan-6.md](giai-doan-6.md) và [`docs/PHAN-CONG-DOT-2.md`](https://github.com/lyhaiquan/ExamNet/blob/feat/server-cong-9000/docs/PHAN-CONG-DOT-2.md) (trên nhánh `feat/server-cong-9000`).

## 1. Đang ở đâu

| PR / nhánh | Nội dung | Trạng thái |
| --- | --- | --- |
| [#7](https://github.com/lyhaiquan/ExamNet/pull/7) | Upload zip, bật/tắt, xoá bài (Vũ Viết Anh) | ✅ merged |
| [#8](https://github.com/lyhaiquan/ExamNet/pull/8) | Dashboard: phân loại log, thống kê học viên | ✅ merged |
| [#9](https://github.com/lyhaiquan/ExamNet/pull/9) | Sửa #7: chặn ghi ra ngoài `content/`, không đè bài, xoá bài giữ DB khớp | ✅ merged |
| [#10](https://github.com/lyhaiquan/ExamNet/pull/10) | Giao diện mới (Đinh Hoàng Vũ), lớp và phiên thực hành (`ClassroomWorkspaceModule`) | ✅ merged |
| [#11](https://github.com/lyhaiquan/ExamNet/pull/11) | 6.A1 migration DB (`PRAGMA user_version`) + 6.A2 vai trò `ADMIN` | ⏳ chờ duyệt |
| [#12](https://github.com/lyhaiquan/ExamNet/pull/12) | Kế hoạch giai đoạn 6 + file này | ⏳ chờ duyệt |
| `feat/server-cong-9000` | **Đợt 2**: hợp đồng 4 người, trang quản trị web `/admin`, `ClassDirectory`, cổng 9000, sửa nối lại mạng | ⚠️ đã push, **chưa có PR**, chưa vào main |

## 2. Việc số 0 — hai kế hoạch đang chồng nhau

Đợt 2 (`docs/PHAN-CONG-DOT-2.md`, nhánh `feat/server-cong-9000`) đã chia việc cho 4 người và chốt mã thông điệp, bảng DB. Nhưng nhánh đó chưa merge, nên #7 và #10 làm thẳng trên main với mã riêng, và [giai-doan-6.md](giai-doan-6.md) lập sau cũng không dựa trên nó. Ba bên đang đụng nhau:

| Chỗ | Đợt 2 (`feat/server-cong-9000`) | main hiện tại / giai đoạn 6 | Xung đột |
| --- | --- | --- | --- |
| Mã `0x80–0x9F` | `CLASS_LIST 80`, `CLASS_INFO 81`, `ADMIN_USER_* 84–86`, `ADMIN_CLASS_* 88–89`, `ADMIN_LESSON_UPLOAD 8C`, `ASSIGN_* 90–94`, `SCHEDULE_* 98–99` | Giai đoạn 6 dùng cùng dải `0x80–0xBF` với nghĩa khác | **Trùng mã**: enum ném lỗi lúc khởi động nếu gộp nguyên |
| Upload bài | `ADMIN_LESSON_UPLOAD 0x8C`, payload JSON base64 | `ADMIN_LESSON_UPLOAD 0x2B` (#7), payload nhị phân | **Trùng tên**: không biên dịch được nếu gộp nguyên |
| Danh sách lớp | `CLASS_LIST 0x80` | `CLASS_LIST_REQ 0x23` / `CLASS_LIST_DATA 0x24` (#10) | Hai lệnh cùng việc |
| Trang quản trị | **Web** `/admin` (cổng 9000, chỉ máy server), đã chạy | Giai đoạn 6 Q2: màn Admin **trong app** + vai trò `ADMIN` (#11) | Chọn một, hoặc web cho quản trị máy chủ + app cho admin từ xa |
| Thực hành / kiểm tra | `assignments`, `ASSIGN_*`: giao bài có giờ mở/đóng, tự bắt đầu, tự nộp (Danh Hải) | #10 có `practice_rooms` (phiên + mã mời); giai đoạn 6.C có `exam_*` | Ba mô hình cho cùng một việc |
| Bật/tắt bài | `lesson_active (class_id, lesson_id, active)` (Vũ Viết Anh) | `.inactive_lessons.json` chung cả server (#7); giai đoạn 6.A6 `lesson_visibility` | Hai thiết kế bảng |
| Nhiều lớp | `class_teachers`, `ClassDirectory` | Giai đoạn 6.A3 `classes.mon` | Bổ sung nhau, cần ghép |
| Lược đồ DB | Thêm bảng bằng `CREATE TABLE IF NOT EXISTS` | #11 thêm migration có phiên bản | Bổ sung nhau: bảng của đợt 2 thêm vào `schema.sql` là đủ |

### Đề xuất cách gỡ

1. **Lấy đợt 2 làm khung** vì đã có người nhận việc và đã có phần chạy được (`/admin`). Giai đoạn 6 rút lại thành **phần bổ sung** cho những gì đợt 2 chưa có (mục 3 bên dưới).
2. **Một PR "hợp đồng v2"** (leader A) đưa `feat/server-cong-9000` lên main sau #11:
   - Giữ mã đã phát hành trên main (`ADMIN_LESSON_* 26/27/2B`, `CLASS_* 23–25`, `PRACTICE_* 67–7B`); bỏ `ADMIN_LESSON_UPLOAD 0x8C` của đợt 2.
   - `CLASS_LIST 80` / `CLASS_INFO 81`: hoặc bỏ, dùng `CLASS_LIST_REQ 23` của #10; hoặc giữ cho trang quản trị — chốt một.
   - Chuyển dải mới của giai đoạn 6 (thảo luận, trang chủ, soạn bài) sang `0xA0–0xBF` để không đụng `0x80–0x9F` của đợt 2.
   - Bảng đợt 2 vào `schema.sql`; bỏ `.inactive_lessons.json`, dùng `lesson_active` (migration đọc file cũ một lần).
   - Gộp `practice_rooms` (#10) với `assignments` (đợt 2): một mô hình giao bài.
3. Sửa [giai-doan-6.md](giai-doan-6.md) theo hợp đồng v2: bỏ 6.A3–6.A6, 6.C (đã thuộc A, B, D của đợt 2), giữ phần còn lại.

### Cần chốt trước khi code tiếp

- [ ] Đợt 2 hay giai đoạn 6 là kế hoạch chính? (đề xuất: đợt 2)
- [ ] Trang quản trị: web `/admin` (đã có) hay màn trong app, hay cả hai?
- [ ] Thực hành/kiểm tra: dùng `assignments` của đợt 2, bỏ `practice_rooms` của #10? Hay giữ phiên có mã mời làm "lớp thực hành"?
- [ ] Ai làm PR hợp đồng v2 (theo đợt 2 là A — leader).

## 3. Việc tiếp theo (sau khi chốt mục 2)

Cột "Ai" theo đợt 2; việc đợt 2 chưa có thì ghi "—" để chia.

| Thứ tự | Việc | Ai | Xong khi |
| --- | --- | --- | --- |
| 1 | Duyệt, merge #11 (migration + ADMIN) và #12 | A | CI xanh, đã merge |
| 2 | PR hợp đồng v2: gộp `feat/server-cong-9000` lên main (mục 2) | A | `./mvnw verify` xanh; `/admin` chạy trên main; không trùng mã |
| 3 | **Ô code tự đóng ngoặc, tự thụt dòng** (6.F) | — | Gõ `def f(a):` Enter thụt 4; `{` Enter trong C++ ra ba dòng; không bật cờ "dán khối lớn" |
| 4 | Một server nhiều lớp, lớp có môn (đợt 2 #1 + 6.A3 `classes.mon`) | A | Hai lớp DSA, một lớp SQL trên một server; học viên chỉ thấy bài môn lớp mình |
| 5 | Bật/tắt bài theo lớp và theo chương, **bài tắt học viên không thấy, không chạy, không nộp được** (đợt 2 #7 + 6.A6) | D — Vũ Viết Anh | Hiện bài ẩn vẫn chạy/nộp được vì chỉ chặn ở `LESSON_LIST`/`LESSON_FETCH`: phải chặn cả `RUN_REQ`, `SUBMIT_REQ`, `HINT_REQ` |
| 6 | Thực hành / kiểm tra theo lịch, tự bắt đầu, tự nộp; báo cáo CSV (đợt 2 #4 + 6.C4) | B — Danh Hải | Nghiệm thu như 6.C |
| 7 | Sidebar 4 nhóm Bài mẫu · Luyện tập · Thực hành · Kiểm tra (đợt 2 #5) | B dữ liệu, C vẽ | Học viên thấy đúng nhóm theo lịch |
| 8 | **Form soạn bài SQL** trong app (6.B1–6.B2): bảng, dữ liệu mẫu, đáp án, `select`/`dml`, xem trước | D — Vũ Viết Anh | Soạn, xem trước, đăng một bài SQL không cần khởi động lại server |
| 9 | **Form soạn bài DSA** (6.B3): lời giải + chọn bộ vẽ cho từng biến để sinh animation | D | Bài DSA mới có animation mảng chạy được |
| 10 | Thảo luận ở từng bài (6.D) | — | Nghiệm thu như 6.D |
| 11 | Trang chủ, thành tích học viên và giáo viên (6.E) | C vẽ, — dữ liệu | Nghiệm thu như 6.E |
| 12 | Tài liệu: spec "mỗi lớp một server" → nhiều lớp; README; bảng mã (6.G) | A | |

## 4. Tồn đọng kỹ thuật

| Vấn đề | Ảnh hưởng | Cách xử lý |
| --- | --- | --- |
| `NgonNguFlowTest.chayThuCpp_vaChayTungDong` hỏng trên Windows (gdb không ra sự kiện dòng) | Hook pre-push (`mvnw verify`) chặn push trên máy Windows; #9–#11 đều phải push `--no-verify` | Cho test bỏ qua khi gdb không chạy được (`assumeTrue`), hoặc chỉ chạy phần gdb trên Linux/Docker |
| Extension Java của VS Code tự biên dịch vào `target/classes` | Jar đóng gói lẫn class lỗi ("Unresolved compilation problems"), app tắt ngay sau đăng nhập | Build bằng `./mvnw clean package`; hoặc tắt auto-build của extension cho repo này |
| Màn "Thêm bài tập" (#10) chỉ ghi file vào thư mục `content` trên máy | Bài mới phải khởi động lại server; ô SQL dùng lại ô input/output của DSA | Việc 8–9 thay màn này |
| `ClassroomWorkspaceModule` chưa có test | Dễ vỡ khi gộp với đợt 2 | Thêm test cùng PR hợp đồng v2 |
| Docker chưa bật trên máy dev | Phải chạy `--khong-sandbox`: code học viên chạy thẳng trên máy, không có trace từng dòng C/C++ | Bật Docker Desktop, build image `labcast-runner` theo README |

## 5. Chạy thử bản mới trên máy

Icon LabCast trên desktop (`D:\LabCast-chay-thu\LabCast.ps1`) chạy jar trong `D:\ExamNet\source`, mà thư mục đó **đang ở nhánh `feat/server-cong-9000`**, không phải main. Muốn icon mở bản mới:

```bash
cd D:/ExamNet && git switch main && git pull
cd source && ./mvnw clean package -DskipTests
```

Sau khi #11 vào: thêm `tao-admin = ad:<mật khẩu>:Họ tên` vào `lop-dsa.conf` để có tài khoản quản trị. DB cũ tự nâng lược đồ lần đầu mở; vẫn nên **sao lưu `lop-dsa.db`, `lop-sql.db` trước**. Bản cũ vẫn mở được DB đã nâng (bản cũ không đọc `user_version`), nhưng không đăng nhập được tài khoản `ADMIN`.
