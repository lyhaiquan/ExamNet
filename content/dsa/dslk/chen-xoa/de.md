# Chèn và xoá trên danh sách liên kết đơn

Cho một danh sách liên kết đơn và *q* lệnh, thực hiện lần lượt:

- `chen i x`: chèn nút mới giá trị *x* vào **vị trí** *i* (tính từ 0): sau lệnh, nút mới là nút thứ *i*. *i* = 0 là chèn vào đầu, *i* bằng độ dài là chèn vào cuối.
- `xoa i`: xoá nút ở vị trí *i*.

Danh sách liên kết không có chỉ số như mảng: muốn tới nút thứ *i* − 1 phải đi từ `ds.dau` theo `tiep` *i* − 1 bước. Nhưng khi đã đứng ở nút trước chỗ cần sửa (gọi là `p`), chèn hay xoá chỉ cần **đổi một hai mũi tên**, không phải dời phần tử nào:

- Chèn sau `p`: `moi.tiep = p.tiep` **trước**, rồi `p.tiep = moi`. Làm ngược thứ tự thì mất phần sau của danh sách.
- Xoá nút sau `p`: `p.tiep = p.tiep.tiep`.
- Ở vị trí 0 thì không có nút trước: sửa thẳng `ds.dau`.

Không dựng lại danh sách mới: mỗi lệnh `chen` tạo đúng một nút.

## Input

Dòng 1 gồm *n* và *q* (0 ≤ *n* ≤ 1000, 1 ≤ *q* ≤ 1000). Dòng 2 gồm *n* giá trị ban đầu (dòng trống nếu *n* = 0). *q* dòng sau, mỗi dòng một lệnh; vị trí trong lệnh luôn hợp lệ.

## Output

Các giá trị của danh sách sau mọi lệnh, cách nhau một dấu cách; danh sách rỗng thì in `RONG`.

## Ví dụ

Input:

```text
4 4
3 7 9 2
chen 0 5
chen 3 8
xoa 1
xoa 4
```

Output:

```text
5 7 8 9
```
