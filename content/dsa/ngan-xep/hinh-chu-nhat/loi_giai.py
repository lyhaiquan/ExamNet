import sys

data = sys.stdin.read().split()
n = int(data[0])
h = list(map(int, data[1 : n + 1])) + [0]  # cột cao 0 ở cuối đẩy mọi cột ra khỏi ngăn xếp
st = []
lon_nhat = 0
for i, x in enumerate(h):
    while st and h[st[-1]] >= x:
        cao = h[st.pop()]
        trai = st[-1] + 1 if st else 0
        lon_nhat = max(lon_nhat, cao * (i - trai))
    st.append(i)
print(lon_nhat)
