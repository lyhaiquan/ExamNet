# Fibonacci đệ quy có ghi nhớ

Dãy Fibonacci: *F*(0) = 0, *F*(1) = 1, *F*(*n*) = *F*(*n* − 1) + *F*(*n* − 2). Cho *n*, hãy tính *F*(*n*) bằng **hàm đệ quy** `fib(n)`.

Gọi đệ quy thẳng theo công thức thì `fib(n - 1)` và `fib(n - 2)` tính lại cùng những giá trị nhỏ rất nhiều lần, số lời gọi tăng theo cấp số nhân (`fib(30)` cần hơn một triệu lời gọi). Cách sửa là **ghi nhớ**: lưu kết quả vào bảng `nho`; lần sau gặp lại *n* đã có trong bảng thì trả ngay, không gọi tiếp.

Animation vẽ **cây lời gọi** của `fib`: mỗi lời gọi là một ô, lời gọi đang chạy tô vàng, đã trả về thì xanh lá kèm kết quả. Gọi `viz.cat("đã có trong bảng nhớ")` khi lấy kết quả từ bảng thì ô đó xám lại: thấy được nhánh bị cắt. Bên dưới là chế độ **chạy từng dòng**, với dòng đang chạy và các biến của lời gọi đang chạy.

Thử chạy cả bản không ghi nhớ với test `nho-2` để so hai cây lời gọi.

Bài chấm cả bằng luật: `fib` phải gọi đệ quy và có ghi nhớ (số lời gọi không quá 2*n* + 1).

## Input

Một số nguyên *n* (0 ≤ *n* ≤ 90).

## Output

*F*(*n*).

## Ví dụ

Input:

```text
6
```

Output:

```text
8
```
