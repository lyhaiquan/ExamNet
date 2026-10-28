# Đoạn dài nhất không có số lặp

Cho dãy *n* số nguyên. Tìm độ dài của **đoạn con liên tiếp** dài nhất mà mọi số trong đoạn đều khác nhau.

Gợi ý: **cửa sổ trượt**. Giữ đoạn [*l*, *r*] không có số lặp và từ điển `cuoi[v]` = vị trí gặp *v* gần nhất. Mở rộng *r*; nếu `a[r]` đã xuất hiện trong đoạn thì dời *l* tới ngay sau lần xuất hiện đó.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵). Dòng 2 là *n* số nguyên, mỗi số có trị tuyệt đối không quá 10⁹.

## Output

Một số: độ dài đoạn dài nhất.

## Ví dụ

Input:

```text
8
1 2 3 1 2 4 5 2
```

Output:

```text
5
```

Input:

```text
3
7 7 7
```

Output:

```text
1
```

Ở ví dụ 1, đoạn `3 1 2 4 5` dài 5.
