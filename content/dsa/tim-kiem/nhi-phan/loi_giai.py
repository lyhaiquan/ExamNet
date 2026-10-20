import viz

n, q = map(int, input().split())
a = viz.Mang(list(map(int, input().split())), ten="a")
for x in map(int, input().split()):
    viz.key(f"Tìm {x} trong đoạn a[0..{n - 1}]")
    lo, hi = 0, n - 1
    kq = -1
    while lo <= hi:
        mid = (lo + hi) // 2
        a.con_tro("lo", lo)
        a.con_tro("mid", mid)
        a.con_tro("hi", hi)
        giua = a[mid]
        if giua == x:
            viz.say(f"a[{mid}] = {x}: tìm thấy")
            kq = mid
            break
        if giua < x:
            viz.say(f"a[{mid}] = {giua} < {x}: bỏ nửa trái, lo = {mid + 1}")
            lo = mid + 1
        else:
            viz.say(f"a[{mid}] = {giua} > {x}: bỏ nửa phải, hi = {mid - 1}")
            hi = mid - 1
    if kq == -1:
        viz.say(f"lo > hi: đoạn rỗng, không có {x}")
    print(kq)
