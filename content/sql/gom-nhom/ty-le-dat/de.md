# Tỉ lệ lần thi đạt của từng môn

Với từng môn, tính **tỉ lệ phần trăm** lần thi **đạt** (điểm từ 4 trở lên) trên tổng số lần thi **có điểm**, làm
tròn 2 chữ số thập phân. Lần vắng thi (`diem` NULL) không tính vào cả tử lẫn mẫu. Chỉ liệt kê môn có ít nhất một
lần thi có điểm.

Chia hai số nguyên trong SQLite ra số nguyên (`1 / 3 = 0`): nhân với `100.0` trước khi chia. Làm tròn bằng
`ROUND(x, 2)`.

Kết quả có 2 cột: mon_hoc_id, ty_le. Thứ tự dòng không quan trọng.

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
| 1 | 2 | 1 | 3.5 |
| 2 | 2 | 1 | 7.0 |
| 3 | 2 | 1 | 2.0 |
| 3 | 2 | 2 | 6.0 |
| 4 | 2 | 1 | 8.0 |
| 5 | 2 | 1 | 4.0 |
| 6 | 2 | 1 | NULL |
| 1 | 3 | 1 | 3.0 |
| 2 | 3 | 1 | 9.0 |
| 4 | 3 | 1 | 1.5 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
