"""Sinh input cho bài cây đối xứng gương: python sinh_test.py <nho|tb|lon> <seed>."""

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

if co == "lon" and seed <= 2:
    n = r.randint(1, 3)
    con = cay_ngau_nhien(n)
    in_cay(n, con, {v: 5 for v in range(1, n + 1)})
else:
    m = {"nho": r.randint(1, 3), "tb": r.randint(3, 12)}.get(co, r.randint(20_000, 49_000))
    nua = cay_ngau_nhien(m, sau=co == "lon" and seed % 2 == 0)
    lim = 3 if co != "lon" else 10**9
    k_nua = {v: r.randint(-lim, lim) for v in nua}
    # gốc 1, cây con trái là nửa (đánh số 2..m+1), cây con phải là ảnh gương (m+2..2m+1)
    n = 2 * m + 1
    con = {1: [2, m + 2]}
    khoa = {1: r.randint(-lim, lim)}
    for v in nua:
        a, b = nua[v]
        con[v + 1] = [a + 1 if a else 0, b + 1 if b else 0]
        con[v + m + 1] = [b + m + 1 if b else 0, a + m + 1 if a else 0]
        khoa[v + 1] = khoa[v + m + 1] = k_nua[v]
    if r.random() < 0.5:
        v = r.randint(2, n)
        if r.random() < 0.5:
            khoa[v] += 1
        else:
            la = [u for u in range(2, n + 1) if con[u] == [0, 0]]
            u = r.choice(la)
            k = r.randint(0, 1)
            n += 1
            con[u][k] = n
            con[n] = [0, 0]
            khoa[n] = 0
    in_cay(n, con, khoa)
