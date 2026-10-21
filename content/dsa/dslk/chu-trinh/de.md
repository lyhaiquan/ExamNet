# Chu trình trong danh sách liên kết

Có *n* nút đánh số 1…*n*. Nút *i* trỏ tới nút `tiep[i]`, với `tiep[i] = 0` nghĩa là không trỏ đi đâu (`None`). Danh sách liên kết bắt đầu từ nút 1: 1 → `tiep[1]` → …

Đi theo danh sách từ nút 1, có thể gặp lại một nút đã đi qua: khi đó danh sách có **chu trình**. Hãy tìm nút đầu tiên của chu trình (nút đầu tiên được gặp lại) và độ dài chu trình.

Gợi ý: thuật toán **rùa và thỏ** của Floyd. Rùa đi 1 bước, thỏ đi 2 bước; nếu gặp nhau thì có chu trình. Đưa rùa về nút 1, cả hai cùng đi 1 bước: chỗ gặp nhau là nút đầu chu trình. Cách này chỉ cần O(1) bộ nhớ.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵). Dòng 2 là *n* số `tiep[1]` … `tiep[n]`, mỗi số từ 0 tới *n*.

## Output

Hai số: nút đầu chu trình và độ dài chu trình. Nếu không có chu trình, in `0 0`.

## Ví dụ

Input:

```text
6
2 3 4 5 6 3
```

Output:

```text
3 4
```

Input:

```text
3
3 0 2
```

Output:

```text
0 0
```

Ở ví dụ 1: 1 → 2 → 3 → 4 → 5 → 6 → 3, chu trình 3 → 4 → 5 → 6 → 3 bắt đầu ở nút 3, dài 4. Ở ví dụ 2: 1 → 3 → 2 → hết.
