# Đếm thành phần liên thông mạnh

Cho đồ thị **có hướng** *n* đỉnh, *m* cạnh. Hai đỉnh *u*, *v* thuộc cùng một **thành phần liên thông mạnh** nếu đi được từ *u* tới *v* và từ *v* về *u*. Đếm số thành phần liên thông mạnh.

Gợi ý: **Kosaraju**. Lần DFS thứ nhất ghi thứ tự các đỉnh theo lúc duyệt xong. Đảo chiều mọi cạnh; lấy các đỉnh theo thứ tự duyệt xong **giảm dần**, mỗi đỉnh chưa thăm bắt đầu một lần DFS trên đồ thị đảo: mỗi lần như vậy là một thành phần. Viết DFS bằng ngăn xếp: đồ thị có thể là một đường 10⁵ đỉnh, đệ quy sẽ tràn.

## Input

Dòng 1 là *n* và *m* (1 ≤ *n* ≤ 10⁵, 0 ≤ *m* ≤ 2·10⁵). *m* dòng sau, mỗi dòng hai số *u*, *v*: cạnh từ *u* tới *v*.

## Output

Một số: số thành phần liên thông mạnh.

## Ví dụ

Input:

```text
5 5
1 2
2 3
3 1
3 4
4 5
```

Output:

```text
3
```

Input:

```text
3 0
```

Output:

```text
3
```
