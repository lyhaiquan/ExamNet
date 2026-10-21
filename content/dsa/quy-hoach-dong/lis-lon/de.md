# Dãy con tăng dài nhất (n lớn)

Cho dãy *n* số nguyên. Tìm độ dài dãy con **tăng ngặt** dài nhất (các phần tử không cần liền nhau nhưng giữ nguyên thứ tự).

Với *n* tới 10⁵, quy hoạch động O(*n*²) quá chậm. Gợi ý O(*n* log *n*): giữ mảng `cuoi`, với `cuoi[k]` là số cuối nhỏ nhất của một dãy con tăng dài *k* + 1. Với mỗi số *x*, tìm nhị phân vị trí đầu tiên trong `cuoi` có giá trị ≥ *x* rồi thay bằng *x* (hoặc thêm vào cuối nếu không có).

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵). Dòng 2 là *n* số nguyên, mỗi số có trị tuyệt đối không quá 10⁹.

## Output

Một số: độ dài dãy con tăng ngặt dài nhất.

## Ví dụ

Input:

```text
8
10 9 2 5 3 7 101 18
```

Output:

```text
4
```

Input:

```text
4
3 3 3 3
```

Output:

```text
1
```
