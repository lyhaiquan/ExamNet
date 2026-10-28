# Hoán vị của dãy có số trùng

Cho dãy *n* số nguyên, có thể có số trùng nhau. Liệt kê mọi hoán vị **khác nhau** của dãy theo thứ tự từ điển tăng dần (so sánh từng số từ trái sang).

Gợi ý: sắp xếp dãy rồi **quay lui** chọn số cho từng vị trí. Ở cùng một vị trí, nếu số `a[i]` bằng số `a[i − 1]` và `a[i − 1]` chưa được dùng thì bỏ qua `a[i]`: chọn nó sẽ sinh lại hoán vị đã có.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 8). Dòng 2 là *n* số nguyên, mỗi số có trị tuyệt đối không quá 100.

## Output

Dòng đầu là số hoán vị. Mỗi dòng sau là một hoán vị, các số cách nhau một dấu cách.

## Ví dụ

Input:

```text
3
1 2 1
```

Output:

```text
3
1 1 2
1 2 1
2 1 1
```

Input:

```text
2
5 5
```

Output:

```text
1
5 5
```
