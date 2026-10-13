import viz

n, S = map(int, input().split())
xu = list(map(int, input().split()))
dp = viz.Mang([-1] * (S + 1), ten="dp")
dp[0] = 0
cuoi = [0] * (S + 1)  # cuoi[s]: đồng cuối cùng trong cách đổi s tốt nhất, để truy vết
viz.key(f"dp[s]: số đồng ít nhất để đổi s; dp[0] = 0, các ô khác -1 (chưa đổi được)")

for s in range(1, S + 1):
    dp.con_tro("s", s)
    for x in xu:
        if x > s:
            continue
        con = dp[s - x]
        if con == -1:
            continue
        hien = dp[s]
        if hien == -1 or con + 1 < hien:
            viz.say(f"Đồng cuối là {x}: đổi {s - x} hết {con} đồng, cộng 1 thành {con + 1}")
            dp[s] = con + 1
            cuoi[s] = x
    if s <= 12 or s == S:
        viz.key(f"dp[{s}] = {dp[s]}" + (": không đổi được" if dp[s] == -1 else ""))

kq = dp[S]
if kq != -1:
    dong, s = [], S
    while s:
        dong.append(cuoi[s])
        s -= cuoi[s]
    viz.key(f"Đổi {S} = {' + '.join(map(str, dong))}: {kq} đồng")
print(kq)
