# Chia dãy thành k đoạn

Cho dãy *n* số nguyên không âm và số *k*. Chia dãy thành đúng *k* **đoạn liên tiếp, không rỗng**. Gọi *M* là tổng lớn nhất trong *k* đoạn. Tìm cách chia để *M* nhỏ nhất và in *M*.

Gợi ý: **tìm nhị phân theo đáp án**. Với một giá trị *M* cho trước, đi từ trái sang phải và tham lam gom phần tử vào đoạn hiện tại khi tổng chưa vượt *M*; nếu số đoạn cần dùng không quá *k* thì *M* làm được. *M* làm được thì mọi số lớn hơn cũng làm được.

## Input

Dòng 1 là *n* và *k* (1 ≤ *k* ≤ *n* ≤ 10⁵). Dòng 2 là *n* số nguyên, mỗi số từ 0 tới 10⁴.

## Output

Một số: giá trị nhỏ nhất của *M*.

## Ví dụ

Input:

```text
5 2
7 2 5 10 8
```

Output:

```text
18
```

Input:

```text
4 4
1 2 3 4
```

Output:

```text
4
```

Ở ví dụ 1, chia thành `7 2 5` và `10 8`: tổng lớn nhất là 18.
