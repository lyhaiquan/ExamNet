import viz


def sap_xep(a):
    n = len(a)
    for i in range(1, n):
        j = i
        a.con_tro("j", j)
        viz.say(f"Chèn a[{i}] vào phần đã sắp a[0..{i - 1}]")
        while j > 0 and a[j - 1] > a[j]:
            a[j - 1], a[j] = a[j], a[j - 1]
            j -= 1
            a.con_tro("j", j)
        viz.key(f"Hết lượt {i}: a[0..{i}] đã sắp")


a = viz.Mang(list(map(int, input().split())), ten="a")
sap_xep(a)
print(*a)
