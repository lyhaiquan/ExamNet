import viz


def vun_xuong(a, i, m):
    # Trong đống m ô đầu: con lớn nhất của ô i (2i + 1, 2i + 2, chỉ xét con < m) lớn hơn a[i]
    # thì đổi chỗ a[i] với con đó, rồi tiếp tục từ chỗ con đó; không thì dừng
    pass


def sap_xep(a):
    n = len(a)
    a.con_tro("n", n)
    # Dựng đống: vun_xuong(a, i, n) với i = n // 2 - 1 lùi về 0
    # Với m = n - 1 lùi về 1: đổi chỗ a[0] với a[m], a.con_tro("n", m), vun_xuong(a, 0, m)


a = viz.Mang(list(map(int, input().split())), ten="a")
sap_xep(a)
print(*a)
