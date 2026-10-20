import viz

n = int(input())
ban = viz.Bang(n, n, ".", ten="ban")
cot = [False] * n
cheo1 = [False] * (2 * n)
cheo2 = [False] * (2 * n)
ds = []
viz.key(f"Đặt từng hàng một quân hậu, từ hàng 0 tới hàng {n - 1}")


def dat(ds):
    r = len(ds)
    if r == n:
        viz.key(f"Đặt đủ {n} quân, không quân nào ăn nhau: được một cách")
        return 1
    dem = 0
    for c in range(n):
        if cot[c] or cheo1[r + c] or cheo2[r - c + n]:
            continue
        viz.say(f"Ô ({r}, {c}) không bị khống chế: đặt quân hậu")
        ban[r][c] = "Q"
        ds.append(c)
        cot[c] = cheo1[r + c] = cheo2[r - c + n] = True
        dem += dat(ds)
        viz.say(f"Nhấc quân ở ({r}, {c}) ra, thử cột sau")
        ban[r][c] = "."
        ds.pop()
        cot[c] = cheo1[r + c] = cheo2[r - c + n] = False
    if dem == 0:
        viz.loai(f"Hàng {r}: không cột nào dẫn tới lời giải, quay lui")
    return dem


kq = dat(ds)
viz.key(f"Có {kq} cách đặt {n} quân hậu")
print(kq)
