"""Luật soi trace bài Fibonacci ghi nhớ: fib phải đệ quy và có ghi nhớ.

Có ghi nhớ thì fib(n) gọi đúng 2n − 1 lần (n ≥ 1): mỗi giá trị tính một lần, cộng một lần lấy lại từ bảng.
Vòng lặp chỉ có một lời gọi; đệ quy không ghi nhớ với n = 12 gọi 465 lần (cây lời gọi chỉ vẽ 200).
"""


def kiem(su_kien, dau_vao):
    n = int(dau_vao.split()[0])
    goi = sum(1 for e in su_kien if e.get("t") == "call" and e.get("v") == "fib")
    if n >= 3 and goi < n:
        return f"fib phải tự gọi lại chính nó (đệ quy): với n = {n} mới thấy {goi} lời gọi"
    if goi > 2 * n + 1:
        return f"fib chưa ghi nhớ: với n = {n} có ít nhất {goi} lời gọi, ghi nhớ thì không quá {2 * n + 1}"
    return None
