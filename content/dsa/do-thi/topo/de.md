# Sắp xếp tô-pô (Kahn)

Có *n* môn học (1 … *n*) và *m* điều kiện "phải học môn *u* trước môn *v*" (cạnh có hướng *u* → *v*). Xếp thứ tự học sao cho mọi điều kiện đều thoả. Nếu các điều kiện tạo thành vòng (*a* trước *b*, *b* trước *c*, *c* trước *a*) thì không xếp được: in −1.

**Thuật toán Kahn:** **bậc vào** của *v* là số môn phải học trước *v* (số cạnh đi vào *v*). Môn có bậc vào 0 thì học được ngay.

1. Tính bậc vào của mọi đỉnh. Cho các đỉnh bậc vào 0 vào hàng đợi `q`, **theo thứ tự số tăng dần**.
2. Lặp khi `q` còn đỉnh: lấy đỉnh *u* ở đầu hàng, xếp *u* vào kết quả. Với mỗi cạnh *u* → *v* (theo thứ tự cạnh trong input), giảm bậc vào của *v* đi 1; về 0 thì cho *v* vào cuối hàng.
3. Kết quả đủ *n* đỉnh là xong; thiếu là còn đỉnh nằm trên vòng (bậc vào không bao giờ về 0): in −1.

Một đồ thị có thể có nhiều thứ tự tô-pô đúng; bài này in đúng thứ tự mà các bước trên tạo ra.

## Input

Dòng 1 gồm *n* và *m* (1 ≤ *n* ≤ 2·10⁴, 0 ≤ *m* ≤ 5·10⁴). *m* dòng sau, mỗi dòng *u* *v* (cạnh *u* → *v*, *u* ≠ *v*, không trùng).

## Output

Thứ tự tô-pô trên một dòng, hoặc −1.

## Ví dụ

| Input | Output |
| --- | --- |
| `6 6`<br>`5 3`<br>`1 3`<br>`3 2`<br>`1 4`<br>`4 2`<br>`2 6` | `1 5 4 3 2 6` |
| `3 3`<br>`1 2`<br>`2 3`<br>`3 1` | `-1` |
