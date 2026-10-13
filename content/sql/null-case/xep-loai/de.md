# Xếp loại điểm bằng CASE

Với mỗi kết quả **lần thi 1**, in **mã sinh viên, mã môn, điểm** và **xếp loại**:

| Điểm | Xếp loại |
| --- | --- |
| từ 8 | `Giỏi` |
| từ 6,5 tới dưới 8 | `Khá` |
| từ 5 tới dưới 6,5 | `Trung bình` |
| dưới 5 | `Yếu` |
| NULL (vắng thi) | `Chưa thi` |

Câu này dạy `CASE`: xét **từng dòng**, các nhánh `WHEN` được thử từ trên xuống, nhánh **đầu tiên** đúng quyết định
kết quả. So sánh với NULL (`diem >= 8` khi `diem` là NULL) ra *không biết*, không phải *đúng*, nên dòng vắng thi
trượt qua mọi nhánh so sánh: phải bắt riêng bằng `WHEN diem IS NULL`.

Kết quả có 4 cột: sinh_vien_id, mon_hoc_id, diem, xep_loai. Thứ tự dòng không quan trọng.

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

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
