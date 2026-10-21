-- Dữ liệu học viên nhìn thấy: nhỏ (≤ 12 dòng mỗi bảng) để animation xem kịp từng dòng.
-- Bẫy cố ý: khách 4, 7 chưa ghi thành phố; khách 4, 7, 8 chưa đặt đơn nào; đơn 5 của khách vãng lai
-- (khach_hang_id NULL); ba sản phẩm cùng giá 16 000 000 (5, 6, 9) ở ranh giới top 5; Hà Nội, Hồ Chí Minh lặp lại.

INSERT INTO khach_hang VALUES
    (1, 'Nguyễn Văn An', 'Hà Nội', '2024-01-15'),
    (2, 'Trần Thị Bình', 'Hồ Chí Minh', '2024-02-20'),
    (3, 'Lê Minh Cường', 'Hà Nội', '2024-03-05'),
    (4, 'Phạm Thu Dung', NULL, '2024-03-18'),
    (5, 'Hoàng Văn Em', 'Đà Nẵng', '2024-04-02'),
    (6, 'Vũ Thị Giang', 'Hồ Chí Minh', '2024-05-11'),
    (7, 'Đặng Quốc Huy', NULL, '2024-06-25'),
    (8, 'Bùi Ngọc Lan', 'Hà Nội', '2024-07-30');

INSERT INTO danh_muc VALUES
    (1, 'Điện tử', NULL),
    (2, 'Điện thoại', 1),
    (3, 'Máy tính xách tay', 1),
    (4, 'Sách', NULL),
    (5, 'Sách lập trình', 4);

INSERT INTO san_pham VALUES
    (1, 'iPhone 15', 2, 22000000, 5),
    (2, 'Galaxy S24', 2, 20000000, 8),
    (3, 'Redmi Note 13', 2, 5000000, 20),
    (4, 'MacBook Air M3', 3, 28000000, 3),
    (5, 'ThinkPad E14', 3, 16000000, 6),
    (6, 'Dell Inspiron 15', 3, 16000000, 4),
    (7, 'Python cơ bản', 5, 150000, 50),
    (8, 'Cấu trúc dữ liệu và giải thuật', 5, 180000, 30),
    (9, 'Màn hình LG 27 inch', 1, 16000000, 7),
    (10, 'Sạc dự phòng', 1, 600000, 0);

INSERT INTO don_hang VALUES
    (1, 1, '2024-08-01', 'da-giao'),
    (2, 2, '2024-08-03', 'da-giao'),
    (3, 1, '2024-08-10', 'huy'),
    (4, 3, '2024-08-15', 'dang-giao'),
    (5, NULL, '2024-08-20', 'da-giao'),
    (6, 5, '2024-09-01', 'moi'),
    (7, 2, '2024-09-05', 'da-giao'),
    (8, 6, '2024-09-12', 'huy'),
    (9, 1, '2024-09-20', 'da-giao'),
    (10, 3, '2024-10-01', 'moi');

INSERT INTO chi_tiet_don VALUES
    (1, 1, 1, 22000000),
    (1, 9, 1, 16000000),
    (2, 7, 2, 150000),
    (2, 8, 1, 180000),
    (3, 4, 1, 28000000),
    (4, 3, 2, 5000000),
    (5, 10, 3, 600000),
    (6, 2, 1, 20000000),
    (7, 5, 1, 16000000),
    (8, 6, 1, 16000000),
    (9, 8, 2, 180000),
    (10, 7, 1, 150000);

-- Thêm riêng cho bài này
INSERT INTO khach_hang VALUES
    (9, 'Trần Nguyễn Hà', 'Huế', '2024-08-05'),
    (10, 'Nguyễn Thị Hồng', 'Hải Phòng', '2024-08-12');
