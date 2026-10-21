# Dự án có nhiều nhân viên từ phòng khác nhất

Một nhân viên được phân công vào dự án là "từ phòng khác" nếu phòng của nhân viên khác phòng phụ trách dự án.
Tìm **dự án có nhiều nhân viên từ phòng khác nhất**; bằng nhau thì **in tất cả**. Chỉ xét dự án đã giao phòng và
nhân viên đã xếp phòng.

Kết quả: mã dự án, tên dự án, số nhân viên từ phòng khác.

Kết quả có 3 cột: id, ten, so_nv. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

`du_an(id, ten, phong_id, ngan_sach)` — `phong_id` NULL: dự án chung, chưa giao phòng nào

| id | ten | phong_id | ngan_sach |
| --- | --- | --- | --- |
| 1 | Hệ thống CRM | 1 | 800000000 |
| 2 | Ứng dụng bán hàng | 1 | 600000000 |
| 3 | Chiến dịch Tết | 2 | 200000000 |
| 4 | Chuyển đổi số | NULL | 900000000 |

`phan_cong(nhan_vien_id, du_an_id, so_gio)`

| nhan_vien_id | du_an_id | so_gio |
| --- | --- | --- |
| 2 | 1 | 120 |
| 3 | 1 | 160 |
| 4 | 2 | 140 |
| 3 | 2 | 40 |
| 5 | 3 | 100 |
| 6 | 3 | 80 |
| 7 | 3 | 60 |
| 2 | 4 | 30 |
| 1 | 4 | 20 |
| 8 | 4 | 50 |
| 8 | 1 | 40 |
| 5 | 2 | 30 |
| 6 | 1 | 20 |
| 2 | 3 | 10 |
| 8 | 3 | 15 |

`nhan_vien(id, ho_ten, phong_id, quan_ly_id, luong, ngay_vao_lam)` — `phong_id` NULL: chưa xếp phòng; `quan_ly_id` NULL: không có người quản lý

| id | ho_ten | phong_id | quan_ly_id | luong | ngay_vao_lam |
| --- | --- | --- | --- | --- | --- |
| 1 | Nguyễn Văn Hùng | 1 | NULL | 40000000 | 2015-03-01 |
| 2 | Trần Thị Mai | 1 | 1 | 25000000 | 2018-06-15 |
| 3 | Lê Văn Nam | 1 | 2 | 18000000 | 2020-01-10 |
| 4 | Phạm Thị Oanh | 1 | 2 | 18000000 | 2021-09-01 |
| 5 | Hoàng Minh Phúc | 2 | 1 | 22000000 | 2017-04-20 |
| 6 | Vũ Thị Quỳnh | 2 | 5 | 15000000 | 2022-02-14 |
| 7 | Đặng Văn Sơn | 2 | 5 | 14000000 | 2023-07-03 |
| 8 | Bùi Thị Thảo | 3 | 1 | 16000000 | 2019-11-11 |
| 9 | Đỗ Văn Tuấn | NULL | 1 | 12000000 | 2024-05-06 |
| 10 | Ngô Thị Uyên | 3 | 8 | 11000000 | 2024-08-19 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
