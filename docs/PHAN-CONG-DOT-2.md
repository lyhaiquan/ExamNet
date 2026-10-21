# Phân công đợt 2 — quản trị, lớp, bài giao, giao diện mới

Đợt này **4 người** — A: leader, B: Danh Hải, C: Đinh Hoàng Vũ, D: Vũ Viết Anh — chia theo **ranh giới file** chứ không theo tính năng: mỗi file chỉ một chủ, nên hai người không bao giờ sửa cùng một file cùng lúc. Bản phân công gốc ở [`PHAN-CONG.md`](PHAN-CONG.md) vẫn giữ cho phần cũ.

Bản demo giao diện (3 màn: học viên làm bài kiểm tra, giáo viên giao bài, trang quản trị): xem link trong PR của file này.

## Việc cần làm

| # | Việc | Ai |
| --- | --- | --- |
| 1 | Một server nhiều lớp: DSA1, DSA2, SQL1… mỗi lớp danh sách học viên riêng, giáo viên phụ trách riêng | A |
| 2 | Trang quản trị web: ai đang online theo lớp, quản lý lớp, học viên, giáo viên | A (**đã xong**, `/admin`) |
| 3 | Output của chương trình hiện trong Trace Table | A |
| 4 | Thực hành / kiểm tra theo lớp, đặt lịch như dbptit: **đến giờ tự bắt đầu, hết giờ tự nộp** | B — Danh Hải |
| 5 | Sidebar 4 nhóm: **Bài mẫu** · **Luyện tập** · **Thực hành** · **Kiểm tra** | B làm dữ liệu, C vẽ |
| 6 | Thiết kế lại giao diện app **cả học viên lẫn giáo viên**, bộ màu và chữ dùng chung | C — Đinh Hoàng Vũ |
| 7 | Upload bài học **ngay trong giao diện giáo viên**; bật/tắt (active/inactive) từng bài cho lớp | D — Vũ Viết Anh |

## Bốn người, bốn vùng

Ai làm tính năng nào thì viết **cả logic lẫn màn hình** của tính năng đó trong package riêng; C giữ khung app (màn chính, bộ màu, CSS) và cắm màn của B, D vào qua `TeacherPanel`. Màn của B, D chỉ dùng class CSS và biến màu của C, không tự đặt màu.

### A — Lớp, quản trị, Trace Table (leader)

| | |
| --- | --- |
| Package / file | `server.classroom`, `server.account`, `server.admin`, `server.ops`, `server.store`, `server.session`, `server.presence`, `common.protocol` (`MessageType`, `ErrorCode`), `schema.sql`, `ServerConfig`, `TraceTablePane.java`, trang `server/src/main/resources/labcast/server/admin/*` |
| Thông điệp | dải `0x80–0x8B` (`CLASS_LIST`, `CLASS_INFO`, `ADMIN_USER_*`, `ADMIN_CLASS_*`) |
| Bảng DB | `classes`, `enrollments`, `class_teachers` |
| Cung cấp | `ClassDirectory` (đăng ký trong `ServerContext`): `all()`, `classesOf(userId)`, `studentsOf(classId)`, `teachersOf(classId)`; `Session.classId()` |

Trang quản trị đã có: `http://localhost:8080/admin`, chỉ mở được trên máy server.

### B — Danh Hải: thực hành, kiểm tra, lịch theo lớp

| | |
| --- | --- |
| Package / file | `server.assign` (mới), `server.practice`, `server.run`, `client.assign` (logic + màn giao bài, lịch ca của giáo viên) |
| Thông điệp | dải `0x90–0x9F`: `ASSIGN_LIST`, `ASSIGN_SET`, `ASSIGN_OPEN`, `ASSIGN_CLOSE`, `ASSIGN_STATE`, `SCHEDULE_LIST`, `SCHEDULE_SET` |
| Bảng DB | `assignments`, `assignment_starts`, `class_schedule` |
| Cung cấp | `AssignmentService.visible(classId, now)`, `AssignmentService.canSubmit(userId, lessonId, now)`; `SidebarModel`, `Countdown`, `AssignController` |

Giống dbptit: giáo viên đặt bài cho một lớp với giờ mở, giờ đóng, số phút. **Đến giờ mở** server tự đẩy `ASSIGN_STATE` cho cả lớp, app tự mở bài và bắt đầu đếm giờ. **Hết giờ** server tự lấy code mới nhất đã lưu (checkpoint) nộp thay học viên, rồi khoá bài. Mọi mốc giờ **chỉ tính bằng giờ server**; `Clock.serverNow()` phía app chỉ để hiện đồng hồ. Nộp ngoài giờ thì server từ chối `SUBMIT_REQ` bằng `NOT_OPEN` hoặc `TIME_UP`.

### C — Đinh Hoàng Vũ: giao diện app học viên và giáo viên

| | |
| --- | --- |
| Package / file | `client.ui` (đăng nhập, `HomeScreen`, `LessonScreen`, `PlaygroundScreen`, `Theme`, trừ `TraceTablePane`), `client.views` (phần màu, chữ), `client/src/main/resources/labcast/client/**/*.css`, icon, font, **`design/tokens.css`** |
| Không có | thông điệp, bảng DB |
| Nhận từ người khác | `SidebarModel`, `Countdown` (B); màn của B và D qua `TeacherPanel`; `Session.classId()` và tên lớp (A) |

### D — Vũ Viết Anh: upload bài, bật/tắt bài

| | |
| --- | --- |
| Package / file | `server.upload` (mới), `client.teacher` (màn upload, danh sách bài có công tắc bật/tắt) |
| Thông điệp | dải `0x8C–0x8F`: `ADMIN_LESSON_UPLOAD` (đã có), thêm `LESSON_ACTIVE_LIST`, `LESSON_ACTIVE_SET` |
| Bảng DB | `lesson_uploads` (đã có); thêm `lesson_active (class_id, lesson_id, active)` bằng PR nhỏ |
| Cung cấp | `LessonActivation.isActive(classId, lessonId)`: B và màn danh sách bài gọi để ẩn bài đang tắt |

Upload: giáo viên chọn file `.zip` trong app, giải nén vào thư mục tạm, kiểm bằng `LessonRepository.load` (không sửa class này), hợp lệ mới chép vào `content/` và nạp lại danh sách bài; lỗi thì hiện nguyên danh sách lỗi. Bật/tắt áp cho **bài mẫu và luyện tập** của từng lớp; bài thực hành, kiểm tra do lịch của B quyết định, không dùng công tắc này.

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
3. File nóng chỉ một chủ: `LessonScreen`, `HomeScreen`, `tokens.css`, CSS (C); `schema.sql`, `MessageType`, `ServerConfig`, `TraceTablePane`, trang quản trị (A). Cần thêm gì thì nhờ chủ, hoặc đưa logic vào class riêng của mình để chủ gọi.
4. **C làm bằng dữ liệu giả** (`mock/*.json`) đúng định dạng đã chốt, không chờ B và D.
5. **Không chạy `spotless:apply` cho cả repo**: chỉ chạy cho module mình sửa và trả lại file ngoài vùng trước khi commit.
6. Không thêm khoá mới vào file `.conf` ngoài A. Lớp, ca học, bài giao lưu trong database.
7. Màu, cỡ chữ, khoảng cách lấy từ biến của `tokens.css`; không ai viết màu cứng trong Java hay HTML. Thêm biến mới thì nhờ C.

## Thứ tự

| Ngày | A | B — Danh Hải | C — Đinh Hoàng Vũ | D — Vũ Viết Anh |
| --- | --- | --- | --- | --- |
| 1–2 | PR hợp đồng; trang quản trị (đã xong) | Duyệt hợp đồng; bảng `assignments`, `AssignmentService` | `tokens.css` từ bản demo, bộ CSS chung | Duyệt hợp đồng; bảng `lesson_active`, `LessonActivation` |
| 3–6 | Đăng nhập vào đúng lớp, `CLASS_LIST` | `ASSIGN_*`, chặn nộp ngoài giờ | Sidebar 4 nhóm, màn làm bài (dữ liệu giả) | Upload phía server: giải nén, kiểm, nạp lại |
| 7–10 | Output trong Trace Table | Tự bắt đầu khi tới giờ, tự nộp khi hết giờ; màn đặt lịch của giáo viên | Ghép `SidebarModel`, `Countdown` thật; màn giáo viên | Màn upload và công tắc bật/tắt trong app giáo viên |
| 11–12 | Sửa lỗi ghép nối | Kiểm tra: khoá bài sau khi nộp tự động | Màn đăng nhập, sửa chi tiết | Sửa chi tiết |
| 13 | Cả 4: chạy thử một buổi với 2 lớp, 1 bài thực hành, 1 bài kiểm tra 15 phút tự nộp | | | |
