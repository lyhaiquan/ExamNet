# Đặt n quân hậu

Đếm số cách đặt *n* quân hậu lên bàn cờ *n* × *n* sao cho không hai quân nào ăn được nhau: không cùng hàng, không cùng cột, không cùng đường chéo.

**Quay lui theo từng hàng.** Mỗi hàng có đúng một quân hậu. `ds[r]` là cột của quân hậu ở hàng *r*. Hàm `dat(ds)` đặt quân cho hàng *r* = `len(ds)`:

- *r* = *n*: đã đặt đủ *n* quân, được một cách.
- Ngược lại thử từng cột *c* = 0, 1, …, *n* − 1. Ô (*r*, *c*) chưa bị quân nào khống chế thì đặt quân vào, gọi `dat(ds)` cho hàng sau, rồi **nhấc quân ra** để thử cột khác. Cộng số cách của mọi cột.

Kiểm một ô có bị khống chế không mà không phải duyệt lại cả bàn cờ: dùng ba mảng đánh dấu.

- `cot[c]`: cột *c* đã có quân.
- `cheo1[r + c]`: đường chéo xuống trái. Mọi ô trên đường này có cùng *r* + *c*.
- `cheo2[r - c + n]`: đường chéo xuống phải. Mọi ô trên đường này có cùng *r* − *c*.

Hàng nào không có ô đặt được dẫn tới lời giải (kết quả 0), gọi `viz.loai("…")` trước khi trả về: nhánh đó thành màu đỏ trên cây lời gọi.

## Input

Một số nguyên *n* (1 ≤ *n* ≤ 10).

## Output

Số cách đặt.

## Ví dụ

| Input | Output |
| --- | --- |
| `4` | `2` |
| `3` | `0` |
| `8` | `92` |
