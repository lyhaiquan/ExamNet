# Danh mục kèm danh mục cha

Liệt kê **tên danh mục** kèm **tên danh mục cha**. Danh mục gốc (không có cha) vẫn có trong kết quả, tên cha để
NULL.

Tự nối `danh_muc` với chính nó: `con.cha_id = cha.id`, và dùng `LEFT JOIN` để giữ danh mục gốc.

Kết quả có 2 cột: ten, ten. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

`danh_muc(id, ten, cha_id)` — `cha_id` NULL: danh mục gốc

| id | ten | cha_id |
| --- | --- | --- |
| 1 | Điện tử | NULL |
| 2 | Điện thoại | 1 |
| 3 | Máy tính xách tay | 1 |
| 4 | Sách | NULL |
| 5 | Sách lập trình | 4 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
