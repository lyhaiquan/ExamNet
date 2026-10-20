import viz


def vun_xuong(a, i, m):
    while True:
        lon = i
        for c in (2 * i + 1, 2 * i + 2):
            if c < m and a[c] > a[lon]:
                lon = c
        if lon == i:
            return
        a[i], a[lon] = a[lon], a[i]
        i = lon


def sap_xep(a):
    n = len(a)
    a.con_tro("n", n)
    viz.key("Dựng đống lớn nhất: vun xuống các nút có con, từ dưới lên")
    for i in range(n // 2 - 1, -1, -1):
        viz.say(f"Vun xuống ô {i}: đổi với con lớn hơn cho tới khi không con nào lớn hơn")
        vun_xuong(a, i, n)
    viz.key(f"Đã là đống: gốc {list(a)[0]} lớn nhất")
    for m in range(n - 1, 0, -1):
        viz.say(f"Đưa gốc (lớn nhất) về ô {m}; đống còn {m} ô")
        a[0], a[m] = a[m], a[0]
        a.con_tro("n", m)
        viz.say("Vun xuống gốc mới để lại thành đống")
        vun_xuong(a, 0, m)
    viz.key("Đống chỉ còn một ô: cả mảng đã sắp")


a = viz.Mang(list(map(int, input().split())), ten="a")
sap_xep(a)
print(*a)
