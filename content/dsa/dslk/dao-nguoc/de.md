# Đảo ngược danh sách liên kết

Cho một danh sách liên kết đơn `ds` gồm *n* nút. Hãy đảo ngược danh sách **bằng cách đổi các con trỏ `tiep`**, không tạo nút mới, rồi in danh sách sau khi đảo.

Dùng ba biến trỏ:

1. `prev = None`, `cur = ds.dau`;
2. khi `cur` khác `None`: giữ nút sau `nxt = cur.tiep`, quay mũi tên của `cur` về nút trước `cur.tiep = prev`, rồi cả hai biến tiến lên một nút: `prev = cur`, `cur = nxt`;
3. cuối cùng nút đầu mới là `prev`: `ds.dau = prev`.

Animation vẽ tên các biến đang trỏ vào từng nút (`prev`, `cur`, `nxt`), nên thấy được từng mũi tên quay đầu. Bài chấm cả bằng luật: danh sách phải được đảo thật, in ngược mà không đổi con trỏ thì không qua.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵). Dòng 2 là *n* số nguyên, giá trị các nút từ đầu tới cuối.

## Output

Một dòng: giá trị các nút của danh sách sau khi đảo, từ đầu tới cuối.

## Ví dụ

Input:

```text
5
1 2 3 4 5
```

Output:

```text
5 4 3 2 1
```
