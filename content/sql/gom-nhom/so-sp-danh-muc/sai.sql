-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT danh_muc_id, COUNT(*) FROM san_pham;

SELECT danh_muc_id, COUNT(DISTINCT danh_muc_id) FROM san_pham GROUP BY danh_muc_id;
