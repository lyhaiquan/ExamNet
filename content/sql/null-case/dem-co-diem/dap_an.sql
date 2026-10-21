SELECT COUNT(*) AS so_lan_thi, COUNT(diem) AS co_diem, COUNT(*) - COUNT(diem) AS vang_thi
FROM ket_qua;
