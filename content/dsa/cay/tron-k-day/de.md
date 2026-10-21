# Trộn k dãy đã sắp

Cho *k* dãy, mỗi dãy đã sắp **tăng dần** (không giảm). Trộn chúng thành một dãy tăng dần.

Gợi ý: dùng **đống nhỏ nhất** chứa phần tử đầu của mỗi dãy, kèm số thứ tự dãy và vị trí. Lấy ra phần tử nhỏ nhất, in nó, rồi đẩy phần tử kế tiếp của cùng dãy vào đống. Mỗi bước O(log *k*). Không dùng sắp xếp hay `heapq.merge`.

## Input

Dòng 1 là *k* (1 ≤ *k* ≤ 10⁴). *k* dòng sau, dòng thứ *i* bắt đầu bằng độ dài *m*ᵢ ≥ 0 rồi tới *m*ᵢ số của dãy thứ *i*. Tổng các *m*ᵢ không quá 2·10⁵, mỗi số có trị tuyệt đối không quá 10⁹.

## Output

Một dòng: dãy sau khi trộn (dòng trống nếu mọi dãy đều rỗng).

## Ví dụ

Input:

```text
3
3 1 4 5
3 1 3 4
2 2 6
```

Output:

```text
1 1 2 3 4 4 5 6
```

Input:

```text
2
0
1 7
```

Output:

```text
7
```
