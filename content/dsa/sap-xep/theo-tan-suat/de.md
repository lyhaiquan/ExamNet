# Sắp xếp theo tần suất

Cho dãy *n* số nguyên. Sắp xếp lại dãy sao cho:

- số xuất hiện **nhiều lần hơn** đứng trước;
- hai số xuất hiện số lần bằng nhau thì số **nhỏ hơn** đứng trước;
- các lần xuất hiện của cùng một số đứng liền nhau.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵). Dòng 2 là *n* số nguyên, mỗi số có trị tuyệt đối không quá 10⁹.

## Output

Một dòng: dãy sau khi sắp xếp, các số cách nhau một dấu cách.

## Ví dụ

Input:

```text
8
4 6 2 2 6 4 4 4
```

Output:

```text
4 4 4 4 2 2 6 6
```

Input:

```text
5
5 -1 3 -1 3
```

Output:

```text
-1 -1 3 3 5
```

Ở ví dụ 1, số 4 xuất hiện 4 lần; số 2 và 6 cùng xuất hiện 2 lần nên 2 đứng trước 6.
