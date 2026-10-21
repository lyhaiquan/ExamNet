SELECT mon_hoc_id, MAX(diem) AS cao_nhat, MIN(diem) AS thap_nhat
FROM ket_qua
GROUP BY mon_hoc_id;
