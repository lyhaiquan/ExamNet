# Sinh viên đã thi cả hai môn

In **mã sinh viên** của các sinh viên **đã có điểm** (điểm khác NULL, lần thi nào cũng được) ở **cả hai
môn**: Lập trình mạng (`mon_hoc_id = 1`) và Cơ sở dữ liệu (`mon_hoc_id = 2`). Mỗi sinh viên một dòng.

Câu này dạy `INTERSECT`: câu thứ nhất ra tập sinh viên có điểm môn 1, câu thứ hai ra tập có điểm môn 2;
`INTERSECT` giữ những dòng có ở **cả hai** tập, và tự bỏ dòng trùng (sinh viên thi lại có hai dòng điểm).

Kết quả có 1 cột: sinh_vien_id. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

`ket_qua(sinh_vien_id, mon_hoc_id, lan_thi, diem)` — `diem` NULL: vắng thi

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

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
