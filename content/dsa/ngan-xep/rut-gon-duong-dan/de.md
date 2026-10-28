# Rút gọn đường dẫn

Cho các đường dẫn tuyệt đối kiểu Unix (bắt đầu bằng `/`). Rút gọn mỗi đường dẫn về dạng chuẩn:

- nhiều dấu `/` liền nhau coi như một;
- `.` là thư mục hiện tại: bỏ đi;
- `..` là lên thư mục cha; ở thư mục gốc `/` thì vẫn ở gốc;
- mọi tên khác (kể cả `...`) là tên thư mục;
- kết quả bắt đầu bằng `/`, các tên cách nhau đúng một `/`, không có `/` ở cuối (trừ khi kết quả là `/`).

Gợi ý: tách theo `/` rồi dùng **ngăn xếp** tên thư mục: gặp tên thì đẩy vào, gặp `..` thì lấy ra.

## Input

Dòng 1 là *t* (1 ≤ *t* ≤ 1000). *t* dòng sau, mỗi dòng một đường dẫn chỉ gồm chữ thường, chữ số, `.`, `_`, `/`, bắt đầu bằng `/`. Tổng độ dài các đường dẫn không quá 2·10⁵.

## Output

*t* dòng, mỗi dòng một đường dẫn đã rút gọn.

## Ví dụ

Input:

```text
4
/home/
/a/./b/../../c/
/../
/home//lan/.../tai_lieu
```

Output:

```text
/home
/c
/
/home/lan/.../tai_lieu
```

Input:

```text
1
/
```

Output:

```text
/
```
