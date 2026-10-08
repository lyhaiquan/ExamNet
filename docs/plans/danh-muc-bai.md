# Danh mục bài học

**Bản tạm**, dựng theo một học phần DSA và SQL thông thường (spec §7.7). Khi có đề cương thật của hai môn (giả định A1, spec §18), sửa file này **trước**, rồi mới soạn. Đổi tên hay đổi chỗ bài thì giữ đúng số lượng của mỗi người ở bảng tổng, hoặc cả nhóm thống nhất chia lại.

Đây là danh sách việc cho phần nội dung ở [giai-doan-4.md](giai-doan-4.md#nội-dung-bài-học). Quy trình soạn một bài và danh sách kiểm khi duyệt PR ở đó.

## Cách đọc

- **id** là đường dẫn thư mục: `dsa.sap-xep.noi-bot` ứng với `content/dsa/sap-xep/noi-bot/`. Đó cũng là giá trị `id` trong `lesson.yaml` và giá trị ghi trong `mo_khoa`. Bảng chỉ ghi phần cuối; phần đầu ở tiêu đề chương.
- **Người** ở tiêu đề chương là chủ chương theo spec §17.2: người soạn và chịu trách nhiệm mọi bài trong chương.
- **Loại:**

  | Ký hiệu | `loai` trong `lesson.yaml` | Nghĩa |
  | --- | --- | --- |
  | GG★ | `giang-giai` | Animation giảng giải, thuộc 20 bài ưu tiên |
  | GG | `giang-giai` | Animation giảng giải |
  | BT◆ | `bai-tap` | Bài tập thuộc **bộ lõi 50 bài**, làm trước mọi bài tập khác |
  | BT | `bai-tap` | Code của học viên chạy thành animation: khung đưa sẵn cấu trúc `viz` hoặc khai báo `khung_nhin` |
  | LT | `luyen-tap` | Chỉ chấm test, không animation; vẫn có chế độ chạy từng dòng |

- **Bộ vẽ:** của lời giải mẫu (GG) hoặc của code học viên (BT). Bộ vẽ đầu tiên là bộ vẽ chính.
- **Bậc:** theo spec §7.2. Mọi bài GG có bậc dự đoán. Bậc tự mô phỏng chỉ có ở bài mà `SimScript` (task 3.15) rút được bước: sắp xếp bằng đổi chỗ kề, và ngăn xếp.
- **Ghi chú:** "Cấm …" ghi vào `cam_dung` / `cam_import`, "cấm cú pháp" ghi vào `cam_cu_phap` (task 3.8); "Luật trace" viết thành `luat.py`; "Cần O(…)" nghĩa là test `lon` phải làm cách chậm hơn quá giờ.
- **Đã làm:** bài mẫu làm ở giai đoạn 1–3 để kiểm bộ máy. Bài đó **tính vào phần của chủ chương**, dù người khác làm; chủ chương duyệt lại và chỉnh ở giai đoạn 4.

## Tổng số

| Người | Giảng giải DSA | Giảng giải SQL | Bài tập + luyện tập DSA | Bài tập + luyện tập SQL | Cộng |
| --- | --- | --- | --- | --- | --- |
| 1 | 11 | 3 | 30 | 20 | 64 |
| 2 | 10 | 3 | 30 | 20 | 63 |
| 3 | 6 | 7 | 20 | 30 | 63 |
| 4 | 13 | 0 | 20 | 30 | 63 |
| **Tổng** | **40** (★ 20) | **13** | **100** (BT 50, LT 50) | **100** (BT 50, LT 50) | **253** |

Khớp spec §17.2: 53 giảng giải (40 DSA, 13 SQL, 20 bài ★), 100 bài DSA, 100 bài SQL. Sửa danh mục thì giữ đúng các con số ở hàng Tổng, hoặc sửa cả spec §17.2.

**Sáu bài đã làm ở giai đoạn 1–3:**

| Bài | Ai làm, ở task nào | Tính vào phần của |
| --- | --- | --- |
| `dsa.sap-xep.noi-bot` | Người 3, GĐ1 task 3.4 | Người 4 |
| `dsa.ngan-xep.trung-to-hau-to` | Người 4, GĐ2 task 4.8 | Người 2 |
| `dsa.quay-lui.n-queens` | Người 1, GĐ3 task 1.15 | Người 2 |
| `dsa.quy-hoach-dong.lcs` | Người 3, GĐ3 task 3.18 | Người 3 |
| `sql.co-ban.loc-sinh-vien` | Người 3, GĐ2 task 3.12 | Người 3 |
| `sql.gom-nhom.diem-trung-binh-lop` | Người 3, GĐ3 task 3.14 | Người 3 |

Bốn bài DSA đầu và bài GROUP BY là bộ demo tối thiểu (spec §1 R5). `loc-sinh-vien` thêm vào để kiểm SQL bản đầu ở M2.

## Một bài cần những file nào

| Loại | DSA | SQL |
| --- | --- | --- |
| GG | `lesson.yaml`, `de.md`, `loi_giai.py` có `viz.say`/`viz.key`, `khung.py`, `sinh_test.py`, `du_doan.py`, `demo.in`, `tests/` | `lesson.yaml`, `de.md`, `dap_an.sql`, `schema.sql`, `du_lieu_mau.sql`, `sinh_du_lieu.py`, `du_doan.py` |
| BT | Như GG nhưng không cần `viz.say`/`viz.key`, `demo.in`; `du_doan.py` chỉ khi bậc có dự đoán | Như GG; `du_doan.py` chỉ khi bậc có dự đoán |
| LT | `lesson.yaml`, `de.md`, `loi_giai.py`, `khung.py` (chỉ chữ ký hàm, đọc input, in output), `sinh_test.py`, `tests/` | `lesson.yaml`, `de.md`, `dap_an.sql`, `schema.sql`, `du_lieu_mau.sql`, `sinh_du_lieu.py` |

## Thứ tự làm và mức cắt

Bảng trong mỗi chương đã xếp theo thứ tự ưu tiên: GG, rồi BT◆, rồi BT, rồi LT; trong cùng loại, bài trên làm trước.

| Đợt | Làm gì | Cộng dồn |
| --- | --- | --- |
| 1 | 20 bài GG★ (5 bài đã làm) | 20 GG |
| 2 | Bộ lõi 50 bài BT◆ (24 DSA, 26 SQL) — mức **50** của spec §1 R5 | 20 GG + 50 bài |
| 3 | 33 bài GG còn lại | 53 GG + 50 bài |
| 4 | Mỗi chương: nửa đầu bảng BT và nửa đầu bảng LT (làm tròn lên, tính cả bài ◆ đã làm) — mức **100** | 53 GG + 112 bài |
| 5 | Phần còn lại | 53 GG + 200 bài |

Thiếu thời gian thì bỏ từ đợt cuối lên: đợt 5, rồi đợt 4, rồi đợt 3. Đúng thứ tự cắt ở spec §1 R5: bớt số bài tập trước, rồi tới animation ngoài 20 bài ★.

## Chuỗi mở bài

`ProgressService` (task 3.10) mở các bài trong `mo_khoa` khi học viên thành thạo một bài. Quy ước điền `mo_khoa`:

- Bài GG đầu tiên của mỗi chương **mở sẵn**: giáo viên dạy các chương theo thứ tự nào cũng được.
- Trong một chương, mỗi bài mở bài **ngay dưới nó** trong bảng, trừ LT.
- Bài LT không chặn nhau: qua bài BT cuối của chương thì mở **mọi** LT của chương. Chương `sql.tong-hop` không có GG, BT nên mọi bài mở sẵn.
- Bài giáo viên giao cho lớp (`LESSON_PUSH`) mở với cả lớp, bỏ qua chuỗi này.

Thứ tự chương gợi ý khi dạy: DSA `sap-xep` → `tim-kiem` → `dslk` → `ngan-xep` → `hang-doi` → `bam` → `de-quy` → `quay-lui` → `quy-hoach-dong` → `cay` → `do-thi`. SQL `co-ban` → `ket-noi` → `gom-nhom` → `truy-van-con` → `tap-hop` → `null-case` → `dml` → `tong-hop`.

## Ba CSDL dùng chung cho bài SQL

Học viên làm nhiều bài trên cùng vài bộ bảng thì quen dữ liệu, tập trung vào câu lệnh. Người 3 soạn ba CSDL trong `content/sql/_csdl/<tên>/` (`schema.sql`, `du_lieu_mau.sql`, `sinh.py`) ngay ngày đầu giai đoạn 4, hoặc sớm hơn cùng task 3.12.

| CSDL | Bảng và cột |
| --- | --- |
| `truong-hoc` | `sinh_vien(id, ho_ten, gioi_tinh, ngay_sinh, que_quan, lop_id)` · `lop(id, ten_lop, khoa, gvcn_id)` · `giang_vien(id, ho_ten, khoa, luong)` · `mon_hoc(id, ma_mon, ten_mon, so_tin_chi)` · `ket_qua(sinh_vien_id, mon_hoc_id, lan_thi, diem)` |
| `ban-hang` | `khach_hang(id, ho_ten, thanh_pho, ngay_dang_ky)` · `danh_muc(id, ten, cha_id)` · `san_pham(id, ten, danh_muc_id, gia, ton_kho)` · `don_hang(id, khach_hang_id, ngay_dat, trang_thai)` · `chi_tiet_don(don_hang_id, san_pham_id, so_luong, don_gia)` |
| `nhan-su` | `phong_ban(id, ten, ngan_sach)` · `nhan_vien(id, ho_ten, phong_id, quan_ly_id, luong, ngay_vao_lam)` · `du_an(id, ten, phong_id, ngan_sach)` · `phan_cong(nhan_vien_id, du_an_id, so_gio)` |

- **Cột được để NULL** (để có bài về NULL và OUTER JOIN): `sinh_vien.lop_id`, `lop.gvcn_id`, `khach_hang.thanh_pho`, `danh_muc.cha_id`, `nhan_vien.phong_id`, `nhan_vien.quan_ly_id`, `du_an.phong_id`, `don_hang.khach_hang_id` (đơn của khách vãng lai).
- Ngày lưu dạng chuỗi `YYYY-MM-DD` (SQLite không có kiểu ngày riêng). `don_hang.trang_thai` ∈ `moi`, `dang-giao`, `da-giao`, `huy`.
- **Mỗi bài vẫn tự đủ file** (spec §7.1): chép `schema.sql` và phần dữ liệu cần dùng vào thư mục bài, vì container chuẩn bị bài chỉ được gắn đúng thư mục bài (task 3.7). Bài cần thêm bảng (ví dụ `hoc_bong`) thì thêm vào `schema.sql` của riêng bài đó.
- `ContentCheck` (task 3.19) bỏ qua thư mục bắt đầu bằng `_` (`_mau`, `_csdl`), và cảnh báo khi `schema.sql` của bài khác bản gốc ở `_csdl` mà bài không khai báo bảng thêm.
- `sinh_du_lieu.py` của mỗi bài sinh 3 database ẩn có NULL, dòng trùng và một bảng rỗng (spec §7.6). Database ẩn của bài `truy-van-con.khach-chua-dat` phải có đơn với `khach_hang_id` NULL, để câu `NOT IN` viết ẩu ra sai.

---

## DSA

### `dsa.sap-xep` — Sắp xếp (Người 4)

7 giảng giải · 4 bài tập · 3 luyện tập.

| id | Bài | Loại | Bộ vẽ | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `noi-bot` | Sắp xếp nổi bọt | GG★ | `mang` | xem · dự đoán · tự mô phỏng · code | Luật trace: chỉ đổi chỗ hai phần tử kề. **Đã làm** ở GĐ1 task 3.4 (Người 3) |
| `chon` | Sắp xếp chọn | GG | `mang` | xem · dự đoán · code | Luật trace: mỗi lượt đổi chỗ tối đa một lần |
| `chen` | Sắp xếp chèn (đổi chỗ kề) | GG | `mang` | xem · dự đoán · tự mô phỏng · code |  |
| `tron` | Sắp xếp trộn | GG | `mang`, `cay-goi` | xem · dự đoán · code | `cay_goi: [sap_xep_tron]` |
| `nhanh` | Sắp xếp nhanh, phân hoạch Lomuto | GG | `mang`, `cay-goi` | xem · dự đoán · code | Con trỏ `i`, `j`, chốt |
| `vun-dong` | Sắp xếp vun đống | GG | `mang`, `cay` (heap) | xem · dự đoán · code | Cùng một mảng vẽ hai cách |
| `dem` | Sắp xếp đếm | GG | `mang` | xem · dự đoán · code | Hai mảng: đếm và kết quả |
| `dem-luot-doi-cho` | Đếm số lần đổi chỗ khi sắp xếp nổi bọt | BT◆ | `mang` | dự đoán · code | n ≤ 1 000 |
| `co-ha-lan` | Sắp xếp dãy chỉ gồm 0, 1, 2 trong một lần duyệt | BT◆ | `mang` | code | Luật trace: mỗi ô đọc ≤ 2 lần |
| `k-nho-nhat` | Phần tử nhỏ thứ k (chọn nhanh) | BT | `mang` | code | Cấm `sorted`, `sort`, `heapq` |
| `tron-hai-day` | Trộn hai dãy đã sắp | BT | `mang` | dự đoán · code |  |
| `nghich-the` | Đếm cặp nghịch thế, n ≤ 10⁵ | LT | — | code | Cần O(n log n) |
| `theo-tan-suat` | Sắp xếp theo tần suất giảm dần, bằng nhau thì theo giá trị | LT | — | code |  |
| `chenh-lech-nho-nhat` | Hiệu nhỏ nhất giữa hai phần tử của dãy | LT | — | code |  |

### `dsa.tim-kiem` — Tìm kiếm (Người 4)

2 giảng giải · 2 bài tập · 3 luyện tập.

| id | Bài | Loại | Bộ vẽ | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `tuan-tu` | Tìm kiếm tuần tự | GG | `mang` | xem · dự đoán · code |  |
| `nhi-phan` | Tìm kiếm nhị phân | GG | `mang` | xem · dự đoán · code | Con trỏ `lo`, `mid`, `hi` |
| `vi-tri-dau-tien` | Vị trí xuất hiện đầu tiên của x trong dãy đã sắp | BT◆ | `mang` | dự đoán · code | Luật trace: số lần đọc ≤ 2·log₂n + 2 |
| `hai-con-tro` | Cặp phần tử có tổng bằng x, dùng hai con trỏ | BT | `mang` | code |  |
| `mang-xoay` | Tìm x trong dãy tăng đã bị xoay vòng | LT | — | code | Cần O(log n) |
| `chia-doan` | Chia dãy thành k đoạn liên tiếp để tổng lớn nhất là nhỏ nhất | LT | — | code | Tìm nhị phân theo đáp án |
| `can-bac-hai` | Phần nguyên căn bậc hai của n ≤ 10¹⁸ | LT | — | code | Cấm `math.isqrt`, `math.sqrt`; cấm cú pháp `**` (`cam_cu_phap: [Pow]`) |

### `dsa.dslk` — Danh sách liên kết (Người 4)

2 giảng giải · 3 bài tập · 1 luyện tập.

| id | Bài | Loại | Bộ vẽ | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `chen-xoa` | Chèn và xoá trên danh sách liên kết đơn | GG | `dslk` | xem · dự đoán · code |  |
| `dao-nguoc` | Đảo ngược danh sách liên kết | GG | `dslk` | xem · dự đoán · code |  |
| `xoa-trung` | Xoá phần tử trùng trong danh sách đã sắp | BT◆ | `dslk` | code | Khung dùng `viz.Nut` |
| `tron-hai-ds` | Trộn hai danh sách liên kết đã sắp | BT | `dslk` | dự đoán · code |  |
| `phan-tu-giua` | Nút giữa bằng con trỏ nhanh và chậm | BT | `dslk` | code | Luật trace: duyệt một lần |
| `chu-trinh` | Phát hiện chu trình và nút bắt đầu chu trình | LT | — | code |  |

### `dsa.bam` — Băm (Người 4)

2 giảng giải · 1 bài tập · 3 luyện tập.

| id | Bài | Loại | Bộ vẽ | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `day-chuyen` | Băm dây chuyền | GG | `bam` | xem · dự đoán · code | `viz.BangBam` |
| `dia-chi-mo` | Băm địa chỉ mở, dò tuyến tính | GG | `bam` | xem · dự đoán · code |  |
| `do-tuyen-tinh` | Tự viết thêm và tìm khoá bằng dò tuyến tính trên bảng m ô | BT◆ | `mang` | dự đoán · code | Cấm `dict`, `set` và cú pháp `{…}` (`cam_cu_phap: [Dict, DictComp, Set, SetComp]`) |
| `hai-tong` | Đếm cặp có tổng bằng x, n ≤ 10⁵ | LT | — | code |  |
| `doan-khong-lap` | Đoạn con dài nhất không có phần tử lặp | LT | — | code |  |
| `nhom-dao-chu` | Gom các từ là đảo chữ của nhau | LT | — | code |  |

### `dsa.ngan-xep` — Ngăn xếp (Người 2)

3 giảng giải · 4 bài tập · 4 luyện tập.

| id | Bài | Loại | Bộ vẽ | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `kiem-tra-ngoac` | Kiểm tra dãy ngoặc hợp lệ | GG★ | `ngan-xep` | xem · dự đoán · tự mô phỏng · code |  |
| `trung-to-hau-to` | Trung tố → hậu tố | GG★ | `ngan-xep` | xem · dự đoán · tự mô phỏng · code | **Đã làm** ở GĐ2 task 4.8 (Người 4) |
| `tinh-hau-to` | Tính biểu thức hậu tố | GG★ | `ngan-xep` | xem · dự đoán · tự mô phỏng · code |  |
| `ngoac-dai-nhat` | Độ dài đoạn ngoặc hợp lệ dài nhất | BT◆ | `ngan-xep` | code |  |
| `lon-hon-ke-tiep` | Phần tử lớn hơn kế tiếp (ngăn xếp đơn điệu) | BT◆ | `ngan-xep`, `mang` | dự đoán · code | Cần O(n) |
| `tinh-trung-to` | Tính biểu thức trung tố có ngoặc, + − × / | BT | `ngan-xep` | code | Cấm `eval`, `exec` |
| `ngan-xep-min` | Ngăn xếp lấy được min trong O(1) | BT | `ngan-xep` | code |  |
| `hinh-chu-nhat` | Hình chữ nhật lớn nhất trong biểu đồ cột | LT | — | code |  |
| `rut-gon-duong-dan` | Rút gọn đường dẫn kiểu Unix | LT | — | code |  |
| `xoa-k-chu-so` | Xoá k chữ số để số còn lại nhỏ nhất | LT | — | code |  |
| `nhip-gia` | Số ngày liên tiếp trước đó có giá không vượt hôm nay | LT | — | code |  |

### `dsa.hang-doi` — Hàng đợi (Người 2)

1 giảng giải · 3 bài tập · 3 luyện tập.

| id | Bài | Loại | Bộ vẽ | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `hang-doi-vong` | Hàng đợi vòng trên mảng | GG★ | `mang` | xem · dự đoán · code | Con trỏ `dau`, `cuoi` |
| `so-nhi-phan` | In các số nhị phân từ 1 đến n bằng hàng đợi | BT◆ | `ngan-xep` | dự đoán · code | `viz.HangDoi` vẽ nằm ngang |
| `cua-so-max` | Max của mọi đoạn độ dài k (hàng đợi hai đầu) | BT | `ngan-xep`, `mang` | code | Cần O(n) |
| `may-in` | Hàng đợi máy in theo độ ưu tiên: khi nào tài liệu k được in | BT | `ngan-xep` | code |  |
| `josephus` | Bài toán Josephus mô phỏng bằng hàng đợi | LT | — | code |  |
| `hai-ngan-xep` | Hàng đợi dựng từ hai ngăn xếp, q truy vấn | LT | — | code |  |
| `nhan-doi-tru-mot` | Số bước ít nhất biến S thành T bằng ×2 và −1 | LT | — | code |  |

### `dsa.de-quy` — Đệ quy (Người 2)

1 giảng giải · 3 bài tập · 3 luyện tập.

| id | Bài | Loại | Bộ vẽ | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `fibonacci-cay-goi` | Cây lời gọi Fibonacci đệ quy (thấy lời gọi lặp lại) | GG★ | `cay-goi` | xem · dự đoán · code | `cay_goi: [fib]` |
| `thap-ha-noi` | Tháp Hà Nội: in các bước chuyển | BT◆ | `cay-goi` | dự đoán · code |  |
| `luy-thua-nhanh` | Luỹ thừa nhanh aⁿ mod m bằng đệ quy | BT◆ | `cay-goi` | dự đoán · code | Cấm `pow`; cấm cú pháp `**` |
| `to-hop-pascal` | C(n, k) đệ quy theo tam giác Pascal | BT | `cay-goi` | code | Test `nho` n ≤ 6 để xem cây |
| `ucln` | UCLN và BCNN bằng thuật toán Euclid đệ quy | LT | — | code | Cấm `math.gcd` |
| `doi-co-so` | Đổi số sang hệ cơ số b bằng đệ quy | LT | — | code |  |
| `xau-nhi-phan` | Liệt kê xâu nhị phân độ dài n theo thứ tự từ điển | LT | — | code |  |

### `dsa.quay-lui` — Quay lui (Người 2)

5 giảng giải · 5 bài tập · 5 luyện tập.

| id | Bài | Loại | Bộ vẽ | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `hoan-vi` | Sinh hoán vị | GG★ | `cay-goi` | xem · dự đoán · code | Cấm `itertools` |
| `to-hop` | Sinh tổ hợp chập k | GG★ | `cay-goi` | xem · dự đoán · code | Cấm `itertools` |
| `n-queens` | Đặt n quân hậu | GG★ | `cay-goi`, `luoi` | xem · dự đoán · code | **Đã làm** ở GĐ3 task 1.15 (Người 1) |
| `tong-tap-con` | Tập con có tổng bằng S, có cắt nhánh | GG★ | `cay-goi` | xem · dự đoán · code | `viz.cat` ở nhánh bị cắt |
| `sudoku-4x4` | Giải Sudoku 4×4 | GG★ | `luoi`, `cay-goi` | xem · dự đoán · code |  |
| `day-ngoac` | Sinh mọi dãy ngoặc đúng độ dài 2n | BT◆ | `cay-goi` | dự đoán · code |  |
| `me-cung` | Chuột trong mê cung: liệt kê mọi đường đi | BT◆ | `luoi`, `cay-goi` | code |  |
| `chia-tap` | Chia tập thành k tập con có tổng bằng nhau | BT | `cay-goi` | code |  |
| `to-mau` | Tô màu đồ thị bằng m màu | BT | `do-thi`, `cay-goi` | code |  |
| `ma-di-tuan` | Mã đi tuần trên bàn 5×5 | BT | `luoi` | code |  |
| `phan-tich-so` | Liệt kê cách phân tích n thành tổng các số nguyên dương | LT | — | code |  |
| `tu-trong-luoi` | Tìm từ trong lưới chữ, đi 4 hướng, không lặp ô | LT | — | code |  |
| `hoan-vi-co-lap` | Hoán vị của dãy có phần tử trùng, không in trùng | LT | — | code |  |
| `tong-to-hop` | Mọi tổ hợp có tổng S, mỗi số dùng nhiều lần | LT | — | code |  |
| `dem-n-queens` | Đếm số cách đặt n hậu, n ≤ 12 | LT | — | code | Cần cắt nhánh tốt |

### `dsa.quy-hoach-dong` — Quy hoạch động (Người 3)

6 giảng giải · 10 bài tập · 10 luyện tập.

| id | Bài | Loại | Bộ vẽ | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `fibonacci-ghi-nho` | Fibonacci có ghi nhớ | GG★ | `cay-goi`, `mang` | xem · dự đoán · code | So với bài `de-quy.fibonacci-cay-goi` |
| `cai-tui` | Cái túi 0/1 | GG★ | `luoi` | xem · dự đoán · code | `viz.Bang`, truy vết |
| `lcs` | Dãy con chung dài nhất | GG★ | `luoi` | xem · dự đoán · code | **Đã làm** ở GĐ3 task 3.18 (Người 3) |
| `lis` | Dãy con tăng dài nhất, O(n²) | GG★ | `mang` | xem · dự đoán · code |  |
| `doi-tien` | Đổi tiền ít đồng nhất | GG★ | `mang` | xem · dự đoán · code |  |
| `duong-di-luoi` | Đường đi tổng nhỏ nhất trên lưới | GG★ | `luoi` | xem · dự đoán · code |  |
| `leo-cau-thang` | Số cách leo n bậc, mỗi bước 1–3 bậc | BT◆ | `mang` | dự đoán · code |  |
| `doan-tong-lon-nhat` | Đoạn con có tổng lớn nhất | BT◆ | `mang` | dự đoán · code |  |
| `khoang-cach-sua` | Khoảng cách sửa xâu (Levenshtein) | BT◆ | `luoi` | dự đoán · code |  |
| `so-cach-doi-tien` | Số cách đổi tiền | BT◆ | `mang` | code |  |
| `tui-khong-gioi-han` | Cái túi không giới hạn số lượng | BT◆ | `mang` | code |  |
| `xau-doi-xung` | Xâu con đối xứng dài nhất | BT | `luoi` | code |  |
| `nhan-ma-tran` | Nhân chuỗi ma trận | BT | `luoi` | code |  |
| `cat-thanh` | Cắt thanh để bán được nhiều tiền nhất | BT | `mang` | code |  |
| `luoi-vat-can` | Số đường đi trên lưới có vật cản | BT | `luoi` | dự đoán · code |  |
| `tam-giac-so` | Đường đi tổng lớn nhất trong tam giác số | BT | `luoi` | code |  |
| `lis-lon` | Dãy con tăng dài nhất, n ≤ 10⁵ | LT | — | code | Cần O(n log n) |
| `chia-hai-tap` | Chia dãy thành hai tập có hiệu tổng nhỏ nhất | LT | — | code |  |
| `tong-tap-con-dp` | Có tập con tổng bằng S? n ≤ 100, S ≤ 10⁵ | LT | — | code |  |
| `xau-chung-lien-tiep` | Xâu con liên tiếp chung dài nhất | LT | — | code |  |
| `hinh-vuong-1` | Hình vuông toàn số 1 lớn nhất trong ma trận | LT | — | code |  |
| `to-hop-mod` | C(n, k) mod 10⁹+7, n ≤ 1 000 | LT | — | code |  |
| `khong-ke-nhau` | Tổng lớn nhất khi không lấy hai phần tử kề nhau | LT | — | code |  |
| `lcs-ba-xau` | Dãy con chung dài nhất của ba xâu | LT | — | code |  |
| `tach-tu` | Tách xâu thành các từ có trong từ điển | LT | — | code |  |
| `giai-ma-so` | Số cách giải mã xâu số thành chữ (1 → A … 26 → Z) | LT | — | code |  |

### `dsa.cay` — Cây (Người 1)

4 giảng giải · 7 bài tập · 7 luyện tập.

| id | Bài | Loại | Bộ vẽ | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `bst` | Cây nhị phân tìm kiếm: chèn, tìm, xoá | GG | `cay` | xem · dự đoán · code | `viz.NutCay` |
| `duyet-cay` | Bốn kiểu duyệt: trước, giữa, sau, theo mức | GG | `cay`, `ngan-xep` | xem · dự đoán · code |  |
| `heap` | Đống nhị phân: thêm và lấy min | GG | `cay` (heap), `mang` | xem · dự đoán · code |  |
| `avl` | Cây AVL: chèn và quay | GG | `cay` | xem · dự đoán · code |  |
| `chieu-cao` | Chiều cao cây nhị phân | BT◆ | `cay` | dự đoán · code |  |
| `kiem-tra-bst` | Một cây có phải cây nhị phân tìm kiếm không | BT◆ | `cay` | code |  |
| `to-tien-chung` | Tổ tiên chung gần nhất trên cây nhị phân tìm kiếm | BT◆ | `cay` | dự đoán · code |  |
| `dung-cay` | Dựng cây từ kết quả duyệt trước và duyệt giữa | BT | `cay` | code |  |
| `duong-di-tong` | Có đường gốc → lá tổng bằng S? | BT | `cay` | code |  |
| `k-nho-nhat-bst` | Phần tử nhỏ thứ k trong cây nhị phân tìm kiếm | BT | `cay` | code |  |
| `k-lon-nhat-heap` | k phần tử lớn nhất bằng đống kích thước k | BT | `cay` (heap) | code | Cấm `heapq`, `sorted` |
| `duong-kinh` | Đường kính cây | LT | — | code |  |
| `cay-doi-xung` | Cây có đối xứng gương không | LT | — | code |  |
| `nhin-tu-trai` | Các nút nhìn thấy từ bên trái | LT | — | code |  |
| `trung-vi-dong` | Trung vị của dãy đang thêm vào (hai đống) | LT | — | code |  |
| `tron-k-day` | Trộn k dãy đã sắp | LT | — | code |  |
| `duyet-zic-zac` | Duyệt cây theo mức, đổi chiều mỗi mức | LT | — | code |  |
| `lca-tong-quat` | Tổ tiên chung gần nhất, n, q ≤ 10⁵ | LT | — | code |  |

### `dsa.do-thi` — Đồ thị (Người 1)

7 giảng giải · 8 bài tập · 8 luyện tập.

| id | Bài | Loại | Bộ vẽ | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `bfs` | Duyệt theo chiều rộng | GG★ | `do-thi`, `ngan-xep` | xem · dự đoán · code | `viz.DoThi` |
| `dfs` | Duyệt theo chiều sâu | GG★ | `do-thi`, `ngan-xep` | xem · dự đoán · code |  |
| `dijkstra` | Đường đi ngắn nhất Dijkstra | GG★ | `do-thi`, `mang`, `cay` (heap) | xem · dự đoán · code | Ví dụ ở spec §7.5 |
| `prim` | Cây khung nhỏ nhất Prim | GG | `do-thi` | xem · dự đoán · code |  |
| `kruskal` | Cây khung nhỏ nhất Kruskal, hợp và tìm | GG | `do-thi`, `mang` | xem · dự đoán · code |  |
| `topo` | Sắp xếp tô-pô (Kahn) | GG | `do-thi`, `ngan-xep` | xem · dự đoán · code |  |
| `floyd` | Floyd–Warshall | GG | `luoi` | xem · dự đoán · code |  |
| `dem-thanh-phan` | Đếm thành phần liên thông | BT◆ | `do-thi` | dự đoán · code |  |
| `it-canh-nhat` | Đường đi ít cạnh nhất, in đường đi | BT◆ | `do-thi` | dự đoán · code |  |
| `co-chu-trinh` | Đồ thị có hướng có chu trình không | BT◆ | `do-thi` | code |  |
| `hai-phia` | Kiểm tra đồ thị hai phía | BT◆ | `do-thi` | code |  |
| `me-cung-bfs` | Đường ngắn nhất trong mê cung dạng lưới | BT | `luoi` | code |  |
| `dem-dao` | Đếm số đảo trên lưới | BT | `luoi` | code |  |
| `bellman-ford` | Đường đi ngắn nhất khi có cạnh âm | BT | `do-thi`, `mang` | code |  |
| `canh-cau` | Tìm các cạnh cầu | BT | `do-thi` | code |  |
| `dijkstra-lon` | Dijkstra, n, m ≤ 2·10⁵ | LT | — | code | Cần `heapq` |
| `khung-lon` | Trọng số cây khung nhỏ nhất, n ≤ 10⁵ | LT | — | code |  |
| `thu-tu-mon` | Thứ tự học các môn có điều kiện tiên quyết, nhỏ nhất theo từ điển | LT | — | code |  |
| `lien-thong-manh` | Số thành phần liên thông mạnh | LT | — | code |  |
| `dem-duong-ngan` | Số đường đi ngắn nhất từ s tới t, mod 10⁹+7 | LT | — | code |  |
| `lua-lan` | Thời gian lửa lan khắp lưới (nhiều nguồn) | LT | — | code |  |
| `quan-ma` | Số bước ít nhất của quân mã | LT | — | code |  |
| `floyd-truy-van` | Trả lời q truy vấn khoảng cách, n ≤ 200 | LT | — | code |  |

---

## SQL

Mọi bài SQL đều có bậc chạy câu lệnh thành animation qua `sqlviz` (spec §7.6), trừ loại LT. Câu ngoài phạm vi animation (CTE, hàm cửa sổ) vẫn chấm được.

Đáp án viết theo SQLite (spec §7.6). Bài ghi **A2** dùng cú pháp khác nhau giữa các hệ quản trị (hàm ngày); chốt xong giả định A2 mà môn dùng hệ khác thì sửa đáp án các bài đó. Bài dùng `LIMIT` cũng phải sửa nếu hệ đó là SQL Server hay Oracle. Câu hỏi có `LIMIT` hay "nhiều nhất", "cao nhất" đều ghi rõ cách xử lý khi bằng nhau, vì database ẩn cố ý có giá trị trùng.

### `sql.co-ban` — Cơ bản: SELECT, WHERE, ORDER BY, LIMIT, DISTINCT (Người 3)

3 giảng giải · 7 bài tập · 3 luyện tập.

| id | Câu hỏi | Loại | CSDL | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `loc-sinh-vien` | Sinh viên quê Hà Nội sinh từ năm 2005 | GG | `truong-hoc` | xem · dự đoán · code | Khái niệm 1. **Đã làm** ở GĐ2 task 3.12 |
| `sap-xep-gioi-han` | 5 sản phẩm đắt nhất; cùng giá thì id nhỏ trước | GG | `ban-hang` | xem · dự đoán · code | Khái niệm 2. `thu_tu: co` |
| `khong-trung` | Các thành phố có khách hàng | GG | `ban-hang` | xem · dự đoán · code | Khái niệm 3 |
| `sv-nu-lop` | Sinh viên nữ của một lớp cho trước | BT◆ | `truong-hoc` | dự đoán · code |  |
| `khoang-gia` | Sản phẩm giá từ 100 000 đến 500 000 và còn hàng | BT◆ | `ban-hang` | code | `BETWEEN` |
| `ho-nguyen` | Khách hàng họ Nguyễn | BT◆ | `ban-hang` | code | `LIKE` |
| `ba-sv-tre-nhat` | 3 sinh viên trẻ nhất; cùng ngày sinh thì theo họ tên, rồi theo id | BT◆ | `truong-hoc` | code | `thu_tu: co` |
| `mon-nhieu-tin-chi` | Môn từ 3 tín chỉ, sắp theo tín chỉ giảm dần, cùng tín chỉ thì theo mã môn | BT | `truong-hoc` | code | `thu_tu: co` |
| `don-thang-3` | Đơn hàng đặt trong tháng 3/2026 | BT | `ban-hang` | code | A2: hàm ngày |
| `dem-thanh-pho` | Số thành phố khác nhau có khách hàng | BT | `ban-hang` | code | `COUNT(DISTINCT …)` |
| `trang-thai-don` | Đơn hàng có trạng thái thuộc một danh sách | LT | `ban-hang` | code | `IN` |
| `vao-truoc-2020` | Nhân viên vào làm trước 2020 và lương dưới 15 triệu | LT | `nhan-su` | code |  |
| `trang-hai` | Dòng 11–20 của danh sách sản phẩm theo tên, cùng tên thì theo id | LT | `ban-hang` | code | `LIMIT … OFFSET`, `thu_tu: co` |

### `sql.gom-nhom` — Gộp nhóm: GROUP BY, hàm gộp, HAVING (Người 3)

2 giảng giải · 7 bài tập · 3 luyện tập.

| id | Câu hỏi | Loại | CSDL | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `si-so-lop` | Sĩ số từng lớp | GG | `truong-hoc` | xem · dự đoán · code | Khái niệm 7 |
| `diem-trung-binh-lop` | Điểm trung bình lần thi 1 của từng lớp, chỉ giữ lớp có trung bình trên 7 | GG | `truong-hoc` | xem · dự đoán · code | Khái niệm 8. Đủ FROM → JOIN → WHERE → GROUP BY → HAVING → SELECT. **Đã làm** ở GĐ3 task 3.14 |
| `so-sp-danh-muc` | Số sản phẩm mỗi danh mục | BT◆ | `ban-hang` | dự đoán · code |  |
| `tong-tien-don` | Tổng tiền từng đơn hàng | BT◆ | `ban-hang` | code |  |
| `diem-cao-thap` | Điểm cao nhất, thấp nhất của từng môn | BT◆ | `truong-hoc` | code |  |
| `lop-dong` | Lớp có trên 40 sinh viên | BT◆ | `truong-hoc` | dự đoán · code | `HAVING` |
| `khach-than-thiet` | Khách có từ 3 đơn trở lên | BT | `ban-hang` | code |  |
| `phong-luong-cao` | Phòng có lương trung bình trên 20 triệu | BT | `nhan-su` | code |  |
| `doanh-thu-thang` | Doanh thu theo tháng năm 2026 | BT | `ban-hang` | code | `thu_tu: co`. A2: hàm ngày |
| `gio-du-an` | Tổng giờ mỗi dự án, chỉ dự án từ 100 giờ | LT | `nhan-su` | code |  |
| `mon-truot-nhieu` | Môn có nhiều lần thi dưới 4 điểm nhất; bằng nhau thì in tất cả | LT | `truong-hoc` | code |  |
| `ty-le-dat` | Tỉ lệ phần trăm lần thi đạt (≥ 4) của từng môn, làm tròn 2 chữ số | LT | `truong-hoc` | code |  |

### `sql.null-case` — NULL và CASE (Người 3)

1 giảng giải · 3 bài tập · 1 luyện tập.

| id | Câu hỏi | Loại | CSDL | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `xep-loai` | Xếp loại điểm bằng CASE; sinh viên chưa thi hiện "Chưa thi" | GG | `truong-hoc` | xem · dự đoán · code | Khái niệm 12 |
| `chua-co-lop` | Sinh viên chưa được xếp lớp | BT◆ | `truong-hoc` | dự đoán · code | `IS NULL`, không phải `= NULL` |
| `chua-phan-phong` | Số nhân viên mỗi phòng; người chưa có phòng gom vào "Chưa phân" | BT◆ | `nhan-su` | code | `COALESCE` |
| `muc-ton-kho` | Phân loại tồn kho Hết / Ít / Nhiều | BT | `ban-hang` | code | `CASE` |
| `dem-co-diem` | Đếm sinh viên có điểm và chưa có điểm | LT | `truong-hoc` | code | `COUNT(*)` khác `COUNT(cột)` |

### `sql.dml` — Thay đổi dữ liệu: INSERT, UPDATE, DELETE (Người 3)

1 giảng giải · 5 bài tập · 1 luyện tập.

| id | Câu hỏi | Loại | CSDL | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `tang-luong` | Tăng 10% lương nhân viên phòng Kỹ thuật | GG | `nhan-su` | xem · dự đoán · code | Khái niệm 13. `loai_cau: dml` |
| `them-sv` | Thêm một sinh viên mới | BT◆ | `truong-hoc` | code | `dml` |
| `xoa-don-huy` | Xoá chi tiết của các đơn đã huỷ | BT◆ | `ban-hang` | code | `dml` |
| `tru-ton-kho` | Trừ tồn kho theo chi tiết của một đơn | BT | `ban-hang` | code | `dml`, có truy vấn con |
| `giam-gia` | Giảm 5% giá sản phẩm một danh mục | BT | `ban-hang` | code | `dml` |
| `chuyen-phong` | Chuyển mọi nhân viên của một phòng sang phòng khác | BT | `nhan-su` | code | `dml` |
| `them-tu-truy-van` | Chép sinh viên đạt học bổng sang bảng `hoc_bong` | LT | `truong-hoc` | code | `dml`, `INSERT … SELECT` |

### `sql.ket-noi` — Kết nối: JOIN (Người 1)

3 giảng giải · 14 bài tập · 6 luyện tập.

| id | Câu hỏi | Loại | CSDL | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `sinh-vien-lop` | Họ tên sinh viên kèm tên lớp | GG | `truong-hoc` | xem · dự đoán · code | Khái niệm 4 |
| `so-don-khach` | Mọi khách hàng kèm số đơn, kể cả khách chưa mua | GG | `ban-hang` | xem · dự đoán · code | Khái niệm 5 |
| `nhan-vien-quan-ly` | Nhân viên kèm tên người quản lý | GG | `nhan-su` | xem · dự đoán · code | Khái niệm 6 |
| `bang-diem` | Họ tên, tên môn, điểm của mọi lần thi | BT◆ | `truong-hoc` | dự đoán · code | Nối 3 bảng |
| `sp-danh-muc` | Sản phẩm kèm tên danh mục | BT◆ | `ban-hang` | code |  |
| `don-ha-noi` | Đơn hàng kèm tên khách, chỉ khách ở Hà Nội | BT◆ | `ban-hang` | code |  |
| `mon-chua-thi` | Môn chưa có ai thi | BT◆ | `truong-hoc` | dự đoán · code | `LEFT JOIN … IS NULL` |
| `lop-gvcn` | Lớp kèm tên giáo viên chủ nhiệm, kể cả lớp chưa có | BT◆ | `truong-hoc` | code |  |
| `cung-phong` | Các cặp nhân viên cùng phòng, mỗi cặp một lần | BT◆ | `nhan-su` | code | Tự nối |
| `hon-quan-ly` | Nhân viên lương cao hơn quản lý của mình | BT◆ | `nhan-su` | code | Tự nối |
| `danh-muc-cha` | Danh mục kèm tên danh mục cha | BT | `ban-hang` | code | Tự nối |
| `nv-du-an` | Nhân viên kèm dự án đang làm và số giờ | BT | `nhan-su` | code |  |
| `du-an-trong` | Dự án chưa có ai được phân công | BT | `nhan-su` | code |  |
| `phong-va-du-an` | Ghép phòng và dự án, giữ cả hai phía không có cặp | BT | `nhan-su` | code | `FULL JOIN` |
| `lich-thi` | Mọi cặp (lớp, môn) để lập lịch thi | BT | `truong-hoc` | code | `CROSS JOIN` |
| `sp-chua-ban` | Sản phẩm chưa từng được bán | BT | `ban-hang` | code |  |
| `tong-chi` | Tổng tiền mỗi khách đã chi, khách chưa mua hiện 0 | BT | `ban-hang` | code | `LEFT JOIN` + `COALESCE` |
| `don-nhieu-sp` | Đơn có từ 3 sản phẩm khác nhau, kèm tên khách | LT | `ban-hang` | code |  |
| `cung-que` | Cặp sinh viên cùng quê nhưng khác lớp | LT | `truong-hoc` | code |  |
| `doanh-thu-danh-muc-cha` | Doanh thu theo danh mục cha | LT | `ban-hang` | code |  |
| `mua-ca-hai` | Khách đã mua cả hai sản phẩm cho trước | LT | `ban-hang` | code |  |
| `du-an-phong-khac` | Nhân viên làm ở dự án không thuộc phòng mình | LT | `nhan-su` | code |  |
| `diem-cao-nhat-lan` | Điểm lần thi cao nhất của mỗi sinh viên ở mỗi môn | LT | `truong-hoc` | code |  |

### `sql.truy-van-con` — Truy vấn con (Người 2)

2 giảng giải · 9 bài tập · 4 luyện tập.

| id | Câu hỏi | Loại | CSDL | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `tren-trung-binh` | Nhân viên lương cao hơn lương trung bình công ty | GG | `nhan-su` | xem · dự đoán · code | Khái niệm 9 |
| `co-don-da-giao` | Khách hàng có ít nhất một đơn đã giao | GG | `ban-hang` | xem · dự đoán · code | Khái niệm 10. `EXISTS`, bảng ngoài ≤ 30 dòng |
| `sp-dat-nhat` | Sản phẩm có giá cao nhất; bằng nhau thì in tất cả | BT◆ | `ban-hang` | dự đoán · code | Truy vấn con một giá trị |
| `sv-khoa-cntt` | Sinh viên thuộc các lớp của khoa CNTT | BT◆ | `truong-hoc` | code | `IN` |
| `khach-chua-dat` | Khách chưa đặt đơn nào | BT◆ | `ban-hang` | code | `NOT IN` gặp NULL — có trong database ẩn |
| `cao-nhat-phong` | Nhân viên lương cao nhất trong phòng mình; bằng nhau thì in tất cả | BT◆ | `nhan-su` | code | Tương quan |
| `tren-tb-mon` | Lần thi có điểm trên trung bình của chính môn đó | BT◆ | `truong-hoc` | code | Tương quan |
| `lon-hon-moi-don` | Đơn có tổng tiền lớn hơn mọi đơn của một khách cho trước | BT | `ban-hang` | code | SQLite không có `> ALL`: đáp án dùng `> (SELECT MAX …)` |
| `ca-lop-deu-thi` | Môn mà mọi sinh viên của một lớp đều đã thi | BT | `truong-hoc` | code | `NOT EXISTS` lồng |
| `phong-dong-nhat` | Phòng có nhiều nhân viên nhất; bằng nhau thì in tất cả | BT | `nhan-su` | code |  |
| `ban-tren-tb` | Sản phẩm có tổng số lượng bán trên trung bình | BT | `ban-hang` | code |  |
| `quan-ly-nhieu` | Người quản lý từ 3 nhân viên trở lên | LT | `nhan-su` | code |  |
| `luong-thu-hai` | Lương cao thứ hai, không dùng LIMIT/OFFSET | LT | `nhan-su` | code |  |
| `mua-moi-danh-muc` | Khách đã mua ở mọi danh mục | LT | `ban-hang` | code | Phép chia |
| `du-an-vuot-ngan-sach` | Phòng có tổng ngân sách dự án vượt ngân sách phòng | LT | `nhan-su` | code |  |

### `sql.tap-hop` — Phép tập hợp: UNION, INTERSECT, EXCEPT (Người 2)

1 giảng giải · 5 bài tập · 2 luyện tập.

| id | Câu hỏi | Loại | CSDL | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `thi-ca-hai-mon` | Mã sinh viên đã thi cả hai môn CSDL và CTDL | GG | `truong-hoc` | xem · dự đoán · code | Khái niệm 11. `INTERSECT` |
| `moi-nguoi-trong-truong` | Họ tên mọi người trong trường: sinh viên và giảng viên | BT◆ | `truong-hoc` | dự đoán · code | `UNION` |
| `thi-a-chua-b` | Sinh viên đã thi CSDL nhưng chưa thi CTDL | BT◆ | `truong-hoc` | code | `EXCEPT` |
| `que-co-trung` | Danh sách quê quán có và không bỏ trùng | BT | `truong-hoc` | code | `UNION` so với `UNION ALL` |
| `ban-ca-hai-thang` | Sản phẩm bán được cả tháng 1 và tháng 2 | BT | `ban-hang` | code | `INTERSECT` |
| `nv-ngoai-du-an` | Nhân viên không tham gia dự án nào | BT | `nhan-su` | code | `EXCEPT` |
| `ton-va-het` | Sản phẩm tồn mà chưa bán, cùng sản phẩm đã bán mà hết hàng | LT | `ban-hang` | code |  |
| `gv-chu-nhiem-luong` | Giảng viên vừa chủ nhiệm lớp vừa có lương trên 20 triệu | LT | `truong-hoc` | code | `INTERSECT` |

### `sql.tong-hop` — Tổng hợp nhiều mệnh đề (chỉ chấm test) (Người 4)

0 giảng giải · 0 bài tập · 30 luyện tập.

| id | Câu hỏi | Loại | CSDL | Bậc | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `top-khach` | 3 khách chi nhiều nhất năm 2026, kèm tổng tiền; bằng tiền thì id nhỏ trước | LT | `ban-hang` | code | `thu_tu: co` |
| `doanh-thu-thanh-pho` | Doanh thu theo thành phố, chỉ đơn đã giao, giảm dần; bằng nhau thì theo tên thành phố | LT | `ban-hang` | code | `thu_tu: co` |
| `ban-chay-danh-muc` | Sản phẩm bán chạy nhất trong từng danh mục; bằng nhau thì in tất cả | LT | `ban-hang` | code |  |
| `mua-lai` | Khách đặt đơn trong cả quý 1 và quý 2 | LT | `ban-hang` | code | A2: hàm ngày |
| `ty-le-huy` | Tỉ lệ đơn huỷ theo tháng | LT | `ban-hang` | code | A2: hàm ngày |
| `don-tb-khach` | Giá trị đơn trung bình của từng khách có từ 2 đơn | LT | `ban-hang` | code |  |
| `ton-lau` | Sản phẩm còn tồn mà 90 ngày chưa bán | LT | `ban-hang` | code | A2: hàm ngày |
| `danh-muc-e` | Danh mục không bán được sản phẩm nào năm 2026 | LT | `ban-hang` | code |  |
| `don-lon-nhat` | Đơn có giá trị lớn nhất của mỗi khách; bằng nhau thì in tất cả | LT | `ban-hang` | code |  |
| `khach-moi` | Số khách mới theo tháng và số người trong đó đã mua | LT | `ban-hang` | code | A2: hàm ngày |
| `hoc-bong` | Sinh viên trung bình ≥ 8 và không môn nào dưới 5 | LT | `truong-hoc` | code |  |
| `thu-khoa-lop` | Sinh viên có trung bình cao nhất mỗi lớp; bằng nhau thì in tất cả | LT | `truong-hoc` | code |  |
| `mon-kho-nhat` | Môn có tỉ lệ trượt lần 1 cao nhất; bằng nhau thì in tất cả | LT | `truong-hoc` | code |  |
| `thi-lai-dat` | Sinh viên trượt lần 1 nhưng đạt lần 2, kèm tên môn | LT | `truong-hoc` | code |  |
| `tb-tin-chi` | Trung bình có trọng số tín chỉ, lấy điểm lần thi cao nhất | LT | `truong-hoc` | code |  |
| `lop-deu-thi` | Lớp mà mọi sinh viên đều đã thi ít nhất một môn | LT | `truong-hoc` | code |  |
| `que-dong-nhat` | Quê có nhiều sinh viên nhất trong từng khoa; bằng nhau thì in tất cả | LT | `truong-hoc` | code |  |
| `gvcn-lop-gioi` | Giảng viên chủ nhiệm lớp có trung bình cao hơn toàn trường | LT | `truong-hoc` | code |  |
| `chua-thi-bat-buoc` | Sinh viên chưa thi môn nào trong danh sách bắt buộc | LT | `truong-hoc` | code |  |
| `pho-diem` | Số lần thi theo khoảng điểm [0, 4), [4, 6.5), [6.5, 8), [8, 10] | LT | `truong-hoc` | code | `CASE` + `GROUP BY` |
| `tren-tb-phong` | Nhân viên lương trên trung bình phòng mình, kèm mức chênh | LT | `nhan-su` | code |  |
| `quan-ly-cap-hai` | Nhân viên có quản lý của quản lý thuộc phòng khác | LT | `nhan-su` | code |  |
| `du-an-lien-phong` | Dự án có nhiều nhân viên từ phòng khác nhất; bằng nhau thì in tất cả | LT | `nhan-su` | code |  |
| `gio-theo-phong` | Tổng giờ dự án theo phòng, phòng không ai làm hiện 0 | LT | `nhan-su` | code |  |
| `moi-du-an-phong` | Nhân viên tham gia mọi dự án của phòng mình | LT | `nhan-su` | code |  |
| `tham-nien` | Số nhân viên và lương trung bình theo năm vào làm | LT | `nhan-su` | code | A2: hàm ngày |
| `chi-phi-du-an` | Chi phí nhân công ước tính mỗi dự án | LT | `nhan-su` | code |  |
| `phong-vuot-luong` | Phòng có tổng lương vượt ngân sách | LT | `nhan-su` | code |  |
| `dong-bo-ton-kho` | Cập nhật tồn kho theo mọi đơn đã giao trong một ngày | LT | `ban-hang` | code | `dml`, truy vấn con tương quan |
| `don-dep-khach` | Xoá khách không có đơn và đăng ký trước 2024 | LT | `ban-hang` | code | `dml` |
