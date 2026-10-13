import viz

h = viz.Mang([], ten="h")


def vun_len(i):
    while i > 0:
        cha = (i - 1) // 2
        if h[i] >= h[cha]:
            return
        h[i], h[cha] = h[cha], h[i]
        i = cha


def vun_xuong(i):
    n = len(h)
    while True:
        nho = i
        for c in (2 * i + 1, 2 * i + 2):
            if c < n and h[c] < h[nho]:
                nho = c
        if nho == i:
            return
        h[i], h[nho] = h[nho], h[i]
        i = nho


for _ in range(int(input())):
    lenh = input().split()
    if lenh[0] == "them":
        x = int(lenh[1])
        viz.key(f"Thêm {x}: đặt vào cuối mảng rồi vun lên, đổi chỗ với cha khi nhỏ hơn cha")
        h.append(x)
        vun_len(len(h) - 1)
    elif not h:
        viz.key("Lấy min: đống rỗng")
        print("RONG")
    else:
        x = h[0]
        viz.key(f"Lấy min: gốc {x} nhỏ nhất. Đưa ô cuối lên gốc rồi vun xuống")
        print(x)
        cuoi = h.pop()
        if h:
            h[0] = cuoi
            vun_xuong(0)
