import viz

n = int(input())
ds = viz.Mang([], ten="ds")
dung = [False] * (n + 1)
viz.key(f"Dựng hoán vị của 1..{n} từ trái sang phải, mỗi tầng thêm một số chưa dùng")


def thu(ds):
    if len(ds) == n:
        viz.say(f"Đủ {n} số: in {' '.join(map(str, ds))}")
        print(*ds)
        return
    for x in range(1, n + 1):
        if not dung[x]:
            viz.say(f"Thêm {x} vào vị trí {len(ds)}")
            ds.append(x)
            dung[x] = True
            thu(ds)
            ds.pop()
            dung[x] = False
            viz.say(f"Quay lui: gỡ {x} ra để thử số khác")


thu(ds)
viz.key(f"Đã in đủ {n}! hoán vị theo thứ tự từ điển")
