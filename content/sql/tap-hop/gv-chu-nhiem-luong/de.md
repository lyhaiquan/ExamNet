# Giảng viên vừa chủ nhiệm vừa lương trên 20 triệu

Liệt kê **mã và họ tên** các giảng viên **vừa** đang chủ nhiệm ít nhất một lớp **vừa** có lương **trên
20 000 000**. Dùng `INTERSECT`.

Kết quả có 2 cột: id, ho_ten. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

`giang_vien(id, ho_ten, khoa, luong)`

| id | ho_ten | khoa | luong |
| --- | --- | --- | --- |
| 1 | Nguyễn Thị Lan | CNTT | 18000000 |
| 2 | Trần Quang Hải | CNTT | 21000000 |
| 3 | Lê Thu Hà | Kinh tế | 16500000 |
| 4 | Phạm Đức Long | ATTT | 19500000 |

`lop(id, ten_lop, khoa, gvcn_id)` — `gvcn_id` NULL: chưa có chủ nhiệm

| id | ten_lop | khoa | gvcn_id |
| --- | --- | --- | --- |
| 1 | D22CQCN01 | CNTT | 1 |
| 2 | D22CQCN02 | CNTT | 2 |
| 3 | D22CQAT01 | ATTT | NULL |
| 4 | D22CQKT01 | Kinh tế | 3 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
