# Truy vấn khoảng cách (Floyd)

Cho đồ thị **có hướng** *n* đỉnh, *m* cạnh có trọng số không âm, và *q* câu hỏi: khoảng cách ngắn nhất từ *u* tới *v* là bao nhiêu?

*n* nhỏ nhưng *q* lớn, nên tính sẵn khoảng cách giữa **mọi cặp đỉnh** một lần bằng **Floyd–Warshall**, rồi trả lời mỗi câu hỏi bằng cách tra bảng. Floyd: với mỗi đỉnh trung gian *k*, với mọi *i*, *j*: `d[i][j] = min(d[i][j], d[i][k] + d[k][j])`.

## Input

Dòng 1 là *n*, *m*, *q* (1 ≤ *n* ≤ 150, 0 ≤ *m* ≤ 10⁴, 1 ≤ *q* ≤ 10⁵). *m* dòng sau, mỗi dòng `u v w` (0 ≤ *w* ≤ 10⁶; có thể có nhiều cạnh giữa hai đỉnh). *q* dòng sau, mỗi dòng hai đỉnh *u*, *v*.

## Output

*q* dòng, mỗi dòng khoảng cách ngắn nhất từ *u* tới *v*, hoặc −1 nếu không tới được. Khoảng cách từ một đỉnh tới chính nó là 0.

## Ví dụ

Input:

```text
4 4 3
1 2 5
2 3 2
1 3 9
3 4 1
1 3
4 1
2 2
```

Output:

```text
7
-1
0
```

Input:

```text
2 0 1
1 2
```

Output:

```text
-1
```
