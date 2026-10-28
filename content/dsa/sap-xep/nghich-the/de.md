# Đếm cặp nghịch thế

Cho dãy *a* gồm *n* số nguyên. Một **cặp nghịch thế** là cặp chỉ số (*i*, *j*) với *i* < *j* mà `a[i] > a[j]`.

Đếm số cặp nghịch thế của dãy. Số cặp nghịch thế cho biết dãy "lộn xộn" tới đâu: dãy tăng dần có 0 cặp, dãy giảm dần có nhiều nhất.

Với *n* tới 10⁵, cách thử mọi cặp (O(*n*²)) sẽ quá thời gian. Gợi ý: sắp xếp trộn, và đếm thêm số cặp nghịch thế lúc trộn hai nửa.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵). Dòng 2 là *n* số nguyên, mỗi số có trị tuyệt đối không quá 10⁹.

## Output

Một số: số cặp nghịch thế.

## Ví dụ

Input:

```text
5
3 1 2 5 4
```

Output:

```text
3
```

Input:

```text
4
2 2 1 1
```

Output:

```text
4
```

Ở ví dụ 1, các cặp nghịch thế là (3, 1), (3, 2), (5, 4). Ở ví dụ 2, hai số bằng nhau không tạo nghịch thế.
