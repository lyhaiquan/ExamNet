import viz

INF = float("inf")
n, m = map(int, input().split())
d = viz.Bang(n, n, INF, ten="d")
for i in range(n):
    d[i][i] = 0
for _ in range(m):
    u, v, w = map(int, input().split())
    if w < d[u][v]:
        d[u][v] = w
viz.key("Ban đầu: độ dài cạnh trực tiếp, không có cạnh là ∞")

for k in range(n):
    viz.key(f"Cho phép đi qua đỉnh {k}: xét mọi cặp i, j qua hàng {k} và cột {k}")
    for i in range(n):
        if i == k:
            continue
        dik = d[i][k]
        if dik == INF:
            continue  # không tới được k từ i: không cặp (i, j) nào cải thiện được qua k
        for j in range(n):
            if j == k or j == i:
                continue
            qua_k = dik + d[k][j]
            if qua_k < d[i][j]:
                viz.say(f"{i} → {k} → {j} dài {qua_k}, ngắn hơn: d[{i}][{j}] = {qua_k}")
                d[i][j] = qua_k

o = d.trang_thai()["o"]
for i in range(n):
    print(*(-1 if o[i][j] in (INF, "∞") else o[i][j] for j in range(n)))
