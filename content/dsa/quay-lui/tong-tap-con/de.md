# Tập con có tổng bằng S (cắt nhánh)

Cho *n* số nguyên dương *a*₁, *a*₂, …, *a*ₙ và số *S*. Đếm số cách chọn ra một tập con các số (mỗi số chọn tối đa một lần) có tổng **đúng bằng** *S*. Hai cách khác nhau khi khác nhau ở vị trí được chọn, kể cả khi các số bằng nhau.

Ví dụ *a* = `3 1 4 2`, *S* = 6: có hai cách là {4, 2} và {3, 1, 2}.

**Quay lui.** Sắp *a* tăng dần. Hàm `thu(i, tong)` nghĩa là đã quyết xong *i* số đầu (lấy hay bỏ), tổng các số đã lấy là `tong`:

- `tong == S`: được một cách. Các số còn lại đều dương, lấy thêm số nào cũng vượt *S*, nên trả 1 luôn.
- Ngược lại thử hai nhánh: **lấy** `a[i]` (gọi `thu(i + 1, tong + a[i])`) và **bỏ** `a[i]` (gọi `thu(i + 1, tong)`); cộng số cách của hai nhánh.

Thử hết mọi tập con là 2ⁿ lá. **Cắt nhánh** để khỏi đi vào những nhánh chắc chắn không có kết quả:

1. `tong + a[i] > S`: *a* tăng dần nên lấy `a[i]` hay bất kỳ số nào sau nó cũng vượt *S*: trả 0.
2. `tong + (a[i] + a[i+1] + … + a[n-1]) < S`: lấy hết các số còn lại vẫn chưa đủ *S*: trả 0. Tính sẵn tổng hậu tố để không phải cộng lại mỗi lần.

Gọi `viz.cat("…")` ngay trước khi trả 0 ở hai chỗ cắt: nhánh đó thành màu xám trên cây lời gọi.

## Input

Dòng 1 gồm *n* và *S* (1 ≤ *n* ≤ 18, 1 ≤ *S* ≤ 10⁹). Dòng 2 gồm *n* số *a*ᵢ (1 ≤ *a*ᵢ ≤ 10⁸).

## Output

Một số nguyên: số cách chọn.

## Ví dụ

| Input | Output |
| --- | --- |
| `4 6`<br>`3 1 4 2` | `2` |
| `3 100`<br>`5 10 20` | `0` |
