# In các số nhị phân từ 1 đến n bằng hàng đợi

Cho số nguyên *n*. Hãy in biểu diễn nhị phân của các số 1, 2, …, *n*, mỗi số một dòng.

Không dùng `bin` hay `format` để đổi cơ số. Hãy dùng **hàng đợi** `q` trong khung code: đưa `"1"` vào hàng đợi; mỗi lần lấy xâu `x` ở đầu hàng ra (`q.deq()`), in `x`, rồi đưa `x + "0"` và `x + "1"` vào cuối hàng (`q.enq(...)`). Làm như vậy *n* lần.

Vì sao đúng: các số nhị phân xếp theo thứ tự tăng dần cũng chính là thứ tự duyệt theo từng mức của cây có gốc `"1"`, mỗi nút `x` có hai con `x0` và `x1`.

## Input

Một số nguyên *n* (1 ≤ *n* ≤ 3·10⁴).

## Output

*n* dòng, dòng thứ *k* là biểu diễn nhị phân của *k*.

## Ví dụ

Input:

```text
5
```

Output:

```text
1
10
11
100
101
```
