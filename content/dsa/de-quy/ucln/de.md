# UCLN và BCNN

Với mỗi cặp số nguyên dương *a*, *b*, in ước chung lớn nhất UCLN(*a*, *b*) và bội chung nhỏ nhất BCNN(*a*, *b*).

Viết UCLN bằng **thuật toán Euclid đệ quy**: UCLN(*a*, 0) = *a*, UCLN(*a*, *b*) = UCLN(*b*, *a* mod *b*). Không dùng `math.gcd`, `math.lcm`. Có UCLN thì BCNN(*a*, *b*) = *a* / UCLN(*a*, *b*) · *b* (chia trước rồi nhân để không tràn số).

## Input

Dòng 1 là *t* (1 ≤ *t* ≤ 10⁵). *t* dòng sau, mỗi dòng hai số *a*, *b* (1 ≤ *a*, *b* ≤ 10⁹).

## Output

*t* dòng, mỗi dòng hai số: UCLN và BCNN.

## Ví dụ

Input:

```text
3
12 18
7 5
10 10
```

Output:

```text
6 36
1 35
10 10
```

Input:

```text
1
1000000000 999999999
```

Output:

```text
1 999999999000000000
```
