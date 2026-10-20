"""Luật soi trace bài sắp xếp chọn: mảng a chỉ đổi bằng các lần đổi chỗ, mỗi lượt tối đa một lần.

Sắp xếp chọn đổi chỗ không quá n − 1 lần. Nổi bọt và chèn đổi chỗ cỡ n²/4 lần trên dãy ngẫu nhiên (test tb
có 20–30 số: hơn trăm lần), nên vượt n − 1. Ghi lẻ một ô (dời phần tử, gán cả mảng) cũng bị bắt.
"""


def kiem(su_kien, dau_vao):
    a = None
    for e in su_kien:
        if e.get("t") == "snap" and isinstance(e.get("state"), dict) and "a" in e["state"]:
            a = list(e["state"]["a"])
            break
    if a is None:
        return "Không thấy mảng a trong trace: hãy sắp xếp trực tiếp trên a = viz.Mang(...) của khung code"
    if any(e.get("v") == "a" and e.get("t") in ("push", "pop", "ins", "all") for e in su_kien):
        return "Chỉ được đổi chỗ các phần tử của a, không thêm, bớt hay gán lại cả mảng"
    ghi = [(e["i"], e["x"]) for e in su_kien if e.get("t") == "set" and e.get("v") == "a"]
    hien_tai = list(a)
    k = 0
    while k < len(ghi):
        if k + 1 >= len(ghi):
            return f"Ghi lẻ vào a[{ghi[k][0]}] mà không phải đổi chỗ"
        (i, x), (j, y) = ghi[k], ghi[k + 1]
        if x != hien_tai[j] or y != hien_tai[i]:
            return f"Lần ghi a[{i}], a[{j}] không phải đổi chỗ hai phần tử. Sắp xếp chọn chỉ đổi chỗ a[i] với a[min]"
        hien_tai[i], hien_tai[j] = x, y
        k += 2
    n = len(a)
    if k // 2 > n - 1:
        return f"Đổi chỗ {k // 2} lần với {n} số: sắp xếp chọn mỗi lượt đổi tối đa một lần, tổng không quá {n - 1}"
    if hien_tai != sorted(a):
        return "Các lần đổi chỗ trên a chưa sắp xếp xong dãy"
    return None
