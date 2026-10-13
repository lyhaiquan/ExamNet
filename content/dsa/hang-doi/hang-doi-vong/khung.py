import viz

k, q = map(int, input().split())
a = viz.Mang([0] * k, ten="a")  # đúng k ô, không thêm bớt; 0 là ô trống
dau = 0  # ô của phần tử đầu hàng
so = 0   # số phần tử đang có trong hàng
for _ in range(q):
    lenh = input().split()
    if lenh[0] == "them":
        x = int(lenh[1])
        # so == k: print("DAY"). Ngược lại a[(dau + so) % k] = x, so += 1
    else:
        # so == 0: print("RONG"). Ngược lại in a[dau], a[dau] = 0, dau = (dau + 1) % k, so -= 1
        pass
    a.con_tro("dau", dau)
    a.con_tro("cuoi", (dau + so) % k)
