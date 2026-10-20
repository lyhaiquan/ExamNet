# Sinh hoán vị

Cho *n*. In ra mọi hoán vị của các số 1, 2, …, *n* theo **thứ tự từ điển** (hoán vị nào có số đầu nhỏ hơn thì in trước; số đầu bằng nhau thì so số thứ hai, …), mỗi hoán vị một dòng, các số cách nhau một dấu cách.

**Quay lui.** Dựng hoán vị từ trái sang phải trong mảng `ds`. Hàm `thu(ds)`:

- `ds` đã đủ *n* số: in ra.
- Ngược lại thử lần lượt *x* = 1, 2, …, *n* mà chưa có trong `ds`: thêm *x* vào cuối (`ds.append(x)`), gọi `thu(ds)` để dựng tiếp, rồi **gỡ *x* ra** (`ds.pop()`) để thử số khác. Bước gỡ ra là "quay lui".

Thử *x* từ nhỏ tới lớn thì các hoán vị tự ra đúng thứ tự từ điển. Dùng mảng `dung` để biết *x* đã có trong `ds` chưa, khỏi phải tìm trong `ds`.

## Input

Một số nguyên *n* (1 ≤ *n* ≤ 8).

## Output

*n*! dòng, mỗi dòng một hoán vị.

## Ví dụ

Input:

```text
3
```

Output:

```text
1 2 3
1 3 2
2 1 3
2 3 1
3 1 2
3 2 1
```
