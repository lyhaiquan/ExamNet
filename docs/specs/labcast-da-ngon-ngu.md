# LabCast — Đa ngôn ngữ (đợt 1: C, C++, Java, Python)

Bổ sung cho [`labcast-design.md`](labcast-design.md) §7–§8. Đọc spec chính trước.

## 1. Phạm vi

| | Bài giảng giải (`giang-giai`) | Bài tập, luyện tập (`bai-tap`, `luyen-tap`) |
| --- | --- | --- |
| Ngôn ngữ học viên viết | Python (như cũ) | Python, C, C++, Java — học viên chọn |
| Animation | Chỉ Python: trace lời giải mẫu do `viz` sinh | Chỉ khi viết bằng Python (như cũ) |
| Chạy từng dòng (Trace Table) | Python | Mọi ngôn ngữ |
| Lời giải mẫu | Xem được bằng mọi ngôn ngữ có file `loi_giai.<đuôi>` | — |
| Chấm | — | So stdout với test ẩn, như cũ |

Ngoài phạm vi đợt 1: Go, Rust, JavaScript; animation cho code không phải Python; luật soi trace
(`luat.py`) cho code không phải Python — bài có luật soi trace mà nộp bằng C/C++/Java thì chỉ chấm stdout
và hàm cấm.

Mã ngôn ngữ dùng ở mọi nơi (protocol, runner, thư mục bài): `python`, `c`, `cpp`, `java`.

| Mã | Tên hiện trên app | File của học viên | Biên dịch | Chạy |
| --- | --- | --- | --- | --- |
| `python` | Python 3.12 | `main.py` | — | `python3` |
| `c` | C (gcc, C17) | `main.c` | `gcc -std=c17 -O2 -lm` | `./main` |
| `cpp` | C++17 (g++) | `main.cpp` | `g++ -std=c++17 -O2` | `./main` |
| `java` | Java 17 | `Main.java` | `javac -encoding UTF-8` | `java -Xss64m -XX:+UseSerialGC Main` |

Chạy từng dòng biên dịch lại với `-g -O0` (C/C++) hoặc `javac -g` (Java) để debugger đọc được biến.

## 2. Thư mục bài

```text
content/dsa/<chuong>/<bai>/
├── lesson.yaml
├── khung.py            # như cũ
├── khung.c | khung.cpp | khung.java   # tuỳ chọn: code khung theo ngôn ngữ; thiếu thì dùng khung mặc định
├── loi_giai.py         # như cũ — sinh test ẩn, sinh trace animation
└── loi_giai.c | loi_giai.cpp | loi_giai.java   # tuỳ chọn: chỉ để học viên đọc ở bài giảng giải
```

`lesson.yaml` thêm hai khoá, đều tuỳ chọn:

```yaml
ngon_ngu: [python, c, cpp, java]   # mặc định: bài DSA cho cả bốn, bài SQL chỉ sql
cam_theo_ngon_ngu:                 # hàm cấm cho C/C++/Java; cam_dung cũ vẫn chỉ áp cho Python
  cpp: [sort, stable_sort, priority_queue]
  c: [qsort]
  java: [Arrays.sort, Collections.sort, PriorityQueue]
```

Hàm cấm C/C++/Java kiểm bằng cách tách token sau khi bỏ chú thích và chuỗi: `sort` bắt `sort(`, `std::sort(`,
`.sort(`; tên có dấu chấm (`Arrays.sort`) bắt đúng chuỗi token đó. Đây là kiểm tra thô (đổi tên qua
`#define`, `using` vẫn lách được); bài nào cần chặt thì để Python với luật soi trace.

## 3. Lưu code theo ngôn ngữ

Mỗi bài có một bản code cho mỗi ngôn ngữ. **Khoá tài liệu** (dùng trong `CODE_DELTA`, `CODE_ACK`, bảng
`code_doc`, mốc lưu, chiếu code cho giáo viên) là:

- Python: `lessonId` — giữ nguyên, code đã lưu trước đây không mất.
- Ngôn ngữ khác: `lessonId + "#" + mã`, ví dụ `dsa.cay.heap-them#cpp`.

Code ban đầu của khoá `…#<mã>` là `khung.<đuôi>` của bài, thiếu thì là khung mặc định của ngôn ngữ
(đọc input từ stdin, in ra stdout).

Bảng `code_deltas`, `checkpoints` có khoá ngoại `lesson_id → lessons(id)`. Để không phải đổi schema, server
ghi thêm vào bảng `lessons` một dòng cho mỗi khoá tài liệu theo ngôn ngữ (cùng môn, chương, tên với bài gốc).
Không chỗ nào liệt kê bài từ bảng `lessons` (danh sách bài lấy từ `content/`), nên các dòng này chỉ phục vụ
khoá ngoại. Tiến độ, lượt nộp, cờ dán khối lớn vẫn ghi theo mã bài gốc.

Bài nộp sai vì lỗi biên dịch (`COMPILE_ERROR`) tính một lượt nộp sai như các verdict khác; Chạy thử biên dịch
trước được nên học viên luôn kiểm được.

## 4. Protocol

| Thông điệp | Thay đổi |
| --- | --- |
| `RUN_REQ` | thêm `str lang` cuối payload, sau `bool lineTrace`. Thiếu (client cũ) = `python` |
| `SUBMIT_REQ` | thêm `str lang` cuối payload. Thiếu = `python` |
| `PLAYGROUND_RUN_REQ` | thêm `lang` vào JSON (Code tự do chạy được cả bốn ngôn ngữ). Thiếu = `python` |
| `CODE_FETCH (0x55)` | **mới** — `str docKey` |
| `CODE_DATA (0x56)` | **mới** — `str docKey, i64 seq, str code`: code đang lưu trên server (hoặc khung nếu chưa có) |
| `LESSON_DATA` (bài) | thêm `ngonNgu: [mã…]` và, ở bài giảng giải, `loiGiai: {mã: code}` cho các ngôn ngữ có file |
| `RUN_RESULT` | thêm `errorLine` (số dòng lỗi, kể cả lỗi biên dịch) và `errorKind` khi có |

Server từ chối `lang` không nằm trong `ngonNgu` của bài (`BAD_FRAME`).

## 5. Runner

`run.py` và `cham.py` nhận thêm khoá `"ngon_ngu"` (mặc định `python`). Python đi đường cũ. Ngôn ngữ khác đi
qua `ngon_ngu.py`:

1. Ghi code vào `/tmp/lc-<ngẫu nhiên>/main.<đuôi>`, biên dịch. Lỗi biên dịch trả `ok: false`, `error` là
   thông báo của trình biên dịch (rút gọn, đường dẫn đổi thành tên file), `error_line` là dòng lỗi đầu tiên.
   **Thời gian biên dịch không tính** vào giới hạn của bài.
2. Chạy chương trình với stdin là input của test, đo giờ từ lúc chạy, giới hạn CPU đặt riêng cho tiến trình
   chương trình (không áp cho trình biên dịch). Java được nhân thời gian ×2 (JVM khởi động chậm).
3. Chạy từng dòng (`line_trace`): chạy chương trình dưới debugger, mỗi dòng của file học viên sinh
   `{"t":"line","n":…}` và `{"t":"vars","f":hàm,"d":[[tên, giá trị]…]}` — đúng định dạng Trace Table đang đọc
   (trace bọc giống chạy từng dòng của Python: `hdr` có `source`, `snap` kiểu `code-bien`, sự kiện mang
   `"v": "__trace_table__"`). Tối đa 800 dòng; quá thì dừng ghi, chương trình vẫn chạy tiếp để có output.
   - **Không dùng lệnh bước dòng của debugger** (`step` của gdb, `StepRequest` của JDI): vòng lặp nằm gọn
     trên một dòng (`while (true) { x++; }`) nhảy về chính dòng đó nên lệnh bước không bao giờ dừng. Thay
     vào đó đặt breakpoint ở **mọi mục của bảng dòng** (line table) của file học viên rồi `continue`: dừng
     đúng mỗi lần tới một dòng, kể cả quay lại; không bao giờ dừng trong thư viện chuẩn. Cùng dòng, cùng lời
     gọi hàm, địa chỉ lệnh đi tiếp về sau thì là cùng một lần ghé (chỉ cập nhật biến); nhảy ngược là vòng
     lặp mới.
   - C, C++: `gdb` (API Python của gdb, `runner/buoc/gdb_buoc.py`). Breakpoint đặt sau khi chương trình đã
     nạp (file chạy PIE). Bỏ lần dừng ở địa chỉ vào hàm (tham số chưa có giá trị). Dòng chỉ có `}` giữ bảng
     biến trước đó (destructor đã giải phóng `vector`).
   - Java: JDI (`runner/buoc/JdiBuoc.java`, biên dịch sẵn khi build image). Breakpoint ở mọi lớp nạp từ
     `Main.java` (kể cả lớp lồng, constructor).
   - Giá trị biến hiện dạng chữ ngắn (tối đa 120 ký tự): số, chuỗi (mảng `char` cắt ở `'\0'`), con trỏ
     (`NULL` hay địa chỉ), mảng và mảng 2 chiều (tối đa 20 phần tử mỗi chiều), struct; `vector`, `string`,
     `map` qua pretty printer của libstdc++; Java: `ArrayList`, `PriorityQueue`, `ArrayDeque`, kiểu bọc số,
     lớp tự viết (tối đa 5 trường), các collection khác hiện `HashMap(size=3)`.
   - **Giới hạn:** vòng lặp viết trọn trên một dòng (`for (...) cin >> a[i];`) chỉ hiện một bước: trình biên
     dịch không ghi mục bảng dòng cho chỗ nhảy ngược giữa dòng. Viết thân vòng lặp xuống dòng thì hiện đủ từng
     vòng. Biến C/C++ chưa khởi tạo hiện giá trị rác thật của bộ nhớ.

`cham.py` biên dịch **một lần** rồi chạy từng test ở tiến trình riêng như cũ.

Image runner (tên giữ nguyên `labcast-runner:py3.12`) chuyển sang `ubuntu:24.04`: có sẵn Python 3.12 và
OpenJDK 17 đúng chuẩn của spec (image `python:3.12-slim` hiện dựa trên Debian 13, chỉ còn OpenJDK 21), thêm
`gcc`, `g++`, `gdb`. `bits/stdc++.h` biên dịch sẵn (precompiled header) cho cả hai bộ cờ, bỏ được 1–2 s mỗi
lượt C++. Image khoảng 1,2 GB.

Sandbox đổi `--tmpfs /tmp:size=16m` thành `--tmpfs /tmp:size=64m,exec`: tmpfs của Docker mặc định `noexec`,
bài C/C++ phải chạy file vừa biên dịch trong `/tmp`; file biên dịch kèm debug cần chỗ hơn 16 MB. Các lớp bảo
vệ khác giữ nguyên (không mạng, ổ gốc chỉ đọc, không root, bỏ mọi capability, 64 tiến trình, 256 MB).

Server cộng thêm vào hạn chờ của lượt chạy: thời gian biên dịch (C/C++ 6 s, Java 8 s) và, khi chạy từng dòng
C/C++/Java, 16 s cho debugger (runner tự cắt ở giới hạn + 15 s). Giới hạn CPU đặt riêng cho tiến trình
chương trình (debugger: giới hạn + 2 s, chương trình kế thừa), không áp cho trình biên dịch.

Thời gian đo được trong sandbox (máy CI, `--cpus 1`): Chạy thử C++ khoảng 0,8 s cả biên dịch; chạy từng dòng
C++ khoảng 1,1 s; Java chạy thử 1,5 s, chạy từng dòng 2–3 s; vòng lặp vô tận khi chạy từng dòng: 5–7 s rồi
báo quá giờ.

## 6. App

- Ô chọn ngôn ngữ cạnh ô code: Python 3.12, C (gcc), C++17, Java 17 — chỉ các ngôn ngữ trong `ngonNgu`.
  Bài SQL giữ "SQL · SQLite". Đổi ngôn ngữ thì app `CODE_FETCH` khoá tài liệu của ngôn ngữ đó, nạp vào
  ô code và từ đó gửi `CODE_DELTA` với khoá mới; Chạy thử, Trace từng dòng, Nộp bài gửi kèm `lang`.
- Code không phải Python: tab Animation hiện "Animation chỉ có với Python — dùng Trace từng dòng"; Trace
  Table chạy như Python.
- Bài giảng giải có `loiGiai`: tab "Lời giải" hiện code mẫu chỉ đọc, chọn được ngôn ngữ.

## 7. Phân công

| Phần | Người |
| --- | --- |
| Runner: `ngon_ngu.py`, `cham.py`, `kiem_cam` C/C++/Java, Dockerfile | Người 3 |
| Debugger: `gdb_buoc.py`, `JdiBuoc.java` | Người 3 (đọc trace: Người 4 duyệt) |
| Server: `RUN_REQ`/`SUBMIT_REQ` có `lang`, `Lesson.ngonNgu`, `loiGiai` | Người 3 |
| Server: khoá tài liệu theo ngôn ngữ, `CODE_FETCH`/`CODE_DATA` | Người 1 |
| App: ô chọn ngôn ngữ, tab Lời giải | Người 1, Người 4 |
