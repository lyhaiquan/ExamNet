# ExamNet — Hệ thống thi qua mạng

> **NETWORK PROGRAMMING – FINAL PROJECT** · Năm học 2026–2027

Hệ thống thi trắc nghiệm trên máy theo mô hình Client/Server, viết bằng **Java 17 socket thuần, không dùng framework mạng**. Thiết kế đầy đủ ở [`docs/specs/examnet-design.md`](docs/specs/examnet-design.md).

**Trạng thái:** phase 0 — đã có khung dự án, CI/CD và luật commit. Code hệ thống do nhóm viết từ phase 1 (spec §16).

Các mục ghi *(phase N)* sẽ được điền khi làm tới phase đó.

---

# 1. Project Information

## 1.1. Project Name

**ExamNet — A Resilient Client/Server Examination System**

## 1.2. Topic

**Chủ đề 3 – Ứng dụng mạng**, định hướng 4.6 *Hệ thống thi qua mạng*

## 1.3. Group

| No. | Student ID | Full Name | Email | Main Responsibility | Contribution |
| --- | ---------- | --------- | ----- | ------------------- | -----------: |
| 1 | | | | ĐG1 ghi đáp án + khôi phục · protocol EXP/1.0, codec | % |
| 2 | | | | ĐG3 heartbeat thích nghi · server core, thread pool, backpressure | % |
| 3 | | | | ĐG2 đồng hồ server · tùy chọn socket, TLS, DB, chấm điểm | % |
| 4 | | | | ĐG4 multicast tin cậy · WebSocket dashboard, bench harness | % |
| | | | | **Total** | **100%** |

Chi tiết phân công: [`docs/PHAN-CONG.md`](docs/PHAN-CONG.md). Cột Contribution điền theo thực tế, khớp với lịch sử Pull Request.

## 1.4. Instructor

*(điền tên giảng viên)*

---

# 2. Project Summary

## 2.1. Problem

Các kỳ thi trên máy tính dựa trên ba giả định mà thực tế đều sai: mạng phòng máy luôn ổn định, máy thí sinh không bao giờ treo, và đồng hồ máy thí sinh đáng tin. Hệ quả là bài làm bị mất và kỳ thi mất công bằng. Đây là vấn đề về **độ tin cậy của truyền thông mạng**, không phải vấn đề giao diện. Chi tiết: spec §1.

## 2.2. Objectives

1. **Không mất bài làm** khi máy thi mất kết nối hoặc bị tắt giữa giờ.
2. **Thời gian thi do server quyết định**, không tin đồng hồ máy thi.
3. **Phát hiện máy thi mất kết nối** nhanh, tốn ít băng thông hơn heartbeat chu kỳ cố định.
4. **Phục vụ hàng trăm máy thi đồng thời**, chịu được đỉnh tải lúc cả phòng cùng nộp bài.

## 2.3. Scope

Xem spec §3.

---

# 3. System Architecture

*(phase 10 — xuất `statics/architecture.png`)*

Bốn transport, mỗi cái dùng đúng chỗ nó mạnh (spec §4):

| Transport | Cổng | Dùng cho |
| --- | --- | --- |
| TCP — protocol tự thiết kế EXP/1.0 | 5000 | Máy thi và Admin: đăng nhập, nhận đề, gửi đáp án, heartbeat |
| WebSocket (tự viết theo RFC 6455) | 5001 | Dashboard giám thị trên trình duyệt |
| UDP multicast `239.255.42.1` | 5002 | Thông báo toàn phòng, có lớp tin cậy và tự lùi về TCP |
| TLS (tùy chọn) | bọc 5000 và 5001 | Mã hoá đường truyền |

Ngoài ra cổng **8080** (`/version`) chỉ dùng cho vận hành: bước deploy gọi vào để xác nhận đúng commit.

---

# 4. Network Communication Design

*(phase 1–2 — tóm tắt protocol, message format và sơ đồ tuần tự; thiết kế gốc ở spec §7–§9)*

---

# 5. Technology Stack

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ | Java 17 — `java.net`, `java.nio`, `javax.net.ssl`, `java.util.concurrent` |
| Build | Maven (multi-module, dùng Maven Wrapper `./mvnw`) |
| Lưu trữ | SQLite qua `sqlite-jdbc` |
| Giao diện | Swing (máy thi, Admin), HTML + JavaScript (dashboard giám thị) |
| Test | JUnit 5 |
| Định dạng code | Spotless + palantir-java-format |
| CI/CD | GitHub Actions → Docker image trên GHCR → VPS |
| Luật commit | husky + commitlint (Conventional Commits) |

---

# 6. Novelty and Contributions

Bốn đóng góp, mỗi cái có baseline và thí nghiệm đo được (spec §11):

| # | Cách làm thông thường | ExamNet |
| --- | --- | --- |
| ĐG1 | Bài làm giữ trong RAM, cuối giờ mới nộp | Ghi từng đáp án xuống đĩa trước khi gửi, gửi bù khi kết nối lại |
| ĐG2 | Máy thi tự đếm giờ bằng đồng hồ của nó | Server giữ giờ, máy thi đồng bộ kiểu NTP |
| ĐG3 | Heartbeat chu kỳ cố định hoặc TCP keepalive (mặc định 2 giờ) | Chu kỳ heartbeat co giãn theo RTT đo được |
| ĐG4 | Gửi thông báo cho N máy bằng N lần unicast | Một gói UDP multicast, máy sót nhận bù qua TCP |

*(phase 9 — bảng so sánh số liệu Baseline vs Proposed)*

---

# 7. Project Structure

```text
ExamNet/
├── README.md, CONTRIBUTING.md, Instruction.md, Topics.md
├── docs/            spec, phân công, hướng dẫn dựng VPS
├── report/          report.pdf (phase 10)
├── statics/         sơ đồ PNG, dataset/, results/ (CSV thí nghiệm)
├── deploy/          Dockerfile, compose.yaml
├── .github/         CI/CD, mẫu Pull Request
├── .husky/          hook kiểm commit
└── source/          Maven multi-module
    ├── common/      protocol EXP/1.0, codec, model, tls
    ├── server/      net, session, app, clock, liveness, journal, notice, ws, ops
    ├── service/     dao, db, grade, auth, model + schema.sql
    ├── client/      kết nối, wal, clock, notice, ui, admin
    └── bench/       harness đo hiệu năng
```

Mỗi package có `package-info.java` mô tả nhiệm vụ và người phụ trách. `client` chỉ phụ thuộc `common`: máy thi không chứa code database hay code chấm điểm.

---

# 8. Requirements

* **JDK 17** trở lên
* **Node.js 22.12+** — chỉ để chạy hook kiểm commit (husky + commitlint)
* Docker — tuỳ chọn, để chạy giống hệt bản trên VPS
* Không cần cài Maven (đã có `./mvnw`) và không cần cài database (SQLite là một file)

---

# 9. Installation

```bash
git clone https://github.com/<chủ-repo>/examnet.git
cd examnet
npm install          # bật hook kiểm commit
cd source
./mvnw verify        # build + test + kiểm định dạng; Windows: mvnw.cmd verify
```

---

# 10. Configuration

| Biến môi trường | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `EXAMNET_DB` | *(phase 2)* | Đường dẫn file SQLite. Trong Docker là `/data/examnet.db`, nằm trên volume |
| `EXAMNET_COMMIT` | `dev` | Mã commit hiện ở `/version`. CI tự gài vào image |

*(phase 2 trở đi — cổng, TLS, chế độ heartbeat…)*

---

# 11. Database Setup

Lược đồ ở [`source/service/src/main/resources/schema.sql`](source/service/src/main/resources/schema.sql) — 9 bảng, thiết kế theo bốn bước của giáo trình (spec §13).

*(phase 2 — server tự tạo database và nạp dữ liệu mẫu ở lần chạy đầu)*

---

# 12. Running the Project

## 12.1. Start Server

Phase 0 mới có server mẫu, chỉ mở cổng trạng thái:

```bash
cd source && ./mvnw -q package -DskipTests
java -jar server/target/examnet-server.jar
# mở http://localhost:8080/version
```

Chạy bằng Docker, đứng ở thư mục gốc repo, giống hệt bản trên VPS:

```bash
docker build -f deploy/Dockerfile -t examnet-server .
docker run --rm -p 8080:8080 examnet-server
```

## 12.2. Start Client

*(phase 2)*

## 12.3. Run Test

```bash
cd source && ./mvnw verify
```

## 12.4. Bản đang chạy trên VPS

`http://<IP VPS>:8080/version` — tự cập nhật mỗi lần merge vào `main`. Xem [`docs/deploy/VPS-SETUP.md`](docs/deploy/VPS-SETUP.md).

---

# 13. Experimental Setup

11 thí nghiệm, định nghĩa ở spec §12. Kết quả ghi vào `statics/results/exp<số>_<tên>.csv`.

| # | Thí nghiệm | Chứng minh | Người |
| - | ---------- | ---------- | ----- |
| 1 | Rút dây mạng giữa lúc gửi đáp án | ★ĐG1 — không mất bài | 1 |
| 2 | Chỉnh lệch đồng hồ máy thi ±5 phút | ★ĐG2 — server làm chủ giờ | 3 |
| 3 | Heartbeat thích nghi so với cố định | ★ĐG3 — phát hiện nhanh hơn, tốn ít hơn | 2 |
| 4 | Hàng trăm máy cùng nộp bài | Chịu tải | 2 |
| 5 | NIO so với thread-per-connection | Chọn mô hình vào/ra | 2 |
| 6 | EXP/1.0 so với JSON | Giá trị của protocol nhị phân | 1 |
| 7 | Bật/tắt `TCP_NODELAY` | Tác động của Nagle | 3 |
| 8 | Một máy đọc chậm giữa nhiều máy thường | Backpressure cô lập được | 2 |
| 9 | Multicast so với unicast (LAN thật) | ★ĐG4 — tiết kiệm băng thông | 4 |
| 10 | Một frame lớn bị TCP chia mấy mảnh | TCP là dòng byte | 1 |
| 11 | TLS so với TCP trần | Cái giá của mã hoá | 3 |

---

# 14. Experimental Results

*(phase 9 — chỉ số đo thật, mỗi cấu hình lặp nhiều lần, báo cáo trung bình ± độ lệch chuẩn)*

---

# 15. Discussion

*(phase 9)*

---

# 16. Limitations

Xem spec §17. *(phase 10 — cập nhật theo hệ thống thật)*

---

# 17. Future Work

*(phase 10)*

---

# 18. References

1. Nguyễn Mạnh Hùng, Nguyễn Trọng Khánh. *Lập trình mạng*. Giáo trình học phần.
2. RFC 6455 — The WebSocket Protocol.
3. RFC 5905 — Network Time Protocol Version 4 (ước lượng offset và delay, ĐG2).
4. RFC 6298 — Computing TCP's Retransmission Timer (công thức SRTT/RTTVAR, ĐG3).
5. Conventional Commits 1.0.0 — https://www.conventionalcommits.org

---

# 19. Đóng góp vào dự án

Quy trình nhánh, commit, Pull Request: [`CONTRIBUTING.md`](CONTRIBUTING.md).
