# Thứ tự học các môn

Có *n* môn học đánh số 1…*n* và *m* điều kiện tiên quyết dạng `a b`: phải học môn *a* trước môn *b*. Tìm một thứ tự học hết *n* môn thoả mọi điều kiện. Nếu có nhiều thứ tự, in thứ tự **nhỏ nhất theo từ điển** (so từng số từ trái sang). Nếu các điều kiện tạo vòng thì không có thứ tự nào.

Gợi ý: **Kahn** với **đống nhỏ nhất**. Đếm số môn tiên quyết còn thiếu của mỗi môn; luôn lấy ra môn có số nhỏ nhất trong các môn đã đủ điều kiện, rồi giảm bộ đếm của các môn sau nó.

## Input

Dòng 1 là *n* và *m* (1 ≤ *n* ≤ 10⁵, 0 ≤ *m* ≤ 2·10⁵). *m* dòng sau, mỗi dòng hai số *a*, *b* (*a* ≠ *b*).

## Output

Một dòng *n* số: thứ tự học, hoặc −1 nếu không có thứ tự nào.

## Ví dụ

Input:

```text
5 4
2 1
4 3
2 3
5 4
```

Output:

```text
2 1 5 4 3
```

Input:

```text
3 3
1 2
2 3
3 1
```

Output:

```text
-1
```
