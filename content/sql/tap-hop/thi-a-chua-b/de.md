# Đã thi Lập trình mạng nhưng chưa thi Cơ sở dữ liệu

Liệt kê **mã sinh viên** của những người **đã thi** môn **Lập trình mạng** (`mon_hoc_id = 1`) nhưng **chưa thi**
môn **Cơ sở dữ liệu** (`mon_hoc_id = 2`). "Đã thi" một môn là có ít nhất một lần thi môn đó **có điểm** (vắng
thi, `diem` NULL, không tính).

`A EXCEPT B` giữ các dòng của A **không có** trong B (và bỏ trùng). Đổi chỗ A và B là ra câu trả lời khác hẳn.

Kết quả có 1 cột: sinh_vien_id. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

`ket_qua(sinh_vien_id, mon_hoc_id, lan_thi, diem)` — `diem` NULL: vắng thi; `lan_thi` 1 là thi lần đầu, 2 là thi lại

| sinh_vien_id | mon_hoc_id | lan_thi | diem |
| --- | --- | --- | --- |
| 1 | 1 | 1 | 8.5 |
| 2 | 1 | 1 | 6.0 |
| 3 | 1 | 1 | 7.0 |
| 4 | 1 | 1 | 9.0 |
| 5 | 1 | 1 | 7.5 |
| 6 | 1 | 1 | 8.0 |
| 7 | 1 | 1 | 5.5 |
| 8 | 1 | 1 | 6.5 |
| 9 | 1 | 1 | 9.5 |
| 2 | 1 | 2 | 7.5 |
| 7 | 1 | 2 | 6.0 |
| 10 | 1 | 1 | NULL |
| 1 | 2 | 1 | 7.5 |
| 3 | 2 | 1 | 5.0 |
| 5 | 2 | 1 | 8.0 |
| 6 | 2 | 1 | NULL |
| 8 | 2 | 1 | 6.5 |

`mon_hoc(id, ma_mon, ten_mon, so_tin_chi)`

| id | ma_mon | ten_mon | so_tin_chi |
| --- | --- | --- | --- |
| 1 | INT1433 | Lập trình mạng | 3 |
| 2 | INT1313 | Cơ sở dữ liệu | 3 |
| 3 | BAS1203 | Giải tích 1 | 3 |
| 4 | INT1155 | Tin học cơ sở 2 | 2 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
