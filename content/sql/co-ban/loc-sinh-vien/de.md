# Lọc sinh viên theo quê và năm sinh

Liệt kê **mã, họ tên, ngày sinh** của các sinh viên **quê Hà Nội** và **sinh từ năm 2005 trở đi**.

Câu này dạy `WHERE`: mỗi dòng của bảng được xét điều kiện, dòng không thoả bị loại. Điều kiện ghép bằng `AND`
thì dòng phải thoả **cả hai** vế. Ngày lưu dạng chuỗi `YYYY-MM-DD` nên so sánh chuỗi cũng đúng thứ tự ngày.

Kết quả có 3 cột: id, ho_ten, ngay_sinh. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

`sinh_vien(id, ho_ten, gioi_tinh, ngay_sinh, que_quan, lop_id)` — `lop_id` NULL: chưa xếp lớp

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

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
