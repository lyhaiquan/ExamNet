import viz

h = viz.Mang([], ten="h")  # đống nhỏ nhất: con của ô i là 2i + 1 và 2i + 2, cha là (i - 1) // 2


def vun_len(i):
    # Khi i > 0 và h[i] < h[cha] (cha = (i - 1) // 2): đổi chỗ h[i] với h[cha], i = cha
    pass


def vun_xuong(i):
    # Con nhỏ hơn trong hai con 2i + 1, 2i + 2 (con nào < len(h)); nhỏ hơn h[i] thì đổi chỗ, đi xuống tiếp
    pass


for _ in range(int(input())):
    lenh = input().split()
    if lenh[0] == "them":
        h.append(int(lenh[1]))
        vun_len(len(h) - 1)
    elif not h:
        print("RONG")
    else:
        print(h[0])
        cuoi = h.pop()
        if h:
            h[0] = cuoi
            vun_xuong(0)
