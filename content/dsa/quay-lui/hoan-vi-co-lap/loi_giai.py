n = int(input())
a = sorted(map(int, input().split()))
da_dung = [False] * n
dang_chon, kq = [], []


def thu():
    if len(dang_chon) == n:
        kq.append(" ".join(map(str, dang_chon)))
        return
    for i in range(n):
        if da_dung[i] or (i and a[i] == a[i - 1] and not da_dung[i - 1]):
            continue
        da_dung[i] = True
        dang_chon.append(a[i])
        thu()
        dang_chon.pop()
        da_dung[i] = False


thu()
print(len(kq))
print("\n".join(kq))
