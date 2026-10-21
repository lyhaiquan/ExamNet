# Môn có tỉ lệ trượt lần 1 cao nhất

Tìm môn có **tỉ lệ trượt ở lần thi 1 cao nhất**. Liệt kê **mã môn, tên môn** và **tỉ lệ trượt** tính theo phần
trăm, làm tròn 2 chữ số. **Bằng nhau thì in tất cả.**

- Chỉ xét **lần thi 1** có điểm; lần vắng thi (`diem` NULL) không tính vào cả tử lẫn mẫu.
- **Trượt** là điểm **dưới 5**.
- Tỉ lệ trượt = số lần trượt × 100 / số lần thi có điểm. Môn chưa có lần thi 1 nào có điểm thì không xét.

Nhớ nhân `100.0` (số thực) trước khi chia, nếu không SQLite chia nguyên ra 0.

Kết quả có 3 cột: ma_mon, ten_mon, ty_le_truot. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

`mon_hoc(id, ma_mon, ten_mon, so_tin_chi)`

| id | ma_mon | ten_mon | so_tin_chi |
| --- | --- | --- | --- |
| 1 | INT1433 | Lập trình mạng | 3 |
| 2 | INT1313 | Cơ sở dữ liệu | 3 |
| 3 | BAS1203 | Giải tích 1 | 3 |
| 4 | INT1155 | Tin học cơ sở 2 | 2 |

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
| 1 | 2 | 1 | 9.0 |
| 2 | 2 | 1 | 3.5 |
| 2 | 2 | 2 | 6.5 |
| 3 | 2 | 1 | 8.0 |
| 4 | 2 | 1 | 7.0 |
| 5 | 2 | 1 | 2.0 |
| 5 | 2 | 2 | 3.0 |
| 6 | 2 | 1 | NULL |
| 8 | 2 | 1 | 9.0 |
| 1 | 3 | 1 | 4.0 |
| 3 | 3 | 1 | 8.0 |
| 4 | 3 | 1 | 6.0 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
