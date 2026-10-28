# Đếm cặp có tổng bằng x

Cho dãy *n* số nguyên và số *x*. Đếm số cặp chỉ số (*i*, *j*) với *i* < *j* mà `a[i] + a[j] = x`.

Gợi ý: đi từ trái sang phải, dùng từ điển (bảng băm) đếm số lần mỗi giá trị đã gặp. Tới `a[j]`, số cặp mới bằng số lần đã gặp giá trị `x − a[j]`.

## Input

Dòng 1 là *n* và *x* (1 ≤ *n* ≤ 10⁵, |*x*| ≤ 2·10⁹). Dòng 2 là *n* số nguyên, mỗi số có trị tuyệt đối không quá 10⁹.

## Output

Một số: số cặp.

## Ví dụ

Input:

```text
5 6
1 5 3 3 5
```

Output:

```text
3
```

Input:

```text
4 2
1 1 1 1
```

Output:

```text
6
```
