# Trượt lần 1, đạt lần 2

Liệt kê **mã, họ tên sinh viên** và **tên môn** của những lần sinh viên **trượt ở lần thi 1** (điểm dưới 5)
nhưng **đạt ở lần thi 2** (điểm từ 5 trở lên) **của cùng môn đó**. Mỗi cặp (sinh viên, môn) chỉ in một lần.

Gợi ý: nối `ket_qua` với chính nó, một bản cho lần 1, một bản cho lần 2, khớp cùng sinh viên và cùng môn. Dữ liệu ẩn
(và cả dữ liệu mẫu: lần 1 môn 2 của sinh viên 2) có lần thi bị ghi trùng hai lần: dùng `DISTINCT`.

Kết quả có 3 cột: id, ho_ten, ten_mon. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

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
| 2 | 2 | 1 | 3.5 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
