# Đống nhị phân: thêm và lấy min

Cài một **hàng đợi ưu tiên** bằng **đống nhỏ nhất** (min-heap) trên mảng `h`: con của ô *i* là ô 2*i* + 1 và 2*i* + 2, cha của ô *i* là ô (*i* − 1) // 2, và mỗi nút **không lớn hơn** các con của nó. Vì vậy `h[0]` luôn là số nhỏ nhất. Thực hiện *q* lệnh:

- `them x`: thêm *x* vào **cuối** mảng (`h.append(x)`), rồi **vun lên**: khi ô đó nhỏ hơn cha của nó thì đổi chỗ với cha, đi lên tiếp.
- `lay`: in `h[0]` rồi bỏ nó đi. Lấy ô **cuối** ra (`h.pop()`), đặt vào ô 0 (nếu đống còn phần tử), rồi **vun xuống**: khi ô đó lớn hơn con nhỏ hơn của nó thì đổi chỗ với con đó, đi xuống tiếp. Đống rỗng thì in `RONG`.

Cây đầy đủ *n* nút cao ⌊log₂ *n*⌋, nên mỗi lệnh chỉ O(log *n*). Chỉ được thêm vào cuối, lấy ra ở cuối, đặt ô cuối lên ô 0 và đổi chỗ cha với con.

## Input

Dòng 1 là *q* (1 ≤ *q* ≤ 10⁵). *q* dòng sau, mỗi dòng `them x` (|*x*| ≤ 10⁹) hoặc `lay`.

## Output

Mỗi lệnh `lay` in một dòng.

## Ví dụ

Input:

```text
8
them 5
them 3
them 8
them 1
lay
lay
them 2
lay
```

Output:

```text
1
3
2
```
