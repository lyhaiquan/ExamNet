# Tìm từ trong lưới chữ

Cho lưới chữ cái *m* × *n* và một số từ. Với mỗi từ, kiểm xem có viết được từ đó bằng một đường đi trong lưới không: bắt đầu ở một ô bất kỳ, mỗi bước sang ô **kề cạnh** (lên, xuống, trái, phải), mỗi ô dùng **không quá một lần**, các chữ cái trên đường đi ghép lại đúng bằng từ.

Gợi ý: **quay lui** từ mỗi ô có chữ cái đầu của từ: đánh dấu ô đang dùng, thử bốn hướng cho chữ cái kế tiếp, quay về thì bỏ đánh dấu.

## Input

Dòng 1 là *m*, *n* (1 ≤ *m*, *n* ≤ 5). *m* dòng sau, mỗi dòng *n* chữ thường viết liền. Dòng tiếp là *q* (1 ≤ *q* ≤ 5). *q* dòng sau, mỗi dòng một từ gồm 1 tới 9 chữ thường.

## Output

*q* dòng, mỗi dòng `YES` nếu tìm được từ tương ứng, `NO` nếu không.

## Ví dụ

Input:

```text
3 4
abce
sfcs
adee
3
abcced
see
abcb
```

Output:

```text
YES
YES
NO
```

Input:

```text
1 1
a
2
a
aa
```

Output:

```text
YES
NO
```
