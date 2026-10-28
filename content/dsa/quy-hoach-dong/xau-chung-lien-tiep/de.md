# Xâu con chung liên tiếp dài nhất

Cho hai xâu *A* và *B*. Tìm độ dài xâu con **liên tiếp** dài nhất xuất hiện trong cả hai xâu.

Gợi ý: `f[i][j]` = độ dài đoạn chung dài nhất **kết thúc** tại `A[i − 1]` và `B[j − 1]`. Nếu hai ký tự bằng nhau thì `f[i][j] = f[i − 1][j − 1] + 1`, ngược lại bằng 0. Đáp án là giá trị lớn nhất của bảng. Chỉ cần giữ hàng trước của bảng.

## Input

Dòng 1 là xâu *A*, dòng 2 là xâu *B*. Mỗi xâu dài từ 1 tới 1000, chỉ gồm chữ thường.

## Output

Một số: độ dài xâu con chung liên tiếp dài nhất (0 nếu không có ký tự chung).

## Ví dụ

Input:

```text
abcdxyz
xyzabcd
```

Output:

```text
4
```

Input:

```text
abc
def
```

Output:

```text
0
```
