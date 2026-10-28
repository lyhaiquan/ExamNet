# Chia thành hai tập cân bằng

Chia *n* số nguyên dương thành hai tập (một tập có thể rỗng) sao cho hiệu giữa tổng hai tập nhỏ nhất. In hiệu đó.

Gợi ý: gọi *T* là tổng mọi số. Dùng quy hoạch động tìm mọi tổng *s* lấy được từ một tập con (`co[s] = True`). Đáp án là min |*T* − 2*s*| trên các *s* lấy được.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 100). Dòng 2 là *n* số nguyên dương, mỗi số không quá 1000.

## Output

Một số: hiệu nhỏ nhất.

## Ví dụ

Input:

```text
4
1 6 11 5
```

Output:

```text
1
```

Input:

```text
1
7
```

Output:

```text
7
```

Ở ví dụ 1, chia thành {1, 5, 6} và {11}: tổng 12 và 11, hiệu 1.
