import viz


def sap_xep(a):
    n = len(a)
    for i in range(n - 1):
        m = i
        a.con_tro("i", i)
        a.con_tro("min", m)
        for j in range(i + 1, n):
            if a[j] < a[m]:
                m = j
                a.con_tro("min", m)
        if m != i:
            viz.say(f"Nhỏ nhất của đoạn từ ô {i} là ô {m}: đổi chỗ với ô {i}")
            a[i], a[m] = a[m], a[i]
        else:
            viz.say(f"Ô {i} đã là nhỏ nhất của đoạn: không đổi chỗ")
        viz.key(f"Hết lượt {i + 1}: {i + 1} phần tử đầu đã đúng chỗ")


a = viz.Mang(list(map(int, input().split())), ten="a")
sap_xep(a)
print(*a)
