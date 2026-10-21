SELECT con.ten, cha.ten
FROM danh_muc con
LEFT JOIN danh_muc cha ON cha.id = con.cha_id;
