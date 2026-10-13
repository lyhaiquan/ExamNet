import viz

k, q = map(int, input().split())
a = viz.Mang([0] * k, ten="a")
dau = 0
so = 0
a.con_tro("dau", dau)
a.con_tro("cuoi", dau)
viz.key(f"Mảng {k} ô, hàng rỗng: dau = cuoi = 0")
for _ in range(q):
    lenh = input().split()
    if lenh[0] == "them":
        x = int(lenh[1])
        if so == k:
            viz.say(f"Thêm {x}: hàng đã đủ {k} phần tử, in DAY")
            print("DAY")
            continue
        cuoi = (dau + so) % k
        viz.say(f"Thêm {x} vào ô cuoi = ({dau} + {so}) % {k} = {cuoi}")
        a[cuoi] = x
        so += 1
    else:
        if so == 0:
            viz.say("Lấy ra: hàng rỗng, in RONG")
            print("RONG")
            continue
        viz.say(f"Lấy ra a[{dau}] ở đầu hàng, rồi dau tiến lên ({dau} + 1) % {k} = {(dau + 1) % k}")
        print(a[dau])
        a[dau] = 0
        dau = (dau + 1) % k
        so -= 1
    a.con_tro("dau", dau)
    a.con_tro("cuoi", (dau + so) % k)
    if so and (dau + so - 1) % k < dau:
        viz.key(f"Hàng quay vòng: đầu ở ô {dau}, cuối ở ô {(dau + so - 1) % k}")
