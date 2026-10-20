"""Luật soi trace bài Fibonacci: fib phải gọi đệ quy và có ghi nhớ.

Vòng lặp cho đúng kết quả nhưng không phải đệ quy (chỉ một lời gọi); đệ quy không ghi nhớ thì đã quá giờ ở test
lớn. Có ghi nhớ thì fib(n) gọi đúng 2n − 1 lần (n ≥ 1): mỗi giá trị tính một lần, cộng một lần lấy lại từ bảng.
"""


def kiem(su_kien, dau_vao):
    n = int(dau_vao.split()[0])
    goi = sum(1 for e in su_kien if e.get("t") == "call" and e.get("v") == "fib")
    if n >= 3 and goi < n:
        return f"fib phải tự gọi lại chính nó (đệ quy): với n = {n} mới thấy {goi} lời gọi"
    if goi > 2 * n + 1:
        return f"fib chưa ghi nhớ: với n = {n} có {goi} lời gọi, ghi nhớ thì không quá {2 * n + 1}"
    return None
