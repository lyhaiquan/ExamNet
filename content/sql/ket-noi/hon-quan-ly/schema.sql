-- CSDL dùng chung "nhan-su" (docs/plans/danh-muc-bai.md). Bài nào dùng thì CHÉP file này vào thư mục bài:
-- container chuẩn bị bài chỉ được gắn đúng thư mục bài.
-- Cột được để NULL: nhan_vien.phong_id (chưa xếp phòng), nhan_vien.quan_ly_id (không có quản lý: giám đốc),
-- du_an.phong_id (dự án chung, chưa giao phòng nào). Ngày lưu dạng chuỗi YYYY-MM-DD.

CREATE TABLE phong_ban (
    id         INTEGER PRIMARY KEY,
    ten        TEXT NOT NULL,
    ngan_sach  INTEGER NOT NULL
);

CREATE TABLE nhan_vien (
    id            INTEGER PRIMARY KEY,
    ho_ten        TEXT NOT NULL,
    phong_id      INTEGER REFERENCES phong_ban (id),
    quan_ly_id    INTEGER REFERENCES nhan_vien (id),
    luong         INTEGER NOT NULL,
    ngay_vao_lam  TEXT NOT NULL
);

CREATE TABLE du_an (
    id         INTEGER PRIMARY KEY,
    ten        TEXT NOT NULL,
    phong_id   INTEGER REFERENCES phong_ban (id),
    ngan_sach  INTEGER NOT NULL
);

CREATE TABLE phan_cong (
    nhan_vien_id  INTEGER NOT NULL REFERENCES nhan_vien (id),
    du_an_id      INTEGER NOT NULL REFERENCES du_an (id),
    so_gio        INTEGER NOT NULL
);
