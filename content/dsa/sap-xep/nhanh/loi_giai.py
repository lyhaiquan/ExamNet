import sys

import viz

sys.setrecursionlimit(5000)


def nhanh(lo, hi):
    if lo >= hi:
        return
    chot = a[hi]
    a.con_tro("chốt", hi)
    viz.say(f"Phân hoạch a[{lo}..{hi}] quanh chốt {chot}: số nhỏ hơn dồn về bên trái")
    i = lo
    a.con_tro("i", i)
    for j in range(lo, hi):
        a.con_tro("j", j)
        if a[j] < chot:
            if i != j:
                a[i], a[j] = a[j], a[i]
            i += 1
            a.con_tro("i", i)
    if i != hi:
        a[i], a[hi] = a[hi], a[i]
    viz.key(f"Chốt {chot} về đúng chỗ ô {i}: bên trái nhỏ hơn, bên phải không nhỏ hơn")
    nhanh(lo, i - 1)
    nhanh(i + 1, hi)


a = viz.Mang(list(map(int, input().split())), ten="a")
nhanh(0, len(a) - 1)
print(*a)
