SELECT CASE
           WHEN diem < 4 THEN '[0, 4)'
           WHEN diem < 6.5 THEN '[4, 6.5)'
           WHEN diem < 8 THEN '[6.5, 8)'
           ELSE '[8, 10]'
       END AS khoang,
       COUNT(*) AS so_lan
FROM ket_qua
WHERE diem IS NOT NULL
GROUP BY khoang;
