# Tính biểu thức hậu tố

Trong biểu thức **hậu tố**, mỗi toán tử đứng sau hai toán hạng của nó: `5 1 2 + 4 * + 3 -` nghĩa là `5 + (1 + 2) * 4 - 3` = 14. Không cần ngoặc, không cần quy tắc ưu tiên.

Cho một biểu thức hậu tố gồm các số nguyên không âm và ba phép `+ - *`, các phần tử cách nhau một dấu cách. Hãy tính giá trị.

**Thuật toán** (ngăn xếp `st` giữ các giá trị chờ dùng). Đọc từng phần tử từ trái sang phải:

- Gặp số: đẩy vào `st`.
- Gặp toán tử: lấy ra hai giá trị. Giá trị lấy ra **trước** là toán hạng **phải** *b*, lấy ra sau là toán hạng trái *a*. Tính *a* op *b* rồi đẩy kết quả vào `st`.

Hết biểu thức thì `st` còn đúng một giá trị: đó là kết quả. Lấy nhầm thứ tự *a*, *b* thì phép `-` sai dấu: `7 2 -` là 7 − 2 = 5, không phải 2 − 7.

## Input

Một dòng gồm không quá 10⁵ phần tử. Mỗi số từ 0 tới 99. Biểu thức hợp lệ, mọi giá trị trung gian có trị tuyệt đối không quá 10¹⁸.

## Output

Giá trị của biểu thức.

## Ví dụ

| Input | Output |
| --- | --- |
| `5 1 2 + 4 * + 3 -` | `14` |
| `7 2 -` | `5` |
| `2 3 4 * 5 - -` | `-5` |
