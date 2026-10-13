"""Luật soi trace bài sắp xếp nhanh: chia đệ quy bằng hàm nhanh, mảng a chỉ đổi bằng các lần đổi chỗ.

Trộn ghi lẻ từng ô (chép từ danh sách tạm), chèn kiểu dời phần tử cũng vậy: bị bắt. Sắp bằng vòng lặp chỉ đổi
chỗ (nổi bọt, chọn) thì không có lời gọi nhanh nào.
"""


def kiem(su_kien, dau_vao):
    a = None
    for e in su_kien:
        if e.get("t") == "snap" and isinstance(e.get("state"), dict) and "a" in e["state"]:
            a = list(e["state"]["a"])
            break
    if a is None:
        return "Không thấy mảng a trong trace: hãy sắp xếp trực tiếp trên a = viz.Mang(...) của khung code"
    n = len(a)
    goi = sum(1 for e in su_kien if e.get("t") == "call" and e.get("v") == "nhanh")
    if n >= 3 and goi < 3:
        return f"Hãy chia đệ quy: nhanh(lo, hi) gọi lại nhanh cho hai phần. Với {n} số mới thấy {goi} lời gọi nhanh"
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
            return f"Lần ghi a[{i}], a[{j}] không phải đổi chỗ hai phần tử. Phân hoạch Lomuto chỉ đổi chỗ"
        hien_tai[i], hien_tai[j] = x, y
        k += 2
    if hien_tai != sorted(a):
        return "Các lần đổi chỗ trên a chưa sắp xếp xong dãy"
    return None
