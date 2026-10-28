# Tổ tiên chung gần nhất

Cho cây có gốc là đỉnh 1, gồm *n* đỉnh. Trả lời *q* câu hỏi: với hai đỉnh *u*, *v*, tìm **tổ tiên chung gần nhất** (LCA) — đỉnh sâu nhất là tổ tiên của cả *u* và *v* (một đỉnh cũng là tổ tiên của chính nó).

Cây có thể là một đường dài 10⁵ đỉnh, nên leo từng bước lên cha cho mỗi câu hỏi sẽ quá thời gian. Gợi ý: **nhảy nhị phân**. Tính `len[j][v]` = tổ tiên cách *v* đúng 2ʲ bước. Đưa đỉnh sâu hơn lên cùng độ sâu với đỉnh kia, rồi cho cả hai cùng nhảy các bước 2ʲ từ lớn tới nhỏ khi tổ tiên còn khác nhau.

## Input

Dòng 1 là *n* và *q* (1 ≤ *n*, *q* ≤ 10⁵). Dòng 2 có *n* − 1 số: cha của đỉnh 2, 3, …, *n* (cha của đỉnh *i* luôn nhỏ hơn *i*; dòng trống nếu *n* = 1). *q* dòng sau, mỗi dòng hai đỉnh *u*, *v*.

## Output

*q* dòng, mỗi dòng LCA của cặp đỉnh tương ứng.

## Ví dụ

Input:

```text
7 4
1 1 2 2 3 5
4 6
7 4
6 3
5 5
```

Output:

```text
1
2
3
5
```

Input:

```text
1 1

1 1
```

Output:

```text
1
```
