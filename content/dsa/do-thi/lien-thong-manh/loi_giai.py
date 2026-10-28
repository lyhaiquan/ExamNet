import sys

data = sys.stdin.buffer.read().split()
n, m = int(data[0]), int(data[1])
ke = [[] for _ in range(n + 1)]
ke_dao = [[] for _ in range(n + 1)]
for i in range(m):
    u, v = int(data[2 + 2 * i]), int(data[3 + 2 * i])
    ke[u].append(v)
    ke_dao[v].append(u)

tham = [False] * (n + 1)
xong = []
for s in range(1, n + 1):
    if tham[s]:
        continue
    tham[s] = True
    st = [(s, 0)]
    while st:
        u, i = st[-1]
        if i < len(ke[u]):
            st[-1] = (u, i + 1)
            v = ke[u][i]
            if not tham[v]:
                tham[v] = True
                st.append((v, 0))
        else:
            st.pop()
            xong.append(u)

tham = [False] * (n + 1)
dem = 0
for s in reversed(xong):
    if tham[s]:
        continue
    dem += 1
    tham[s] = True
    st = [s]
    while st:
        u = st.pop()
        for v in ke_dao[u]:
            if not tham[v]:
                tham[v] = True
                st.append(v)
print(dem)
