# Lần thi trên trung bình của môn

Liệt kê các lần thi (**mã sinh viên, mã môn, lần thi, điểm**) có điểm **cao hơn điểm trung bình của chính môn đó**
(trung bình mọi lần thi của môn, `AVG` tự bỏ qua điểm NULL).

So với trung bình **cả bảng** là sai: mỗi môn có mức điểm riêng. Câu con phải tương quan theo `mon_hoc_id`.

Kết quả có 4 cột: sinh_vien_id, mon_hoc_id, lan_thi, diem. Thứ tự dòng không quan trọng.

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
| 1 | 2 | 1 | 5.0 |
| 3 | 2 | 1 | 6.5 |
| 4 | 2 | 1 | 4.0 |
| 5 | 2 | 1 | 7.0 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
