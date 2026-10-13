import viz


def tron(trai, phai):
    if trai >= phai:
        return
    giua = (trai + phai) // 2
    tron(trai, giua)
    tron(giua + 1, phai)
    viz.say(f"Trộn hai nửa đã sắp a[{trai}..{giua}] và a[{giua + 1}..{phai}]")
    tam = []
    i, j = trai, giua + 1
    while i <= giua and j <= phai:
        if a[i] <= a[j]:
            tam.append(a[i])
            i += 1
        else:
            tam.append(a[j])
            j += 1
    while i <= giua:
        tam.append(a[i])
        i += 1
    while j <= phai:
        tam.append(a[j])
        j += 1
    for k, x in enumerate(tam):
        a[trai + k] = x
    viz.key(f"a[{trai}..{phai}] đã sắp")


a = viz.Mang(list(map(int, input().split())), ten="a")
tron(0, len(a) - 1)
print(*a)
