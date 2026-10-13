import viz

m, q = map(int, input().split())
T = viz.BangBam(m, mo=True, ten="T")


def do(x):
    h = x % m
    xoa_dau = -1
    for k in range(m):
        i = (h + k) % m
        o = T[i]
        if o is None:
            return -1, (xoa_dau if xoa_dau != -1 else i)
        if o is viz.XOA:
            if xoa_dau == -1:
                xoa_dau = i
        elif o == x:
            return i, -1
    return -1, xoa_dau


for _ in range(q):
    lenh, x = input().split()
    x = int(x)
    h = x % m
    if lenh == "them":
        viz.key(f"Thêm {x}: bắt đầu dò ở ô {x} mod {m} = {h}")
        vt, cho = do(x)
        if vt == -1:
            viz.say(f"Chưa có {x}: đặt vào ô {cho}")
            T[cho] = x
        else:
            viz.say(f"{x} đã có ở ô {vt}, không thêm")
    elif lenh == "xoa":
        viz.key(f"Xoá {x}: dò từ ô {h}")
        vt, _ = do(x)
        if vt != -1:
            viz.say(f"Ghi dấu đã xoá ở ô {vt}, không làm ô trống: số khác có thể đã dò vượt qua ô này")
            T[vt] = viz.XOA
    else:
        viz.key(f"Tìm {x}: dò từ ô {h} tới khi gặp {x} hoặc ô trống")
        vt, _ = do(x)
        print("YES" if vt != -1 else "NO")
