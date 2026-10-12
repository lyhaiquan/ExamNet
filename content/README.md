# content — bài học và bài tập

Nội dung là **dữ liệu, không phải code** (spec §1 R4): thêm một bài là thêm một thư mục ở đây, không sửa Java.

```text
content/
├── dsa/<chương>/<bài>/   lesson.yaml, de.md, loi_giai.py, khung.py, sinh_test.py,
│                         du_doan.py, luat.py, demo.in, tests/
└── sql/<chương>/<bài>/   lesson.yaml, de.md, dap_an.sql, schema.sql, du_lieu_mau.sql,
                          sinh_du_lieu.py, du_doan.py
```

- Định dạng từng file và các trường của `lesson.yaml`: spec §7.1.
- Bốn bậc của một bài: spec §7.2.
- Danh sách animation dự kiến và người soạn: spec §7.7, §17.2.

**Định dạng thư mục do Người 3 giữ.** Nội dung từng chương do người soạn chương đó giữ (spec §17.2).

**Hai file cho phần chấm bài nộp** (task 3.8):

- `sinh_test.py <nho|tb|lon> <seed>` in một input ra stdout. Cùng seed thì cùng input. Server sinh `so_test_an` test ẩn cỡ `lon` (seed 1…N, output từ `loi_giai.py`) và một test `tb` (seed 1) để kiểm luật trace. Test `tb` phải đủ nhỏ để trace của lời giải mẫu dưới 10 000 sự kiện.
- `luat.py` (tuỳ chọn) định nghĩa `kiem(su_kien, dau_vao) -> str | None`: nhận danh sách sự kiện trace (dict) của code học viên chạy test `tb`, trả `None` nếu đạt hoặc một câu giải thích vi phạm. Lời giải mẫu phải qua luật của chính nó — xem `runner/tests/test_chuan_bi_luat.py`.

Mỗi bài phải qua kiểm nội dung trên CI trước khi merge: lời giải mẫu qua hết test, trace không vượt trần, `lesson.yaml` hợp lệ. genAI được dùng để soạn đề, lời giải mẫu và bộ sinh test; người soạn chịu trách nhiệm kiểm lại.
