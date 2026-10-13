"""Luật soi trace bài hàng đợi vòng: hàng đợi phải nằm trong mảng a cố định k ô.

append/pop/insert/sort… trên a đổi số ô (sự kiện push, pop, ins, all): đó là dời mảng, không phải quay vòng.
Có lệnh them mà a không bao giờ được ghi: hàng đợi nằm ở cấu trúc khác (list riêng, deque).
"""


def kiem(su_kien, dau_vao):
    doi = [e for e in su_kien if e.get("v") == "a" and e.get("t") in ("push", "pop", "ins", "all")]
    if doi:
        return "Mảng a phải giữ đúng k ô: chỉ ghi a[i] = x và dời chỉ số dau, không append, pop, insert"
    co_them = any(d.startswith("them") for d in dau_vao.splitlines()[1:])
    if co_them and not any(e.get("t") == "set" and e.get("v") == "a" for e in su_kien):
        return "Hãy cất các phần tử của hàng đợi vào mảng a (a[cuoi] = x), không dùng cấu trúc khác"
    return None
