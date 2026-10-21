SELECT nv.id, nv.ho_ten
FROM nhan_vien nv
WHERE EXISTS (SELECT 1 FROM du_an da WHERE da.phong_id = nv.phong_id)
  AND NOT EXISTS (
      SELECT 1
      FROM du_an da
      WHERE da.phong_id = nv.phong_id
        AND NOT EXISTS (
            SELECT 1 FROM phan_cong pc WHERE pc.du_an_id = da.id AND pc.nhan_vien_id = nv.id
        )
  );
