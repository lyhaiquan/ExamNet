# runner — chạy bài làm Python và ghi trace

Chủ: **Người 3**. Thiết kế ở spec §7.3, §7.5, §8.1, §8.2.

Thư mục này đóng thành image Docker `labcast-runner:py3.12`. Server (`labcast.server.run`) mỗi lượt chạy tạo một container dùng một lần, gửi công việc vào stdin dạng JSON, đọc trace và kết quả từ stdout dạng JSON từng dòng.

| File (sẽ có) | Việc |
| --- | --- |
| `run.py` | Điểm vào: đọc công việc, chạy code học viên với input, in trace và kết quả |
| `tracer.py` | `sys.settrace`: sự kiện `line`, `vars`, `call`, `ret`; chụp biến khai báo trong `khung_nhin` |
| `viz/` | Cấu trúc "gắn camera" `Mang`, `Bang`; `viz.say`, `viz.key` |
| `kiem_cam.py` | Kiểm hàm cấm và import cấm bằng `ast` |
| `tests/` | pytest |

Chỉ dùng thư viện chuẩn của Python trong image. `pytest` chỉ để test, cài trên máy mình:

```bash
python -m pip install -r runner/requirements-dev.txt
python -m pytest runner
docker build -t labcast-runner:py3.12 runner
```
