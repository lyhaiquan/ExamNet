# Kế hoạch chi tiết theo giai đoạn

Mỗi file là kế hoạch triển khai của **một giai đoạn** trong [`../specs/labcast-ke-hoach.md`](../specs/labcast-ke-hoach.md). Kế hoạch tổng nói *ai làm gì, xong khi nào*; các file ở đây nói *làm từng bước ra sao*: file nào, lớp và hàm nào, payload từng thông điệp, test nào phải viết trước, lệnh chạy, câu commit.

| File | Giai đoạn | Tuần |
| --- | --- | --- |
| [giai-doan-0.md](giai-doan-0.md) | Đề xuất và chuẩn bị | trước T0 → 1 |
| [giai-doan-1.md](giai-doan-1.md) | Xương sống | 2–3 |
| [giai-doan-2.md](giai-doan-2.md) | Lõi từng người | 4–6 |
| [giai-doan-3.md](giai-doan-3.md) | Trợ giúp và hoàn thiện | 7–8 |
| [giai-doan-4.md](giai-doan-4.md) | Thí nghiệm và nội dung | 9–10 |
| [giai-doan-5.md](giai-doan-5.md) | Đóng gói và bảo vệ | 11 |
| [danh-muc-bai.md](danh-muc-bai.md) | Danh mục 53 bài giảng giải, 100 bài DSA, 100 bài SQL (bản tạm, chờ đề cương) | soạn ở tuần 9–10 |

## Mức chi tiết

Các file này cho **hợp đồng** (chữ ký lớp, payload, giao diện giữa các người), **test viết trước** và **thuật toán ở chỗ khó**. Phần thân còn lại mỗi người tự viết: nhóm phải tự giải thích được code của mình khi bảo vệ (`Instruction.md` §11).

Chỗ nào ghi "tham khảo bản cũ" là có lớp tương tự trong code ExamNet ở `gitlab.com/ptit-networking/btl_networking` (`source/…/examnet/…`). Đọc để hiểu cách làm, viết lại theo tên và hợp đồng của LabCast.

## Quy ước chung cho mọi task

**Một task = một nhánh = một PR.** Nhánh `<type>/<scope>-<mô-tả>`, ví dụ `feat/common-frame-codec`.

**Viết test trước (TDD):**

1. Viết test theo phần "Test" của task. Chạy, thấy **đỏ** đúng lý do (chưa có lớp hoặc sai kết quả).
2. Viết code tối thiểu cho test xanh.
3. Chạy lại cả module, thấy **xanh**.
4. Commit đúng câu ghi ở task.

**Chạy một lớp test** (đứng trong `source/`; Windows dùng `mvnw.cmd`):

```bash
./mvnw -q -pl common -am test -Dtest=FrameCodecTest -Dsurefire.failIfNoSpecifiedTests=false
```

**Chạy toàn bộ trước khi mở PR:** `./mvnw verify` (gồm kiểm định dạng; sửa nhanh bằng `./mvnw spotless:apply`). Phần Python: `python -m pytest runner`.

**Tên test** viết tiếng Việt không dấu theo kiểu `hanhVi_ketQua`, ví dụ `docTungByteMot_vanRaDungFrame`.

**Test cần mạng thật** mở cổng `0` (để hệ điều hành chọn cổng trống) và địa chỉ `127.0.0.1`, đóng tài nguyên bằng `try-with-resources`. Test cần Docker hoặc Python thì bỏ qua khi máy không có, bằng `Assumptions.assumeTrue(...)`.

**Không có test cho giao diện JavaFX.** Tách logic ra lớp thuần Java (model, controller) rồi test lớp đó; lớp `View`/`Node` chỉ vẽ.

## Bảng mã thông điệp

`MessageType` dùng một byte. Mỗi người một dải mã, **không đánh số lại** mã đã có. Leader điền đủ bảng này ngay trong task 1.2 của giai đoạn 1.

| Dải | Chủ | Mã |
| --- | --- | --- |
| `0x01–0x0F` | 2 | `HELLO 01`, `HELLO_ACK 02`, `AUTH 03`, `AUTH_OK 04`, `AUTH_FAIL 05`, `HEARTBEAT 06`, `HEARTBEAT_ACK 07`, `RESUME_REQ 08`, `RESUME_STATE 09`, `ERROR 0E`, `BYE 0F` |
| `0x10–0x17` | 2 | `DISCOVER 10`, `DISCOVER_REPLY 11` |
| `0x18–0x1F` | 3 | `TIME_SYNC_REQ 18`, `TIME_SYNC_RESP 19` |
| `0x20–0x2F` | 2 | `CLASS_JOIN 20`, `CLASS_STATE 21`, `LESSON_PUSH 22`, `ADMIN_ROSTER_IMPORT 28`, `ADMIN_PROGRESS_EXPORT 29`, `ADMIN_OK 2A`, `QUIZ_OPEN 2C`, `QUIZ_ANSWER 2D`, `QUIZ_CLOSE 2E`, `QUIZ_RESULT 2F` |
| `0x30–0x4F` | 3 | `LESSON_LIST 30`, `LESSON_FETCH 31`, `LESSON_DATA 32`, `PREDICT_REQ 38`, `PREDICT_QUESTION 39`, `PREDICT_ANSWER 3A`, `PREDICT_RESULT 3B`, `SIM_START 3C`, `SIM_STEP 3D`, `SIM_RESULT 3E`, `RUN_REQ 40`, `RUN_QUEUED 41`, `TRACE_CHUNK 42`, `RUN_RESULT 43`, `SUBMIT_REQ 44`, `SUBMIT_RESULT 45`, `HINT_REQ 46`, `HINT_DATA 47` |
| `0x50–0x6F` | 1 | `CODE_DELTA 50`, `CODE_ACK 51`, `CHECKPOINT_LIST 52`, `CHECKPOINT_FETCH 53`, `CHECKPOINT_DATA 54`, `HAND_RAISE 58`, `HAND_LOWER 59`, `MIRROR_SUBSCRIBE 5A`, `MIRROR_UNSUBSCRIBE 5B`, `MIRROR_SNAPSHOT 5C`, `MIRROR_DELTA 5D`, `COMMENT_ADD 60`, `COMMENT 61`, `VIEW_JOIN 64`, `VIEW_CONTROL 65`, `VIEW_STATE 66` |
| `0x70–0x7F` | 4 | `CAST_CONTROL 70`, `CAST_DATA 71`, `CAST_KEYFRAME 72`, `CAST_NACK 73`, `CAST_REPAIR 74` |

## Ký hiệu payload

Payload nhị phân viết theo thứ tự trường, kiểu lấy từ `PayloadWriter`:

| Ký hiệu | Nghĩa |
| --- | --- |
| `u8`, `u16` | Số không dấu 1, 2 byte |
| `i32`, `i64` | Số có dấu 4, 8 byte, big-endian |
| `bool` | 1 byte, 0 hoặc 1 |
| `str` | `[độ dài: u16][UTF-8]`, tối đa 65 535 byte |
| `blob` | `[độ dài: i32][byte]` |
| `list<X>` | `[số phần tử: u16]` rồi từng phần tử X |
| `JSON{…}` | Cả payload là JSON UTF-8, frame đặt cờ `FLAG_JSON` |

Thời gian luôn là `i64` mili giây theo **đồng hồ server** (`Clock.serverNow()` phía app), trừ khi ghi rõ khác.
