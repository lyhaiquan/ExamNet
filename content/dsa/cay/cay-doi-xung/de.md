# Cây đối xứng gương

Một cây nhị phân là **đối xứng gương** nếu nhìn trong gương (đổi trái thành phải ở mọi nút) vẫn được đúng cây đó, kể cả khoá của các nút. Nói cách khác, cây con trái của gốc là ảnh gương của cây con phải.

Kiểm xem cây đã cho có đối xứng gương không.

Gợi ý: so từng cặp nút (*x*, *y*) bắt đầu từ (con trái của gốc, con phải của gốc): hai nút cùng rỗng thì được; một rỗng một không, hoặc khác khoá, thì không đối xứng; còn lại so tiếp (trái của *x*, phải của *y*) và (phải của *x*, trái của *y*). Dùng hàng đợi hay ngăn xếp các cặp thay cho đệ quy vì cây có thể sâu 10⁵ tầng.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵), số nút. *n* dòng sau, dòng thứ *i* gồm ba số `khoa trai phai`: khoá của nút *i* (|khoá| ≤ 10⁹), số thứ tự con trái và con phải của nút *i* (0 nếu không có). Nút 1 là gốc.

## Output

`YES` nếu cây đối xứng gương, `NO` nếu không.

## Ví dụ

Input:

```text
7
1 2 3
2 4 5
2 6 7
3 0 0
4 0 0
4 0 0
3 0 0
```

Output:

```text
YES
```

Input:

```text
3
1 2 3
2 0 0
3 0 0
```

Output:

```text
NO
```
