# Fibonacci có ghi nhớ (bảng nhớ là mảng)

Dãy Fibonacci: *F*(0) = 0, *F*(1) = 1, *F*(*i*) = *F*(*i* − 1) + *F*(*i* − 2). Cho *n*, tính *F*(*n*).

Đệ quy thẳng theo công thức thì *F*(*n* − 2) được tính lại hai lần, *F*(*n* − 3) ba lần, … : số lời gọi tăng theo hàm mũ (bài `de-quy.fibonacci` cho thấy cây lời gọi to thế nào). **Ghi nhớ:** tính xong *F*(*i*) thì ghi vào ô `nho[i]` của một mảng; lần sau cần *F*(*i*) thì lấy luôn từ mảng, khỏi gọi đệ quy nữa.

Hàm `fib(i)`:

- `nho[i]` khác −1 (đã tính): gọi `viz.cat("…")` để thấy nhánh bị cắt trên cây lời gọi, rồi trả `nho[i]`.
- Ngược lại tính *F*(*i*) (*i* < 2 thì *F*(*i*) = *i*, không thì `fib(i - 1) + fib(i - 2)`), **ghi vào `nho[i]`**, rồi trả về.

Xem mảng `nho` khi chạy: các ô được điền theo thứ tự 1, 0, 2, 3, …, *n*, từ nhỏ tới lớn. Đó là ý của quy hoạch động: nếu đã biết thứ tự điền, ta điền bảng bằng một vòng lặp từ dưới lên, không cần đệ quy (các bài sau).

## Input

Một số nguyên *n* (0 ≤ *n* ≤ 45).

## Output

*F*(*n*).

## Ví dụ

| Input | Output |
| --- | --- |
| `6` | `8` |
| `0` | `0` |
| `45` | `1134903170` |
