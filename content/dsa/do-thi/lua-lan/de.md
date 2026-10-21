# Lửa lan trên lưới

Cho lưới *r* × *c*. Mỗi ô là `F` (đang cháy), `.` (cỏ khô) hoặc `#` (đá, không cháy). Mỗi phút, lửa lan từ mọi ô đang cháy sang các ô cỏ **kề cạnh** (lên, xuống, trái, phải). Sau bao nhiêu phút thì mọi ô cỏ đều cháy?

Gợi ý: **BFS nhiều nguồn**: cho mọi ô `F` vào hàng đợi ngay từ đầu với thời gian 0. Thời gian cháy của mỗi ô là khoảng cách BFS tới ô `F` gần nhất.

## Input

Dòng 1 là *r*, *c* (1 ≤ *r*, *c* ≤ 500). *r* dòng sau, mỗi dòng *c* ký tự `F`, `.` hoặc `#`.

## Output

Một số: số phút để mọi ô cỏ cháy hết; 0 nếu không có ô cỏ nào; −1 nếu có ô cỏ không bao giờ cháy.

## Ví dụ

Input:

```text
3 4
F..#
.#..
...F
```

Output:

```text
2
```

Input:

```text
2 3
F#.
.#.
```

Output:

```text
-1
```
