# Băm địa chỉ mở, dò tuyến tính

Cài một tập hợp số nguyên không âm bằng **bảng băm địa chỉ mở** `T` có *m* ô. Mỗi ô hoặc **trống** (`None`), hoặc giữ **một** số, hoặc mang dấu **đã xoá** (`viz.XOA`). Thực hiện *q* lệnh:

- `them x`: thêm *x* (đã có thì thôi);
- `xoa x`: xoá *x* (không có thì thôi);
- `tim x`: in `YES` nếu *x* có trong tập, ngược lại `NO`.

**Dò tuyến tính:** số *x* bắt đầu ở ô `x % m`; ô đó đã bị số khác chiếm thì sang ô kế, kế nữa… (hết bảng thì quay về ô 0). Dò từ ô `x % m`:

- gặp ô giữ đúng *x*: đã có *x*;
- gặp ô **trống**: dừng, không có *x*;
- gặp ô **đã xoá** hay ô giữ số khác: dò tiếp.

Xoá không được làm ô trống trở lại: một số khác có thể đã đi **vượt qua** ô này khi được thêm vào; ô trống sẽ làm việc tìm số đó dừng sớm. Vì thế xoá chỉ ghi dấu `viz.XOA`.

Thêm *x* chưa có: đặt vào ô **đã xoá đầu tiên** gặp trên đường dò; nếu không gặp ô đã xoá nào thì đặt vào ô trống nơi dừng dò.

## Input

Dòng 1 gồm *m* và *q* (1 ≤ *m* ≤ 2·10⁵, 1 ≤ *q* ≤ 10⁵). *q* dòng sau, mỗi dòng một lệnh, 0 ≤ *x* ≤ 10⁹. Số lệnh `them` không quá *m* / 2: bảng luôn còn ô trống.

## Output

Mỗi lệnh `tim` in một dòng.

## Ví dụ

Input:

```text
7 8
them 10
them 17
them 3
tim 17
xoa 10
tim 17
them 24
tim 10
```

Output:

```text
YES
YES
NO
```

10 và 17 cùng ô 3: 17 dò sang ô 4, rồi 3 dò sang ô 5. Xoá 10 để lại dấu đã xoá ở ô 3, nên tìm 17 vẫn dò qua được. 24 cũng bắt đầu ở ô 3: ô 3 mang dấu đã xoá nên 24 được đặt vào đó.
