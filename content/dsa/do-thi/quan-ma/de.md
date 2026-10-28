# Số bước ít nhất của quân mã

Trên bàn cờ *n* × *n* (hàng và cột đánh số 1…*n*), quân mã đứng ở ô (*x*₁, *y*₁). Mỗi bước, quân mã đi theo hình chữ L: 2 ô theo một hướng và 1 ô theo hướng vuông góc (8 cách đi, không ra ngoài bàn cờ). Tìm số bước ít nhất để tới ô (*x*₂, *y*₂).

Gợi ý: **BFS** trên các ô của bàn cờ, mỗi ô là một đỉnh, mỗi nước đi là một cạnh.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 500). Dòng 2 là *x*₁, *y*₁, *x*₂, *y*₂ (1 ≤ *x*, *y* ≤ *n*).

## Output

Một số: số bước ít nhất, hoặc −1 nếu không tới được.

## Ví dụ

Input:

```text
8
1 1 8 8
```

Output:

```text
6
```

Input:

```text
3
1 1 2 2
```

Output:

```text
-1
```
