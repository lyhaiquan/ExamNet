n, m = map(int, input().split())
ke = [[] for _ in range(n + 1)]
ke_dao = [[] for _ in range(n + 1)]
for _ in range(m):
    u, v = map(int, input().split())
    ke[u].append(v)
    ke_dao[v].append(u)
