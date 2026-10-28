# Giải mã xâu số

Mỗi chữ cái được mã hoá thành một số: `A` → 1, `B` → 2, …, `Z` → 26. Một xâu chữ cái mã hoá xong viết liền thành một xâu chữ số, ví dụ `AB` → `12`, nhưng `L` cũng → `12`.

Cho xâu chữ số *s*, đếm số xâu chữ cái có thể đã tạo ra nó, chia lấy dư cho 10⁹ + 7. Chú ý chữ số `0` không tự đứng một mình được, và `05` không phải là mã của chữ nào.

Gợi ý: `f[i]` = số cách giải mã *i* chữ số đầu. Chữ số thứ *i* khác `0` thì cộng `f[i − 1]`; hai chữ số cuối tạo số từ 10 tới 26 thì cộng `f[i − 2]`.

## Input

Một dòng *s* gồm 1 tới 10⁵ chữ số.

## Output

Một số: số cách giải mã, mod 10⁹ + 7 (0 nếu không giải mã được).

## Ví dụ

Input:

```text
226
```

Output:

```text
3
```

Input:

```text
1201234
```

Output:

```text
3
```

Ở ví dụ 1 có ba cách: `BZ` (2 26), `VF` (22 6), `BBF` (2 2 6).
