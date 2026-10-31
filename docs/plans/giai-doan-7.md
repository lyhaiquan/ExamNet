# Giai đoạn 7 — Ngân hàng bài, độ khó, thông báo mật khẩu, ca thực hành có giám sát

> **Bản nháp chờ duyệt.** Chưa có dòng code nào. Viết sau khi đọc code trên `main` (c57e433) ngày 31/10/2026.

## 1. Hiện trạng (đã đối chiếu code)

| Việc bạn nêu | Thực tế trong code |
| --- | --- |
| "Ngân hàng câu hỏi hardcode trong database" | **Không phải trong DB.** Bài là thư mục `content/<mon>/<chuong>/<bai>/` (`lesson.yaml`, `de.md`, `tests/*.in/.out`…). Bảng `lessons` chỉ là chỉ mục nạp từ thư mục lúc server khởi động. Muốn có bài mới phải chép thư mục và **khởi động lại server**. |
| Form tạo câu hỏi | `AddExerciseScreen` ghi file **thẳng vào ổ đĩa máy đang chạy app** (chỉ đúng khi app chạy cùng máy server), rồi bảo khởi động lại server. Chưa dùng đường upload zip `ADMIN_LESSON_UPLOAD` đã có từ #7. |
| Thiết kế test | Form chỉ có **một** cặp Input/Output (`tests/mau-1.in/.out`), ghi `so_test_an: 0`. Không thêm nhiều test, không đánh dấu test ẩn/hiện, không có nút kiểm tra lời giải mẫu qua các test. Test ẩn chỉ có khi viết tay `sinh_test.py`. Bài SQL không có test: chấm bằng database ẩn sinh từ `sinh_du_lieu.py`. |
| Độ khó | **Chưa có** ở bất kỳ đâu: `lesson.yaml`, bảng `lessons`, `LESSON_LIST`, form tạo, kho bài. |
| Đổi mật khẩu không hiện cho admin | Luồng "Quên mật khẩu" **đã có**: ghi `password_requests`, admin thấy ở khung đầu trang Tổng quan. Nhưng khung chỉ **tải lại khi danh sách lớp đổi** (`DashboardHome.setClasses` → `refresh`), không có bộ đếm giờ, không có số đỏ trên menu, bấm "Tổng quan" cũng không tải lại. Admin đang mở app sẽ **không thấy** yêu cầu mới cho tới khi đổi lớp hoặc đăng nhập lại. Thêm: nhập sai tên đăng nhập thì server im lặng (cố ý, chống dò tên), không ai biết có người thử. Giáo viên chỉ thấy học viên đã ghi danh lớp mình. |
| "Tự đổi mật khẩu" và "admin đặt lại" | Chỉ ghi `audit_log`, không thông báo, không có trang lịch sử. (Bạn đã chọn kịch bản "Quên mật khẩu"; hai thao tác này nêu để trang mới phủ luôn.) |
| Code tự do ở admin | Nút "Code tự do" nằm trong menu **cả ba vai trò** (`HomeScreen` dòng ~291). Bạn muốn bỏ ở admin. |
| Ca thực hành | `practice_rooms` (tên, mã tham gia, bài được giao) đã có. **Chưa có**: giờ mở/đóng, chính sách nộp, IP, sơ đồ máy, chia đề, bảng giám sát. `STUDENT_OVERVIEW` chỉ trả: màn đang mở, bài đã giải, online. |
| Dữ liệu sẵn dùng để giám sát | `AppFocus` (số lần rời app, tổng thời gian ngoài app — **mất khi khởi động lại server**), cờ dán khối lớn (`ClassEvents`), `Presence`, `student_activity`, `submissions`, `Connection.remoteAddress()` (dạng `ip:port`, cần cắt bỏ cổng). |
| Kiểm soát truy cập bài | **Chưa có `LessonAccess`** (task 6.A6 chưa làm). `RUN/SUBMIT/HINT/LESSON_FETCH` không kiểm bài có được giao cho học viên đó không. Chưa có cái này thì chia đề so le chỉ là làm cảnh: học viên gọi thẳng bài của bạn bên cạnh. → **Điều kiện bắt buộc** của Đợt 7.G. |

## 2. Quyết định đã chốt (từ câu trả lời của bạn)

| # | Quyết định |
| --- | --- |
| D1 | Bài vẫn là **thư mục trong `content/`**; thêm upload từ app (đường `ADMIN_LESSON_UPLOAD`), thêm trường độ khó, thêm trình thiết kế test. Không chuyển bài vào DB. |
| D2 | **Sơ đồ phòng** do giáo viên dựng một lần, lưu theo phòng, gán IP vào từng ghế; server gợi ý theo thứ tự IP, giáo viên chỉnh. Chia đề theo **ghế liền kề** (bàn cờ), không theo IP thô. |
| D3 | **Chính sách nộp chọn theo từng ca**: nộp nhiều lần hoặc chỉ một lần. Chạy thử luôn không giới hạn. Server chặn lần nộp thứ hai chứ không chỉ ẩn nút. |
| D4 | Thông báo mật khẩu: tải lại tự động + số đỏ trên menu + trang riêng. **Không bao giờ hiện mật khẩu học viên tự đặt** cho admin. |

## 3. Mặc định tôi tự chọn (bạn đổi được ở bước duyệt)

- **Độ khó 3 mức:** Dễ / Trung bình / Khó (`do_kho: de|vua|kho`). Bài cũ để "Chưa phân loại"; giáo viên gán hàng loạt ngay trong kho bài. Nơi lưu: bảng `lesson_meta` ghi đè `lesson.yaml` (không sửa file trong `content/` của bài có sẵn, không đổi `content_hash`, không làm sinh lại test ẩn).
- **Bỏ "Code tự do" chỉ ở menu admin.** Giáo viên, học viên giữ. Server vẫn cho role ADMIN gọi `RUN_REQ`, chỉ ẩn nút.
- **Số bộ đề mỗi ca: 2** (A/B), giáo viên chọn 1–4. Ghế (hàng + cột) chẵn lẻ → bộ A/B; với 3–4 bộ thì `(hàng + cột) mod N`.
- **Trạng thái trên bảng giám sát:** Chưa vào · Online · Rời app · Mất kết nối · Đã nộp. "Mất kết nối" = phiên mất mạng quá 30 giây (khớp với `Presence`).
- **Điểm từng test:** chưa làm trọng số; bài chấm đạt/không như hiện nay.
- **Thiết lập bảo mật thông báo:** gửi tới admin đang online ngay lúc có yêu cầu (`NOTICE_NEW`), admin offline thì thấy số đỏ lần đăng nhập sau.

## 4. Thứ tự làm (nhỏ, chắc trước; chưa chắc để sau)

```
7.A Bỏ Code tự do ở admin        (nửa giờ, độc lập)
7.B Thông báo mật khẩu           (độc lập)
7.C Độ khó                       (độc lập)
7.D Form tạo bài qua upload + thiết kế test + kiểm lời giải (cần 7.C để chọn độ khó)
7.E Cài đặt ca thực hành: giờ, chính sách nộp (chặn nộp ở server)
7.F Bảng giám sát (cần 7.E)
7.G Sơ đồ phòng + chia đề so le (cần 7.E, 7.F, và LessonAccess)
```

7.A–7.C làm được ngay, song song. 7.D–7.G tuần tự. Mỗi task một nhánh, một PR dưới ~400 dòng, chạy `source/mvnw` (Maven hệ thống trên máy đang lỗi `NoClassDefFoundError`), chỉ `spotless:apply` cho module mình sửa.

### Mã thông điệp mới (dải còn trống sau `ADMIN_CLASS_DELETE B6`)

| Mã | Tên | Task |
| --- | --- | --- |
| `B7` `B8` | `NOTICES_REQ` / `NOTICES_DATA` (yêu cầu quên mật khẩu đang chờ + lịch sử sự kiện bảo mật) | 7.B |
| `B9` | `NOTICE_NEW` (server đẩy cho admin online) | 7.B |
| `BA` | `LESSON_META_SET` (độ khó hàng loạt) | 7.C |
| `BB` `BC` | `LESSON_PREVIEW_REQ` / `LESSON_PREVIEW_DATA` (kiểm lời giải qua các test) | 7.D |
| `BD` | `ROOM_SETTINGS_SET` | 7.E |
| `BE` `BF` `C0` | `LAB_ROOM_LIST_REQ` / `LAB_ROOM_LIST_DATA` / `LAB_ROOM_SAVE` | 7.G |
| `C1` `C2` `C3` | `ROOM_MONITOR_REQ` / `ROOM_MONITOR_DATA` / `ROOM_SETS_SET` | 7.F, 7.G |

(Bảng giai đoạn 6 từng dành dải `80–AF` cho tính năng khác; main đã dùng `A0–B6`, nên các mã trên lấy từ vùng còn trống. Mã đã ghi vào bảng mã của `README.md` khi làm.)

---

## Đợt 7.A — Bỏ Code tự do ở admin

**Làm:** `HomeScreen`: nhánh `Role.ADMIN` bỏ `navSection("Công cụ")` và `playgroundButton` khỏi `menu`. Phần mở playground khi chiếu (`playgroundOpen`) không đổi.

**Test trước:** `HomeScreenMenuTest` (logic chọn mục menu tách khỏi JavaFX): `admin_khongCoCodeTuDo`, `giaoVien_vanCo`, `hocVien_vanCo`.

**Commit:** `feat(client): bỏ Code tự do khỏi menu quản trị`

---

## Đợt 7.B — Thông báo mật khẩu cho admin

### Task 7.B1: Sửa lỗi "không thấy" ngay

**Làm:**
- `DashboardHome`: `Timeline` 5 giây gọi `pwdRequests.reload()` khi trang Tổng quan đang hiện; bấm "Tổng quan" cũng tải lại.
- `HomeScreen`: số đỏ trên nút "Tổng quan"/"Thông báo" bằng số yêu cầu đang chờ.
- Giáo viên: ngoài học viên lớp mình, **học viên chưa vào lớp nào** chỉ admin thấy; ghi rõ ở khung ("cần admin").

**Test trước:** `PasswordRequestsTest` (đã có `PasswordTest`, thêm): `yeuCauMoi_adminDangOnlineNhanNoticeNew`; `giaoVienKhongThayHocVienChuaVaoLop`.

### Task 7.B2: Trang "Thông báo bảo mật"

Mục mới ở sidebar admin (dưới "Quản lý"). Nội dung từ `password_requests` (đang chờ) và `audit_log` (lịch sử):

| Sự kiện | Lấy từ | Ghi chú |
| --- | --- | --- |
| Quên mật khẩu | `PASSWORD_FORGOT` | kèm IP máy bấm |
| Quên mật khẩu với tên không tồn tại | **thêm** `PASSWORD_FORGOT_UNKNOWN` | server vẫn trả lời giống nhau cho người bấm; chỉ admin thấy |
| Tự đổi mật khẩu | `PASSWORD_CHANGE` | chỉ ghi "đã đổi", không có mật khẩu |
| Đổi sai mật khẩu hiện tại | `PASSWORD_CHANGE_FAIL` | gợi ý bị dò |
| Admin/giáo viên đặt lại | `PASSWORD_RESET` | ai đặt cho ai |
| Khoá do nhập sai nhiều lần | `AUTH_LOCKED` | |

Có nút "Đặt mật khẩu tạm" ngay trên dòng đang chờ, lọc theo loại, nút "Đánh dấu đã đọc". Mốc "đã đọc" lưu theo từng admin: bảng `notice_reads(user_id PRIMARY KEY, last_audit_id)` (kiểm `audit_log` có khoá số tăng dần khi làm; nếu chưa thì dùng `created_at`).

**Migration:** bước mới trong `Migrations.java` + `schema.sql`.

**Test trước:** `NoticesTest` — `quenMatKhauTenLa_adminThayGiaoVienKhong`; `danhDauDaDoc_soDoVeKhong`; `khongBaoGioTraMatKhau`; `hocVienGoiNoticesReq_biTuChoi`.

**Commit:** `fix(client): khung yêu cầu quên mật khẩu tự tải lại và báo số`; `feat(account): trang thông báo bảo mật cho admin`

---

## Đợt 7.C — Độ khó

**Làm:**
- `Lesson`/`LessonRepository`: đọc `do_kho` (thiếu thì `""`), kiểm giá trị ∈ {`de`,`vua`,`kho`}, sai thì cảnh báo log, không làm hỏng bài.
- Migration: bảng `lesson_meta(lesson_id, do_kho, updated_by, updated_at)`; `LESSON_LIST` trả `doKho` = ghi đè nếu có, không thì từ yaml.
- `LESSON_META_SET(lessonIds[], doKho)`: STAFF; ghi `audit_log`.
- Client: chip Dễ/TB/Khó (màu từ `tokens`/CSS sẵn), bộ lọc độ khó và sắp xếp trong `ExerciseCatalogPane`/`LessonFilter`, ô chọn trong form tạo bài, chọn nhiều bài rồi "Đặt độ khó".
- `LessonYaml.dsa/sql` thêm khoá `do_kho`.

**Test trước:** `LessonDifficultyTest` — `yamlCoDoKho_listTraVe`; `ghiDeBangMeta`; `giaTriLa_baoLoiKhongHong`; `hocVienDatDoKho_biTuChoi`; `LessonFilterTest.locTheoDoKho`; `LessonYamlTest.ghiDoKho`.

**Commit:** `feat(lesson): độ khó của bài, lọc và gán hàng loạt`

---

## Đợt 7.D — Ngân hàng bài: tạo qua upload, thiết kế test

Đây là phần 6.B1–6.B3 của giai đoạn 6, thu nhỏ vào đúng thứ bạn cần.

### Task 7.D1: Tạo bài qua upload (không ghi đĩa máy app)

**Làm:** `AddExerciseScreen` dựng **zip trong bộ nhớ** rồi gửi `ADMIN_LESSON_UPLOAD` (kiểm đường dẫn từ #9); thành công thì server nạp lại danh sách bài, **không cần khởi động lại**. Bỏ ô "root" (đường dẫn thư mục). Bài mới mặc định **ẩn**, giáo viên bật khi sẵn sàng. Kiểm tra mới: trùng mã bài (hiện đã làm), thiếu ô bắt buộc.

**Test trước:** `AddExerciseZipTest` (dựng zip thuần Java): `zipCoDuFile_parseLessonQua`; `thieuDapAn_baoLoi`; `maTrung_baoLoi`.

### Task 7.D2: Trình thiết kế test (DSA)

Bảng test thay hai ô Input/Output đơn:

| Cột | Ý nghĩa | Ghi vào |
| --- | --- | --- |
| Tên test | `mau-1`, `bien-1`, `lon-1`… | `tests/<tên>.in/.out` |
| Input, Output mong đợi | nhiều dòng | file `.in`, `.out` |
| Hiện cho học viên | tick: học viên xem được; bỏ tick: ẩn | `test_xem` |
| Ghi chú (tuỳ chọn) | "mảng rỗng", "phần tử trùng" | hiển thị cho giáo viên |

Có nút **Thêm test, nhân đôi, xoá, nhập từ file**. Nút **"Sinh output từ lời giải mẫu"**: chạy lời giải qua từng input để điền output (không phải gõ tay, đỡ sai). Nút **"Kiểm lời giải qua mọi test"** (`LESSON_PREVIEW_REQ`): chạy trong container runner, báo test nào lệch. Bài SQL: giữ dữ liệu mẫu + đáp án, thêm nút "Xem kết quả đáp án" (bảng kết quả) thay cho test. **An toàn:** `loi_giai.py` do giáo viên viết nên luôn chạy trong sandbox runner; server chạy `--khong-sandbox` thì từ chối xem trước (như 6.B1).

**Test trước:** `TestDesignerModelTest` (thuần Java): `nhieuTest_ghiDungTestXem`; `testAn_khongNamTrongTestXem`; `inputOutputLech_baoLoi`; `LessonPreviewTest` — `loiGiaiSaiMotTest_baoDungTenTest`; `khongSandbox_tuChoi`.

**Commit:** `feat(client): tạo bài qua upload, không cần khởi động lại server`; `feat(client): trình thiết kế test nhiều test ẩn/hiện`; `feat(lesson): kiểm lời giải qua các test trước khi đăng`

> **Chưa làm ở đợt này (nói rõ để khỏi hiểu nhầm):** sửa bài đã đăng (phải xoá và tạo lại), điểm từng test, soạn bộ vẽ animation DSA bằng form (task 6.B3 của giai đoạn 6).

---

## Đợt 7.E — Cài đặt ca thực hành: giờ và chính sách nộp

**Dữ liệu (migration mới):**
- `practice_rooms` thêm `opens_at` (NULL = mở ngay), `closes_at` (NULL = không giới hạn), `submit_policy` (`MULTI` | `ONCE`, mặc định `MULTI`).
- `submissions` thêm `room_id` (NULL với bài ngoài ca).

**Làm:**
- `ROOM_SETTINGS_SET(roomId, opensAt, closesAt, submitPolicy)` — STAFF, chỉ giáo viên của lớp hoặc admin.
- `SubmitModule`: học viên đang trong ca (phiên có `roomId`, lấy từ `practice_members`) mà `ONCE` và đã có lần `SUBMIT` của bài đó trong ca → trả `ERROR` (`ErrorCode.ALREADY_SUBMITTED`, mã mới do bạn duyệt). Ngoài `opens_at`–`closes_at` → từ chối. **`RUN` luôn được.** Mốc giờ chỉ tính bằng giờ server.
- Màn "Lớp thực hành": ô giờ mở/đóng, nút chọn "Nộp nhiều lần / Chỉ nộp một lần" có chú thích.

**Test trước:** `PracticeSubmitPolicyTest` — `once_lanHaiBiTuChoi_chayThuVanDuoc`; `multi_nopNhieuLan`; `ngoaiGio_tuChoi`; `baiNgoaiCa_khongBiAnhHuong`.

**Commit:** `feat(practice): giờ mở đóng và chính sách nộp của ca thực hành`

---

## Đợt 7.F — Bảng giám sát ca thực hành

### Task 7.F1: Máy chủ gom dữ liệu

`ROOM_MONITOR_REQ(roomId)` → `ROOM_MONITOR_DATA`; sau đó server **đẩy** bản cập nhật mỗi 2 giây khi có thay đổi (gộp, không gửi từng sự kiện). Thêm bảng `room_student_stats(room_id, user_id, leaves, away_ms, big_pastes, first_ip, last_ip, ip_changes, last_seen)` để **số lần rời app tính riêng cho ca** và không mất khi khởi động lại server (hiện `AppFocus` chỉ giữ trong bộ nhớ).

| Cột | Lấy từ |
| --- | --- |
| Họ tên, tên đăng nhập | `users` |
| IP | `Connection.remoteAddress()` cắt cổng; ghi `first_ip`, `last_ip` |
| Ghế | `lab_seats` (7.G), chưa có thì "—" |
| Bộ đề | 7.G, chưa có thì "—" |
| Trạng thái | Chưa vào · Online · Rời app · Mất kết nối · Đã nộp |
| Bài đang làm | `student_activity` |
| Tiến độ | số bài đã qua / số bài được giao + từng bài ✓ ✗ … |
| Chạy / Nộp / Nộp sai | `submissions` theo `room_id` |
| Rời app | số lần + tổng thời gian ngoài app (`AppFocus` lưu vào `room_student_stats`) |
| Dán khối lớn | cờ của `ClassEvents` |
| Không hoạt động | phút từ lần gõ code cuối |
| **Cảnh báo** | gộp các dấu hiệu bên dưới |

**Dấu hiệu cảnh báo** (ngưỡng đặt trong `ServerConfig`/mặc định, giáo viên chỉnh được ở màn): rời app ≥ 3 lần hoặc ≥ 60 giây; dán khối lớn; đổi IP giữa ca; **hai tài khoản cùng IP** (có thể là đăng nhập hộ, cũng có thể là NAT/nhiều cửa sổ thử — chỉ gắn nhãn, không kết luận); mất kết nối > 30 giây; không hoạt động > 10 phút khi chưa nộp.

### Task 7.F2: Màn giáo viên

Bảng dạng bạn đưa, mở rộng:

| Sinh viên | IP | Ghế | Đề | Trạng thái | Bài / tiến độ | Rời app | Cảnh báo |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Nguyễn Văn A | 192.168.1.10 | B3 | A | Online | 2/3 | 0 | 0 |
| Trần Văn B | 192.168.1.11 | B4 | B | Rời app | 1/3 | 2 (45 s) | 2 |
| Lê Văn C | 192.168.1.12 | C1 | A | Mất kết nối | 0/3 | 1 | 1 |
| Phạm Văn D | 192.168.1.13 | — | — | Chưa vào | — | 0 | 0 |

Thẻ tóm tắt trên cùng (đã vào / chưa vào / đã nộp / có cảnh báo). Lọc theo trạng thái, sắp xếp theo cột, bấm dòng để xem chi tiết (dòng thời gian sự kiện của học viên trong ca) và nút **Tải CSV** (chống công thức Excel như dashboard). Bảng dùng được lại cho "Lớp kiểm tra" (6.C2).

**Test trước:** `RoomMonitorTest` — `chuaVao_hienChuaVao`; `roiApp_demRiengTungCa`; `khoiDongLaiServer_khongMatDem`; `haiTaiKhoanCungIp_gatNhan`; `giaoVienLopKhac_biTuChoi`; `RoomMonitorModelTest` (lọc/sắp xếp thuần Java).

**Commit:** `feat(practice): thống kê giám sát theo ca, lưu bền`; `feat(client): bảng giám sát ca thực hành`

---

## Đợt 7.G — Sơ đồ phòng máy và chia đề so le

> **Điều kiện bắt buộc:** làm `LessonAccess` (task 6.A6 của giai đoạn 6) cho bài trong ca: `LESSON_FETCH`, `RUN_REQ`, `SUBMIT_REQ`, `HINT_REQ`, `CODE_FETCH` phải kiểm "bài này có thuộc bộ đề của học viên này trong ca này không". Nếu không thì học viên mở thẳng bài của bạn bên cạnh bằng mã bài.

### Task 7.G1: Sơ đồ phòng

- Bảng `lab_rooms(id, name, rows, cols, class_id?)` và `lab_seats(lab_room_id, row, col, label, ip)`.
- Màn dựng sơ đồ: lưới hàng × cột, bấm ô để gán IP; nút **"Gợi ý từ máy đang online"** lấy IP các học viên đang vào ca, sắp theo số (192.168.1.9 < .10) rồi xếp từng hàng; giáo viên kéo đổi chỗ. Lưu một lần dùng lại nhiều buổi.
- Một IP có thể gán một ghế; hai máy cùng IP (NAT, nhiều cửa sổ chạy thử trên một máy) thì ghi cảnh báo và để giáo viên gán tay theo tài khoản.
- **Học viên có IP không nằm trong sơ đồ:** gắn "Chưa xếp chỗ", tự vào bộ đề theo vòng tròn (thứ tự vào ca) và hiện nổi bật trên bảng giám sát để giáo viên xếp.

> **Rủi ro cần biết:** IP phản ánh vị trí ngồi chỉ khi máy cắm LAN/IP tĩnh hoặc DHCP giữ chỗ. Ghi nhớ của bạn cho biết Wi-Fi trường **cách ly máy khách** (máy khác không vào được server), Wi-Fi trọ chạy được; nếu thực hành bằng Wi-Fi thì IP đổi mỗi buổi và sơ đồ phải gán lại. Vì vậy sơ đồ lưu theo **ghế ↔ IP của buổi đó** và cho sửa nhanh, không coi IP là sự thật cố định.

### Task 7.G2: Chia bộ đề

- Bảng `room_sets(room_id, set_no, lesson_id)`: giáo viên kéo các bài đã giao vào Bộ A, Bộ B… (một bài có thể nằm nhiều bộ). Nút **"Chia ngẫu nhiên đều"**.
- Quy tắc: `bộ = (hàng + cột) mod N`. Hai ghế sát nhau (trước/sau/trái/phải) không bao giờ cùng bộ khi N ≥ 2.
- Bộ đề của học viên **chốt khi vào ca** (lưu `room_student_stats.set_no`), đổi chỗ giữa ca không đổi bộ; giáo viên có nút "xếp lại" cho từng học viên.
- Học viên chỉ thấy bài của bộ mình trong ca; xem trước "Học viên X sẽ thấy gì" cho giáo viên.

**Test trước:** `SeatingTest` — `banCo_haiGheKeNhauKhacBo` (nhiều kích thước, nhiều N); `ipChuaCoTrongSoDo_tuVongTron_vaGatNhan`; `haiMayCungIp_canhBao`; `LessonAccessTest` — `hocVienBoA_khongMoDuocBaiBoB_khongChay_khongNop`; `giaoVienThayHet`.

**Commit:** `feat(practice): sơ đồ phòng máy gán IP vào ghế`; `feat(practice): chia bộ đề so le theo ghế`; `feat(lesson): LessonAccess chặn bài ngoài bộ đề của học viên`

---

## Nghiệm thu giai đoạn 7

- [ ] Học viên bấm "Quên mật khẩu": admin đang mở app thấy số đỏ và dòng yêu cầu trong **vài giây**, không cần đổi lớp; đặt mật khẩu tạm được ngay từ dòng đó. Tự đổi mật khẩu cũng hiện trong lịch sử, **không lộ mật khẩu**.
- [ ] Menu admin không còn "Code tự do"; giáo viên, học viên còn.
- [ ] Mọi bài có độ khó (hoặc "Chưa phân loại"); lọc và gán hàng loạt hoạt động.
- [ ] Giáo viên tạo một bài DSA có 5 test (2 hiện, 3 ẩn) bằng form, bấm "Kiểm lời giải", đăng; học viên làm được **không cần khởi động lại server**; bài SQL cũng tạo được.
- [ ] Ca thực hành "chỉ nộp một lần": nộp lần hai bị server từ chối, chạy thử vẫn chạy.
- [ ] Bảng giám sát: học viên Alt+Tab 3 lần hiện đúng 3 lần; khởi động lại server không mất số đếm; học viên chưa vào hiện "Chưa vào"; CSV tải được.
- [ ] Phòng 6 máy, 2 bộ đề: hai máy cạnh nhau luôn khác bộ; học viên mở thẳng bài của bộ kia bằng mã bài bị từ chối.
- [ ] `source/mvnw verify` xanh; DB cũ trong `D:\LabCast-chay-thu` mở được bằng bản mới (migration chạy trên DB trống và DB cũ).

## Câu hỏi còn lại (mặc định ở mục 3, bạn đổi nếu muốn)

1. Độ khó 3 mức có đủ không, hay muốn 5 mức / thang điểm?
2. Ca thực hành có cần **hẹn giờ tự bắt đầu / tự nộp khi hết giờ** như dbptit không (đang nằm ở 6.C — kiểm tra)? Kế hoạch này chỉ làm giờ mở/đóng và chặn nộp, chưa tự nộp thay học viên.
3. Cho phép giáo viên **sửa bài đã đăng** (đổi test, đề) ngay không, hay giai đoạn sau?
