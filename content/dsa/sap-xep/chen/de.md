# Sắp xếp chèn (đổi chỗ kề)

Cho một dãy số nguyên. Sắp xếp dãy **tăng dần** bằng thuật toán **chèn**, giống cách xếp bài trên tay: phần đầu dãy luôn đã sắp; lấy phần tử kế tiếp và **chèn** nó vào đúng chỗ trong phần đã sắp.

Lượt *i* (*i* = 1, 2, …, *n* − 1): `a[0..i-1]` đã sắp. Đưa `a[i]` lùi dần về bên trái bằng cách **đổi chỗ với phần tử kề trước** khi phần tử đó lớn hơn nó; gặp phần tử không lớn hơn (hoặc tới đầu dãy) thì dừng.

Chỉ được đổi chỗ hai phần tử kề nhau. Dãy gần như đã sắp thì mỗi lượt dừng rất sớm: sắp xếp chèn chạy gần O(*n*).

## Input

Một dòng gồm *n* số nguyên cách nhau bởi dấu cách (1 ≤ *n* ≤ 1 000, mỗi số có trị tuyệt đối ≤ 10⁹).

## Output

Dãy đã sắp, các số cách nhau bởi dấu cách.

## Ví dụ

| Input | Output |
| --- | --- |
| `5 2 4 6 1 3` | `1 2 3 4 5 6` |
