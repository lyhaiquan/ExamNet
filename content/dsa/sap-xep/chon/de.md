# Sắp xếp chọn

Cho một dãy số nguyên. Sắp xếp dãy **tăng dần** bằng thuật toán **chọn**:

- Lượt thứ *i* (*i* = 0, 1, …, *n* − 2): tìm phần tử **nhỏ nhất** trong đoạn `a[i]`, `a[i+1]`, …, `a[n-1]`, rồi **đổi chỗ** nó với `a[i]`.
- Sau lượt *i*, *i* + 1 phần tử đầu đã đúng chỗ.

Mỗi lượt đổi chỗ **tối đa một lần**: so sánh thì nhiều, nhưng chỉ đổi chỗ khi đã biết phần tử nhỏ nhất ở đâu. Không dùng `sorted`, `list.sort` hay `min`.

## Input

Một dòng gồm *n* số nguyên cách nhau bởi dấu cách (1 ≤ *n* ≤ 1 000, mỗi số có trị tuyệt đối ≤ 10⁹).

## Output

Dãy đã sắp, các số cách nhau bởi dấu cách.

## Ví dụ

| Input | Output |
| --- | --- |
| `64 25 12 22 11` | `11 12 22 25 64` |
