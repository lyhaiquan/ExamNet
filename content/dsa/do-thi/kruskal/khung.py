import viz

n, m = map(int, input().split())
g = viz.DoThi(n, co_trong_so=True, ten="g")
canh = []
for _ in range(m):
    u, v, w = map(int, input().split())
    g.them_canh(u, v, w)
    canh.append((w, len(canh), u, v))  # sắp theo (w, thứ tự trong input)
canh.sort()
cha = viz.Mang(list(range(n)), ten="cha")  # cha[v] = v: mỗi đỉnh một mảnh


def tim(v):
    # Đi theo cha tới gốc r (cha[r] == r); nén đường: gán cha của các đỉnh đã đi qua bằng r. Trả r
    return v


tong = 0
for w, _, u, v in canh:
    # a = tim(u), b = tim(v); khác nhau thì cha[b] = a, tong += w (g.danh_dau_canh(u, v, "xong"))
    # cùng mảnh thì bỏ cạnh (g.danh_dau_canh(u, v, "loai"))
    pass
print(tong)
