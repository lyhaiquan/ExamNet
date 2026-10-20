import viz

n, x = map(int, input().split())
a = viz.Mang(list(map(int, input().split())), ten="a")
kq = -1
# Duyệt i từ 0 tới n - 1 (a.con_tro("i", i) để thấy con trỏ): a[i] == x thì kq = i và dừng (break)
print(kq)
