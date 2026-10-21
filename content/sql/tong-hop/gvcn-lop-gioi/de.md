# Chủ nhiệm lớp học giỏi hơn toàn trường

Liệt kê **mã, họ tên giảng viên** và **tên lớp** mà giảng viên đó chủ nhiệm, với điều kiện **điểm trung bình của
lớp cao hơn điểm trung bình toàn trường**.

- Chỉ xét **điểm lần thi 1**, bỏ lần vắng thi (`diem` NULL).
- Trung bình của lớp: trung bình mọi điểm lần 1 của các sinh viên trong lớp.
- Trung bình toàn trường: trung bình **mọi** điểm lần 1 trong `ket_qua`, **kể cả** sinh viên chưa xếp lớp.
- Lớp không có điểm nào hoặc chưa có chủ nhiệm thì không xét. Giảng viên chủ nhiệm nhiều lớp đạt thì in mỗi lớp
  một dòng.

Đừng chép số trung bình nhìn thấy vào câu: dữ liệu ẩn khác.

Kết quả có 3 cột: id, ho_ten, ten_lop. Thứ tự dòng không quan trọng.

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

`sinh_vien(id, ho_ten, gioi_tinh, ngay_sinh, que_quan, lop_id)` — `lop_id` NULL: chưa xếp lớp; `que_quan` có thể NULL

| id | ho_ten | gioi_tinh | ngay_sinh | que_quan | lop_id |
| --- | --- | --- | --- | --- | --- |
| 1 | Nguyễn Văn An | Nam | 2005-03-14 | Hà Nội | 1 |
| 2 | Trần Thị Bình | Nữ | 2004-11-02 | Nam Định | 1 |
| 3 | Lê Minh Cường | Nam | 2005-07-21 | Hà Nội | 1 |
| 4 | Phạm Thu Dung | Nữ | 2004-05-30 | Hà Nội | 2 |
| 5 | Hoàng Văn Em | Nam | 2005-01-09 | Hải Phòng | 2 |
| 6 | Vũ Thị Giang | Nữ | 2005-12-25 | Hà Nội | 2 |
| 7 | Đặng Quốc Huy | Nam | 2004-09-17 | Thanh Hoá | 3 |
| 8 | Bùi Ngọc Lan | Nữ | 2005-04-03 | Hà Nội | 3 |
| 9 | Đỗ Văn Minh | Nam | 2005-08-08 | Nghệ An | NULL |
| 10 | Ngô Thị Ngọc | Nữ | 2004-02-28 | Hà Nội | NULL |
| 11 | Lý Thị Mơ | Nữ | 2005-06-06 | Huế | 1 |

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
| 11 | 1 | 1 | 8.0 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
