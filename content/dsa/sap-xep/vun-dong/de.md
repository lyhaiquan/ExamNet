# Sắp xếp vun đống

Cho một dãy số nguyên. Sắp xếp dãy **tăng dần** bằng thuật toán **vun đống** (heap sort), ngay trên mảng `a`, không dùng mảng phụ.

Mảng `a` được nhìn như một **cây nhị phân đầy đủ**: con của ô *i* là ô 2*i* + 1 và 2*i* + 2, cha của ô *i* là ô (*i* − 1) // 2. **Đống lớn nhất** (max-heap): mỗi nút không nhỏ hơn các con của nó, nên gốc `a[0]` là số lớn nhất.

**Vun xuống** ô *i* trong đống có *m* ô: so `a[i]` với hai con (con nào có chỉ số nhỏ hơn *m*); con lớn nhất lớn hơn `a[i]` thì đổi chỗ với nó rồi tiếp tục vun xuống từ chỗ con đó; không thì dừng.

1. **Dựng đống:** vun xuống lần lượt các ô *i* = *n* // 2 − 1, …, 1, 0 (các ô từ *n* // 2 trở đi là lá).
2. **Lấy dần số lớn nhất:** với *m* = *n* − 1, …, 1: đổi chỗ gốc `a[0]` với `a[m]` (số lớn nhất về cuối, đúng chỗ), đống còn *m* ô; vun xuống ô 0 trong đống *m* ô.

Gọi `a.con_tro("n", m)` mỗi khi đống còn *m* ô: hình cây chỉ vẽ *m* ô đầu, phần đã sắp ở cuối mảng tô xanh lá. Chỉ được đổi chỗ nút cha với nút con, hoặc gốc với ô cuối của đống.

## Input

Một dòng gồm *n* số nguyên cách nhau bởi dấu cách (1 ≤ *n* ≤ 1 000, mỗi số có trị tuyệt đối ≤ 10⁹).

## Output

Dãy đã sắp, các số cách nhau bởi dấu cách.

## Ví dụ

| Input | Output |
| --- | --- |
| `4 10 3 5 1 8` | `1 3 4 5 8 10` |
