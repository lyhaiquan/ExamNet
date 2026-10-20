# Trò chơi trên bảng đen

*Phỏng theo bài "Blackboard Game" của kỳ thi ICPC. Bản này đổi yêu cầu in ra (in số chẵn **nhỏ nhất** để đáp án là duy nhất) và giảm giới hạn để chạy bằng Python.*

Trên bảng viết các số nguyên từ 1 đến *n*. Hai người lần lượt đi:

- Người thứ nhất mở đầu bằng cách khoanh tròn một số **chẵn** bất kỳ.
- Từ đó, mỗi lượt người chơi phải chọn một số **còn trên bảng** bằng số đang khoanh **nhân** hoặc **chia** cho một số nguyên tố, xoá số đang khoanh và khoanh số vừa chọn.
- Ai tới lượt mà không đi được thì thua.

Hỏi người thứ nhất hay người thứ hai thắng nếu cả hai chơi tối ưu. Nếu người thứ nhất thắng, chỉ ra số chẵn **nhỏ nhất** mà khoanh đầu tiên thì người thứ nhất thắng.

## Ý tưởng: đồ thị và cặp ghép

**Đồ thị.** Mỗi số là một đỉnh; *x* nối với *x*·*p* (*p* nguyên tố). Khoanh một số rồi đi tiếp chính là đi trên đồ thị này, đỉnh đã đi qua bị xoá.

**Hai phía.** Nhân hay chia một số nguyên tố luôn làm số thừa số nguyên tố (tính cả lặp) của số tăng hoặc giảm 1, nên đổi chẵn thành lẻ. Đỉnh có số thừa số chẵn xếp phía trái, lẻ xếp phía phải: mọi cạnh nối hai phía. Sàng ước nguyên tố nhỏ nhất `spf` cho tính chẵn lẻ đó: `phia[x] = phia[x // spf[x]] ^ 1`.

**Định lý trò chơi.** Khoanh *v* xong thì người thứ hai đi trước từ *v*. Trên đồ thị, người đi trước từ *v* thắng **khi và chỉ khi mọi cặp ghép cực đại đều chứa *v***:

- Nếu mọi cặp ghép cực đại *M* đều chứa *v*: người đi trước luôn đi theo cạnh của *M*. Người kia mà đi tới được một đỉnh chưa ghép thì đổi cặp ghép dọc đường đã đi sẽ được cặp ghép cực đại không chứa *v*, vô lý.
- Nếu có cặp ghép cực đại *M* không chứa *v*: đổi vai, người đi sau luôn đi theo cạnh của *M* và thắng.

Vậy người thứ nhất thắng khi có một số chẵn **né được**: tồn tại cặp ghép cực đại không chứa nó.

**Thuật toán.**

1. Sàng `spf`, tính phía của từng số.
2. Dựng đồ thị: nối *x* với *x*·*p*.
3. Tìm cặp ghép cực đại bằng đường tăng (Kuhn): với từng đỉnh *u* phía trái, `tim(u)` thử từng đỉnh kề *v* chưa thăm. *v* chưa ghép, hoặc người đang ghép với *v* tìm được chỗ khác (gọi đệ quy), thì ghép *u* với *v*. Đỉnh đã thăm trong một lượt tìm giữ bằng một mặt nạ bit `da_tham`.
4. Đỉnh né được: mọi đỉnh chưa ghép. Từ một đỉnh né được *x*, đi một cạnh **không ghép** tới *y*, rồi cạnh **ghép** của *y* tới *z*: *z* cũng né được (đổi cặp ghép dọc đường đó thì *z* được thả ra). Loang bằng hàng đợi.
5. Số chẵn nhỏ nhất né được là đáp án. Không có thì người thứ hai thắng.

## Input

Dòng 1 là số test *t* (1 ≤ *t* ≤ 40). Mỗi dòng sau là một số *n* (2 ≤ *n* ≤ 2000). Tổng các *n* không quá 5000.

## Output

Mỗi test một dòng: `first v` nếu người thứ nhất thắng, *v* là số chẵn nhỏ nhất khoanh đầu tiên mà thắng được; `second` nếu người thứ hai thắng.

## Ví dụ

Input:

```text
3
5
12
17
```

Output:

```text
second
first 2
first 4
```

Với *n* = 5: khoanh 2 thì người kia khoanh 4 và hết nước đi; khoanh 4 thì người kia khoanh 2, người thứ nhất buộc phải khoanh 1, người kia khoanh 3 hoặc 5 và thắng.

Với *n* = 12: đề gốc in `first 8` cũng là một nước thắng, nhưng bài này yêu cầu số nhỏ nhất là 2.
