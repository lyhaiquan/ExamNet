# Bảng băm dây chuyền

Cài một tập hợp số nguyên bằng **bảng băm dây chuyền** có *m* ô: số *x* nằm ở ô *h* = *x* mod *m*, mỗi ô là một danh sách (dây chuyền) các số cùng ô. Thực hiện lần lượt *q* lệnh:

- `them x`: thêm *x* vào tập (đã có thì thôi);
- `xoa x`: bỏ *x* khỏi tập (không có thì thôi);
- `tim x`: in `YES` nếu *x* đang có trong tập, ngược lại in `NO`.

Dùng bảng `T` trong khung code: `T[h]` là dây chuyền của ô *h*. Tìm bằng `x in T[h]`, thêm bằng `T[h].append(x)`, bỏ bằng `T[h].remove(x)`. Animation tô vàng từng số trong dây chuyền khi so với *x*, nên thấy được vì sao ô càng dài thì tìm càng lâu.

Không dùng `set`, `dict` hay `collections` có sẵn của Python. Bài chấm cả bằng luật: mỗi số được thêm phải nằm đúng ô *x* mod *m*.

## Input

Dòng 1 là *m* và *q* (1 ≤ *m* ≤ 10⁴, 1 ≤ *q* ≤ 10⁵). *q* dòng sau, mỗi dòng một lệnh `them x`, `xoa x` hoặc `tim x` (0 ≤ *x* ≤ 10⁹).

## Output

Với mỗi lệnh `tim`, in `YES` hoặc `NO` trên một dòng.

## Ví dụ

Input:

```text
5 9
them 12
them 7
them 22
tim 7
them 17
xoa 7
tim 7
tim 17
tim 3
```

Output:

```text
YES
NO
YES
NO
```
