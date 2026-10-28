# Gom các từ đảo chữ

Hai từ là **đảo chữ** của nhau nếu đổi chỗ các chữ cái của từ này được từ kia, ví dụ `tra` và `rat`. Mọi từ là đảo chữ của chính nó.

Cho *n* từ. Gom các từ là đảo chữ của nhau vào cùng một nhóm.

Gợi ý: hai từ là đảo chữ khi và chỉ khi sắp xếp chữ cái của chúng được cùng một xâu. Dùng xâu đã sắp làm khoá của từ điển.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 5·10⁴). Dòng 2 là *n* từ, mỗi từ gồm 1 tới 20 chữ cái thường a–z.

## Output

Dòng đầu là số nhóm *g*. *g* dòng sau, mỗi dòng một nhóm: các từ trong nhóm theo thứ tự xuất hiện trong input, cách nhau một dấu cách. Các nhóm xếp theo thứ tự xuất hiện của từ đầu tiên trong nhóm. Từ trùng nhau vẫn in đủ số lần.

## Ví dụ

Input:

```text
6
tra rat ta at art an
```

Output:

```text
3
tra rat art
ta at
an
```

Input:

```text
3
abc abc cab
```

Output:

```text
1
abc abc cab
```
