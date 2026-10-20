SELECT sinh_vien_id, mon_hoc_id, diem,
       CASE
           WHEN diem IS NULL THEN 'Chưa thi'
           WHEN diem >= 8 THEN 'Giỏi'
           WHEN diem >= 6.5 THEN 'Khá'
           WHEN diem >= 5 THEN 'Trung bình'
           ELSE 'Yếu'
       END AS xep_loai
FROM ket_qua
WHERE lan_thi = 1;
