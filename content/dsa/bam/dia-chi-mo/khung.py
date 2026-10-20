import viz

m, q = map(int, input().split())
T = viz.BangBam(m, mo=True, ten="T")  # T[i]: None (trống), một số, hoặc viz.XOA (đã xoá)


def do(x):
    """Dò từ ô x % m. Trả (ô đang giữ x hoặc -1, ô để thêm x: ô đã xoá đầu tiên, không có thì ô trống nơi dừng)."""
    # Với k = 0, 1, …: i = (x % m + k) % m, o = T[i]
    #   o is None: dừng, không có x
    #   o is viz.XOA: nhớ ô này nếu là ô đã xoá đầu tiên, dò tiếp
    #   o == x: tìm thấy ở ô i
    return -1, x % m


for _ in range(q):
    lenh, x = input().split()
    x = int(x)
    vt, cho = do(x)
    if lenh == "them":
        # chưa có (vt == -1): T[cho] = x
        pass
    elif lenh == "xoa":
        # có (vt != -1): T[vt] = viz.XOA
        pass
    else:
        print("YES" if vt != -1 else "NO")
