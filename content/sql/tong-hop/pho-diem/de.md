# Phổ điểm theo khoảng

Đếm **số lần thi** rơi vào từng khoảng điểm: `[0, 4)`, `[4, 6.5)`, `[6.5, 8)`, `[8, 10]` (ngoặc vuông: lấy đầu mút,
ngoặc tròn: không lấy). Mỗi dòng kết quả gồm **nhãn khoảng** (viết đúng như trên, ví dụ `[4, 6.5)`) và **số lần
thi**.

Tính mọi lần thi (cả lần 1 và lần 2), bỏ lần vắng thi (`diem` NULL). Chỉ liệt kê khoảng có ít nhất một lần thi.

Gợi ý: `CASE WHEN ... THEN '[0, 4)' ... END` cho nhãn, rồi `GROUP BY` theo nhãn đó. Cẩn thận điểm đúng bằng 6.5
và 8.

Kết quả có 2 cột: khoang, so_lan. Thứ tự dòng không quan trọng.

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
| 1 | 2 | 1 | 9.0 |
| 2 | 2 | 1 | 3.5 |
| 2 | 2 | 2 | 6.5 |
| 3 | 2 | 1 | 8.0 |
| 4 | 2 | 1 | 7.0 |
| 5 | 2 | 1 | 2.0 |
| 5 | 2 | 2 | 3.0 |
| 6 | 2 | 1 | NULL |
| 8 | 2 | 1 | 9.0 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
