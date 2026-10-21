# Trung vị của dãy đang thêm vào

Lần lượt thêm *n* số vào một dãy ban đầu rỗng. Sau mỗi lần thêm, in **trung vị** của dãy: sắp dãy *k* phần tử tăng dần, trung vị là phần tử ở vị trí ⌈*k* / 2⌉ (đánh số từ 1). Với *k* chẵn, đó là phần tử nhỏ hơn trong hai phần tử ở giữa.

Gợi ý: dùng **hai đống**. Đống `duoi` (đống lớn nhất) giữ nửa nhỏ, đống `tren` (đống nhỏ nhất) giữ nửa lớn, sao cho `duoi` có bằng hoặc hơn `tren` một phần tử. Trung vị là đỉnh của `duoi`. Trong Python, `heapq` là đống nhỏ nhất; để có đống lớn nhất, cất số đối −*x*. Không dùng sắp xếp.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵). Dòng 2 là *n* số nguyên, mỗi số có trị tuyệt đối không quá 10⁹.

## Output

Một dòng *n* số: trung vị sau mỗi lần thêm.

## Ví dụ

Input:

```text
6
5 15 1 3 8 7
```

Output:

```text
5 5 5 3 5 5
```

Input:

```text
3
2 2 2
```

Output:

```text
2 2 2
```
