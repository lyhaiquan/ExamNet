import sys

import viz

sys.setrecursionlimit(10000)


def tim(u):
    """Tìm đường tăng từ đỉnh u (phía trái). Ghép thêm được thì trả True."""
    global da_tham
    # Với mỗi v trong g.ke(u):
    #   bỏ qua nếu bit v của da_tham đã bật (da_tham >> v & 1); bật bit đó (da_tham |= 1 << v)
    #   w = ghep[v]; nếu w == 0 hoặc tim(w): ghép u với v (ghep[u] = v, ghep[v] = u) rồi trả True
    #   (g.danh_dau_canh(u, v, "xong") tô cạnh ghép; g.danh_dau_canh(w, v, None) xoá cạnh w–v thôi ghép)
    return False


t = int(input())
for _ in range(t):
    n = int(input())
    spf = viz.Mang([0] * (n + 1), ten="spf")  # spf[x]: ước nguyên tố nhỏ nhất của x (sàng)
    # 1. Sàng spf. Rồi phia[x] = phia[x // spf[x]] ^ 1: chẵn hay lẻ số thừa số nguyên tố của x

    g = viz.DoThi(n, ten="g", bat_dau=1)  # đỉnh 1..n
    # 2. Nối x với x * p (p nguyên tố, x * p <= n) bằng g.them_canh(x, x * p)

    ghep = viz.Mang([0] * (n + 1), ten="ghep")  # ghep[x]: đỉnh đang ghép với x, 0 là chưa ghép
    # 3. Với mỗi u phía trái (phia[u] == 0): da_tham = 0; tim(u)

    q = viz.HangDoi(ten="q")
    # 4. Đỉnh né được = có cặp ghép cực đại không chứa nó. Đưa mọi đỉnh chưa ghép vào q. Lấy x ra; với mỗi
    #    y kề x mà y != ghep[x]: z = ghep[y] cũng né được (chưa có thì đưa vào q)
    # 5. Số chẵn nhỏ nhất né được v: in "first v". Không có: in "second"
