# Mọi cặp lớp và môn để lập lịch thi

Để lập lịch thi, liệt kê **mọi cặp (tên lớp, tên môn)**: lớp nào cũng thi mọi môn.

`CROSS JOIN` ghép **mỗi** dòng bảng này với **mỗi** dòng bảng kia, không cần điều kiện: 4 lớp × 4 môn = 16 dòng.

Kết quả có 2 cột: ten_lop, ten_mon. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

`lop(id, ten_lop, khoa, gvcn_id)` — `gvcn_id` NULL: chưa có chủ nhiệm

| id | ten_lop | khoa | gvcn_id |
| --- | --- | --- | --- |
| 1 | D22CQCN01 | CNTT | 1 |
| 2 | D22CQCN02 | CNTT | 2 |
| 3 | D22CQAT01 | ATTT | NULL |
| 4 | D22CQKT01 | Kinh tế | 3 |

`mon_hoc(id, ma_mon, ten_mon, so_tin_chi)`

| id | ma_mon | ten_mon | so_tin_chi |
| --- | --- | --- | --- |
| 1 | INT1433 | Lập trình mạng | 3 |
| 2 | INT1313 | Cơ sở dữ liệu | 3 |
| 3 | BAS1203 | Giải tích 1 | 3 |
| 4 | INT1155 | Tin học cơ sở 2 | 2 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
