s = input().strip()
k = int(input())
st = []
for c in s:
    while k and st and st[-1] > c:
        st.pop()
        k -= 1
    st.append(c)
if k:
    st = st[:-k]
print("".join(st).lstrip("0") or "0")
