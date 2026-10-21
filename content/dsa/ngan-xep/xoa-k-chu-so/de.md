# Xoá k chữ số

Cho số tự nhiên *s* có *n* chữ số và số *k*. Xoá đúng *k* chữ số của *s* (các chữ số còn lại giữ nguyên thứ tự) để được số **nhỏ nhất**. In số đó, bỏ các chữ số 0 ở đầu; nếu không còn chữ số nào hoặc toàn số 0 thì in `0`.

Gợi ý: tham lam với **ngăn xếp**. Đi từ trái sang phải; khi chữ số ở đỉnh ngăn xếp lớn hơn chữ số đang xét và còn được xoá thì lấy đỉnh ra (xoá nó). Hết dãy mà còn lượt xoá thì xoá ở cuối.

## Input

Dòng 1 là *s* (1 ≤ *n* ≤ 10⁵, *s* không có chữ số 0 ở đầu trừ khi *s* = 0). Dòng 2 là *k* (0 ≤ *k* ≤ *n*).

## Output

Một số: số nhỏ nhất còn lại.

## Ví dụ

Input:

```text
1432219
3
```

Output:

```text
1219
```

Input:

```text
10200
1
```

Output:

```text
200
```

Ở ví dụ 1, xoá 4, 3, 2 được 1219. Ở ví dụ 2, xoá chữ số 1 được 0200, tức 200.
