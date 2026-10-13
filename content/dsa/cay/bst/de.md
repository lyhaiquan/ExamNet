# Cây nhị phân tìm kiếm: chèn, tìm, xoá

**Cây nhị phân tìm kiếm** (BST): mọi khoá trong cây con trái của một nút nhỏ hơn khoá của nút, mọi khoá trong cây con phải lớn hơn. Thực hiện lần lượt *q* lệnh trên một cây ban đầu rỗng:

- `chen k`: chèn khoá *k* (đã có thì thôi). Đi từ gốc xuống: nhỏ hơn sang trái, lớn hơn sang phải, tới chỗ trống thì tạo nút.
- `tim k`: in `YES` nếu có khoá *k*, ngược lại `NO`. Cũng đi từ gốc xuống như chèn.
- `xoa k`: xoá khoá *k* (không có thì thôi). Tìm nút giữ *k*, rồi:
  - nút **không có con trái**: thay nút bằng cây con phải của nó (có thể là `None`);
  - nút **không có con phải**: thay nút bằng cây con trái;
  - nút **đủ hai con**: tìm nút **nhỏ nhất của cây con phải** (đi sang phải một bước rồi sang trái tới cùng), chép khoá của nó lên nút cần xoá, rồi xoá khoá đó khỏi cây con phải.

Sau mọi lệnh, in các khoá theo **thứ tự duyệt trước** (gốc, cây con trái, cây con phải), hoặc `RONG` nếu cây rỗng. Thứ tự này phụ thuộc hình dạng cây, nên phải xoá đúng cách trên.

## Input

Dòng 1 là *q* (1 ≤ *q* ≤ 2·10⁴). *q* dòng sau, mỗi dòng một lệnh, |*k*| ≤ 10⁹.

## Output

Mỗi lệnh `tim` in một dòng; dòng cuối là thứ tự duyệt trước.

## Ví dụ

Input:

```text
9
chen 50
chen 30
chen 70
chen 20
chen 40
chen 60
tim 40
xoa 30
tim 30
```

Output:

```text
YES
NO
50 40 20 70 60
```

Nút 30 đủ hai con: nhỏ nhất của cây con phải là 40, chép 40 lên chỗ 30 rồi xoá nút 40 cũ.
