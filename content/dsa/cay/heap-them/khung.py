import viz

n = int(input())
h = viz.Mang([], ten="h")
for x in map(int, input().split()):
    # Thêm x vào cuối: h.append(x). Rồi vun lên:
    #   i = vị trí vừa thêm; khi i > 0 và h[(i - 1) // 2] > h[i] thì đổi chỗ ô i với ô cha, i = cha.
    pass
print(*h)
