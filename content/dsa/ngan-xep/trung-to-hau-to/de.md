# Trung tố → hậu tố

Biểu thức **trung tố** viết toán tử ở giữa hai toán hạng, như `a+b*c`. Biểu thức **hậu tố** viết toán tử sau hai toán hạng của nó, như `abc*+`. Hậu tố không cần ngoặc và không cần quy tắc ưu tiên: máy tính đọc từ trái sang phải là tính được (bài sau).

Cho một biểu thức trung tố gồm các toán hạng là một chữ cái thường, bốn phép `+ - * /`, và ngoặc tròn. `*` và `/` ưu tiên hơn `+` và `-`. Các phép cùng mức tính từ trái sang phải: `a-b-c` là `(a-b)-c`. Hãy đổi sang hậu tố.

**Thuật toán** (dùng ngăn xếp `st` giữ các toán tử và ngoặc mở chưa xử lý, hàng `ra` giữ kết quả). Đọc từng ký tự *c*:

- *c* là chữ cái: đưa thẳng vào `ra`.
- *c* là `(`: đẩy vào `st`.
- *c* là `)`: lấy các toán tử trên `st` đưa vào `ra` cho tới khi gặp `(`; bỏ `(` đi.
- *c* là toán tử: khi đỉnh `st` là toán tử (không phải `(`) có mức ưu tiên **lớn hơn hoặc bằng** *c* thì lấy nó ra đưa vào `ra`; sau đó đẩy *c* vào `st`.

Hết biểu thức thì lấy hết các toán tử còn trên `st` đưa vào `ra`.

"Lớn hơn hoặc bằng" làm các phép cùng mức tính từ trái sang phải: với `a-b+c`, `-` ra trước `+`, được `ab-c+`.

## Input

Một dòng: biểu thức trung tố hợp lệ, không có dấu cách, dài không quá 2·10⁵ ký tự.

## Output

Biểu thức hậu tố, không có dấu cách.

## Ví dụ

| Input | Output |
| --- | --- |
| `a+b*(c-d)/e` | `abcd-*e/+` |
| `a-b+c` | `ab-c+` |
| `(a+b)*(c+d)` | `ab+cd+*` |
