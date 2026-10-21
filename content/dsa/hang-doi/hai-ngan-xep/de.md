# Hàng đợi từ hai ngăn xếp

Dựng một **hàng đợi** chỉ bằng hai **ngăn xếp** (hai `list` của Python, chỉ dùng `append`, `pop()` ở cuối và xem phần tử cuối). Không dùng `collections.deque` hay `queue`.

Thực hiện *q* thao tác:

- `1 x`: thêm *x* vào cuối hàng đợi;
- `2`: lấy phần tử ở đầu hàng đợi ra và in nó; hàng đợi rỗng thì in −1;
- `3`: in phần tử ở đầu hàng đợi (không lấy ra); hàng đợi rỗng thì in −1.

Gợi ý: ngăn xếp `vao` nhận phần tử mới. Khi cần lấy hoặc xem đầu hàng mà ngăn xếp `ra` rỗng, đổ hết `vao` sang `ra` (thứ tự đảo lại): đỉnh của `ra` là đầu hàng. Mỗi phần tử chỉ bị đổ một lần nên tổng thời gian là O(*q*). Dùng `list.pop(0)` sẽ chậm vì mỗi lần mất O(*n*).

## Input

Dòng 1 là *q* (1 ≤ *q* ≤ 2·10⁵). *q* dòng sau, mỗi dòng một thao tác; |*x*| ≤ 10⁹.

## Output

Với mỗi thao tác `2` và `3`, in một dòng.

## Ví dụ

Input:

```text
6
1 5
1 7
3
2
2
2
```

Output:

```text
5
5
7
-1
```

Input:

```text
3
3
1 -4
3
```

Output:

```text
-1
-4
```
