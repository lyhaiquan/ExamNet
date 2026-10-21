SELECT ma_mon, ten_mon, so_tin_chi
FROM mon_hoc
WHERE so_tin_chi >= 3
ORDER BY so_tin_chi DESC, ma_mon;
