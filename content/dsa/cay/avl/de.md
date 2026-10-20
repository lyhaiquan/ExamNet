# Cây AVL: chèn và quay

Chèn các khoá 1, 2, 3, … theo thứ tự tăng vào cây nhị phân tìm kiếm thường thì cây thành một **đường thẳng**, tìm kiếm chậm như danh sách: O(*n*). **Cây AVL** giữ cây cân bằng: ở mọi nút, chiều cao hai cây con chênh nhau **không quá 1**. Khi đó cây cao không quá khoảng 1,44 log₂ *n*.

Chiều cao của cây rỗng là 0, của một nút là 1 + chiều cao lớn hơn trong hai cây con. Lưu chiều cao ở mỗi nút (`nut.cao`).

**Chèn:** chèn như cây nhị phân tìm kiếm (khoá đã có thì thôi). Trên đường đi ngược từ chỗ chèn về gốc, cập nhật chiều cao từng nút và tính độ lệch `lech = cao(trai) - cao(phai)`. Gặp nút lệch quá 1 thì **quay** để cân bằng lại. Có bốn trường hợp, với *k* là khoá vừa chèn:

- `lech > 1` và *k* < khoá con trái (**trái – trái**): quay phải tại nút.
- `lech > 1` và *k* > khoá con trái (**trái – phải**): quay trái tại con trái, rồi quay phải tại nút.
- `lech < -1` và *k* > khoá con phải (**phải – phải**): quay trái tại nút.
- `lech < -1` và *k* < khoá con phải (**phải – trái**): quay phải tại con phải, rồi quay trái tại nút.

**Quay phải** tại *y* (con trái là *x*): `y.trai = x.phai`, `x.phai = y`; *x* thành gốc của cây con này. Cập nhật chiều cao *y* trước rồi *x*. Quay trái đối xứng. Phép quay giữ đúng thứ tự trái nhỏ – phải lớn.

Sau khi chèn hết, in các khoá theo **thứ tự duyệt trước**.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 2·10⁴). Dòng 2 gồm *n* khoá, |khoá| ≤ 10⁹ (có thể đã sắp tăng, giảm, hay có khoá trùng).

## Output

Thứ tự duyệt trước của cây AVL.

## Ví dụ

| Input | Output |
| --- | --- |
| `6`<br>`10 20 30 40 50 25` | `30 20 10 25 40 50` |
| `3`<br>`3 2 1` | `2 1 3` |
