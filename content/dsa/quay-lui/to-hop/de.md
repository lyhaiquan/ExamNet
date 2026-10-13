# Sinh tổ hợp chập k

Cho *n* và *k*. In ra mọi cách chọn *k* số khác nhau trong 1, 2, …, *n* (không tính thứ tự chọn). Mỗi cách in một dòng, các số **tăng dần**, cách nhau một dấu cách; các dòng theo **thứ tự từ điển**.

**Quay lui.** Vì các số trong một dòng tăng dần, số chọn sau luôn lớn hơn số chọn trước. Hàm `thu(bat_dau, ds)`: `ds` là các số đã chọn, số chọn tiếp phải từ `bat_dau` trở lên.

- `ds` đã đủ *k* số: in ra.
- Ngược lại thử *x* = `bat_dau`, `bat_dau + 1`, …, *n*: thêm *x* vào `ds`, gọi `thu(x + 1, ds)`, rồi gỡ *x* ra.

**Cắt nhánh:** còn `n - bat_dau + 1` số để chọn mà ít hơn số còn thiếu `k - len(ds)` thì có chọn hết cũng không đủ *k* số: gọi `viz.cat("…")` rồi trả về ngay (nhánh màu xám trên cây lời gọi).

## Input

Hai số *n* và *k* (1 ≤ *k* ≤ *n* ≤ 18).

## Output

Mỗi tổ hợp một dòng.

## Ví dụ

Input:

```text
4 2
```

Output:

```text
1 2
1 3
1 4
2 3
2 4
3 4
```
