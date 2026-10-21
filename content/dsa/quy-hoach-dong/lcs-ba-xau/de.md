# Dãy con chung dài nhất của ba xâu

Cho ba xâu *A*, *B*, *C*. Tìm độ dài **dãy con chung** dài nhất của cả ba xâu (các ký tự không cần liền nhau nhưng giữ nguyên thứ tự).

Gợi ý: mở rộng bài LCS của hai xâu thành bảng ba chiều `f[i][j][k]`. Nếu `A[i − 1] = B[j − 1] = C[k − 1]` thì `f[i][j][k] = f[i − 1][j − 1][k − 1] + 1`; ngược lại lấy lớn nhất của ba ô bỏ bớt một ký tự ở một xâu.

## Input

Ba dòng là ba xâu *A*, *B*, *C*, mỗi xâu dài từ 1 tới 60, chỉ gồm chữ thường.

## Output

Một số: độ dài dãy con chung dài nhất.

## Ví dụ

Input:

```text
abcde
ace
xaybzce
```

Output:

```text
3
```

Input:

```text
abc
abc
def
```

Output:

```text
0
```
