-- CSDL dùng chung "ban-hang" (docs/plans/danh-muc-bai.md). Bài nào dùng thì CHÉP file này vào thư mục bài:
-- container chuẩn bị bài chỉ được gắn đúng thư mục bài.
-- Cột được để NULL: khach_hang.thanh_pho (chưa ghi), danh_muc.cha_id (danh mục gốc),
-- don_hang.khach_hang_id (đơn của khách vãng lai).
-- don_hang.trang_thai ∈ moi, dang-giao, da-giao, huy. Ngày lưu dạng chuỗi YYYY-MM-DD.

CREATE TABLE khach_hang (
    id            INTEGER PRIMARY KEY,
    ho_ten        TEXT NOT NULL,
    thanh_pho     TEXT,
    ngay_dang_ky  TEXT NOT NULL
);

CREATE TABLE danh_muc (
    id      INTEGER PRIMARY KEY,
    ten     TEXT NOT NULL,
    cha_id  INTEGER REFERENCES danh_muc (id)
);

CREATE TABLE san_pham (
    id           INTEGER PRIMARY KEY,
    ten          TEXT NOT NULL,
    danh_muc_id  INTEGER NOT NULL REFERENCES danh_muc (id),
    gia          INTEGER NOT NULL,
    ton_kho      INTEGER NOT NULL
);

CREATE TABLE don_hang (
    id             INTEGER PRIMARY KEY,
    khach_hang_id  INTEGER REFERENCES khach_hang (id),
    ngay_dat       TEXT NOT NULL,
    trang_thai     TEXT NOT NULL
);

CREATE TABLE chi_tiet_don (
    don_hang_id  INTEGER NOT NULL REFERENCES don_hang (id),
    san_pham_id  INTEGER NOT NULL REFERENCES san_pham (id),
    so_luong     INTEGER NOT NULL,
    don_gia      INTEGER NOT NULL
);
