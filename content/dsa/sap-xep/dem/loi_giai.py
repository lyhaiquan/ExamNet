import viz

a = viz.Mang(list(map(int, input().split())), ten="a")
n = len(a)
k = max(a)
dem = viz.Mang([0] * (k + 1), ten="dem")
kq = viz.Mang([0] * n, ten="kq")
viz.key(f"Số lớn nhất là {k}: mảng dem có {k + 1} ô, dem[v] đếm số lần v xuất hiện")

for i in range(n):
    x = a[i]
    viz.say(f"a[{i}] = {x}: tăng dem[{x}]")
    dem[x] += 1
viz.key("Đếm xong. Ghi lần lượt từ giá trị nhỏ nhất: dem[v] lần số v")

vt = 0
for v in range(k + 1):
    c = dem[v]
    if c:
        viz.say(f"dem[{v}] = {c}: ghi {c} lần số {v}")
    for _ in range(c):
        kq[vt] = v
        vt += 1
viz.key("kq đã sắp, không có lần so sánh nào giữa hai phần tử")
print(*kq)
