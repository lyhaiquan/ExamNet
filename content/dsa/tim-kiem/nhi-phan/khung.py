import viz

n, q = map(int, input().split())
a = viz.Mang(list(map(int, input().split())), ten="a")
for x in map(int, input().split()):
    lo, hi = 0, n - 1
    kq = -1
    # Khi lo <= hi: mid = (lo + hi) // 2
    #   a[mid] == x: kq = mid, dừng;  a[mid] < x: lo = mid + 1;  a[mid] > x: hi = mid - 1
    # (a.con_tro("lo", lo), a.con_tro("mid", mid), a.con_tro("hi", hi) để thấy ba con trỏ)
    print(kq)
