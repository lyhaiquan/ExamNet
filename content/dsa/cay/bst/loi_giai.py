import viz

T = viz.Cay(ten="T")


def chen(nut, k):
    if nut is None:
        viz.say(f"Chỗ trống: tạo nút {k}")
        return T.Nut(k)
    x = nut.khoa
    if k < x:
        nut.trai = chen(nut.trai, k)
    elif k > x:
        nut.phai = chen(nut.phai, k)
    else:
        viz.say(f"{k} đã có, không chèn")
    return nut


def tim(nut, k):
    while nut is not None:
        x = nut.khoa
        if k == x:
            viz.say(f"Gặp {k}: có")
            return True
        viz.say(f"{k} {'<' if k < x else '>'} {x}: sang {'trái' if k < x else 'phải'}")
        nut = nut.trai if k < x else nut.phai
    viz.say(f"Gặp chỗ trống: không có {k}")
    return False


def xoa(nut, k):
    if nut is None:
        viz.say(f"Không có {k}, không xoá")
        return None
    x = nut.khoa
    if k < x:
        nut.trai = xoa(nut.trai, k)
        return nut
    if k > x:
        nut.phai = xoa(nut.phai, k)
        return nut
    if nut.trai is None:
        viz.say(f"Nút {k} không có con trái: cây con phải lên thay")
        return nut.phai
    if nut.phai is None:
        viz.say(f"Nút {k} không có con phải: cây con trái lên thay")
        return nut.trai
    s = nut.phai
    while s.trai is not None:
        s = s.trai
    ks = s.khoa
    viz.say(f"Nút {k} đủ hai con: nhỏ nhất của cây con phải là {ks}, chép {ks} lên rồi xoá {ks} ở cây con phải")
    nut.khoa = ks
    nut.phai = xoa(nut.phai, ks)
    return nut


def duyet_truoc(nut, kq):
    if nut is not None:
        kq.append(nut.khoa)
        duyet_truoc(nut.trai, kq)
        duyet_truoc(nut.phai, kq)


for _ in range(int(input())):
    lenh, k = input().split()
    k = int(k)
    if lenh == "chen":
        viz.key(f"Chèn {k}: đi từ gốc xuống")
        T.goc = chen(T.goc, k)
    elif lenh == "xoa":
        viz.key(f"Xoá {k}")
        T.goc = xoa(T.goc, k)
    else:
        viz.key(f"Tìm {k}: đi từ gốc xuống")
        print("YES" if tim(T.goc, k) else "NO")
kq = []
viz.key("Duyệt trước: gốc, cây con trái, cây con phải")
duyet_truoc(T.goc, kq)
print(*kq) if kq else print("RONG")
