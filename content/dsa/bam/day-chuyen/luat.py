"""Luật soi trace bài băm dây chuyền: mỗi số được thêm phải nằm đúng ô x mod m.

Dùng list tự viết hay đếm bằng mảng thì kết quả `tim` vẫn đúng, nên phải xem trace: mỗi lệnh `them x` mà x
chưa có trong tập phải sinh đúng một lần thêm x vào ô x mod m của bảng T.
"""


def kiem(su_kien, dau_vao):
    dong = dau_vao.split("\n")
    m, q = map(int, dong[0].split())
    tap, can = set(), []
    for d in dong[1 : q + 1]:
        lenh, x = d.split()
        x = int(x)
        if lenh == "them" and x not in tap:
            tap.add(x)
            can.append((x % m, x))
        elif lenh == "xoa":
            tap.discard(x)
    them = [(e["i"], e["x"]) for e in su_kien
            if e.get("t") == "bucket" and e.get("v") == "T" and e.get("op") == "add"]
    if them != can:
        return "Hãy lưu từng số vào bảng T: số x mới thêm phải vào dây chuyền ô x mod m bằng T[x % m].append(x)"
    return None
