# Sắp xếp nổi bọt

Cho một dãy số nguyên. Hãy sắp xếp dãy theo thứ tự **tăng dần** bằng thuật toán **nổi bọt**: duyệt dãy nhiều lượt, mỗi lượt so sánh từng cặp phần tử **kề nhau** và đổi chỗ nếu phần tử đứng trước lớn hơn phần tử đứng sau. Sau lượt thứ *k*, *k* phần tử lớn nhất đã nằm đúng chỗ ở cuối dãy.

Chỉ được đổi chỗ hai phần tử kề nhau. Không dùng `sorted` hay `list.sort`.

## Input

Một dòng gồm *n* số nguyên cách nhau bởi dấu cách (1 ≤ *n* ≤ 1 000, mỗi số có trị tuyệt đối ≤ 10⁹).

## Output

Một dòng gồm dãy đã sắp xếp, các số cách nhau bởi dấu cách.

## Ví dụ

| Input | Output |
| --- | --- |
| `5 1 4 2 8` | `1 2 4 5 8` |
