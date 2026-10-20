import viz

T = viz.Cay(ten="T")


def chen(nut, k):
    if nut is None:
        viz.say(f"Chỗ trống: tạo nút {k}")
        return T.Nut(k)
    x = nut.khoa
    if k < x:
        viz.say(f"{k} < {x}: đi sang cây con trái")
        nut.trai = chen(nut.trai, k)
    else:
        viz.say(f"{k} > {x}: đi sang cây con phải")
        nut.phai = chen(nut.phai, k)
    return nut


def duyet_truoc(nut, kq):
    if nut is None:
        return
    kq.append(nut.khoa)
    T.danh_dau(nut, "xong")
    viz.say(f"Thăm {kq[-1]}, rồi tới cây con trái, cây con phải")
    duyet_truoc(nut.trai, kq)
    duyet_truoc(nut.phai, kq)


n = int(input())
for k in map(int, input().split()):
    viz.key(f"Chèn {k}: đi từ gốc xuống")
    T.goc = chen(T.goc, k)
kq = []
viz.key("Đã chèn xong. Duyệt trước: gốc → trái → phải")
duyet_truoc(T.goc, kq)
print(*kq)
