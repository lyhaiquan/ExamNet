import viz

de = [input().strip() for _ in range(4)]
g = viz.Bang.tu([list(h) for h in de], ten="g")
trong = [(i, j) for i in range(4) for j in range(4) if de[i][j] == "."]
hang = [set() for _ in range(4)]
cot = [set() for _ in range(4)]
khoi = [set() for _ in range(4)]
for i in range(4):
    for j in range(4):
        if de[i][j] != ".":
            hang[i].add(de[i][j])
            cot[j].add(de[i][j])
            khoi[i // 2 * 2 + j // 2].add(de[i][j])
ke_tiep = {o: k + 1 for k, o in enumerate(trong)}  # ô trống (i, j) → chỉ số ô trống sau nó
viz.key(f"Có {len(trong)} ô trống, điền lần lượt từ trái sang phải, trên xuống dưới")


def dien(i, j):
    b = i // 2 * 2 + j // 2
    for d in "1234":
        if d in hang[i] or d in cot[j] or d in khoi[b]:
            continue
        viz.say(f"Ô ({i}, {j}): {d} chưa có ở hàng, cột, khối nên điền thử")
        g[i][j] = d
        hang[i].add(d)
        cot[j].add(d)
        khoi[b].add(d)
        k = ke_tiep[(i, j)]
        if k == len(trong) or dien(*trong[k]):
            return True
        viz.say(f"Điền {d} ở ({i}, {j}) không đi tới đâu: xoá đi, thử số khác")
        g[i][j] = "."
        hang[i].discard(d)
        cot[j].discard(d)
        khoi[b].discard(d)
    viz.loai(f"Ô ({i}, {j}) không điền được số nào: quay lui ô trước")
    return False


if trong:
    dien(*trong[0])
viz.key("Đã điền đủ mọi ô")
for i in range(4):
    print("".join(g.trang_thai()["o"][i]))
