SELECT id, ten, gia
FROM san_pham
WHERE gia = (SELECT MAX(gia) FROM san_pham);
