# Sắp xếp nhanh, phân hoạch Lomuto

Cho một dãy số nguyên. Sắp xếp dãy **tăng dần** bằng thuật toán **sắp xếp nhanh**. Hàm `nhanh(lo, hi)` sắp đoạn `a[lo..hi]`:

1. Đoạn có không quá một phần tử thì đã sắp: trả về.
2. **Phân hoạch Lomuto:** lấy `chot = a[hi]` (phần tử cuối). Con trỏ *i* bắt đầu ở `lo`: mọi ô trước *i* đều nhỏ hơn `chot`. Cho *j* chạy từ `lo` tới `hi - 1`: gặp `a[j] < chot` thì **đổi chỗ** `a[i]` với `a[j]` rồi tăng *i*. Cuối cùng đổi chỗ `a[i]` với `a[hi]`: chốt về đúng chỗ *i*, bên trái nhỏ hơn nó, bên phải không nhỏ hơn.
3. Gọi `nhanh(lo, i - 1)` và `nhanh(i + 1, hi)`.

Chỉ đổi chỗ, không ghi lẻ ô nào. Trung bình O(*n* log *n*); dãy đã sắp sẵn thì chốt luôn là số lớn nhất, cây lời gọi lệch hẳn một bên: O(*n*²). Khung code đã nâng giới hạn đệ quy cho trường hợp đó.

## Input

Một dòng gồm *n* số nguyên cách nhau bởi dấu cách (1 ≤ *n* ≤ 1 000, mỗi số có trị tuyệt đối ≤ 10⁹).

## Output

Dãy đã sắp, các số cách nhau bởi dấu cách.

## Ví dụ

| Input | Output |
| --- | --- |
| `10 80 30 90 40 50 70` | `10 30 40 50 70 80 90` |
