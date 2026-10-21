# Tách xâu thành các từ

Cho từ điển gồm *m* từ và một xâu *s* viết liền không dấu cách. Đếm số cách tách *s* thành dãy các từ có trong từ điển (mỗi từ dùng bao nhiêu lần cũng được). In số cách chia lấy dư cho 10⁹ + 7.

Gợi ý: `f[i]` = số cách tách *i* ký tự đầu của *s*; `f[0] = 1`. Với mỗi *i*, thử mọi độ dài *L* ≤ 20: nếu `s[i − L : i]` có trong từ điển thì cộng `f[i − L]` vào `f[i]`. Dùng `set` để tra từ trong O(1).

## Input

Dòng 1 là *m* (1 ≤ *m* ≤ 1000). Dòng 2 là *m* từ khác nhau, mỗi từ gồm 1 tới 20 chữ thường. Dòng 3 là xâu *s* (1 ≤ |*s*| ≤ 5000), chỉ gồm chữ thường.

## Output

Một số: số cách tách, mod 10⁹ + 7.

## Ví dụ

Input:

```text
5
cat cats and sand dog
catsanddog
```

Output:

```text
2
```

Input:

```text
2
a aa
aaaa
```

Output:

```text
5
```

Ở ví dụ 1 có hai cách: `cats and dog` và `cat sand dog`. Ở ví dụ 2 có 5 cách.
