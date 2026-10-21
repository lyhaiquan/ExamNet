# Quê đông sinh viên nhất trong từng khoa

Khoa của sinh viên là khoa của **lớp** sinh viên đó (`lop.khoa`). Với **mỗi khoa**, tìm **quê quán có nhiều sinh
viên nhất**. Liệt kê **khoa, quê quán, số sinh viên**. **Bằng nhau thì in tất cả.**

Sinh viên chưa xếp lớp hoặc chưa ghi quê (`que_quan` NULL) không tính. Khoa không có sinh viên nào thì không có
trong kết quả.

Gợi ý: gộp nhóm theo (khoa, quê), rồi trong `HAVING` so số sinh viên với số lớn nhất **của chính khoa đó**.

Kết quả có 3 cột: khoa, que_quan, so_sv. Thứ tự dòng không quan trọng.

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

`lop(id, ten_lop, khoa, gvcn_id)` — `gvcn_id` NULL: chưa có chủ nhiệm

| id | ten_lop | khoa | gvcn_id |
| --- | --- | --- | --- |
| 1 | D22CQCN01 | CNTT | 1 |
| 2 | D22CQCN02 | CNTT | 2 |
| 3 | D22CQAT01 | ATTT | NULL |
| 4 | D22CQKT01 | Kinh tế | 3 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
