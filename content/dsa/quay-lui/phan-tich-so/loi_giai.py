n = int(input())
kq = []
dang_chon = []


def thu(con_lai, toi_da):
    if con_lai == 0:
        kq.append(" ".join(map(str, dang_chon)))
        return
    for x in range(min(con_lai, toi_da), 0, -1):
        dang_chon.append(x)
        thu(con_lai - x, x)
        dang_chon.pop()


thu(n, n)
print(len(kq))
print("\n".join(kq))
