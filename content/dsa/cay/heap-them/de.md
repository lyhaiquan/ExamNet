# Thêm vào heap nhỏ nhất

**Heap nhỏ nhất** là một mảng `h` mà mỗi ô `h[i]` không lớn hơn hai ô con của nó, `h[2i+1]` và `h[2i+2]`. Vẽ mảng thành cây nhị phân (ô 0 là gốc, con của ô *i* là ô 2*i*+1 và 2*i*+2) thì cha luôn không lớn hơn con, nên phần tử nhỏ nhất nằm ở gốc.

Cho *n* số. Lần lượt thêm từng số vào heap `h` ban đầu rỗng theo cách sau, rồi in mảng `h` cuối cùng:

1. thêm số *x* vào cuối mảng: `h.append(x)`;
2. **vun lên**: gọi *i* là vị trí vừa thêm. Khi *i* > 0 và ô cha `h[(i - 1) // 2]` lớn hơn `h[i]` thì đổi chỗ hai ô, rồi *i* lấy vị trí của cha.

Không dùng `heapq`: hãy tự vun lên để animation thấy số mới nổi dần lên gốc.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵). Dòng 2 là *n* số nguyên, mỗi số có trị tuyệt đối không quá 10⁹.

## Output

Một dòng: mảng `h` sau khi thêm hết *n* số, các phần tử cách nhau một dấu cách.

## Ví dụ

Input:

```text
6
5 3 8 1 9 2
```

Output:

```text
1 3 2 5 9 8
```
