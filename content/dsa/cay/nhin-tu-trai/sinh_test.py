"""Sinh input cho bài nhìn cây từ bên trái: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
def cay_ngau_nhien(n, sau=False):
    con = {1: [0, 0]}
    ds = [1]
    for v in range(2, n + 1):
        while True:
            u = ds[-1] if sau and r.random() < 0.9 else r.choice(ds)
            trong = [k for k in (0, 1) if con[u][k] == 0]
            if trong:
                break
        con[u][r.choice(trong)] = v
        con[v] = [0, 0]
        ds.append(v)
    return con


def in_cay(n, con, khoa):
    nhan = list(range(2, n + 1))
    r.shuffle(nhan)
    doi = {0: 0, 1: 1}
    doi.update({v: nhan[v - 2] for v in range(2, n + 1)})
    dong = [None] * (n + 1)
    for v in range(1, n + 1):
        dong[doi[v]] = f"{khoa[v]} {doi[con[v][0]]} {doi[con[v][1]]}"
    print(n)
    print(*dong[1:], sep="\n")

n = {"nho": r.randint(1, 8), "tb": r.randint(8, 50)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(50_000, 100_000))
con = cay_ngau_nhien(n, sau=co == "lon" and seed % 2 == 0)
lim = 20 if co != "lon" else 10**9
in_cay(n, con, {v: r.randint(-lim, lim) for v in con})
