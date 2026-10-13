import viz

n, m = map(int, input().split())
canh = sorted(tuple(sorted(map(int, input().split()))) for _ in range(m))
g = viz.DoThi(n, ten="g", bat_dau=1)
for u, v in canh:  # thêm theo thứ tự tăng: g.ke(u) cho các đỉnh kề tăng dần
    g.them_canh(u, v)

tham = [False] * (n + 1)
thu_tu = []  # các đỉnh theo thứ tự thăm
st = viz.NganXep(ten="st")
st.push(1)
while st:
    u = st.pop()
    # u đã thăm thì bỏ qua (continue)
    # Thăm u: tham[u] = True, thu_tu.append(u)  (g.danh_dau(u, "xong") để tô đỉnh đã thăm)
    # Đẩy các đỉnh kề CHƯA THĂM của u (for v in g.ke(u)) vào st theo thứ tự GIẢM DẦN
print(*thu_tu)
