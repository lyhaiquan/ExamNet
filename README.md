# LabCast — Phòng thực hành DSA và SQL có animation

> **NETWORK PROGRAMMING – FINAL PROJECT** · Năm học 2026–2027

Hệ thống client/server cho buổi thực hành trong phòng máy: giáo viên giảng bằng animation phát đồng bộ tới mọi máy, học viên luyện tập và xem code của chính mình chạy thành animation, giáo viên theo dõi cả lớp và trợ giúp trực tiếp. Viết bằng **Java 17**, phần mạng chỉ dùng thư viện chuẩn của Java.

- Thiết kế: [`docs/specs/labcast-design.md`](docs/specs/labcast-design.md)
- Kế hoạch triển khai: [`docs/specs/labcast-ke-hoach.md`](docs/specs/labcast-ke-hoach.md)
- Phân công: [`docs/PHAN-CONG.md`](docs/PHAN-CONG.md)

**Trạng thái:** đã chạy được đầy đủ: đăng nhập 3 vai trò, 207 bài DSA/SQL, chạy và chấm bài, chiếu animation multicast, lưu code liên tục, ca thực hành có đồng bộ giờ. Ba thí nghiệm TN1–TN3 đã đo (mục 13–14).

---

# Chạy nhanh (5 phút, Windows)

Dành cho người mới clone về, muốn mở app lên thử ngay trên **một máy**.

**Cần cài:** [JDK 17 trở lên](https://adoptium.net) (17 → 26 đều được) và Git. Kiểm tra: `java -version`.
Không cần Maven, không cần database, không cần Docker để chạy thử.

**1. Lấy code và build** (PowerShell, lần đầu mất 2–5 phút để tải thư viện):

```powershell
git clone https://github.com/lyhaiquan/ExamNet.git LabCast
cd LabCast\source
.\mvnw.cmd -q package -DskipTests
```

**2. Bật server** (giữ cửa sổ này mở):

```powershell
java -jar server/target/labcast-server.jar --content ../content --khong-sandbox --tao-admin "ad:admin123:Quan tri" --tao-giao-vien "gv:gv123:Giao vien"
```

Thấy dòng `LabCast nghe TCP 7000` là server đã chạy. Họ tên trong lệnh viết **không dấu** (Windows làm hỏng chữ có dấu trên dòng lệnh; đổi lại họ tên có dấu trong app sau). `--khong-sandbox` cho chạy code bài Python bằng Python trên máy (cần cài Python 3); có Docker thì bỏ cờ này và xem mục 12.1.

**3. Mở app** (mở **cửa sổ PowerShell thứ hai**, cũng đứng ở `LabCast\source`):

```powershell
java -jar client/target/labcast-client.jar
```

App tự tìm thấy lớp trong máy/mạng LAN → chọn → đăng nhập.

**4. Tài khoản:**

| Vai trò | Tài khoản | Mật khẩu | Ghi chú |
| --- | --- | --- | --- |
| Quản trị | `ad` | `admin123` | tạo ở bước 2 (`--tao-admin`) |
| Giáo viên | `gv` | `gv123` | tạo ở bước 2 (`--tao-giao-vien`) |
| Học viên | tự đặt | mật khẩu tạm | đăng nhập `ad` → **Tài khoản** → *Thêm tài khoản* → chọn vai trò học viên, lớp **Lớp thực hành** (server tạo sẵn lớp này). Lần đầu đăng nhập học viên phải đổi mật khẩu. Cách khác: giáo viên nhập danh sách CSV ở **Quản lý sinh viên** |

Mật khẩu ở trên chỉ là ví dụ. Tài khoản chỉ được tạo **lần chạy đầu** (đã có thì bỏ qua); database nằm ở file `labcast.db` trong `source/` — xoá file đó là về trạng thái trắng.

**5. Thử nhiều máy (giáo viên chiếu cho học viên):** các máy cùng một Wi-Fi/LAN; máy khác chỉ cần chép `client/target/labcast-client.jar` (hoặc build như bước 1) rồi chạy bước 3. Lần đầu Windows hỏi tường lửa cho Java → **Allow**.

**Lỗi hay gặp**

| Hiện tượng | Cách xử lý |
| --- | --- |
| `java` không phải lệnh | Chưa cài JDK hoặc chưa mở lại PowerShell sau khi cài |
| Build lỗi `Unsupported class file major version` | JDK quá mới với một plugin: cài thêm JDK 17, đặt `$env:JAVA_HOME` trỏ tới nó rồi build lại |
| App không thấy lớp nào | Nhập IP máy server bằng tay; kiểm tra cùng mạng. Wi-Fi trường thường cách ly các máy — dùng Wi-Fi nhà hoặc hotspot điện thoại |
| Đăng nhập báo sai mật khẩu | Tài khoản đã tạo từ lần chạy trước với mật khẩu khác: xoá `labcast.db` rồi chạy lại bước 2 |
| `Address already in use` / cổng 7000 | Đang có server khác chạy: tắt cửa sổ server cũ |
| Học viên không thấy animation chiếu | Tường lửa chặn UDP 7001: cho Java qua tường lửa (cả mạng **Public** nếu Wi-Fi bị xếp là Public). Không cho thì sau vài giây server tự chuyển máy đó sang nhận qua TCP |
| Chữ tiếng Việt thành `?` trong tham số | Ghi cấu hình vào file UTF-8 và chạy `--cau-hinh lop.conf` (mục 12.1) |
| Chạy/nộp bài báo lỗi sandbox | Chưa bật Docker: thêm `--khong-sandbox` (bước 2) hoặc bật Docker Desktop |

Mọi tham số server: `java -jar server/target/labcast-server.jar --help`. Cấu hình lớp thật, nhiều lớp, Docker, dashboard: mục 12.

---

# 1. Project Information

## 1.1. Project Name

**LabCast — a networked classroom for visual DSA and SQL practice**

## 1.2. Topic

**Chủ đề 3 – Ứng dụng mạng** — trực quan hoá học liệu DSA và SQL cho buổi thực hành trong phòng máy

## 1.3. Group

| No. | Student ID | Full Name | Email | Main Responsibility | Contribution |
| --- | ---------- | --------- | ----- | ------------------- | -----------: |
| 1 | | | | Làm bài và trợ giúp · ĐG2 dòng code tin cậy | % |
| 2 | | | | Leader · kết nối, lớp học · ĐG4 câu hỏi nhanh công bằng | % |
| 3 | | | | Bài tập, chạy và chấm · ĐG3 đồng bộ đồng hồ | % |
| 4 | | | | Giảng và chiếu · ĐG1 chiếu animation qua multicast | % |
| | | | | **Total** | **100%** |

Cột Contribution điền theo thực tế, khớp với lịch sử Pull Request.

## 1.4. Instructor

*(điền tên giảng viên)*

---

# 2. Project Summary

## 2.1. Problem

Trong buổi thực hành ở phòng máy, sinh viên khó hình dung thuật toán và câu truy vấn; giáo viên không biết ai đang kẹt; phần mềm chiếu màn hình truyền video nên tốn băng thông và không tương tác được; máy phòng lab hay treo, khởi động lại làm mất code; luyện tập có điểm dễ bị học đối phó. Ba vấn đề sau là **bài toán mạng**: đồng bộ trạng thái thời gian thực giữa hàng chục máy, giữ dữ liệu khi kết nối và máy gặp sự cố, giữ công bằng khi mỗi máy có độ trễ khác nhau. Chi tiết: spec §2.

## 2.2. Objectives

1. Chiếu animation tới cả phòng: các màn hình lệch nhau ≤ 50 ms, băng thông thấp hơn chiếu màn hình video ít nhất 100 lần.
2. Không mất code khi rớt mạng, tắt app đột ngột hay đổi máy.
3. Giáo viên thấy code học viên đang gõ trong ≤ 200 ms khi cả lớp cùng gõ.
4. Câu hỏi nhanh công bằng giữa máy mạng tốt và mạng chậm.

Mục tiêu đo được đầy đủ: spec §3.

## 2.3. Scope

Một lớp trong phòng máy, mạng dây, tối đa 50 máy học viên. Hai môn: DSA (bài làm viết bằng Python) và SQL, mỗi môn khoảng 100 bài. Chi tiết: spec §4.

---

# 3. System Architecture

*(giai đoạn 5 — xuất `statics/architecture.png`)*

Server chạy trên máy giáo viên, kèm Docker để chạy code học viên an toàn. Một app desktop JavaFX, hai chế độ: giáo viên và học viên (spec §5).

| Kênh | Cổng | Dùng cho |
| --- | --- | --- |
| TCP — protocol tự thiết kế LCP/1.0 | 7000 | Phiên làm việc: đăng nhập, bài học, dòng code, chạy và nộp, đồng bộ giờ, trợ giúp |
| UDP multicast `239.255.70.1` | 7001 | Chiếu animation cho cả phòng, có lớp tin cậy và tự chuyển sang TCP |
| UDP broadcast | 7002 | App tự tìm server trong phòng máy |
| HTTP | 8080 | `/version` (máy nào cũng gọi được) và dashboard (chỉ mở trên chính máy server) |

Chưa có TLS: dữ liệu trên cổng 7000 đi dạng rõ trong LAN (mật khẩu lưu trên server bằng PBKDF2, không lưu bản rõ).

---

# 4. Network Communication Design

*(giai đoạn 1–3 — tóm tắt protocol, message format và sơ đồ tuần tự; thiết kế gốc ở spec §9–§12)*

---

# 5. Technology Stack

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ | Java 17 — `java.net`, `java.nio`, `java.util.concurrent`. Bài làm của học viên: Python 3.12 (có animation), C, C++, Java ([spec đa ngôn ngữ](docs/specs/labcast-da-ngon-ngu.md)) |
| Build | Maven (multi-module, Maven Wrapper `./mvnw`) |
| Giao diện | JavaFX 21, RichTextFX (ô soạn code) |
| Lưu trữ | SQLite qua `sqlite-jdbc` |
| SQL cho bài tập | SQLite sau giao diện `SqlEngine`, JSqlParser |
| Chạy code an toàn | Docker (`--network none`, giới hạn RAM/CPU/tiến trình); image runner Ubuntu 24.04 có Python 3.12, gcc/g++, gdb, JDK 17 |
| Test | JUnit 5, pytest |
| Định dạng code | Spotless + palantir-java-format |
| CI/CD | GitHub Actions → Docker image trên GHCR → VPS |
| Luật commit | husky + commitlint (Conventional Commits) |

---

# 6. Novelty and Contributions

Bốn đóng góp, mỗi cái có baseline và thí nghiệm đo được (spec §13):

| # | Cách làm thông thường | LabCast |
| --- | --- | --- |
| # | Cách làm thông thường | LabCast | Trạng thái |
| --- | --- | --- | --- |
| ĐG1 | Phần mềm phòng máy chiếu **hình ảnh** màn hình giáo viên tới từng máy | Phát **sự kiện** animation một lần qua multicast, mỗi máy tự vẽ theo giờ hẹn; gói thiếu xin lại bằng NACK (gộp NACK, phát lại cho nhóm), khung khoá 2 s cho máy vào muộn, tự chuyển sang TCP khi multicast bị chặn | Đã làm, TN2 |
| ĐG2 | Code nằm trên máy học viên, mất khi máy treo hay rớt mạng | Mỗi thay đổi gửi lên kèm số thứ tự, server xác nhận; client giữ phần chưa xác nhận và gửi lại khi nối lại; server áp đúng một lần, đúng thứ tự; đổi máy làm tiếp | Đã làm, TN3 (phần chưa xác nhận mới giữ trong RAM) |
| ĐG3 | Mỗi máy tin đồng hồ của chính nó | Đồng bộ kiểu NTP: 5 mẫu lúc vào lớp, giữ mẫu RTT nhỏ nhất; giờ ca thực hành và giờ hẹn chiếu tính theo đồng hồ server | Đã làm, TN1 (chưa đồng bộ lại định kỳ, chưa bù trôi) |
| ĐG4 | Hạn chót tính theo lúc gói tin đến, máy mạng chậm bị thiệt | Bù độ trễ theo RTT do server tự đo, có trần | Chưa làm (mới khai báo mã thông điệp) |

**Baseline so với LabCast (số đo thật, chi tiết mục 13–14):**

| | Baseline | LabCast |
| --- | --- | --- |
| ĐG1 băng thông chiếu | Chiếu video màn hình: ~1–2 Mbit/s mỗi máy (ước lượng, không đo) | **~1,3 kbit/s** cho cả lớp (đo, bài Dijkstra 156 bước) |
| ĐG1 độ lệch giữa 2 máy, mạng tốt | — | **≤ 2,5 ms** (cận trên, 35/35 lệnh) |
| ĐG2 rút dây 2–30 s giữa lúc gõ | Không gửi lại: mất toàn bộ phần gõ khi mất mạng | **38/40 lần không mất phím nào**, đồng bộ lại sau ~2–5 s |
| ĐG3 sai số giờ khi đồng hồ máy lệch 5 phút | 300 000 ms | **≤ 3 ms** (trễ mạng đối xứng tới 100 ms) |

---

# 7. Project Structure

```text
LabCast/
├── README.md, CONTRIBUTING.md, Instruction.md, Topics.md
├── docs/            spec, kế hoạch, phân công, hướng dẫn dựng VPS
├── report/          report.pdf (giai đoạn 5)
├── statics/         sơ đồ PNG, dataset/, results/ (CSV thí nghiệm)
├── deploy/          Dockerfile, compose.yaml (server trên VPS cho nhóm thử)
├── packaging/       bộ cài jpackage cho phòng máy
├── runner/          Python: chạy và chấm bài làm (Python, C, C++, Java), ghi trace, đóng thành image Docker
├── content/         bài học và bài tập (dsa/, sql/) — dữ liệu, không phải code
├── .github/         CI/CD, mẫu Pull Request
├── .husky/          hook kiểm commit
└── source/          Maven multi-module
    ├── common/      protocol LCP/1.0, định dạng trace, kênh truyền
    ├── server/      net, session, presence, discovery, classroom, quiz, cast, sync,
    │                lesson, practice, run, grade, sqlviz, journal, mirror + schema.sql
    ├── client/      app, net, clock, cast, player, views/*, editor, journal, mirror, …
    └── bench/       học viên ảo, cấy độ trễ, chạy thí nghiệm
```

Mỗi package có `package-info.java` mô tả nhiệm vụ và người phụ trách. `client` chỉ phụ thuộc `common`: app trên máy học viên không chứa đáp án hay code chấm điểm.

---

# 8. Requirements

* **JDK 17** trở lên
* **Docker** — máy chạy server cần có để chạy code học viên trong sandbox
* **Python 3.12** — chỉ cần khi làm phần `runner/` hoặc soạn nội dung
* **Node.js 22.12+** — chỉ để chạy hook kiểm commit (husky + commitlint)
* Không cần cài Maven (đã có `./mvnw`) và không cần cài database (SQLite là một file)

Máy học viên trong phòng máy **không cần cài gì**: bộ cài đã kèm Java và JavaFX (giai đoạn 3).

---

# 9. Installation

```bash
git clone https://github.com/lyhaiquan/ExamNet.git LabCast
cd LabCast/source
./mvnw -q package -DskipTests   # chỉ build ra jar; Windows: .\mvnw.cmd -q package -DskipTests
```

Người viết code cho dự án thêm hai bước:

```bash
cd ..  && npm install             # bật hook kiểm commit (cần Node.js)
cd source && ./mvnw verify       # build + toàn bộ test + kiểm định dạng (dùng JDK 17–21; JDK mới hơn có thể lỗi bước định dạng)
```

---

# 10. Configuration

| Biến môi trường | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `LABCAST_DB` | `labcast.db` | Đường dẫn file SQLite. Trong Docker là `/data/labcast.db`, nằm trên volume |
| `LABCAST_COMMIT` | `dev` | Mã commit hiện ở `/version`. CI tự gài vào image |

Các tham số còn lại (tên lớp, môn, cổng, card mạng LAN, thư mục `content/`, tài khoản tạo sẵn) đặt bằng dòng lệnh, file cấu hình hoặc biến môi trường — xem mục 12.1.

---

# 11. Database Setup

Lược đồ ở [`source/server/src/main/resources/schema.sql`](source/server/src/main/resources/schema.sql) — 28 bảng, mỗi nhóm bảng ghi rõ người phụ trách (spec §15). **Không cần cài hay khởi tạo gì:** server tự tạo file database ở lần chạy đầu và tự nâng lược đồ khi mở database của bản cũ (`PRAGMA user_version`).

---

# 12. Running the Project

## 12.1. Start Server

Đứng ở thư mục gốc repo, Docker Desktop đã bật:

```bash
docker build -t labcast-runner:py3.12 runner
cd source && ./mvnw -q package -DskipTests
java -jar server/target/labcast-server.jar --content ../content --tao-giao-vien "gv:matkhau:Giáo viên"
# trạng thái: http://localhost:8080/version
```

Image runner nặng khoảng 1,2 GB (có gcc/g++, gdb, JDK 17 cho bài làm C, C++, Java): lần build đầu mất vài phút.

**Mỗi lần thư mục `runner/` đổi thì build lại image runner** (lệnh `docker build` ở trên): image chứa các script chạy và chấm bài. Ví dụ nộp bài SQL cần `runner/sinh_csdl.py` để sinh database ẩn; image build trước khi có file này thì mọi lượt nộp bài SQL báo "Server chưa chấm được bài" (không tính lượt).

Server nghe TCP 7000 (app), UDP 7002 (tìm server), HTTP 8080 (`/version` và dashboard; đổi bằng `port-dashboard`), và phát chiếu lên nhóm multicast `239.255.70.1:7001` (UDP, TTL 1) trên card LAN.

**Tên có dấu tiếng Việt:** trên Windows, chữ như ớ, ư, ạ không đi qua được dòng lệnh (Java nhận thành `?`), nên `--lop "Lớp D22CQCN01"` bị server từ chối kèm hướng dẫn. Ghi tên lớp, họ tên giáo viên, đường dẫn có dấu vào một file UTF-8 (Notepad lưu UTF-8 mặc định).

**Mỗi lớp một môn, mỗi lớp một server.** DSA và SQL là hai lớp riêng: mỗi lớp một file cấu hình, khoá `mon` chọn môn (server chỉ nạp bài môn đó). Hai server chạy cùng một máy thì server thứ hai đổi `port`, `port-dashboard` và `db`:

```text
# lop-dsa.conf
lop = Lớp D22CQCN01 — Cấu trúc dữ liệu và giải thuật
mon = dsa
tao-giao-vien = gv:matkhau:Nguyễn Thị Lan
content = ../content
db = lop-dsa.db
```

```text
# lop-sql.conf
lop = Lớp D22CQCN01 — Cơ sở dữ liệu
mon = sql
tao-giao-vien = gv:matkhau:Nguyễn Thị Lan
content = ../content
db = lop-sql.db
port = 7010
port-dashboard = 8081
```

```bash
java -jar server/target/labcast-server.jar --cau-hinh lop-dsa.conf
```

```bash
java -jar server/target/labcast-server.jar --cau-hinh lop-sql.conf
```

App tìm thấy cả hai lớp trong danh sách lúc đăng nhập. Mỗi lớp có tài khoản, danh sách học viên và tiến độ riêng (database riêng): nhập danh sách lớp trên từng server. Hai lớp chiếu cùng lúc vẫn không lẫn: app chỉ nhận gói chiếu của server mình đăng nhập. Không ghi `mon` thì server nạp bài cả hai môn (chỉ nên dùng khi phát triển).

**Tài khoản quản trị:** thêm dòng `tao-admin = ad:matkhau:Họ tên` (hoặc `--tao-admin`) để tạo tài khoản ADMIN lúc khởi động. Quản trị đăng nhập cùng app, làm được mọi việc quản lý của giáo viên trên mọi lớp; nhập danh sách học viên thì chọn lớp trước. DB của bản cũ được nâng lược đồ tự động khi mở (`PRAGMA user_version`).

Hoặc dùng biến môi trường `LABCAST_LOP`, `LABCAST_MON`, `LABCAST_TAO_GIAO_VIEN`, `LABCAST_TAO_ADMIN`, `LABCAST_CONTENT`, `LABCAST_DB`, `LABCAST_LAN`. Thứ tự thắng: dòng lệnh > file cấu hình > biến môi trường > mặc định. Không cần `chcp 65001`: server tự chuyển cửa sổ cmd sang UTF-8 lúc chạy và trả lại bảng mã cũ khi tắt.

**Dashboard:** mở `http://localhost:8080/` trên chính máy chạy server. Trang tự cập nhật 2 giây một lần: học viên nào đang vào, mạng tốt hay chập chờn, ai đang rời app (Alt+Tab) và bao lâu, ai bị đánh dấu "dán khối lớn" (rê chuột lên cờ để xem bài nào, bao nhiêu ký tự; cờ mất khi học viên nộp đúng bài đó), lượt chạy code gần đây, nhật ký sự kiện (`audit_log`), log server. Máy khác trong LAN gọi vào thì bị trả 403, vì trang có tên và kết quả của học viên. Trên VPS server chạy trong Docker nên dashboard cũng trả 403; ở đó xem log bằng `docker compose logs`.

 Máy phát triển không có Docker: thêm `--khong-sandbox` (code học viên chạy bằng Python cục bộ, không cách ly). Bài viết bằng C/C++/Java ở chế độ này cần `gcc`/`g++`, `javac` có trên máy (Trace từng dòng C/C++ cần thêm `gdb`, Windows không có sẵn: dùng Docker). Mọi tham số: `java -jar server/target/labcast-server.jar --help`.

App: `java -jar client/target/labcast-client.jar`, chọn lớp tự tìm thấy, đăng nhập.

Chạy bằng Docker, đứng ở thư mục gốc repo, giống hệt bản trên VPS:

```bash
docker build -f deploy/Dockerfile -t labcast-server .
docker run --rm -p 8080:8080 labcast-server
```

## 12.2. Start Client

`java -jar client/target/labcast-client.jar`, chọn lớp tự tìm thấy (hoặc nhập IP server), đăng nhập.

**Chiếu animation cho cả lớp:** giáo viên mở một bài giảng giải, bấm **📡 Chiếu cho lớp** trên thanh tiêu đề bài. Từ đó các nút Phát, Dừng, tua, tốc độ, ⏭ của giáo viên điều khiển cả lớp: mọi máy ở cùng một bước, cùng lúc. Máy học viên đang không làm bài thì tự mở màn chiếu; đang làm bài thì có nút **📡 Giáo viên đang chiếu** trên thanh trên cùng. Học viên bấm bất kỳ nút nào của trình phát là tách ra tự xem; **⟲ Theo giáo viên** về lại đúng bước đang chiếu.

**Tường lửa:** app nhận chiếu qua UDP 7001. Lần đầu chạy, Windows có thể hiện hộp thoại tường lửa hỏi có cho Java nhận kết nối: chọn cho phép trên mạng **Private**. Không cho phép (hoặc switch chặn multicast) thì sau khoảng 6 giây server tự chuyển máy đó sang nhận chiếu qua TCP, chậm hơn một chút nhưng vẫn đúng bước. Dashboard (ô "Chiếu cho lớp") cho biết bao nhiêu máy đang phải nhận qua TCP.

## 12.3. Run Test

```bash
cd source && ./mvnw verify
python -m pytest runner
```

## 12.4. Bản đang chạy trên VPS

`http://<IP VPS>:8080/version` — tự cập nhật mỗi lần merge vào `main`. Bản trên VPS chỉ để nhóm thử phần TCP; multicast phải thử trong LAN thật. Xem [`docs/deploy/VPS-SETUP.md`](docs/deploy/VPS-SETUP.md).

---

# 13. Experimental Setup

Ba thí nghiệm, code ở `source/bench/src/main/java/labcast/bench/exp/`, số liệu và biểu đồ ở [`statics/results/`](statics/results/).

| TN | Chứng minh | Môi trường | Lặp |
| --- | --- | --- | --- |
| TN1 đồng bộ giờ (`Exp1DongBoGio`) | ĐG3 | 1 máy, server và client cùng JVM qua loopback; `DelayProxy` cấy trễ mỗi chiều 0/0, 25/25, 100/100, 20/180 ms; đồng hồ học viên giả lệch 0, +30 s, −60 s, +5 phút | 4 × 4 × 10 = 160 lần |
| TN2 chiếu multicast (`Exp2Chieu`, `Exp2Nghe`, `tn2/tn2.py`) | ĐG1 | **2 máy thật** qua Wi-Fi (hotspot điện thoại, RTT ≈ 5 ms); bài `dsa.do-thi.dijkstra` 156 bước, nén còn 1078 byte; 4 lượt: bình thường, bỏ 10 % gói, bỏ 30 % gói (giả lập phía học viên), chặn multicast 1 máy | 5 lần chiếu × 7 lệnh = 35 lệnh / lượt / máy |
| TN3 mất mạng (`Exp3MatMang`) | ĐG2 | 1 máy, loopback; học viên ảo gõ 20 phím/giây; `DelayProxy` rút dây 0/2/5/10/30 s; tắt app khi mất mạng / khi có mạng | 10 lần mỗi cấu hình |

Máy đo: Intel Core i5-12500H (16 luồng), 15,7 GB RAM, Windows 11, OpenJDK 17.0.19. Máy học viên thứ hai ở TN2: desktop Windows, JDK 26.

Chạy lại (đứng ở thư mục gốc repo, đã build ở mục 9):

```powershell
cd source; .\mvnw.cmd -q dependency:build-classpath -pl bench "-Dmdep.outputFile=../cp.txt"; cd ..
java -cp "source/bench/target/classes;$(Get-Content cp.txt)" labcast.bench.exp.Exp1DongBoGio 10
java -cp "source/bench/target/classes;$(Get-Content cp.txt)" labcast.bench.exp.Exp3MatMang 5
python statics/ve_bieu_do.py statics/results          # vẽ lại biểu đồ TN1, TN3
```

TN2 cần nhiều máy: hướng dẫn từng bước trong [`tn2/HUONG-DAN.txt`](tn2/HUONG-DAN.txt) (một máy `python tn2.py giao-vien`, mỗi máy còn lại `python tn2.py hoc-vien <IP>`).

---

# 14. Experimental Results

**TN1 — đồng bộ giờ** (sai số so với giờ thật của server, n = 10 mỗi ô)

| Trễ lên/xuống | Không đồng bộ | LabCast |
| --- | --- | --- |
| 0/0 ms | = độ lệch đồng hồ (tới 300 000 ms) | 0,0–0,2 ms, tối đa 1 ms |
| 25/25 ms | như trên | tối đa 1 ms |
| 100/100 ms | như trên | tối đa 3 ms |
| 20/180 ms (lệch hai chiều) | như trên | ≈ −80 ms, đúng bằng (lên − xuống)/2 theo lý thuyết |

Đo thật ở TN2: đồng hồ máy học viên nhanh hơn máy giáo viên **101,4 s**, ClockSync bù với RTT 5 ms.

**TN2 — chiếu multicast** (2 máy, 35 lệnh mỗi lượt mỗi máy)

| Lượt | Lệnh nhận | Lệnh tới sau giờ hẹn | Độ lệch giữa 2 máy (cận trên) p50 / p95 |
| --- | --- | --- | --- |
| A bình thường | 35/35 cả 2 máy | 0 | 2,5 / 2,5 ms |
| B bỏ 10 % gói | 35/35 | 3 + 3 | 2,5 ms / 1,8 s |
| C bỏ 30 % gói | 35/35 | 14 + 9 | 0,4 / 3,5 s |
| D chặn multicast máy 1 | 35/35 (máy 1 qua TCP) | 6 | 2,5 ms / 6,1 s |

Băng thông server phát: **~1,3 kbit/s** ở mọi lượt.

**TN3 — mất mạng giữa lúc gõ** (n = 10 mỗi cấu hình)

| Tình huống | Phím mất | Đồng bộ lại sau khi có mạng |
| --- | --- | --- |
| Rút dây 2 / 5 / 10 / 30 s | 0 ở 38/40 lần; 2 lần kẹt (5 s, 10 s) | trung vị 2,6 / 2,6 / 2,8 / 3,9 s |
| Tắt app khi đang mất mạng 2 / 5 / 10 s | 32 / 80 / 160 (toàn bộ phần gõ lúc mất mạng), 10/10 lần | — |
| Tắt app đột ngột khi có mạng | 0, 10/10 lần | — |

Code trên server luôn khớp từng ký tự với phần đầu tương ứng trên máy học viên.

Biểu đồ: `statics/results/tn1_*.png`, `tn2_*.png`, `tn3_*.png`.

---

# 15. Discussion

- **ĐG1:** gửi sự kiện thay vì hình ảnh giảm băng thông khoảng ba bậc độ lớn so với chiếu video (video là ước lượng). Khi mạng tốt các máy khớp nhau vài ms. Khi mất gói, lệnh **không mất** nhưng có thể tới muộn tới vài giây: gói điều khiển mất chỉ được phát hiện khi có gói sau hoặc khung khoá (2 s), và chuyển sang TCP cần vài heartbeat.
- **ĐG2:** số thứ tự + xác nhận + gửi lại giữ đủ code qua các lần rút dây; server không bao giờ áp sai thứ tự hay trùng. Phần chưa xác nhận chỉ nằm trong RAM nên tắt app lúc mất mạng thì mất đúng phần đó.
- **ĐG3:** sai số nhỏ hơn nhiều so với độ lệch đồng hồ thật giữa các máy (101 s đo được), đủ cho giờ ca thực hành và giờ hẹn chiếu; giới hạn là trễ không đối xứng.
- Quan sát thêm trên mạng thật: tường lửa Windows chặn UDP multicast đi vào khi mạng bị xếp loại *Public* → mọi lệnh đi qua TCP; khi Wi-Fi máy giáo viên chập, server không phát multicast lại được nữa (chỉ còn TCP).

---

# 16. Limitations

- Chưa có TLS; dữ liệu đi dạng rõ trong LAN.
- TN2 mới đo 2 máy qua hotspot, chưa đo phòng máy 50 máy mạng dây; mất gói ở lượt B, C là giả lập.
- Mất gói multicast làm lệnh tới muộn tới vài giây (p95 1,8–6 s), vượt mục tiêu 50 ms.
- Socket multicast của server không tự mở lại sau khi card mạng chập.
- Delta chưa xác nhận chỉ giữ trong RAM; 2/40 lần rút dây bị kẹt nối lại chưa rõ nguyên nhân.
- Đồng bộ giờ chỉ đo lúc vào lớp, chưa đồng bộ lại định kỳ, chưa bù trôi.
- Câu hỏi nhanh công bằng (ĐG4) và phản chiếu code cho giáo viên mới có mã thông điệp, chưa làm.

---

# 17. Future Work

- Gửi lặp mỗi lệnh điều khiển 2–3 lần và rút chu kỳ khung khoá để giảm thời gian phục hồi khi mất gói.
- Tự mở lại socket multicast khi card mạng đổi; đo TN2 trên phòng máy thật nhiều máy.
- Ghi nhật ký delta xuống đĩa phía client; tìm nguyên nhân 2 lần kẹt nối lại.
- Đồng bộ giờ định kỳ, bù trôi; làm ĐG4; thêm TLS.

---

# 18. References

1. Nguyễn Mạnh Hùng, Nguyễn Trọng Khánh. *Lập trình mạng*. Giáo trình học phần.
2. RFC 5905 — Network Time Protocol Version 4 (ước lượng độ lệch và độ trễ, ĐG3).
3. RFC 6298 — Computing TCP's Retransmission Timer (công thức SRTT/RTTVAR, heartbeat và ĐG4).
4. RFC 1112 — Host Extensions for IP Multicasting.
5. Conventional Commits 1.0.0 — https://www.conventionalcommits.org

---

# 19. Đóng góp vào dự án

Quy trình nhánh, commit, Pull Request: [`CONTRIBUTING.md`](CONTRIBUTING.md).
