-- CSDL dùng chung "truong-hoc" (docs/plans/danh-muc-bai.md). Bài nào dùng thì CHÉP file này vào thư mục bài:
-- container chuẩn bị bài chỉ được gắn đúng thư mục bài.
-- Cột được để NULL: sinh_vien.lop_id (chưa xếp lớp), lop.gvcn_id (chưa có chủ nhiệm), ket_qua.diem (vắng thi).
-- Ngày lưu dạng chuỗi YYYY-MM-DD (SQLite không có kiểu ngày riêng).

CREATE TABLE giang_vien (
    id      INTEGER PRIMARY KEY,
    ho_ten  TEXT NOT NULL,
    khoa    TEXT NOT NULL,
    luong   INTEGER NOT NULL
);

CREATE TABLE lop (
    id       INTEGER PRIMARY KEY,
    ten_lop  TEXT NOT NULL,
    khoa     TEXT NOT NULL,
    gvcn_id  INTEGER REFERENCES giang_vien (id)
);

CREATE TABLE sinh_vien (
    id         INTEGER PRIMARY KEY,
    ho_ten     TEXT NOT NULL,
    gioi_tinh  TEXT NOT NULL,
    ngay_sinh  TEXT NOT NULL,
    que_quan   TEXT,
    lop_id     INTEGER REFERENCES lop (id)
);

CREATE TABLE mon_hoc (
    id          INTEGER PRIMARY KEY,
    ma_mon      TEXT NOT NULL,
    ten_mon     TEXT NOT NULL,
    so_tin_chi  INTEGER NOT NULL
);

CREATE TABLE ket_qua (
    sinh_vien_id  INTEGER NOT NULL REFERENCES sinh_vien (id),
    mon_hoc_id    INTEGER NOT NULL REFERENCES mon_hoc (id),
    lan_thi       INTEGER NOT NULL,
    diem          REAL
);

-- Bảng riêng của bài này
CREATE TABLE hoc_bong (
    sinh_vien_id  INTEGER NOT NULL REFERENCES sinh_vien (id),
    ho_ten        TEXT NOT NULL,
    diem          REAL NOT NULL
);
