# Liệt kê xâu nhị phân

Liệt kê mọi xâu nhị phân độ dài *n* (chỉ gồm chữ số `0` và `1`) **không có hai chữ số `1` đứng liền nhau**, theo thứ tự từ điển.

Gợi ý: **đệ quy** dựng xâu từng ký tự. Ở mỗi vị trí luôn thử `0` trước rồi tới `1`; chỉ được đặt `1` khi ký tự trước đó không phải `1`.

## Input

Một dòng *n* (1 ≤ *n* ≤ 20).

## Output

Dòng đầu là số xâu. Mỗi dòng sau là một xâu, theo thứ tự từ điển.

## Ví dụ

Input:

```text
3
```

Output:

```text
5
000
001
010
100
101
```

Input:

```text
1
```

Output:

```text
2
0
1
```
