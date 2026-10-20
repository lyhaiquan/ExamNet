# Nhân viên lương cao hơn trung bình

Liệt kê **họ tên** và **lương** của các nhân viên có lương **cao hơn lương trung bình** của cả công ty.

Câu này dạy **truy vấn con trả về một giá trị**: `(SELECT AVG(luong) FROM nhan_vien)` chạy trước, ra đúng một số,
rồi `WHERE luong > (…)` so từng dòng với số đó. Không viết được `WHERE luong > AVG(luong)`: hàm gộp không dùng
được trong `WHERE` (WHERE xét từng dòng, chưa có nhóm nào để gộp).

Kết quả có 2 cột: ho_ten, luong. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

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
