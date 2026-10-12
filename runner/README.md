# runner — chạy bài làm Python và ghi trace

Chủ: **Người 3**. Thiết kế ở spec §7.3, §7.5, §8.1, §8.2.

Thư mục này đóng thành image Docker `labcast-runner:py3.12`. Server (`labcast.server.run`) mỗi lượt chạy tạo một container dùng một lần, gửi công việc vào stdin dạng JSON, đọc trace và kết quả từ stdout dạng JSON từng dòng.

| File | Việc | Container |
| --- | --- | --- |
| `run.py` | Điểm vào mặc định của image: chạy thử code học viên với một input, in trace và kết quả | học viên |
| `cham.py` | Chấm bài nộp: `kiem_cam.py` rồi chạy từng test ở một tiến trình con mới (`chay_mot.py`), mỗi dòng kết quả kèm khoá bí mật của lượt chấm | học viên |
| `chay_mot.py` | Chạy một test, stdout của bài làm ra nguyên văn; `viz.Mang` thành list thường cho nhanh | học viên |
| `kiem_cam.py` | Kiểm hàm cấm, import cấm, cú pháp cấm bằng `ast`, không chạy code | học viên |
| `chuan_bi.py` | Sinh test ẩn `an-1…an-N` và test `tb` của một bài từ `sinh_test.py` + `loi_giai.py` | **tin cậy**, gắn thư mục bài chỉ đọc |
| `kiem_luat.py` | Chạy `luat.py` của bài trên trace code học viên | **tin cậy**, gắn thư mục bài chỉ đọc |
| `tracer.py` (sẽ có) | `sys.settrace`: sự kiện `line`, `vars`, `call`, `ret`; chụp biến khai báo trong `khung_nhin` | học viên |
| `viz/` | Cấu trúc "gắn camera" `Mang`; `viz.say`, `viz.key` | |
| `tests/` | pytest | |

Container **học viên** không bao giờ được gắn thư mục bài: code học viên chỉ nhận code và input qua stdin, nên không đọc được `loi_giai.py` hay output của test ẩn. Đúng/sai do server so, ngoài container.

Chỉ dùng thư viện chuẩn của Python trong image. `pytest` chỉ để test, cài trên máy mình:

```bash
python -m pip install -r runner/requirements-dev.txt
python -m pytest runner
docker build -t labcast-runner:py3.12 runner
```
