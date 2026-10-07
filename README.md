# LabCast — Phòng thực hành DSA và SQL có animation

> **NETWORK PROGRAMMING – FINAL PROJECT** · Năm học 2026–2027

Hệ thống client/server cho buổi thực hành trong phòng máy: giáo viên giảng bằng animation phát đồng bộ tới mọi máy, học viên luyện tập và xem code của chính mình chạy thành animation, giáo viên theo dõi cả lớp và trợ giúp trực tiếp. Viết bằng **Java 17**, phần mạng chỉ dùng thư viện chuẩn của Java.

- Thiết kế: [`docs/specs/labcast-design.md`](docs/specs/labcast-design.md)
- Kế hoạch triển khai: [`docs/specs/labcast-ke-hoach.md`](docs/specs/labcast-ke-hoach.md)
- Phân công: [`docs/PHAN-CONG.md`](docs/PHAN-CONG.md)

**Trạng thái:** giai đoạn 0 — đã có khung dự án, CI/CD và luật commit; đề tài đang chờ giảng viên xác nhận. Code hệ thống do nhóm viết từ giai đoạn 1.

Các mục ghi *(giai đoạn N)* sẽ được điền khi làm tới giai đoạn đó.

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
| TCP — protocol tự thiết kế LCP/1.0 | 7000 | Phiên làm việc: đăng nhập, bài học, dòng code, chạy và nộp, câu hỏi nhanh, trợ giúp |
| UDP multicast `239.255.70.1` | 7001 | Chiếu animation cho cả phòng, có lớp tin cậy và tự chuyển sang TCP |
| UDP broadcast | 7002 | App tự tìm server trong phòng máy |
| TLS (tùy chọn) | bọc 7000 | Mã hoá phiên làm việc |

Ngoài ra cổng **8080** (`/version`) chỉ dùng cho vận hành trên VPS: bước deploy gọi vào để xác nhận đúng commit.

---

# 4. Network Communication Design

*(giai đoạn 1–3 — tóm tắt protocol, message format và sơ đồ tuần tự; thiết kế gốc ở spec §9–§12)*

---

# 5. Technology Stack

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ | Java 17 — `java.net`, `javax.net.ssl`, `java.util.concurrent`; Python 3.12 cho bài làm của học viên |
| Build | Maven (multi-module, Maven Wrapper `./mvnw`) |
| Giao diện | JavaFX 21, RichTextFX (ô soạn code) |
| Lưu trữ | SQLite qua `sqlite-jdbc` |
| SQL cho bài tập | SQLite sau giao diện `SqlEngine`, JSqlParser |
| Chạy code an toàn | Docker (`--network none`, giới hạn RAM/CPU/tiến trình) |
| Test | JUnit 5, pytest |
| Định dạng code | Spotless + palantir-java-format |
| CI/CD | GitHub Actions → Docker image trên GHCR → VPS |
| Luật commit | husky + commitlint (Conventional Commits) |

---

# 6. Novelty and Contributions

Bốn đóng góp, mỗi cái có baseline và thí nghiệm đo được (spec §13):

| # | Cách làm thông thường | LabCast |
| --- | --- | --- |
| ĐG1 | Phần mềm phòng máy chiếu **hình ảnh** màn hình giáo viên tới từng máy | Phát **sự kiện** animation một lần qua multicast, mỗi máy tự vẽ, phát hẹn giờ; máy thiếu gói xin lại qua TCP |
| ĐG2 | Code nằm trên máy học viên, mất khi máy treo; giáo viên phải đi tới tận máy | Mỗi thay đổi được ghi trước rồi gửi lên kèm số thứ tự; đổi máy làm tiếp; giáo viên xem code đang gõ |
| ĐG3 | Mỗi máy tin đồng hồ của chính nó | Đồng bộ kiểu NTP, giữ mẫu RTT nhỏ nhất, bù trôi, chỉnh dần |
| ĐG4 | Hạn chót tính theo lúc gói tin đến, máy mạng chậm bị thiệt | Bù độ trễ theo RTT do server tự đo, có trần |

*(giai đoạn 4 — bảng so sánh số liệu Baseline vs Proposed)*

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
├── runner/          Python: chạy bài làm và ghi trace, đóng thành image Docker
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
cd LabCast
npm install          # bật hook kiểm commit
cd source
./mvnw verify        # build + test + kiểm định dạng; Windows: mvnw.cmd verify
```

---

# 10. Configuration

| Biến môi trường | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `LABCAST_DB` | *(giai đoạn 1)* | Đường dẫn file SQLite. Trong Docker là `/data/labcast.db`, nằm trên volume |
| `LABCAST_COMMIT` | `dev` | Mã commit hiện ở `/version`. CI tự gài vào image |

*(giai đoạn 1 trở đi — cổng, card mạng LAN, TLS, đường dẫn `content/`…)*

---

# 11. Database Setup

Lược đồ ở [`source/server/src/main/resources/schema.sql`](source/server/src/main/resources/schema.sql) — 15 bảng, mỗi nhóm bảng ghi rõ người phụ trách (spec §15). Server tự tạo database ở lần chạy đầu *(giai đoạn 1)*.

---

# 12. Running the Project

## 12.1. Start Server

Giai đoạn 0 mới có server mẫu, chỉ mở cổng trạng thái:

```bash
cd source && ./mvnw -q package -DskipTests
java -jar server/target/labcast-server.jar
# mở http://localhost:8080/version
```

Chạy bằng Docker, đứng ở thư mục gốc repo, giống hệt bản trên VPS:

```bash
docker build -f deploy/Dockerfile -t labcast-server .
docker run --rm -p 8080:8080 labcast-server
```

## 12.2. Start Client

*(giai đoạn 1)*

## 12.3. Run Test

```bash
cd source && ./mvnw verify
python -m pytest runner
```

## 12.4. Bản đang chạy trên VPS

`http://<IP VPS>:8080/version` — tự cập nhật mỗi lần merge vào `main`. Bản trên VPS chỉ để nhóm thử phần TCP; multicast phải thử trong LAN thật. Xem [`docs/deploy/VPS-SETUP.md`](docs/deploy/VPS-SETUP.md).

---

# 13. Experimental Setup

Định nghĩa đầy đủ ở spec §14. Kết quả ghi vào `statics/results/exp<số>_<tên>.csv`.

| # | Thí nghiệm | Chứng minh | Người |
| - | ---------- | ---------- | ----- |
| 1 | Băng thông chiếu: multicast sự kiện, TCP từng máy, video màn hình | ★ĐG1 | 4 |
| 2 | Sai số đồng hồ và độ lệch giữa các màn hình | ★ĐG3 | 3 |
| 3 | Mất gói multicast, NACK, máy vào lớp muộn | ★ĐG1 | 4 |
| 4 | Rút dây, tắt app, đổi máy giữa lúc gõ | ★ĐG2 | 1 |
| 5 | Độ trễ phản chiếu code, có và không gộp thay đổi | ★ĐG2 | 1 |
| 6 | Một máy spam 1000 thông điệp/giây | Giới hạn tốc độ | 3 |
| 7 | 40 lượt chạy thử dồn trong 10 giây | Hàng đợi chạy | 3 |
| 8 | Câu hỏi nhanh với nhóm máy chậm thêm 200 ms | ★ĐG4 | 2 |
| 9 | Heartbeat thích nghi so với cố định *(tuỳ chọn)* | Phát hiện máy rớt | 2 |
| 10 | Chi phí TLS *(tuỳ chọn)* | Cái giá của mã hoá | 2 |

---

# 14. Experimental Results

*(giai đoạn 4 — chỉ số đo thật, mỗi cấu hình lặp nhiều lần, báo cáo trung bình ± độ lệch chuẩn)*

---

# 15. Discussion

*(giai đoạn 4)*

---

# 16. Limitations

Xem spec §19. *(giai đoạn 5 — cập nhật theo hệ thống thật)*

---

# 17. Future Work

*(giai đoạn 5)*

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
