SELECT id, ten,
       CASE WHEN ton_kho = 0 THEN 'Hết'
            WHEN ton_kho < 10 THEN 'Ít'
            ELSE 'Nhiều' END AS muc
FROM san_pham;
