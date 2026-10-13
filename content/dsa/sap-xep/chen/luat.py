"""Luật soi trace bài sắp xếp chèn (giống bài nổi bọt) (spec §8.2 bước 3): mảng a chỉ được đổi bằng cách đổi chỗ HAI PHẦN TỬ KỀ NHAU,
và cuối cùng phải được sắp xếp nhờ chính các lần đổi chỗ đó.

Bắt được: sắp xếp chọn (đổi chỗ xa), sắp xếp chèn kiểu dời phần tử (ghi lẻ a[j + 1] = a[j]), và mọi cách
in ra dãy đã sắp mà không thật sự sắp trên a (a.sort() lách được kiểm hàm cấm, a[:] = ..., sắp một list khác).
Không phân biệt được nổi bọt với sắp xếp chèn bằng đổi chỗ kề nhau: cả hai đều đúng yêu cầu "chỉ đổi chỗ hai
phần tử kề nhau" của đề.
"""


def kiem(su_kien, dau_vao):
    """Trả None nếu đạt, hoặc một câu giải thích vi phạm."""
    a = None
    for e in su_kien:
        if e.get("t") == "snap" and isinstance(e.get("state"), dict) and "a" in e["state"]:
            a = list(e["state"]["a"])
            break
    if a is None:
        return "Không thấy mảng a trong trace: hãy sắp xếp trực tiếp trên a = viz.Mang(...) của khung code"
    vao = [int(x) for x in dau_vao.split()]
    if sorted(a) != sorted(vao):
        return "Mảng a không chứa đúng dãy đầu vào"

    ghi = [(e["i"], e["x"]) for e in su_kien if e.get("t") == "set" and e.get("v") == "a"]
    hien_tai = list(a)
    k = 0
    while k < len(ghi):
        if k + 1 >= len(ghi):
            i, _ = ghi[k]
            return f"Ghi lẻ vào a[{i}] mà không đổi chỗ với phần tử kề bên"
        (i, x), (j, y) = ghi[k], ghi[k + 1]
        if abs(i - j) != 1 or x != hien_tai[j] or y != hien_tai[i]:
            return (f"Lần ghi thứ {k + 1}–{k + 2} (a[{i}], a[{j}]) không phải đổi chỗ hai phần tử kề nhau. "
                    "Đề chỉ cho đổi chỗ a[j - 1] với a[j]")
        hien_tai[i], hien_tai[j] = x, y
        k += 2
    if hien_tai != sorted(a):
        return "Các lần đổi chỗ trên a chưa sắp xếp xong dãy: kết quả in ra không đến từ việc sắp xếp a"
    return None
