# Phân công 4 người

Nguyên tắc: **mỗi người giữ đúng một đóng góp ★** (Novelty chiếm 15% điểm), cộng một mảng mạng nền và các thí nghiệm đi kèm. Như vậy ai cũng có phần network programming để trình bày khi bảo vệ.

Điền tên và tài khoản GitHub vào bảng dưới, rồi chép sang README §1.3.

| Người | Họ tên | GitHub | Đóng góp ★ | Mạng nền | Thí nghiệm |
| --- | --- | --- | --- | --- | --- |
| 1 | | | ĐG1 — ghi đáp án xuống đĩa + khôi phục | Protocol EXP/1.0, frame, codec | 1, 6, 10 |
| 2 | | | ĐG3 — heartbeat tự điều chỉnh | Server core: accept, thread pool, backpressure | 3, 4, 5, 8 |
| 3 | | | ĐG2 — đồng hồ do server quyết định | Tùy chọn socket, TLS, DB, kiểm quyền, chấm điểm | 2, 7, 11 |
| 4 | | | ĐG4 — multicast có NACK qua TCP | WebSocket dashboard, bench harness | 9 |

---

## Người 1 — dữ liệu đi trên dây và không bao giờ mất

| | |
| --- | --- |
| Package | `common.protocol`, `server.journal`, `client` (kết nối, tự kết nối lại), `client.wal` |
| Thông điệp | `HELLO`/`HELLO_ACK`, `ANSWER_DELTA`/`ANSWER_ACK`, `RESUME_REQ`/`RESUME_STATE`, `ERROR`, `BYE` |
| Giao diện | Màn làm bài: hiện câu hỏi, chọn đáp án, trạng thái "đã lưu" |
| Thí nghiệm | 1 (rút dây giữa lúc gửi đáp án), 6 (EXP/1.0 so với JSON), 10 (TCP cắt frame thành mấy mảnh) |
| Phải giải thích được | Vì sao cần LEN và MAGIC; vì sao `read()` phải đọc lặp; vì sao kiểm LEN trước khi cấp bộ nhớ; ghi đĩa *trước* khi gửi; `seq` + upsert có điều kiện cho ra "at-least-once trên dây, exactly-once về trạng thái" |

## Người 2 — server chịu được nhiều máy cùng lúc

| | |
| --- | --- |
| Package | `server.net`, `server.session`, `server.app` (điều phối), `server.liveness` |
| Thông điệp | `AUTH`/`AUTH_OK`/`AUTH_FAIL` (cấp token, phiên), `HEARTBEAT`/`HEARTBEAT_ACK` |
| Giao diện | Băng trạng thái kết nối trên máy thi (Online / Đang kết nối lại) |
| Thí nghiệm | 3 (heartbeat thích nghi so với cố định), 4 (100+ máy cùng nộp), 5 (NIO so với thread-per-connection), 8 (một máy đọc chậm) |
| Phải giải thích được | Vòng `accept()` và thread pool hoạt động ra sao; vì sao chỉ một luồng được ghi ra mỗi socket; hàng đợi gửi đầy thì xử lý thế nào (backpressure); công thức SRTT/RTTVAR/RTO; máy trạng thái ALIVE → SUSPECT → DISCONNECTED |

## Người 3 — thời gian, bảo mật và dữ liệu

| | |
| --- | --- |
| Package | `server.clock`, `client.clock`, `common.tls`, `common.model`, toàn bộ `service`, `client.admin` |
| Thông điệp | `TIME_SYNC_REQ`/`TIME_SYNC_RESP`, `EXAM_FETCH`/`EXAM_DATA`, `SUBMIT_REQ`/`SUBMIT_ACK`, `FORCE_SUBMIT`, các lệnh `ADMIN_*` |
| Giao diện | Màn đăng nhập, đồng hồ đếm ngược, màn Admin |
| Thí nghiệm | 2 (chỉnh lệch giờ máy thi ±5 phút), 7 (bật/tắt `TCP_NODELAY`), 11 (chi phí TLS) |
| Phải giải thích được | Công thức offset/delay kiểu NTP và vì sao giữ mẫu RTT nhỏ nhất; Nagle là gì; TLS bắt tay và truststore; 9 bảng trong `schema.sql` và vì sao cột `is_correct` không bao giờ rời server; PBKDF2 + salt; hai yêu cầu ở spec §16 "Hai yêu cầu dễ bị sót" |

## Người 4 — một tới nhiều

| | |
| --- | --- |
| Package | `server.notice`, `client.notice`, `server.ws`, `bench` |
| Thông điệp | `NOTICE`, `NOTICE_NACK`, `PROCTOR_SUBSCRIBE`/`PROCTOR_STATE`/`PROCTOR_NOTICE_SEND` |
| Giao diện | Dashboard giám thị chạy trên trình duyệt |
| Thí nghiệm | 9 (multicast so với unicast — phải chạy trong LAN thật, không chạy trên VPS) |
| Phải giải thích được | Địa chỉ nhóm 239.x và TTL = 1; vì sao NACK đi qua TCP chứ không qua multicast; thông báo CRITICAL luôn kèm TCP; tự lùi về TCP khi không join được nhóm; bắt tay WebSocket (`Sec-WebSocket-Accept`); vì sao phải lặp mỗi thí nghiệm nhiều lần |

Người 4 chỉ có 1 thí nghiệm nhưng giữ **bench harness** — công cụ mà cả 11 thí nghiệm đều chạy qua — nên khối lượng vẫn cân. Mỗi người tự viết kịch bản đo cho thí nghiệm của mình trên harness đó.

---

## Cả nhóm phải nắm

Giảng viên có thể hỏi **bất kỳ ai về bất kỳ phần nào** (`Instruction.md` §17):

- Kiến trúc tổng thể và vì sao có bốn transport (TCP, UDP multicast, WebSocket, TLS) — spec §4.
- Khung EXP/1.0 13 byte và luồng giao tiếp từ `HELLO` tới `FORCE_SUBMIT` — spec §8.
- Đường đi của một đáp án: bấm chọn → ghi đĩa → `ANSWER_DELTA` → ghi DB → `ANSWER_ACK`.
- Kết quả thí nghiệm của nhóm có ý nghĩa gì, đo như thế nào, hạn chế ở đâu.

## Chứng minh đóng góp

- Mỗi người làm trên nhánh riêng và mở PR **bằng tài khoản GitHub của chính mình**. Lịch sử PR là bằng chứng ai làm gì.
- Cột "Contribution %" ở README §1.3 điền theo thực tế, khớp với lịch sử PR.
