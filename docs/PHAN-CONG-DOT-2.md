# Phân công đợt 2 — quản trị, lớp, bài giao, giao diện mới

Đợt này **4 người**, chia theo **ranh giới file** chứ không theo tính năng: mỗi file chỉ một chủ, nên hai người không bao giờ sửa cùng một file cùng lúc. Bản phân công gốc ở [`PHAN-CONG.md`](PHAN-CONG.md) vẫn giữ cho phần cũ.

Bản demo giao diện (3 màn: học viên làm bài kiểm tra, giáo viên giao bài, trang quản trị): xem link trong PR của file này.

## Việc cần làm

| # | Việc | Ai |
| --- | --- | --- |
| 1 | Một server nhiều lớp: DSA1, DSA2, SQL1… mỗi lớp danh sách học viên riêng, giáo viên phụ trách riêng | A |
| 2 | Trang quản trị web: ai đang online theo lớp, quản lý học viên/giáo viên/lớp, upload bài học | A làm API, D làm trang |
| 3 | Bài giao: giáo viên chọn bài cho lớp làm **thực hành** hoặc **kiểm tra**, bật/tắt, giới hạn thời gian (ví dụ 60 phút), khung giờ mở (từ … đến …), ca học của lớp | B làm logic, D làm màn giáo viên |
| 4 | Sidebar chia 4 nhóm: **Bài mẫu** · **Luyện tập** · **Thực hành** · **Kiểm tra** (2 nhóm sau chỉ hiện bài đang được giao cho lớp) | B làm dữ liệu, C vẽ |
| 5 | Output của chương trình hiện trong Trace Table | B |
| 6 | Thiết kế lại app học viên, bộ màu và chữ dùng chung | C |
| 7 | Thiết kế lại màn giáo viên và trang quản trị theo bộ màu của C | D |

## Bốn người, bốn vùng

### A — Lớp và quản trị phía server (leader)

| | |
| --- | --- |
| Package / file | `server.classroom`, `server.account`, `server.admin` (mới, API JSON), `server.ops`, `server.store`, `common.protocol` (`MessageType`, `ErrorCode`), `schema.sql`, `ServerConfig` |
| Thông điệp mới | dải `0x80–0x8F`: `CLASS_LIST`, `CLASS_INFO`, `ADMIN_USER_*`, `ADMIN_CLASS_*`, `ADMIN_LESSON_UPLOAD` |
| API web mới | `/api/admin/overview`, `/api/admin/classes`, `/api/admin/users`, `/api/admin/lessons/upload` (chỉ máy server hoặc tài khoản quản trị) |
| Bảng DB | `classes`, `enrollments` (đã có, dùng thật), `class_teachers` (mới), `lesson_uploads` (mới) |
| Cung cấp cho người khác | `ClassDirectory.classesOf(userId)`, `ClassDirectory.studentsOf(classId)`, `Session.classId()` |

Upload bài học: nhận file `.zip`, giải nén vào thư mục tạm, kiểm bằng `LessonRepository.load` (không sửa class này), hợp lệ mới chép vào `content/` và nạp lại danh sách bài; lỗi thì trả nguyên danh sách lỗi.

### B — Bài giao, thời gian, Trace Table

| | |
| --- | --- |
| Package / file | `server.assign` (mới), `server.practice`, `server.run`, `client.assign` (mới, **chỉ logic, không có Node JavaFX**), `TraceTablePane.java` |
| Thông điệp mới | dải `0x90–0x9F`: `ASSIGN_LIST`, `ASSIGN_SET`, `ASSIGN_OPEN`, `ASSIGN_CLOSE`, `ASSIGN_STATE` |
| Bảng DB | `assignments` (mới), `class_schedule` (mới) |
| Cung cấp cho người khác | `AssignmentService.visible(classId, now)` → bài theo 4 nhóm; `AssignmentService.canSubmit(userId, lessonId, now)`; `client.assign.SidebarModel`, `client.assign.Countdown`, `client.assign.AssignController` (giáo viên giao bài) |

Luật thời gian **chỉ kiểm ở server** bằng giờ server; `Clock.serverNow()` phía app chỉ để hiện đồng hồ đếm ngược. Hết giờ hoặc ngoài khung giờ thì server từ chối `SUBMIT_REQ` bằng `ErrorCode` riêng — không tin giờ máy học viên.

### C — App học viên và bộ màu chung

| | |
| --- | --- |
| Package / file | `client.ui` (màn đăng nhập, `HomeScreen`, `LessonScreen`, `PlaygroundScreen`, `Theme`), `client.views` (phần màu, chữ), `client/src/main/resources/labcast/client/ui/*` (CSS, icon, font), **`design/tokens.css`** (bộ màu, chữ, khoảng cách dùng chung) |
| Không có | thông điệp, bảng DB |
| Nhận từ người khác | `SidebarModel`, `Countdown` (B); `Session.classId()` và tên lớp (A) |

### D — Màn giáo viên và trang quản trị

| | |
| --- | --- |
| Package / file | `client.teacher` (mới: màn giao bài, chọn lớp, lịch ca, theo dõi lớp), `client/src/main/resources/labcast/client/teacher/*.css`, `server/src/main/resources/labcast/server/admin/*` (HTML, JS, CSS của trang quản trị) |
| Không có | thông điệp, bảng DB, code server Java |
| Nhận từ người khác | `AssignController` (B), API `/api/admin/*` (A), `design/tokens.css` (C) |

`HomeScreen` của C chỉ chừa **một chỗ gắn** cho giáo viên (`TeacherPanel`, interface do D định nghĩa trong PR hợp đồng); D cắm màn của mình vào đó, không sửa file của C.

## Hợp đồng chốt trong 2 ngày đầu (A mở một PR, cả 4 duyệt)

PR "hợp đồng" là PR **duy nhất** được sửa cùng lúc nhiều vùng. Trong đó:

1. Toàn bộ mã `MessageType` mới (hai dải ở trên) và `ErrorCode` mới, chưa cần xử lý.
2. Toàn bộ bảng mới trong `schema.sql`, mỗi bảng ghi `-- chủ: A` hoặc `-- chủ: B`.
3. Interface Java rỗng: `ClassDirectory`, `AssignmentService`, `SidebarModel`, `Countdown`, `AssignController`, `TeacherPanel`.
4. `design/tokens.css` bản đầu (lấy từ bản demo) và `.github/CODEOWNERS`.
5. Định dạng JSON, tối thiểu:

```json
// LESSON_LIST trả thêm nhóm và trạng thái bài giao
{"lessons": [{"id": "dsa.sap-xep.chon", "nhom": "mau|luyen-tap|thuc-hanh|kiem-tra",
  "giao": {"moLuc": 1730000000000, "dongLuc": 1730003600000, "phut": 60, "batDauLuc": null}}]}

// ASSIGN_SET (giáo viên giao bài cho lớp)
{"classId": 3, "lessonIds": ["dsa.tim-kiem.mang-xoay"], "loai": "thuc-hanh|kiem-tra",
 "moLuc": 1730000000000, "dongLuc": 1730007200000, "phut": 60}

// GET /api/admin/overview
{"online": 51, "tong": 148, "lop": [{"id": 3, "ten": "DSA2", "mon": "dsa", "giaoVien": "…",
  "online": 27, "siSo": 30, "ca": "13:30–16:00", "baiGiao": "Kiểm tra · 3 bài"}]}
```

Sau PR này **mỗi người chỉ sửa vùng của mình**. Cần đổi hợp đồng thì mở PR nhỏ riêng, cả 4 duyệt.

## Luật chống giẫm chân

1. **CODEOWNERS bắt buộc duyệt**: gán chủ cho từng đường dẫn ở trên; bật "Require review from Code Owners" ở nhánh `main`. PR đụng file của người khác phải chờ người đó duyệt.
2. Một task một nhánh, PR nhỏ (dưới ~400 dòng), merge trong ngày. Mỗi sáng `git pull --rebase origin main`.
3. File nóng chỉ một chủ: `LessonScreen`, `HomeScreen`, `tokens.css` (C); `schema.sql`, `MessageType`, `ServerConfig` (A); `TraceTablePane` (B); trang quản trị (D). Cần thêm gì thì nhờ chủ, hoặc đưa logic vào class riêng của mình để chủ gọi.
4. **C và D làm bằng dữ liệu giả** (`mock/*.json`) đúng định dạng đã chốt, không chờ A và B.
5. **Không chạy `spotless:apply` cho cả repo**: chỉ chạy cho module mình sửa và trả lại file ngoài vùng trước khi commit.
6. Không thêm khoá mới vào file `.conf` ngoài A. Lớp, ca học, bài giao lưu trong database.
7. Màu, cỡ chữ, khoảng cách lấy từ biến của `tokens.css`; không ai viết màu cứng trong Java hay HTML. Thêm biến mới thì nhờ C.

## Thứ tự

| Ngày | A | B | C | D |
| --- | --- | --- | --- | --- |
| 1–2 | PR hợp đồng | Duyệt; sửa output trong Trace Table | `tokens.css` từ bản demo | Duyệt; khung trang quản trị bằng dữ liệu giả |
| 3–6 | Nhiều lớp, `ClassDirectory`, đăng nhập vào đúng lớp | `assignments`, `class_schedule`, `AssignmentService`, chặn nộp ngoài giờ | Sidebar 4 nhóm, màn làm bài (dữ liệu giả) | Màn giao bài, chọn lớp, lịch ca (dữ liệu giả) |
| 7–10 | API quản trị: online, học viên, giáo viên, lớp | `AssignController`, đồng hồ đếm ngược | Ghép với `SidebarModel`, `Countdown` thật | Ghép màn giáo viên với B, trang quản trị với A |
| 11–12 | Upload bài học | Kiểm tra: khoá bài khi hết giờ, nộp tự động | Màn đăng nhập, sửa chi tiết | Upload bài trên trang quản trị, sửa chi tiết |
| 13 | Cả 4: chạy thử một buổi với 2 lớp, 1 bài thực hành, 1 bài kiểm tra 15 phút | | | |
