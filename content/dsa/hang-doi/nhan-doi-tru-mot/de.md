# Nhân đôi và trừ một

Bắt đầu từ số *S*, mỗi bước được làm một trong hai thao tác: nhân đôi (*x* → 2*x*) hoặc trừ một (*x* → *x* − 1). Tìm số bước ít nhất để biến *S* thành *T*.

Gợi ý: **BFS** trên các số, mỗi số là một đỉnh, mỗi thao tác là một cạnh. Không bao giờ cần đi qua số lớn hơn 2·max(*S*, *T*) hay nhỏ hơn 1.

## Input

Dòng 1 là *t* (1 ≤ *t* ≤ 100). *t* dòng sau, mỗi dòng hai số *S* và *T* (1 ≤ *S*, *T* ≤ 10⁴).

## Output

*t* dòng, mỗi dòng số bước ít nhất.

## Ví dụ

Input:

```text
3
4 6
10 1
3 3
```

Output:

```text
2
9
0
```

Input:

```text
1
1 1024
```

Output:

```text
10
```

Ở ví dụ 1: 4 → 3 → 6 (2 bước); 10 → 9 → … → 1 (9 bước); 3 = 3 (0 bước).
