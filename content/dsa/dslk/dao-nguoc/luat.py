"""Luật soi trace bài đảo ngược: danh sách phải được đảo thật bằng cách đổi con trỏ.

In ngược (`print(*list(ds)[::-1])`) mà không đổi con trỏ thì kết quả vẫn đúng, nên phải xem trace: sau cùng con
trỏ đầu phải chỉ vào nút tạo sau cùng (nút cuối cũ), và mỗi nút (trừ nút đầu cũ) phải được quay mũi tên về nút
tạo ngay trước nó.
"""


def kiem(su_kien, dau_vao):
    n = int(dau_vao.split()[0])
    if n <= 1:
        return None
    dau = None
    tiep = {}
    for e in su_kien:
        if e.get("t") != "link" or e.get("v") != "ds":
            continue
        if e.get("id") is None:
            dau = e.get("w")
        else:
            tiep[e["id"]] = e.get("w")
    if dau != n - 1:
        return "Danh sách chưa được đảo thật: cuối cùng ds.dau phải là nút cuối cũ (gán ds.dau = prev)"
    if any(tiep.get(k) != k - 1 for k in range(1, n)) or tiep.get(0) is not None:
        return "Hãy đảo bằng cách quay từng mũi tên cur.tiep = prev, không in ngược hay tạo danh sách mới"
    return None
