-- Dữ liệu học viên nhìn thấy: nhỏ (≤ 12 dòng mỗi bảng) để animation xem kịp từng dòng.
-- Bẫy cố ý: lớp 4 chưa có sinh viên; sinh viên 9, 10 chưa xếp lớp; lớp 3 chưa có chủ nhiệm;
-- sinh viên 2, 7 thi lại (lan_thi = 2); sinh viên 10 vắng thi (diem NULL).

INSERT INTO giang_vien VALUES
    (1, 'Nguyễn Thị Lan', 'CNTT', 18000000),
    (2, 'Trần Quang Hải', 'CNTT', 21000000),
    (3, 'Lê Thu Hà', 'Kinh tế', 16500000),
    (4, 'Phạm Đức Long', 'ATTT', 19500000);

INSERT INTO lop VALUES
    (1, 'D22CQCN01', 'CNTT', 1),
    (2, 'D22CQCN02', 'CNTT', 2),
    (3, 'D22CQAT01', 'ATTT', NULL),
    (4, 'D22CQKT01', 'Kinh tế', 3);

INSERT INTO sinh_vien VALUES
    (1, 'Nguyễn Văn An', 'Nam', '2005-03-14', 'Hà Nội', 1),
    (2, 'Trần Thị Bình', 'Nữ', '2004-11-02', 'Nam Định', 1),
    (3, 'Lê Minh Cường', 'Nam', '2005-07-21', 'Hà Nội', 1),
    (4, 'Phạm Thu Dung', 'Nữ', '2004-05-30', 'Hà Nội', 2),
    (5, 'Hoàng Văn Em', 'Nam', '2005-01-09', 'Hải Phòng', 2),
    (6, 'Vũ Thị Giang', 'Nữ', '2005-12-25', 'Hà Nội', 2),
    (7, 'Đặng Quốc Huy', 'Nam', '2004-09-17', 'Thanh Hoá', 3),
    (8, 'Bùi Ngọc Lan', 'Nữ', '2005-04-03', 'Hà Nội', 3),
    (9, 'Đỗ Văn Minh', 'Nam', '2005-08-08', 'Nghệ An', NULL),
    (10, 'Ngô Thị Ngọc', 'Nữ', '2004-02-28', 'Hà Nội', NULL);

INSERT INTO mon_hoc VALUES
    (1, 'INT1433', 'Lập trình mạng', 3),
    (2, 'INT1313', 'Cơ sở dữ liệu', 3),
    (3, 'BAS1203', 'Giải tích 1', 3),
    (4, 'INT1155', 'Tin học cơ sở 2', 2);

INSERT INTO ket_qua VALUES
    (1, 1, 1, 8.5),
    (2, 1, 1, 6.0),
    (3, 1, 1, 7.0),
    (4, 1, 1, 9.0),
    (5, 1, 1, 7.5),
    (6, 1, 1, 8.0),
    (7, 1, 1, 5.5),
    (8, 1, 1, 6.5),
    (9, 1, 1, 9.5),
    (2, 1, 2, 7.5),
    (7, 1, 2, 6.0),
    (10, 1, 1, NULL);

-- Thêm riêng cho bài này
INSERT INTO ket_qua VALUES
    (1, 2, 1, 9.0),
    (2, 2, 1, 3.5),
    (2, 2, 2, 6.5),
    (3, 2, 1, 8.0),
    (4, 2, 1, 7.0),
    (5, 2, 1, 2.0),
    (5, 2, 2, 3.0),
    (6, 2, 1, NULL),
    (8, 2, 1, 9.0);

INSERT INTO ket_qua VALUES
    (2, 2, 1, 3.5);
