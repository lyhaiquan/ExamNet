# Giải Sudoku 4×4

Điền các số 1, 2, 3, 4 vào các ô trống của lưới 4 × 4 sao cho mỗi **hàng**, mỗi **cột** và mỗi **khối 2 × 2** (bốn khối ở bốn góc) đều có đủ bốn số 1, 2, 3, 4. Đề bảo đảm có đúng một cách điền.

**Quay lui.** Xét các ô trống theo thứ tự từ trái sang phải, từ trên xuống dưới. Hàm `dien(i, j)` điền ô trống (*i*, *j*):

- Thử lần lượt *d* = 1, 2, 3, 4. Nếu *d* chưa có ở hàng *i*, cột *j* và khối chứa ô (*i*, *j*) thì điền *d* vào, rồi điền ô trống kế tiếp. Ô vừa điền là ô trống cuối cùng thì đã xong.
- Điền tiếp thành công thì trả `True`. Không thì xoá *d* đi (quay lui) và thử số sau.
- Thử hết bốn số mà không được thì gọi `viz.loai("…")` rồi trả `False`: lời gọi đó thành màu đỏ trên cây lời gọi.

Ô (*i*, *j*) thuộc khối số `(i // 2) * 2 + j // 2`. Ghi các số đã có của từng hàng, cột, khối vào `hang`, `cot`, `khoi` (mỗi cái là 4 tập hợp) để kiểm nhanh, khỏi duyệt lại lưới.

## Input

4 dòng, mỗi dòng 4 ký tự: `1`–`4` là ô đã có số, `.` là ô trống.

## Output

4 dòng: lưới đã điền đủ.

## Ví dụ

Input:

```text
1...
..2.
.3..
...4
```

Output:

```text
1243
3421
4312
2134
```
