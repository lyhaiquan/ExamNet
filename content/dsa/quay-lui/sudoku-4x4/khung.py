import viz

de = [input().strip() for _ in range(4)]
g = viz.Bang.tu([list(h) for h in de], ten="g")  # g[i][j]: "1".."4", "." là ô trống
trong = [(i, j) for i in range(4) for j in range(4) if de[i][j] == "."]  # các ô trống theo thứ tự
hang = [set() for _ in range(4)]  # hang[i]: các số đã có ở hàng i
cot = [set() for _ in range(4)]
khoi = [set() for _ in range(4)]  # khối của ô (i, j) là (i // 2) * 2 + j // 2
for i in range(4):
    for j in range(4):
        if de[i][j] != ".":
            hang[i].add(de[i][j])
            cot[j].add(de[i][j])
            khoi[i // 2 * 2 + j // 2].add(de[i][j])


def dien(i, j):
    # Thử d = "1", "2", "3", "4" chưa có trong hang[i], cot[j], khoi[…]:
    #   g[i][j] = d, thêm d vào ba tập; rồi điền ô trống kế tiếp trong `trong`
    #   (hết ô trống thì xong). Được thì trả True; không thì g[i][j] = ".", bỏ d khỏi ba tập.
    # Thử hết mà không được: viz.loai("…") rồi trả False
    return False


if trong:
    dien(*trong[0])
for i in range(4):
    print("".join(g[i][j] for j in range(4)))
