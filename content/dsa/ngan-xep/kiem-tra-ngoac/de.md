# Kiểm tra dãy ngoặc hợp lệ

Cho một dãy chỉ gồm các dấu ngoặc `(`, `)`, `[`, `]`, `{`, `}`. Dãy **hợp lệ** khi mỗi ngoặc mở đều được đóng bằng đúng loại ngoặc của nó, và ngoặc mở sau thì phải đóng trước. Ví dụ `([]{()})` hợp lệ; `([)]` không hợp lệ vì `[` mở sau `(` nhưng `)` lại đóng trước.

Dùng **ngăn xếp** `st` trong khung code: gặp ngoặc mở thì đẩy vào (`st.push(c)`), gặp ngoặc đóng thì lấy ngoặc mở gần nhất ra (`st.pop()`) và kiểm tra có cùng loại không. Hết dãy mà ngăn xếp còn ngoặc mở thì cũng không hợp lệ.

## Input

Một dòng gồm *n* dấu ngoặc viết liền (1 ≤ *n* ≤ 10⁵).

## Output

`YES` nếu dãy hợp lệ, ngược lại `NO`.

## Ví dụ

| Input | Output |
| --- | --- |
| `([]{()})` | `YES` |
| `([)]` | `NO` |
| `((` | `NO` |
