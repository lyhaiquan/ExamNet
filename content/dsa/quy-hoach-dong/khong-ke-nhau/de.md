# Tổng lớn nhất không lấy hai số kề nhau

Cho dãy *n* số nguyên (có thể âm). Chọn một số phần tử, không chọn hai phần tử **kề nhau**, sao cho tổng lớn nhất. Được phép không chọn phần tử nào (tổng 0).

Gợi ý: `f[i]` = tổng lớn nhất khi chỉ xét *i* phần tử đầu. Hoặc bỏ phần tử thứ *i*: `f[i − 1]`; hoặc lấy nó: `f[i − 2] + a[i]`.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵). Dòng 2 là *n* số nguyên, mỗi số có trị tuyệt đối không quá 10⁹.

## Output

Một số: tổng lớn nhất.

## Ví dụ

Input:

```text
5
2 7 9 3 1
```

Output:

```text
12
```

Input:

```text
3
-1 -2 -3
```

Output:

```text
0
```
