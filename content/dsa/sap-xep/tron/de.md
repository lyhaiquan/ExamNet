# Sắp xếp trộn

Cho một dãy số nguyên. Sắp xếp dãy **tăng dần** bằng thuật toán **trộn** (chia để trị). Hàm `tron(trai, phai)` sắp đoạn `a[trai..phai]`:

1. Đoạn có một phần tử thì đã sắp: trả về.
2. **Chia** đôi tại `giua = (trai + phai) // 2`, gọi `tron(trai, giua)` và `tron(giua + 1, phai)`: hai nửa đã sắp.
3. **Trộn** hai nửa: hai con trỏ *i*, *j* ở đầu hai nửa; mỗi bước lấy số nhỏ hơn trong `a[i]`, `a[j]` cho vào danh sách tạm (bằng nhau thì lấy bên trái); một nửa hết thì lấy nốt nửa kia. Cuối cùng chép danh sách tạm đè lên `a[trai..phai]`.

Mỗi tầng của cây lời gọi trộn tổng cộng *n* phần tử, cây có khoảng log₂ *n* tầng: O(*n* log *n*).

## Input

Một dòng gồm *n* số nguyên cách nhau bởi dấu cách (1 ≤ *n* ≤ 1 000, mỗi số có trị tuyệt đối ≤ 10⁹).

## Output

Dãy đã sắp, các số cách nhau bởi dấu cách.

## Ví dụ

| Input | Output |
| --- | --- |
| `38 27 43 3 9 82 10` | `3 9 10 27 38 43 82` |
