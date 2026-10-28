import sys

data = sys.stdin.read().split()
t = int(data[0])
kq = []
for duong_dan in data[1 : 1 + t]:
    st = []
    for ten in duong_dan.split("/"):
        if ten == "" or ten == ".":
            continue
        if ten == "..":
            if st:
                st.pop()
        else:
            st.append(ten)
    kq.append("/" + "/".join(st))
print("\n".join(kq))
