# Tập con có tổng S

Cho *n* số nguyên dương và số *S*. Có chọn được một tập con (mỗi số dùng nhiều nhất một lần) có tổng đúng bằng *S* không?

Gợi ý: quy hoạch động `co[s]` = có tập con tổng *s*. Với mỗi số *x*, duyệt *s* **từ lớn về nhỏ**: `co[s] = co[s] or co[s − x]` (duyệt từ nhỏ lên sẽ dùng *x* nhiều lần).

## Input

Dòng 1 là *n* và *S* (1 ≤ *n* ≤ 100, 1 ≤ *S* ≤ 2·10⁴). Dòng 2 là *n* số nguyên dương, mỗi số không quá 2·10⁴.

## Output

`YES` nếu chọn được, `NO` nếu không.

## Ví dụ

Input:

```text
5 9
3 34 4 12 5
```

Output:

```text
YES
```

Input:

```text
3 30
3 34 4
```

Output:

```text
NO
```
