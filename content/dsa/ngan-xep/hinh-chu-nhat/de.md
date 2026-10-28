# Hình chữ nhật lớn nhất trong biểu đồ cột

Biểu đồ cột có *n* cột đứng sát nhau, mỗi cột rộng 1, cột *i* cao `h[i]`. Tìm diện tích hình chữ nhật lớn nhất nằm gọn trong biểu đồ, các cạnh song song với trục.

Gợi ý: với mỗi cột, hình chữ nhật cao đúng bằng cột đó kéo dài tới cột thấp hơn gần nhất bên trái và bên phải. Dùng **ngăn xếp đơn điệu** (các cột tăng dần chiều cao) để tìm hai cột đó cho mọi cột trong O(*n*).

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵). Dòng 2 là *n* số `h[i]` (0 ≤ `h[i]` ≤ 10⁹).

## Output

Một số: diện tích lớn nhất.

## Ví dụ

Input:

```text
6
2 1 5 6 2 3
```

Output:

```text
10
```

Input:

```text
3
4 4 4
```

Output:

```text
12
```

Ở ví dụ 1, hình chữ nhật trên hai cột cao 5 và 6, chiều cao 5, diện tích 10.
