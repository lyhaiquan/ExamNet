import viz

n, m = map(int, input().split())
canh = sorted(tuple(sorted(map(int, input().split()))) for _ in range(m))
g = viz.DoThi(n, ten="g", bat_dau=1)
for u, v in canh:
    g.them_canh(u, v)

tham = [False] * (n + 1)
thu_tu = []
cha = {}  # cha[v]: đỉnh đã đẩy v vào ngăn xếp gần nhất; thăm v thì cạnh cha[v]–v thuộc cây DFS
st = viz.NganXep(ten="st")
st.push(1)
viz.key("Đẩy đỉnh 1 vào ngăn xếp")
while st:
    u = st.pop()
    if tham[u]:
        viz.say(f"Lấy {u} ra: đã thăm rồi, bỏ qua")
        continue
    tham[u] = True
    thu_tu.append(u)
    viz.key(f"Lấy {u} ra: thăm, là đỉnh thứ {len(thu_tu)}")
    if u in cha:
        g.danh_dau_canh(cha[u], u, "xong")
    g.danh_dau(u, "xong")
    g.nhan(u, len(thu_tu))
    ke = [v for v in g.ke(u) if not tham[v]]
    if ke:
        viz.say(f"Đẩy {', '.join(map(str, reversed(ke)))}: đỉnh nhỏ nhất vào sau cùng, nằm trên đỉnh")
    else:
        viz.say(f"{u} không còn đỉnh kề chưa thăm: lùi lại")
    for v in reversed(ke):
        cha[v] = u
        st.push(v)
viz.key(f"Ngăn xếp rỗng: thứ tự thăm {' '.join(map(str, thu_tu))}")
print(*thu_tu)
