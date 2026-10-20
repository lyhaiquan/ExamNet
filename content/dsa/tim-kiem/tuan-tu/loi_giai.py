import viz

n, x = map(int, input().split())
a = viz.Mang(list(map(int, input().split())), ten="a")
viz.key(f"Tìm {x}: so lần lượt với từng phần tử từ trái sang phải")
kq = -1
for i in range(n):
    a.con_tro("i", i)
    if a[i] == x:
        viz.key(f"a[{i}] = {x}: tìm thấy ở vị trí {i}, dừng")
        kq = i
        break
    viz.say(f"a[{i}] khác {x}, sang ô sau")
if kq == -1:
    viz.key(f"Đã so hết {n} phần tử: không có {x}")
print(kq)
