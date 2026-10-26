# Giai đoạn 6 — Nhiều lớp, Admin, soạn bài, kiểm tra, thảo luận

Kế hoạch mở rộng sau khi gộp giao diện mới ([#10](https://github.com/lyhaiquan/ExamNet/pull/10)). Mục tiêu:

- **Một server, nhiều lớp.** DSA 1–5, SQL 1–n chạy chung một server; **Admin** tạo lớp, chọn môn và giáo viên.
- **Giáo viên tự soạn bài** ngay trong app: bài SQL (bảng, dữ liệu, đáp án) và bài DSA (lời giải, chọn bộ vẽ cho từng biến để sinh animation), xem trước rồi mới đăng.
- **Kiểm tra**: ca thi tính giờ, chỉ thấy bài của ca, hết giờ tự nộp, có báo cáo.
- **Bật/tắt bài theo chương, theo lớp**; bài tắt thì học viên không thấy.
- **Thảo luận ở từng bài**, **trang chủ và thành tích** cho học viên, **ô code tự đóng ngoặc và tự thụt dòng**.

Giao diện tham khảo ảnh mẫu (bố cục sidebar + thẻ số liệu + biểu đồ), không lấy tên hay logo của ảnh.

## Quyết định đã chốt

| # | Quyết định | Thay cho |
| --- | --- | --- |
| Q1 | Một server chứa nhiều lớp; mỗi lớp một môn (`classes.mon`). Server chạy không đặt `mon` thì nạp cả hai môn | Spec §12 "mỗi lớp một server" (task 6.G1 sửa spec) |
| Q2 | Vai trò mới **ADMIN**, đăng nhập cùng app JavaFX, có sidebar riêng. Dashboard web giữ nguyên để xem log, chỉ đọc | — |
| Q3 | Kiểm tra là **ca thi tính giờ**: giáo viên soạn *đề thi* (bộ bài + thời lượng) rồi mở *lớp kiểm tra* (đề + lớp + giờ bắt đầu) | — |
| Q4 | Thảo luận **chung trong lớp** ở từng bài: học viên hỏi và trả lời nhau, giáo viên xem, trả lời, ẩn. Ô gõ comment cũng chặn dán như ô code | `comments` hiện có (giáo viên góp ý theo dòng code học viên) giữ nguyên, không dùng lại |

## Điều kiện trước

- ✅ [#9](https://github.com/lyhaiquan/ExamNet/pull/9) (sửa tải lên/xoá bài) và [#10](https://github.com/lyhaiquan/ExamNet/pull/10) (giao diện mới, `ClassroomWorkspaceModule`) đã merge; bảng mã trong [README.md](README.md) đã ghi các mã `CLASS_*`, `PRACTICE_*`, `STUDENT_*`.
- 6.A1 và 6.A2 đang ở [#11](https://github.com/lyhaiquan/ExamNet/pull/11).

## Thứ tự và mức ưu tiên

```
6.F Ô code (độc lập, làm song song từ đầu)
6.A Nền: A1 migration → A2 ADMIN → A3 nhiều lớp → A4 màn Admin
                                    └→ A5 tách theo lớp
                                    └→ A6 chính sách truy cập bài ──┬→ 6.C Kiểm tra
6.B Soạn bài (cần A6 để đăng bài theo lớp)                          ├→ 6.D Thảo luận
                                                                     └→ 6.E Trang chủ, thành tích
6.G Tài liệu (cuối mỗi đợt)
```

| Mức | Gồm | Vì sao |
| --- | --- | --- |
| **Phải có** | A1, A2, A3, A4, A6, B1, B2, F1, F2 | Nhiều lớp + Admin + soạn bài SQL + ô code là yêu cầu chính; A6 bịt lỗ bài ẩn vẫn chạy/nộp được |
| **Nên có** | A5, B3, C1–C5 | Chiếu/câu hỏi nhanh theo lớp (A5) cần khi hai lớp học cùng lúc; kiểm tra |
| **Có thì tốt** | D1–D2, E1–E3 | Thảo luận, thành tích |

Nếu gần ngày bảo vệ: chỉ làm hàng "Phải có", và **không đụng ĐG1–ĐG4** (chiếu, đồng bộ đồng hồ, dòng code, câu hỏi nhanh) trừ A5. A5 đổi nhóm multicast nên phải chạy lại TN1, TN3 ([giai-doan-4.md](giai-doan-4.md)) sau khi làm.

## Bảng mã thông điệp mới

Dải mới `0x80–0xBF` cho giai đoạn này, chia theo tính năng. Không đánh số lại mã cũ.

| Dải | Tính năng | Mã |
| --- | --- | --- |
| `0x80–0x87` | Admin | `ADMIN_USERS_REQ 80`, `ADMIN_USERS_DATA 81`, `ADMIN_USER_SAVE 82`, `ADMIN_CLASSES_REQ 83`, `ADMIN_CLASSES_DATA 84`, `ADMIN_CLASS_SAVE 85`, `ADMIN_OVERVIEW_REQ 86`, `ADMIN_OVERVIEW_DATA 87` |
| `0x88–0x8F` | Hiện/ẩn bài, soạn bài | `LESSON_VISIBILITY_SET 88`, `LESSON_PREVIEW_REQ 89`, `LESSON_PREVIEW_DATA 8A` |
| `0x90–0x9F` | Kiểm tra | `EXAM_TEMPLATE_SAVE 90`, `EXAM_TEMPLATES_REQ 91`, `EXAM_TEMPLATES_DATA 92`, `EXAM_SCHEDULE 93`, `EXAM_LIST_REQ 94`, `EXAM_LIST_DATA 95`, `EXAM_ENTER 96`, `EXAM_STATE 97`, `EXAM_FINISH 98`, `EXAM_REPORT_REQ 99`, `EXAM_REPORT_DATA 9A` |
| `0xA0–0xA7` | Thảo luận | `DISCUSS_REQ A0`, `DISCUSS_DATA A1`, `DISCUSS_POST A2`, `DISCUSS_HIDE A3`, `DISCUSS_NEW A4` |
| `0xA8–0xAF` | Trang chủ, thành tích | `HOME_REQ A8`, `HOME_DATA A9`, `LEADERBOARD_REQ AA`, `LEADERBOARD_DATA AB` |

Trả lời thành công của các lệnh ghi dùng `ADMIN_OK(str message)` như hiện tại; lỗi dùng `ERROR`. `ADMIN_LESSON_TOGGLE 27` (#7) giữ để app cũ không vỡ, server chuyển nó thành `LESSON_VISIBILITY_SET` cho lớp đang chọn.

## Dữ liệu mới

Mọi bảng mới vào `schema.sql`, kèm bước migration ở task 6.A1. Tóm tắt:

| Bảng / cột | Task | Ghi chú |
| --- | --- | --- |
| `users.role` thêm `'ADMIN'` | A2 | SQLite không sửa được `CHECK`: dựng lại bảng `users` trong migration |
| `classes.mon`, `classes.join_code`, `classes.archived` | A3 | `join_code` thay bảng `class_invites` đang tạo trong code của `ClassroomWorkspaceModule` |
| `practice_rooms`, `practice_members`, `practice_room_lessons`, `student_activity` | A1 | Đang tạo bằng `CREATE TABLE` trong `ClassroomWorkspaceModule`, chuyển vào `schema.sql` |
| `lesson_visibility(class_id, scope, key, visible, updated_by, updated_at)` | A6 | `scope` = `chuong` hoặc `bai`; thay file `content/.inactive_lessons.json` (một file chung cho cả server) |
| `exam_templates`, `exam_template_lessons`, `exam_sittings`, `exam_attempts`; `submissions.sitting_id` | C1 | Đề thi tách khỏi ca thi |
| `discussions(id, class_id, lesson_id, parent_id, author_id, text, created_at, hidden_by, hidden_at)` | D1 | |
| *(không có bảng thành tích)* | E1 | XP, chuỗi ngày, huy hiệu tính từ `submissions`, không thêm đường ghi mới |

---

## Đợt 6.A — Nền: migration, ADMIN, nhiều lớp

### Task 6.A1: Migration có đánh số phiên bản

**Vì sao trước tiên:** DB đang dùng (`D:\LabCast-chay-thu\*.db`, DB trên VPS) tạo bằng `CREATE TABLE IF NOT EXISTS`, nên cột mới không tự xuất hiện. Hiện chưa có cơ chế phiên bản nào.

**Làm:**
- `store/Migrations.java` (mới): danh sách bước `(phiên bản, tên, SqlWork)`. `Database.open` đọc `PRAGMA user_version`, chạy các bước còn thiếu trong một giao dịch, rồi đặt `user_version`.
- Bước 1: chuyển các `CREATE TABLE` trong `ClassroomWorkspaceModule.install` vào `schema.sql`; DB cũ đã có bảng thì bước này không làm gì.
- Bước nào cũng phải chạy được trên DB trống và DB của bản trước.

**Test trước:** `MigrationsTest` — `dbTrong_chayHet_dungPhienBan`; `dbBanCu_giuDuLieu_themCot` (tạo DB theo `schema.sql` của main hiện tại, chèn vài dòng, mở lại bằng bản mới, kiểm dữ liệu còn và cột mới có); `chayHaiLan_khongLoi`.

**Commit:** `feat(server): migration DB có đánh số phiên bản`

### Task 6.A2: Vai trò ADMIN

**Làm:**
- `Role.ADMIN` (thêm cuối enum; trên dây đã gửi `role().name()` nên app cũ gặp `ADMIN` sẽ lỗi `valueOf` — chấp nhận, admin dùng app mới).
- Migration bước 2: dựng lại `users` với `CHECK (role IN ('ADMIN','TEACHER','STUDENT'))`, chép dữ liệu, giữ `id`.
- Cờ `--tao-admin user:mật-khẩu:họ-tên` và khoá `tao-admin` trong file cấu hình, giống `tao-giao-vien`.
- **Rà 48 chỗ so `Role.TEACHER` / `Role.STUDENT`** trong `source/*/src/main/java`. Quy tắc: lệnh quản trị lớp, bài, tài khoản → TEACHER **và** ADMIN; việc học (nộp bài, giơ tay, focus) → chỉ STUDENT; `router().on(..., Set.of(Role.TEACHER), ...)` đổi sang hằng `Roles.STAFF = Set.of(TEACHER, ADMIN)` ở chỗ nào admin cần.

**Test trước:** `AdminRoleTest` — `taoAdminTuCauHinh_dangNhapDuoc_roleAdmin`; `adminGoiLenhGiaoVien_duoc`; `adminNopBai_biTuChoi`; `MigrationsTest.dbCoGiaoVien_sauMigration_vanDangNhapDuoc`.

**Commit:** `feat(account): vai trò ADMIN`

### Task 6.A3: Một server nhiều lớp

**Làm:**
- `classes.mon` (`dsa`/`sql`); server bỏ trống `mon` thì nạp cả hai môn. `lop`/`mon` trong file cấu hình giữ cho chế độ cũ (một lớp): khi có, server tự tạo đúng một lớp đó lúc khởi động.
- Sau đăng nhập, học viên thuộc nhiều lớp thì chọn lớp (`CLASS_SELECT_REQ` của #10 đã có; `Session.classId` đã đổi được). `LESSON_LIST` lọc theo `mon` của lớp đang chọn.
- `DISCOVER_REPLY` quảng bá **tên server** (khoá mới `ten-server`, mặc định "LabCast") thay vì tên một lớp.
- **Tạo lớp chỉ dành cho ADMIN**: `CLASS_CREATE_REQ` (0x6D, #10) đổi quyền từ TEACHER sang ADMIN, payload thêm `mon` và `teacherId`. Giáo viên chỉ thấy lớp mình phụ trách.

**Test trước:** `MultiClassTest` — `haiLopKhacMon_hocVienMoiLopChiThayBaiMonMinh`; `giaoVienTaoLop_biTuChoi_adminTaoDuoc`; `cauHinhCoLopMon_tuTaoMotLop` (chế độ cũ vẫn chạy).

**Commit:** `feat(classroom): một server nhiều lớp, admin tạo lớp`

### Task 6.A4: Màn Admin trong app

**Sidebar Admin:** Tổng quan · Người dùng · Giáo viên · Lớp học · Thống kê.

| Màn | Nội dung | Lệnh |
| --- | --- | --- |
| Tổng quan | Thẻ số liệu: người dùng, giáo viên, học viên, lớp, bài, lượt nộp 30 ngày; biểu đồ lượt nộp theo ngày; hoạt động gần đây (`audit_log`) | `ADMIN_OVERVIEW_REQ/DATA` |
| Người dùng / Giáo viên | Danh sách, tìm, tạo tài khoản giáo viên, đặt lại mật khẩu, khoá tài khoản | `ADMIN_USERS_REQ/DATA`, `ADMIN_USER_SAVE` |
| Lớp học | Tạo lớp (tên, môn, giáo viên), đổi giáo viên, lưu trữ lớp, nhập CSV học viên vào lớp | `ADMIN_CLASSES_REQ/DATA`, `ADMIN_CLASS_SAVE`, `ADMIN_ROSTER_IMPORT` |
| Thống kê | Như bảng "Thống kê học viên" của dashboard, lọc theo lớp; tải CSV | dùng truy vấn của `Dashboard.stats()` |

Logic tách khỏi JavaFX (`AdminModel`) để test được; màn chỉ vẽ.

**Test trước:** `AdminModuleTest` cho từng lệnh (quyền, payload, lỗi trùng tên đăng nhập); `AdminModelTest` cho phần lọc/sắp xếp.

**Commit:** `feat(client): màn Admin quản lý người dùng và lớp`

### Task 6.A5: Tách theo lớp những thứ đang dùng chung cả server

Hiện mọi thứ dưới đây là của **cả server**, hai lớp học cùng lúc sẽ lẫn nhau:

| Thứ | Hiện tại | Sửa |
| --- | --- | --- |
| Chiếu (ĐG1) | Một nhóm `239.255.70.1:7001` | Mỗi lớp một nhóm `239.255.70.<1 + classId mod 250>`; `CLASS_STATE` báo nhóm cho app; nhánh TCP dự phòng lọc theo `classId` |
| Câu hỏi nhanh (ĐG4, gói `quiz` mới có mô tả), `LESSON_PUSH` | Thiết kế gửi mọi phiên | Chỉ phiên của lớp; ĐG4 làm theo lớp ngay từ đầu |
| Phản chiếu code, giơ tay, presence | Giáo viên thấy mọi học viên | Chỉ học viên lớp mình |
| Dashboard web | Một lớp | Thêm ô chọn lớp; mặc định tất cả |

**Test trước:** `CastScopeTest.haiLopChieuCungLuc_khongNhanLan`; `PresenceScopeTest.giaoVienChiThayHocVienLopMinh`. Chạy lại TN1, TN3.

**Commit:** `feat(cast): chiếu và câu hỏi nhanh theo từng lớp`

### Task 6.A6: Một chính sách truy cập bài, hiện/ẩn theo chương

**Vấn đề:** #7 chỉ chặn bài ẩn ở `LESSON_LIST`/`LESSON_FETCH`; `RUN_REQ`, `SUBMIT_REQ`, `HINT_REQ`, `CODE_FETCH`, trace giảng giải không kiểm. Kiểm tra (6.C) và phiên thực hành sẽ mở lại đúng lỗ đó nếu mỗi handler tự kiểm.

**Làm:**
- `lesson/LessonAccess.java` (mới): `void check(Session s, String lessonId, Action a)` với `Action ∈ {LIST, OPEN, RUN, SUBMIT, HINT, DEMO, SOLUTION}`. Xét lần lượt: môn của lớp → bài có bị ẩn ở lớp này không (theo bài, rồi theo chương) → học viên đang trong ca thi thì chỉ bài của ca, và `HINT`/`DEMO`/`SOLUTION` bị chặn → phiên thực hành có gán bài thì chỉ bài đã gán. Giáo viên/Admin qua hết.
- Mọi handler kể trên gọi `LessonAccess.check` đầu tiên.
- `lesson_visibility` thay `.inactive_lessons.json`; lần đầu chạy, migration đọc file cũ (nếu có) và ghi thành dòng `scope='bai'` cho mọi lớp.
- `LESSON_VISIBILITY_SET(i64 classId, str scope, str key, bool visible)`. Học viên **không nhận** bài ẩn trong `LESSON_LIST` (không còn trạng thái `LOCKED` cho bài ẩn).
- Giáo viên: ở sidebar "Bài tập", công tắc ở **dòng chương** (bật/tắt cả chương) và ở từng bài.

**Test trước:** `LessonAccessTest` — `baiAn_hocVienKhongThay_khongChay_khongNop`; `anCaChuong_moMotBai_chiBaiDoHien`; `trongCaThi_chiBaiCuaCa_khongGoiY`; `phienCoGanBai_chiBaiDaGan`; `giaoVienThayHet`.

**Commit:** `feat(lesson): một chính sách truy cập bài, ẩn hiện theo chương và lớp`

---

## Đợt 6.B — Giáo viên soạn bài trong app

Thay màn "Thêm bài tập" của #10 (đang chỉ ghi file vào thư mục `content` trên máy, phải khởi động lại server). Cả hai form dựng **zip trong bộ nhớ** rồi gửi `ADMIN_LESSON_UPLOAD` (đã có từ #7, kiểm tra đường dẫn từ #9). Bài mới mặc định **ẩn** ở mọi lớp cho tới khi giáo viên bật.

### Task 6.B1: Xem trước trên server

**Làm:** `LESSON_PREVIEW_REQ(blob zip)` → `LESSON_PREVIEW_DATA JSON{ok, loi[], trace, ketQua}`. Server giải nén vào thư mục tạm, `parseLesson`, rồi:
- SQL: tạo DB từ `schema.sql` + `du_lieu_mau.sql`, chạy `dap_an.sql`, trả bảng kết quả và trace từng bước (bộ vẽ `bang-sql`); có `sinh_du_lieu.py` thì sinh thử seed 1 để báo lỗi sớm.
- DSA: chạy `loi_giai.py` với `demo.in`, trả trace (để app phát animation bằng các bộ vẽ trong `khung_nhin`) và output; chạy test cố định, báo test nào sai.

**An toàn:** `loi_giai.py`, `sinh_test.py`, `luat.py`, `sinh_du_lieu.py` giờ do giáo viên viết, không còn là file tin cậy trong repo, nên **luôn chạy trong container runner**. Server chạy `--khong-sandbox` thì từ chối xem trước và tải lên bài có các file này (báo rõ lý do).

**Test trước:** `LessonPreviewTest` — `sqlDapAnDung_traBangKetQua`; `sqlSchemaLoi_baoDongLoi`; `dsaLoiGiaiChay_traTraceCoBien`; `khongSandbox_tuChoiFilePython`.

**Commit:** `feat(lesson): xem trước bài trước khi đăng`

### Task 6.B2: Form bài SQL

| Ô | Ghi ra |
| --- | --- |
| Mã chương, mã bài, tên, loại bài (`giang-giai`/`bai-tap`/`luyen-tap`) | `lesson.yaml` |
| Bảng (`CREATE TABLE`) — có nút "Lấy bảng của bài khác" để chép `schema.sql` + `sinh_du_lieu.py` của bộ CSDL có sẵn (vd. "trường học") | `schema.sql` |
| Dữ liệu mẫu (`INSERT`, gợi ý ≤ 12 dòng mỗi bảng) | `du_lieu_mau.sql` |
| Đáp án | `dap_an.sql` |
| Loại câu: `select` / `dml`; chấm thứ tự dòng: có / không | `loai_cau`, `thu_tu` |
| Đề (Markdown), có nút chèn bảng dữ liệu mẫu dạng bảng Markdown | `de.md` |
| Code khởi đầu | `khung.sql` |

`khung_nhin` tự đặt `[{bien: sql, kieu: bang-sql}]` để có animation bảng như bài SQL có sẵn. Nút **Xem trước** (6.B1) hiện bảng kết quả và animation; nút **Đăng** chỉ bật khi xem trước không lỗi.

Logic dựng zip và kiểm ô (`SqlLessonDraft`) tách khỏi JavaFX.

**Test trước:** `SqlLessonDraftTest` — `duOThongTin_zipCoDuFile_parseLessonQua`; `thieuDapAn_baoLoi`; `chonDml_ghiLoaiCauDml`.

**Commit:** `feat(client): form soạn bài SQL`

### Task 6.B3: Form bài DSA — lời giải và bộ vẽ

**Animation của bài DSA sinh từ:** `loi_giai.py` (lời giải Python), `khung_nhin` (biến nào vẽ bằng bộ vẽ nào) và `demo.in` (input chạy demo). Form cho giáo viên khai đúng ba thứ đó:

1. **Lời giải** (Python; C/C++/Java tuỳ chọn cho bài đa ngôn ngữ).
2. **Thuật toán / cấu trúc dữ liệu cần vẽ:** bấm "Phân tích" → server chạy lời giải (6.B1) và trả danh sách biến xuất hiện trong trace kèm kiểu Python. Mỗi biến chọn một bộ vẽ đang có (giá trị `kieu` trong `lesson.yaml`): mảng `mang`, ngăn xếp `ngan-xep`, hàng đợi `hang-doi`, danh sách liên kết `dslk`, cây `cay`, heap `heap`, cây gọi đệ quy `cay-goi`, đồ thị `do-thi`, lưới `luoi`, bảng băm `bam`, hoặc chỉ hiện giá trị `code-bien`. App gợi ý sẵn theo kiểu: `list` → mảng, `deque` → hàng đợi, `dict` → bảng băm, `list[list]` → lưới; hàm đệ quy → cây gọi.
3. **Input demo** và **test**: bảng test cố định (input/output), tuỳ chọn `sinh_test.py` cho test ẩn.
4. **Luật cấm** (tuỳ chọn): `cam_dung`, `cam_import` — vd. bài bảng băm cấm `dict`.

Xem trước phát đúng animation học viên sẽ thấy. Đăng như 6.B2.

**Test trước:** `DsaLessonDraftTest` — `khungNhinTuLuaChon_ghiDungYaml`; `goiYBoVe_listThanhMang_dictThanhBam`; `thieuDemoIn_baoLoi`.

**Commit:** `feat(client): form soạn bài DSA với lời giải và bộ vẽ`

---

## Đợt 6.C — Kiểm tra (ca thi tính giờ)

### Task 6.C1: Đề thi và ca thi trên server

- `EXAM_TEMPLATE_SAVE JSON{id?, ten, mon, thoiLuongPhut, bai:[{lessonId, diem}]}` — đề thi dùng lại được.
- `EXAM_SCHEDULE JSON{templateId, classId, batDau}` → tạo `exam_sittings` với `ketThuc = batDau + thoiLuong`.
- `EXAM_ENTER(i64 sittingId)` → `EXAM_STATE JSON{sittingId, ketThuc, bai[], daNop{}}`. Đồng hồ server là chuẩn (app đếm ngược theo `Clock.serverNow()`, ĐG3).
- Hết giờ: server tự khoá; mỗi bài lấy **lần nộp cuối trước giờ kết thúc**; học viên chưa nộp thì nộp bản code cuối cùng server đang giữ (`code_deltas`). `EXAM_FINISH` cho học viên nộp sớm.
- `submissions.sitting_id` ghi ca thi để báo cáo tách khỏi luyện tập.

**Test trước:** `ExamTest` — `truocGioBatDau_khongVaoDuoc`; `trongGio_chiThayBaiCuaDe`; `hetGio_tuNopCodeCuoi`; `nopSauGio_biTuChoi`; `hocVienLopKhac_khongVaoDuoc`.

### Task 6.C2: Màn giáo viên — Đề thi, Lớp kiểm tra

- **Đề thi:** danh sách đề, tạo/sửa đề (chọn bài từ thư viện, điểm từng bài, thời lượng).
- **Lớp kiểm tra:** mở ca (đề + lớp + giờ bắt đầu), theo dõi trực tiếp: ai đã vào, đang làm bài nào, đã nộp, rời app bao nhiêu lần (dữ liệu `APP_FOCUS` có sẵn).

### Task 6.C3: Màn học viên — Kiểm tra

Mục "Kiểm tra" ở sidebar **chỉ hiện khi lớp có ca thi** sắp tới hoặc đang diễn ra. Trong ca: đồng hồ đếm ngược, chỉ các bài của đề, không có animation mẫu, gợi ý, lời giải; nút "Nộp bài và kết thúc".

### Task 6.C4: Báo cáo

`EXAM_REPORT_REQ(i64 sittingId)` → `EXAM_REPORT_DATA JSON{hocVien:[{username, hoTen, diem, bai:[{lessonId, diem, soLanNop, nopLuc}], roiApp}]}`; app tải CSV (cùng cách chống công thức Excel như dashboard). `ADMIN_PROGRESS_EXPORT` (có mã từ giai đoạn 1 nhưng chưa có handler) làm luôn ở đây: tiến độ cả lớp ngoài ca thi.

**Commit (mỗi task một PR):** `feat(exam): đề thi và ca thi tính giờ`, `feat(client): màn đề thi và lớp kiểm tra`, `feat(client): màn kiểm tra của học viên`, `feat(exam): báo cáo ca thi và xuất tiến độ lớp`

---

## Đợt 6.D — Thảo luận ở từng bài

### Task 6.D1: Server

- `DISCUSS_REQ(str lessonId)` → `DISCUSS_DATA JSON{binhLuan:[{id, parentId, tacGia, vaiTro, noiDung, luc, an}]}` — chỉ của **lớp đang chọn**; học viên không nhận dòng đã ẩn.
- `DISCUSS_POST JSON{lessonId, parentId?, noiDung}` (≤ 2 000 ký tự, giới hạn tần suất như `RATE_LIMITED`); server phát `DISCUSS_NEW` cho người đang mở bài đó trong cùng lớp.
- `DISCUSS_HIDE(i64 id)` — giáo viên/Admin; ghi `audit_log`.
- Bài đang trong ca thi: không đọc, không gửi (qua `LessonAccess`).

**Test trước:** `DiscussionTest` — `hocVienHaiLop_khongThayCuaNhau`; `giaoVienAn_hocVienKhongThay`; `trongCaThi_biChan`; `quaDai_biTuChoi`.

### Task 6.D2: App

- Học viên: thẻ "Thảo luận" cạnh Trace Table trong màn bài; trả lời lồng một cấp. **Ô gõ dùng `ClipboardGuard`** như ô code — chặn dán là lý do thảo luận chung không làm lộ lời giải.
- Giáo viên: ở mỗi bài thấy số bình luận mới; màn "Bài tập" có lọc "Có thảo luận mới"; ẩn/trả lời ngay trong thẻ.

**Commit:** `feat(discuss): thảo luận ở từng bài`, `feat(client): thẻ thảo luận trong màn bài`

---

## Đợt 6.E — Trang chủ và thành tích

### Task 6.E1: Số liệu tính từ bài nộp

`HOME_REQ` → `HOME_DATA` tính từ `submissions`, `progress`, `audit_log`, không thêm bảng:
- **XP:** nộp đúng lần đầu một bài: bài tập 50, luyện tập 30, giảng giải 10; cấp độ mỗi 500 XP.
- **Chuỗi ngày:** số ngày liên tiếp có ít nhất một lần nộp đúng.
- **Huy hiệu:** giải 10/50/100 bài; xong một chương; chuỗi 7 ngày; nộp đúng ngay lần đầu 10 bài.
- **Bài tiếp theo:** bài đang dở gần nhất, hoặc bài đầu tiên chưa làm của chương đang mở.

`LEADERBOARD_REQ(i64 classId)` → xếp hạng XP **trong lớp**.

Các con số XP trên là đề xuất, chốt lại khi làm.

**Test trước:** `HomeStatsTest` — `nopDungHaiLanMotBai_chiTinhXpMotLan`; `chuoiNgay_ngatMotNgay_tinhLai`; `bangXepHang_chiTrongLop`.

### Task 6.E2: Trang chủ học viên

Sidebar học viên: **Trang chủ · Bài tập · Lớp thực hành · Kiểm tra** (chỉ khi có ca) **· Thành tích**. Trang chủ: lời chào, thẻ "Tiếp tục luyện tập", chuỗi ngày, XP/cấp độ, tiến độ theo chương, bài làm gần đây, huy hiệu, bảng xếp hạng lớp.

### Task 6.E3: Trang chủ giáo viên

Sidebar giáo viên: **Trang chủ · Lớp học · Bài tập · Đề thi · Lớp thực hành · Lớp kiểm tra · Upload bài tập** (các nút tải zip, nhập CSV, bật/tắt, xoá hiện có vẫn giữ). Trang chủ: thẻ số liệu các lớp mình dạy (học viên, bài, tỉ lệ hoàn thành), danh sách lớp với thanh tiến độ, hoạt động gần đây, bình luận mới.

**Commit:** `feat(home): số liệu trang chủ và thành tích`, `feat(client): trang chủ học viên`, `feat(client): trang chủ giáo viên`

---

## Đợt 6.F — Ô code tự đóng ngoặc, tự thụt dòng

Làm được ngay, không phụ thuộc đợt nào.

### Task 6.F1: Quy tắc soạn thảo thuần Java

`editor/EditRules.java` (mới, không dùng JavaFX): nhận `(text, caret, phím, ngôn ngữ)` → trả về thay đổi `(vị trí, xoá, chèn, caret mới)`.

| Phím | Hành vi |
| --- | --- |
| `(` `[` `{` `"` `'` | Chèn cặp, con trỏ ở giữa. Không chèn cặp nếu ngay sau con trỏ là chữ/số |
| `)` `]` `}` `"` `'` | Nếu ký tự ngay sau con trỏ đúng là nó (do tự chèn) thì chỉ nhảy qua |
| Backspace | Đứng giữa cặp rỗng `()` thì xoá cả hai |
| Enter | Giữ thụt lề dòng trên. Python: dòng trên kết thúc bằng `:` thì thêm 4 dấu cách. C/C++/Java: sau `{` thêm một mức; Enter giữa `{}` thì tách thành ba dòng, `}` lùi về mức cũ |
| `}` ở dòng chỉ có khoảng trắng | Lùi một mức (C/C++/Java) |
| Tab / Shift+Tab | Thụt / lùi 4 dấu cách, cả khi chọn nhiều dòng (hiện Tab chỉ thay vùng chọn bằng 4 dấu cách) |

Không tự đóng trong chuỗi và chú thích; SQL chỉ tự đóng `(` và `'`.

**Test trước:** `EditRulesTest` — mỗi dòng của bảng trên một test, cho cả `python`, `cpp`, `java`, `sql`.

### Task 6.F2: Gắn vào `CodeEditor`

- `CodeEditor` gọi `EditRules` trong bộ lọc phím có sẵn (đang xử lý Tab), áp thay đổi bằng `area.replaceText(...)` để **đi qua đường `DeltaBatcher` ghi nhật ký code** như gõ tay (ĐG2 không đổi).
- Ký tự tự chèn (vd. 8 dấu cách thụt lề, `}` đóng) **không được tính là dán khối lớn** (`ClipboardGuard`/cờ "dán khối lớn" trên dashboard): đánh dấu thay đổi do quy tắc sinh ra, hoặc chỉ tính cờ khi nguồn là clipboard.
- Tuỳ chọn tắt trong menu cài đặt của app.

**Test trước:** `CodeEditorRulesTest` (logic gói ngoài JavaFX): gõ chuỗi phím → nội dung và chuỗi `CodeDelta` đúng; `thutLeTuDong_khongBatCoDan`.

**Commit:** `feat(editor): tự đóng ngoặc và tự thụt dòng`

---

## Đợt 6.G — Tài liệu

- `docs/specs/labcast-design.md` §12: ghi quyết định Q1 (một server nhiều lớp), cách chạy cũ vẫn dùng được.
- `README.md`: cách tạo admin, tạo lớp, chạy một server cho mọi lớp; sơ đồ sidebar ba vai trò.
- Bảng mã trong [README.md](README.md) của thư mục này: thêm dải `0x80–0xBF`.
- [danh-muc-bai.md](danh-muc-bai.md): hướng dẫn soạn bài bằng form (6.B).

## Nghiệm thu M6

- [ ] Admin tạo được 2 lớp DSA và 1 lớp SQL trên **một** server, mỗi lớp một giáo viên; học viên mỗi lớp chỉ thấy bài môn mình.
- [ ] Giáo viên tắt một chương ở lớp A: học viên lớp A không thấy, không chạy/nộp được bài của chương; lớp B không bị ảnh hưởng.
- [ ] Giáo viên soạn một bài SQL và một bài DSA (có animation mảng) bằng form, xem trước, đăng, học viên làm được, không cần khởi động lại server.
- [ ] Một ca kiểm tra 15 phút: học viên chỉ thấy bài của đề, hết giờ tự nộp, giáo viên tải được CSV điểm.
- [ ] Hai học viên cùng lớp thảo luận ở một bài; giáo viên ẩn được một bình luận; học viên lớp khác không thấy.
- [ ] Gõ `def f(a):` rồi Enter: dòng sau thụt 4; gõ `{` trong C++ rồi Enter: ba dòng, `}` đúng mức; dashboard không bật cờ "dán khối lớn".
- [ ] `./mvnw verify` và `python -m pytest runner` xanh; DB của bản trước mở được bằng bản mới (6.A1).
