-- Dữ liệu học viên nhìn thấy: nhỏ (≤ 12 dòng mỗi bảng) để animation xem kịp từng dòng.
-- Bẫy cố ý: nhân viên 1 là giám đốc (quan_ly_id NULL); nhân viên 9 chưa xếp phòng (phong_id NULL);
-- phòng 4 chưa có nhân viên; dự án 4 chưa giao phòng nào; nhân viên 3, 4 cùng lương.

INSERT INTO phong_ban VALUES
    (1, 'Kỹ thuật', 500000000),
    (2, 'Kinh doanh', 300000000),
    (3, 'Nhân sự', 150000000),
    (4, 'Pháp chế', 100000000);

INSERT INTO nhan_vien VALUES
    (1, 'Nguyễn Văn Hùng', 1, NULL, 40000000, '2015-03-01'),
    (2, 'Trần Thị Mai', 1, 1, 25000000, '2018-06-15'),
    (3, 'Lê Văn Nam', 1, 2, 18000000, '2020-01-10'),
    (4, 'Phạm Thị Oanh', 1, 2, 18000000, '2021-09-01'),
    (5, 'Hoàng Minh Phúc', 2, 1, 22000000, '2017-04-20'),
    (6, 'Vũ Thị Quỳnh', 2, 5, 15000000, '2022-02-14'),
    (7, 'Đặng Văn Sơn', 2, 5, 14000000, '2023-07-03'),
    (8, 'Bùi Thị Thảo', 3, 1, 16000000, '2019-11-11'),
    (9, 'Đỗ Văn Tuấn', NULL, 1, 12000000, '2024-05-06'),
    (10, 'Ngô Thị Uyên', 3, 8, 11000000, '2024-08-19');

INSERT INTO du_an VALUES
    (1, 'Hệ thống CRM', 1, 800000000),
    (2, 'Ứng dụng bán hàng', 1, 600000000),
    (3, 'Chiến dịch Tết', 2, 200000000),
    (4, 'Chuyển đổi số', NULL, 900000000);

INSERT INTO phan_cong VALUES
    (2, 1, 120),
    (3, 1, 160),
    (4, 2, 140),
    (3, 2, 40),
    (5, 3, 100),
    (6, 3, 80),
    (7, 3, 60),
    (2, 4, 30),
    (1, 4, 20),
    (8, 4, 50);

-- Thêm riêng cho bài này
INSERT INTO phan_cong VALUES
    (8, 1, 30),
    (5, 2, 20);
