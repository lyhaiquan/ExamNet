# Tổng giờ mỗi dự án, từ 100 giờ

Tính **tổng số giờ** được phân công cho mỗi dự án, chỉ giữ dự án có tổng **từ 100 giờ trở lên**.

Kết quả có 2 cột: du_an_id, tong_gio. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

`phan_cong(nhan_vien_id, du_an_id, so_gio)`

| nhan_vien_id | du_an_id | so_gio |
| --- | --- | --- |
| 2 | 1 | 120 |
| 3 | 1 | 160 |
| 4 | 2 | 140 |
| 3 | 2 | 40 |
| 5 | 3 | 100 |
| 6 | 3 | 80 |
| 7 | 3 | 60 |
| 2 | 4 | 30 |
| 1 | 4 | 20 |
| 8 | 4 | 50 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
