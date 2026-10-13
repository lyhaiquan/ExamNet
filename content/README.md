# content — bài học và bài tập

Nội dung là **dữ liệu, không phải code** (spec §1 R4): thêm một bài là thêm một thư mục ở đây, không sửa Java.

```text
content/
├── dsa/<chương>/<bài>/   lesson.yaml, de.md, loi_giai.py, khung.py, sinh_test.py,
│                         du_doan.py, luat.py, demo.in, tests/
└── sql/<chương>/<bài>/   lesson.yaml, de.md, dap_an.sql, schema.sql, du_lieu_mau.sql,
                          khung.sql, sinh_du_lieu.py, du_doan.py
```

- Định dạng từng file và các trường của `lesson.yaml`: spec §7.1.
- Bốn bậc của một bài: spec §7.2.
- Danh sách animation dự kiến và người soạn: spec §7.7, §17.2.

**Định dạng thư mục do Người 3 giữ.** Nội dung từng chương do người soạn chương đó giữ (spec §17.2).

**Hai file cho phần chấm bài nộp** (task 3.8):

- `sinh_test.py <nho|tb|lon> <seed>` in một input ra stdout. Cùng seed thì cùng input. Server sinh `so_test_an` test ẩn cỡ `lon` (seed 1…N, output từ `loi_giai.py`) và một test `tb` (seed 1) để kiểm luật trace. Test `tb` phải đủ nhỏ để trace của lời giải mẫu dưới 10 000 sự kiện.
- `luat.py` (tuỳ chọn) định nghĩa `kiem(su_kien, dau_vao) -> str | None`: nhận danh sách sự kiện trace (dict) của code học viên chạy test `tb`, trả `None` nếu đạt hoặc một câu giải thích vi phạm. Lời giải mẫu phải qua luật của chính nó — xem `runner/tests/test_chuan_bi_luat.py`.

**Cấu trúc "gắn camera" có sẵn** (`runner/viz`, khai báo trong `khung_nhin` để chọn bộ vẽ):

| Lớp Python | `kieu` | Thao tác sinh sự kiện |
| --- | --- | --- |
| `viz.Mang(ds, ten="a")` | `mang` | `a[i]` đọc, `a[i] = x` ghi, `a.con_tro("i", i)`. `append`, `pop`, `insert`, `sort`, `reverse`, `extend`, `del`… cũng được ghi lại |
| `viz.NganXep(ten="st")` | `ngan-xep` | `push`/`append`, `pop`, xem đỉnh `top()`/`st[-1]` |
| `viz.HangDoi(ten="q")` | `hang-doi` | `enq`/`append`, `deq`/`popleft`, xem đầu `dau()`/`q[0]` |
| `viz.Bang(hang, cot, ten="dp", nhan_hang=…, nhan_cot=…)`, `viz.Bang.tu(luoi)` | `luoi` | `dp[i][j]` đọc/ghi (mũi tên từ ô vừa đọc tới ô được ghi), `dp.danh_dau(i, j)` tô xanh lá |
| `viz.Cay(ten="T")`, nút `T.Nut(k)` | `cay` | `T.goc = …`, `nut.trai`/`nut.phai = …` gắn hay cắt nhánh, đọc `nut.khoa` tô vàng nút đang xét, `T.danh_dau(nut, "xong")`. Tên tiếng Anh `left`/`right`/`key`/`T.root` cũng được |
| `viz.Mang(ds, ten="h")` | `heap` | Như `mang`, vẽ thành cây nhị phân đầy đủ. Bài heap cấm `heapq` (`cam_import`): `heapq` viết bằng C, đổi mảng mà không ghi lại được |

Thêm vào đó `viz.say("…")` (lời thuyết minh) và `viz.key("…")` (bước quan trọng, nút ⏭ nhảy tới; lời kèm theo cũng hiện như thuyết minh). Ngăn xếp, hàng đợi, bảng không kế thừa `list`: học viên không có cách nào đổi dữ liệu mà không sinh sự kiện. Muốn đọc bảng mà không làm rối hình (ví dụ khi truy vết trong lời giải mẫu) thì dùng `dp.trang_thai()["o"]`.

Mỗi bài phải qua kiểm nội dung trên CI trước khi merge: lời giải mẫu qua hết test, trace không vượt trần, `lesson.yaml` hợp lệ. genAI được dùng để soạn đề, lời giải mẫu và bộ sinh test; người soạn chịu trách nhiệm kiểm lại.

**Bài SQL** (bộ máy SQL, spec §7.6):

- `schema.sql`, `du_lieu_mau.sql`: chép từ CSDL dùng chung `sql/_csdl/<tên>/` (thư mục bắt đầu bằng `_` không phải bài). Dữ liệu mẫu nên ≤ 12 dòng mỗi bảng: animation đánh dấu WHERE/HAVING và nối JOIN từng dòng khi bảng nhỏ như vậy. Mọi file `.sql` lưu UTF-8.
- `dap_an.sql`: đúng một câu (SELECT, hoặc INSERT/UPDATE/DELETE nếu `loai_cau: dml`). Bài giảng giải lấy animation từ chính câu này chạy trên dữ liệu mẫu.
- `khung.sql`: câu khởi đầu đưa cho học viên.
- `sinh_du_lieu.py <seed>` in ra các câu INSERT (chạy sau `schema.sql`) của một database ẩn; server gọi seed 1, 2, 3. Ba bộ phải có NULL, dòng trùng và một bảng rỗng (spec §7.6) — chép bộ sinh của CSDL dùng chung là đủ.
- `lesson.yaml` thêm `loai_cau: select | dml` và `thu_tu: co | khong` (`co`: so kết quả theo đúng thứ tự dòng, dùng cho bài có ORDER BY). `khung_nhin` mặc định là bảng SQL.
- Chấm: câu học viên và đáp án chạy trên dữ liệu mẫu rồi 3 database ẩn; SELECT so kết quả như đa tập (số thực tới 1e-6), DML so toàn bộ nội dung database sau khi chạy.

