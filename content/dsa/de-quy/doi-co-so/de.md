# Đổi sang hệ cơ số b

Đổi số tự nhiên *n* sang hệ cơ số *b*. Các chữ số từ 10 tới 35 viết bằng chữ in hoa `A` … `Z`.

Viết bằng **đệ quy**: biểu diễn của *n* là biểu diễn của *n* // *b* (nếu *n* ≥ *b*) rồi viết thêm chữ số *n* mod *b* ở cuối. Không dùng `bin`, `oct`, `hex`, `format`.

## Input

Dòng 1 là *t* (1 ≤ *t* ≤ 10⁴). *t* dòng sau, mỗi dòng hai số *n* và *b* (0 ≤ *n* ≤ 10¹⁸, 2 ≤ *b* ≤ 36).

## Output

*t* dòng, mỗi dòng biểu diễn của *n* trong hệ cơ số *b*.

## Ví dụ

Input:

```text
4
10 2
255 16
0 7
35 36
```

Output:

```text
1010
FF
0
Z
```

Input:

```text
1
1000000000000000000 36
```

Output:

```text
7LIEEXZX4KXS
```
