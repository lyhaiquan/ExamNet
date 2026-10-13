# Sắp xếp đếm

Cho một dãy số nguyên **không âm, không lớn**. Sắp xếp dãy **tăng dần** mà **không so sánh** hai phần tử nào với nhau.

Gọi *k* là số lớn nhất trong dãy. Dùng hai mảng phụ:

1. **Đếm:** mảng `dem` có *k* + 1 ô, `dem[v]` là số lần giá trị *v* xuất hiện. Duyệt `a`, gặp *x* thì tăng `dem[x]` thêm 1.
2. **Ghi kết quả:** duyệt *v* = 0, 1, …, *k*; ghi `dem[v]` lần số *v* vào mảng `kq` từ trái sang phải.

Hai bước đều là vòng lặp đơn: O(*n* + *k*). Nhanh hơn mọi cách sắp bằng so sánh khi *k* nhỏ, nhưng *k* lớn (ví dụ 10⁹) thì mảng `dem` quá lớn, không dùng được.

## Input

Một dòng gồm *n* số nguyên cách nhau bởi dấu cách (1 ≤ *n* ≤ 10⁵, 0 ≤ mỗi số ≤ 10⁵).

## Output

Dãy đã sắp, các số cách nhau bởi dấu cách.

## Ví dụ

| Input | Output |
| --- | --- |
| `4 2 2 8 3 3 1` | `1 2 2 3 3 4 8` |
