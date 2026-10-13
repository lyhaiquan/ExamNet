# Bốn kiểu duyệt cây: trước, giữa, sau, theo mức

Khung code đã chèn *n* khoá vào một cây nhị phân tìm kiếm `T`. In các khoá theo bốn thứ tự, mỗi thứ tự một dòng:

1. **Duyệt trước** (pre-order): thăm nút, rồi duyệt cây con trái, rồi cây con phải.
2. **Duyệt giữa** (in-order): cây con trái, nút, cây con phải. Với cây nhị phân tìm kiếm, thứ tự này là các khoá **tăng dần**.
3. **Duyệt sau** (post-order): cây con trái, cây con phải, rồi mới thăm nút. Ví dụ dùng khi xoá cả cây: xoá con trước rồi mới xoá cha.
4. **Theo mức** (BFS): gốc, rồi các nút ở độ sâu 1 từ trái sang phải, rồi độ sâu 2… Ba cách đầu viết đệ quy; cách này dùng **hàng đợi** `q`: cho gốc vào; lặp lấy nút đầu hàng ra, thăm, cho con trái rồi con phải (nếu có) vào cuối hàng.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 2·10⁴). Dòng 2 gồm *n* khoá đôi một khác nhau, |khoá| ≤ 10⁹, thứ tự ngẫu nhiên.

## Output

Bốn dòng: duyệt trước, giữa, sau, theo mức.

## Ví dụ

Input:

```text
7
50 30 70 20 40 60 80
```

Output:

```text
50 30 20 40 70 60 80
20 30 40 50 60 70 80
20 40 30 60 80 70 50
50 30 70 20 40 60 80
```
