n, S = map(int, input().split())
a = sorted(map(int, input().split()))
dang_chon, kq = [], []


def thu(bat_dau, con_lai):
    if con_lai == 0:
        kq.append(" ".join(map(str, dang_chon)))
        return
    for i in range(bat_dau, n):
        if a[i] > con_lai:
            break
        dang_chon.append(a[i])
        thu(i, con_lai - a[i])
        dang_chon.pop()


thu(0, S)
print(len(kq))
if kq:
    print("\n".join(kq))
