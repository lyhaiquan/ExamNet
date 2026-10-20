# Chèn vào cây nhị phân tìm kiếm

Cho *n* khoá đôi một khác nhau. Lần lượt chèn từng khoá theo đúng thứ tự cho trước vào một **cây nhị phân tìm kiếm** ban đầu rỗng, rồi in các khoá theo thứ tự **duyệt trước** (gốc → cây con trái → cây con phải).

Chèn khoá *k* vào cây con có gốc `nut`:

- `nut` là `None`: chỗ này còn trống, tạo nút mới `T.Nut(k)`;
- *k* nhỏ hơn `nut.khoa`: chèn vào cây con trái, `nut.trai = chen(nut.trai, k)`;
- ngược lại: chèn vào cây con phải, `nut.phai = chen(nut.phai, k)`.

Hàm trả về gốc của cây con sau khi chèn. Gốc của cả cây là `T.goc`.

Dùng cây `T` trong khung code: mỗi lần đọc `nut.khoa` là một lần xét nút, animation tô vàng nút đó nên thấy được đường đi từ gốc xuống chỗ chèn.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 2·10⁴). Dòng 2 là *n* khoá nguyên đôi một khác nhau, mỗi khoá có trị tuyệt đối không quá 10⁹. Các khoá xếp ngẫu nhiên nên cây không quá cao.

## Output

Một dòng: *n* khoá theo thứ tự duyệt trước, cách nhau một dấu cách.

## Ví dụ

Input:

```text
8
50 30 70 20 40 60 80 35
```

Output:

```text
50 30 20 40 35 70 60 80
```
