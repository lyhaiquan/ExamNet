# Hình vuông toàn số 1 lớn nhất

Cho ma trận *m* × *n* chỉ gồm 0 và 1. Tìm cạnh của hình vuông lớn nhất chỉ chứa số 1.

Gợi ý: `f[i][j]` = cạnh hình vuông toàn 1 lớn nhất có góc dưới phải tại ô (*i*, *j*). Ô bằng 1 thì `f[i][j] = 1 + min(f[i − 1][j], f[i][j − 1], f[i − 1][j − 1])`.

## Input

Dòng 1 là *m*, *n* (1 ≤ *m*, *n* ≤ 500). *m* dòng sau, mỗi dòng một xâu *n* ký tự `0` hoặc `1`.

## Output

Một số: cạnh hình vuông lớn nhất (0 nếu không có số 1 nào).

## Ví dụ

Input:

```text
4 5
10100
10111
11111
10010
```

Output:

```text
2
```

Input:

```text
2 2
00
00
```

Output:

```text
0
```
