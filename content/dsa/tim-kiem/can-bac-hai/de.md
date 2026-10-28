# Phần nguyên căn bậc hai

Với mỗi số tự nhiên *n*, in ⌊√*n*⌋: số nguyên *s* lớn nhất mà *s*·*s* ≤ *n*.

Không dùng hàm căn có sẵn (`math.isqrt`, `math.sqrt`, `pow`, toán tử `**`): hãy **tìm nhị phân** *s* trong đoạn [0, 10⁹ + 1]. Căn bằng số thực cũng sai khi *n* gần 10¹⁸ vì số thực không đủ chữ số.

## Input

Dòng 1 là *t* (1 ≤ *t* ≤ 10⁴). *t* dòng sau, mỗi dòng một số *n* (0 ≤ *n* ≤ 10¹⁸).

## Output

*t* dòng, mỗi dòng ⌊√*n*⌋ của số tương ứng.

## Ví dụ

Input:

```text
4
0
8
9
1000000000000000000
```

Output:

```text
0
2
3
1000000000
```

Input:

```text
2
99
999999999999999999
```

Output:

```text
9
999999999
```
