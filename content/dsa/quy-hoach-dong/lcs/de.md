# Dãy con chung dài nhất (LCS)

Cho hai xâu *X* và *Y*. Một **dãy con** của một xâu có được bằng cách xoá đi một số ký tự (có thể không xoá) mà giữ nguyên thứ tự các ký tự còn lại. Hãy tìm độ dài của dãy con chung dài nhất của *X* và *Y*.

Ví dụ *X* = `ABCBDAB`, *Y* = `BDCABA` có dãy con chung dài nhất là `BCBA` (cũng có thể là `BDAB`), độ dài 4.

Dùng bảng `dp` trong khung code: `dp[i][j]` là độ dài dãy con chung dài nhất của *i* ký tự đầu của *X* và *j* ký tự đầu của *Y*. Hàng 0 và cột 0 bằng 0. Với *i*, *j* ≥ 1:

- nếu `X[i-1] == Y[j-1]` thì `dp[i][j] = dp[i-1][j-1] + 1`;
- ngược lại `dp[i][j] = max(dp[i-1][j], dp[i][j-1])`.

Đáp án là `dp[n][m]`.

## Input

Dòng 1 là *X*, dòng 2 là *Y*, chỉ gồm chữ cái in hoa (1 ≤ |*X*|, |*Y*| ≤ 300).

## Output

Một số nguyên: độ dài dãy con chung dài nhất.

## Ví dụ

Input:

```text
ABCBDAB
BDCABA
```

Output:

```text
4
```

Input:

```text
AGGTAB
GXTXAYB
```

Output:

```text
4
```
