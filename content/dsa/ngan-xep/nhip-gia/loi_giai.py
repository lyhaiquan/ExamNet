import sys

data = sys.stdin.read().split()
n = int(data[0])
gia = list(map(int, data[1 : n + 1]))
st, nhip = [], []
for i, g in enumerate(gia):
    while st and gia[st[-1]] <= g:
        st.pop()
    nhip.append(i - st[-1] if st else i + 1)
    st.append(i)
print(*nhip)
