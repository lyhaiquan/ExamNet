# Nhịp giá cổ phiếu

Cho giá cổ phiếu của *n* ngày liên tiếp. **Nhịp giá** của ngày *i* là số ngày liên tiếp kết thúc ở ngày *i* (tính cả ngày *i*) mà giá không vượt giá ngày *i*.

Tính nhịp giá của mọi ngày.

Gợi ý: **ngăn xếp đơn điệu** giữ các ngày có giá giảm dần. Tới ngày *i*, lấy ra mọi ngày có giá ≤ giá ngày *i*; ngày còn ở đỉnh là ngày gần nhất có giá cao hơn.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵). Dòng 2 là *n* số nguyên dương không quá 10⁹: giá các ngày.

## Output

Một dòng *n* số: nhịp giá của từng ngày.

## Ví dụ

Input:

```text
7
100 80 60 70 60 75 85
```

Output:

```text
1 1 1 2 1 4 6
```

Input:

```text
3
5 5 5
```

Output:

```text
1 2 3
```
