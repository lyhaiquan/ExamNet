"""Luật soi trace bài sắp xếp vun đống: chỉ đổi chỗ nút cha với nút con, hoặc gốc với ô cuối của đống.

Mọi lần ghi phải đi thành cặp đổi chỗ a[i] ↔ a[j] (i < j) với i là cha của j ((j − 1) // 2 == i) hoặc i = 0.
Nổi bọt, chèn đổi chỗ hai ô kề nhau (j = i + 1 chỉ là cha – con khi i = 0); chọn đổi chỗ xa: đều bị bắt.
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
            return f"Lần ghi a[{i}], a[{j}] không phải đổi chỗ hai phần tử"
        cha, con = min(i, j), max(i, j)
        if cha != 0 and (con - 1) // 2 != cha:
            return f"Đổi chỗ a[{cha}] với a[{con}]: không phải nút cha với nút con, cũng không phải gốc với ô cuối"
        hien_tai[i], hien_tai[j] = x, y
        k += 2
    if hien_tai != sorted(a):
        return "Các lần đổi chỗ trên a chưa sắp xếp xong dãy"
    return None
