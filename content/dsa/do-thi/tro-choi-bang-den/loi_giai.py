import sys

import viz

sys.setrecursionlimit(10000)


def tim(u):
    """Tìm đường tăng từ u (phía trái): ghép được u thì True."""
    global da_tham
    for v in g.ke(u):
        if da_tham >> v & 1:  # bit v của mặt nạ: v đã thăm trong lượt tìm này
            continue
        da_tham |= 1 << v
        w = ghep[v]
        if w == 0 or tim(w):
            if w != 0:
                g.danh_dau_canh(w, v, None)  # w đã sang ghép với đỉnh khác, cạnh w–v thôi ghép
            ghep[v] = u
            ghep[u] = v
            g.danh_dau_canh(u, v, "xong")
            return True
    viz.loai(f"Không tìm được đường tăng từ {u}")
    return False


t = int(input())
for _ in range(t):
    n = int(input())

    # 1. Sàng: spf[x] là ước nguyên tố nhỏ nhất của x
    spf = viz.Mang([0] * (n + 1), ten="spf")
    for i in range(2, n + 1):
        if spf[i] == 0:
            for j in range(i, n + 1, i):
                if spf[j] == 0:
                    spf[j] = i
    # Số thừa số nguyên tố của x (tính cả lặp) chẵn hay lẻ: nhân hay chia một số nguyên tố là đổi phía
    phia = [0] * (n + 1)
    for x in range(2, n + 1):
        phia[x] = phia[x // spf[x]] ^ 1
    viz.key("Sàng xong. Nhân hay chia một số nguyên tố luôn đổi tính chẵn lẻ của số thừa số: đồ thị hai phía")

    # 2. Đồ thị: x nối x·p. Phía trái (chẵn thừa số) xếp cột trái, phía phải xếp cột phải
    g = viz.DoThi(n, ten="g", bat_dau=1)
    cot = [[x for x in range(1, n + 1) if phia[x] == s] for s in (0, 1)]
    for s in (0, 1):
        for k, x in enumerate(cot[s]):
            g.vi_tri(x, 0.2 + 0.6 * s, (k + 0.5) / len(cot[s]))
    for x in range(1, n + 1):
        p = 2
        while x * p <= n:
            if spf[p] == p:
                g.them_canh(x, x * p)
            p += 1
    viz.key("Mỗi số nối với số gấp nó một số nguyên tố. Hình tròn đang khoanh đi dọc các cạnh này")

    # 3. Cặp ghép cực đại (Kuhn): tìm đường tăng từ từng đỉnh phía trái
    ghep = viz.Mang([0] * (n + 1), ten="ghep")
    for u in cot[0]:
        da_tham = 0
        if tim(u):
            viz.say(f"Ghép thêm được {u}. Cạnh xanh là cặp ghép hiện tại")
    viz.key("Đã có cặp ghép cực đại")

    # 4. Đỉnh né được: có cặp ghép cực đại không chứa nó. Bắt đầu từ các đỉnh chưa ghép, đi một cạnh
    #    không ghép rồi một cạnh ghép thì tới một đỉnh cũng né được (đổi cặp ghép dọc đường đó)
    ne = [False] * (n + 1)
    q = viz.HangDoi(ten="q")
    for x in range(1, n + 1):
        if ghep[x] == 0:
            ne[x] = True
            g.danh_dau(x, "xong")
            q.enq(x)
    viz.key("Đỉnh chưa ghép thì né được (tô xanh). Từ đỉnh né được đi một cạnh không ghép rồi một cạnh ghép")
    while q:
        x = q.deq()
        for y in g.ke(x):
            z = ghep[y]  # y chắc chắn đã ghép: nếu không thì có đường tăng, trái với cặp ghép cực đại
            if y != ghep[x] and not ne[z]:
                ne[z] = True
                g.danh_dau(z, "xong")
                q.enq(z)
    # Người đi đầu khoanh v, người kia đi trước từ v. Người đi trước từ v thắng khi và chỉ khi mọi cặp ghép
    # cực đại đều chứa v. Vậy khoanh một số chẵn né được là thắng
    tot = []
    for v in range(2, n + 1, 2):
        if ne[v]:
            tot.append(v)
        else:
            g.danh_dau(v, "loai")
    if tot:
        g.nhan(tot[0], "khoanh")
        viz.key(f"{tot[0]} chẵn và né được: khoanh {tot[0]} thì người đi đầu thắng")
        print("first", tot[0])
    else:
        viz.key("Số chẵn nào (tô đỏ) cũng nằm trong mọi cặp ghép cực đại: người đi sau thắng")
        print("second")
