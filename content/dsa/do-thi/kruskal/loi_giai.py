import viz

n, m = map(int, input().split())
g = viz.DoThi(n, co_trong_so=True, ten="g")
canh = []
for _ in range(m):
    u, v, w = map(int, input().split())
    g.them_canh(u, v, w)
    canh.append((w, len(canh), u, v))
canh.sort()
cha = viz.Mang(list(range(n)), ten="cha")


def tim(v):
    r = v
    while cha[r] != r:
        r = cha[r]
    while cha[v] != r:  # nén đường
        sau = cha[v]
        cha[v] = r
        v = sau
    return r


viz.key("Xét các cạnh theo trọng số tăng dần; cha[v] = v: mỗi đỉnh là một mảnh")
tong, lay = 0, 0
for w, _, u, v in canh:
    if lay == n - 1:
        break
    a, b = tim(u), tim(v)
    if a == b:
        viz.say(f"Cạnh {u}–{v} nặng {w}: {u} và {v} cùng mảnh (gốc {a}), lấy sẽ tạo chu trình, bỏ")
        g.danh_dau_canh(u, v, "loai")
        continue
    viz.say(f"Cạnh {u}–{v} nặng {w}: hai mảnh khác nhau (gốc {a}, {b}), lấy và gộp: cha[{b}] = {a}")
    cha[b] = a
    tong += w
    lay += 1
    g.danh_dau_canh(u, v, "xong")
    viz.key(f"Đã lấy {lay} cạnh, tổng {tong}")
print(tong)
