# Bài toán Josephus

*n* người đứng thành vòng tròn, đánh số 1…*n* theo chiều kim đồng hồ. Bắt đầu đếm từ người 1, đếm tới người thứ *k* thì người đó ra khỏi vòng; tiếp tục đếm từ người kế tiếp, lại loại người thứ *k*… cho tới khi hết người.

In thứ tự những người bị loại.

Gợi ý: mô phỏng bằng **hàng đợi**. Lấy người ở đầu hàng đưa xuống cuối hàng *k* − 1 lần, rồi lấy người ở đầu ra: đó là người bị loại.

## Input

Một dòng gồm *n* và *k* (1 ≤ *n*, *k* ≤ 1000).

## Output

Một dòng *n* số: số thứ tự của người bị loại, theo thứ tự bị loại.

## Ví dụ

Input:

```text
7 3
```

Output:

```text
3 6 2 7 5 1 4
```

Input:

```text
5 1
```

Output:

```text
1 2 3 4 5
```
