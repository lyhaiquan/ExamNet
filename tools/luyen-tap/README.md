# Bài luyện tập (phỏng theo LeetCode) và chạy từng bước

`bai.py` là danh sách 34 bài: 10 easy, 20 intermediate, 4 advanced. Đề tiếng Việt do nhóm viết lại, không chép
đề hay test của LeetCode; mỗi bài ghi số bài LeetCode tương ứng để tham khảo. Mỗi mục có: `muc`, `slug`, `ten`,
`lc`, `mo_ta`, `vao`, `ra`, `rang_buoc`, `khung` (code đưa cho học viên, chạy được ngay), `giai` (lời giải mẫu),
`tests()` (danh sách input; 2 cái đầu là test nhỏ xem được `nho-1`, `nho-2`, còn lại là test ẩn `an-1`…).

Việc còn lại (nhánh `feat/luyen-tap-tung-buoc`):

1. `gen.py`: sinh `content/dsa/luyen-<muc>/<slug>/` gồm `lesson.yaml` (`loai: luyen-tap`, `bac: [code]`,
   `khung_nhin: []`, `test_xem: [nho-1, nho-2]`, `so_test_an: 0`, `gioi_han.thoi_gian_ms: 2000`), `de.md` (mô tả,
   Input, Output, Ràng buộc, Ví dụ lấy từ `nho-1`, dòng cuối "Tương tự LeetCode …"), `khung.py`, `loi_giai.py`,
   `tests/*.in` và `*.out` (output tính bằng chạy `loi_giai.py`). Thêm pytest: mọi bài `luyen-tap` có lời giải mẫu
   qua hết test, khung chạy không lỗi, test nhỏ dưới 500 bước khi chạy từng bước.
2. Server: `LessonModule.views(l)` thêm `{v: buoc, kieu: tung-dong}` cho bài `luyen-tap`; `RunModule.run` từ chối
   `testId` không nằm trong `test_xem` (bài DSA), nếu không thì in được input test ẩn qua Chạy thử.
3. App: cột trái chia hai nửa: trên "Thuật toán mẫu" (giang-giai, bai-tap; giữ id `#lessons`), dưới "Bài tập luyện
   tập" (luyen-tap, nhóm Easy → Intermediate → Advanced). Màn bài luyện tập không có tab Animation: tab Đề bài và
   bảng "Chạy từng bước" (biến, biến vừa đổi tô màu, ô kiểm điều kiện, ngăn gọi hàm, output tới bước đó, nút về
   đầu/trước/phát/sau, thanh kéo). Khung bao dòng đang chạy trong `CodeEditor` (kiểu đoạn RichTextFX + mũi tên ở
   cột số dòng), dòng lỗi tô đỏ, bỏ đánh dấu ngay khi sửa code.
4. Cập nhật spec §7.3 (sự kiện `buoc`, `buoc-ve`, `buoc-het`, `buoc-xong`, `loi`) và README content.

Runner đã xong (`runner/viz/tung_buoc.py`, `runner/goi_y.py`, `runner/tests/test_tung_buoc.py`).
