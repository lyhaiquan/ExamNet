# Hàng đợi vòng trên mảng

Cài một **hàng đợi** (vào ở cuối, ra ở đầu) chứa được tối đa *k* phần tử, chỉ dùng một mảng `a` cố định *k* ô.

Nếu mỗi lần lấy ra lại dời cả mảng lên một ô thì mất O(*k*). Thay vào đó, giữ chỉ số `dau` của phần tử đầu hàng và số phần tử `so`. Lấy ra chỉ cần tăng `dau`. Thêm vào thì ghi vào ô ngay sau phần tử cuối. Hai việc đều O(1).

Đi tới ô cuối mảng thì **quay vòng** về ô 0: ô sau ô *i* là `(i + 1) % k`. Phần tử cuối ở ô `(dau + so - 1) % k`, ô sẽ ghi phần tử tiếp theo là `cuoi = (dau + so) % k`.

- `them x`: hàng đã đủ *k* phần tử thì in `DAY` và bỏ qua; ngược lại ghi *x* vào ô `cuoi`, tăng `so`.
- `lay`: hàng rỗng thì in `RONG`; ngược lại in `a[dau]`, xoá ô đó (ghi 0, cho dễ nhìn), `dau = (dau + 1) % k`, giảm `so`.

Không được thêm hay bớt ô của `a` (`append`, `pop`, `insert`…): mảng luôn đúng *k* ô.

## Input

Dòng 1 gồm *k* và *q* (1 ≤ *k* ≤ 10⁵, 1 ≤ *q* ≤ 10⁵). *q* dòng sau, mỗi dòng một lệnh `them x` (1 ≤ *x* ≤ 10⁶) hoặc `lay`.

## Output

Mỗi lệnh `lay` in một dòng: số lấy ra, hoặc `RONG`. Mỗi lệnh `them` vào hàng đầy in một dòng `DAY`.

## Ví dụ

Input:

```text
3 7
them 5
them 8
lay
them 2
them 7
them 9
lay
```

Output:

```text
5
DAY
8
```

Sau lần `lay` đầu, `dau` = 1. `them 7` ghi vào ô (1 + 2) % 3 = 0: đã quay vòng về đầu mảng.
