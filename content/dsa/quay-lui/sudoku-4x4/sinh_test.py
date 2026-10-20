"""Sinh input cho bài Sudoku 4×4: python sinh_test.py <nho|tb|lon> <seed>.

Lấy một lưới đầy đủ hợp lệ, xáo bằng các phép giữ tính hợp lệ (đổi tên số, đổi hàng trong một dải, đổi hai
dải, đổi cột trong một chồng, đổi hai chồng, lật chéo), rồi xoá dần từng ô theo thứ tự ngẫu nhiên, chỉ xoá khi
đề vẫn có đúng một cách giải. Test nhỏ xoá ít ô; test lớn xoá tới khi không xoá thêm được ô nào.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")

g = [[1, 2, 3, 4], [3, 4, 1, 2], [2, 1, 4, 3], [4, 3, 2, 1]]
ten = [1, 2, 3, 4]
r.shuffle(ten)
g = [[ten[x - 1] for x in h] for h in g]
for d in (0, 2):
    if r.random() < 0.5:
        g[d], g[d + 1] = g[d + 1], g[d]
if r.random() < 0.5:
    g = g[2:] + g[:2]
g = [list(c) for c in zip(*g)]  # cột thành hàng: đổi cột như đổi hàng
for d in (0, 2):
    if r.random() < 0.5:
        g[d], g[d + 1] = g[d + 1], g[d]
if r.random() < 0.5:
    g = g[2:] + g[:2]
if r.random() < 0.5:
    g = [list(c) for c in zip(*g)]


def so_cach(o, toi_da=2):
    """Số cách giải của lưới o (0 là ô trống), dừng khi đã thấy toi_da cách."""
    for k in range(16):
        i, j = divmod(k, 4)
        if o[i][j] == 0:
            dem = 0
            for d in range(1, 5):
                b = [o[x][y] for x in range(i // 2 * 2, i // 2 * 2 + 2) for y in range(j // 2 * 2, j // 2 * 2 + 2)]
                if d not in o[i] and d not in (o[x][j] for x in range(4)) and d not in b:
                    o[i][j] = d
                    dem += so_cach(o, toi_da - dem)
                    o[i][j] = 0
                    if dem >= toi_da:
                        break
            return dem
    return 1


toi_da_xoa = {"nho": 10, "tb": 11}.get(co, 16)
o = [h[:] for h in g]
xoa = 0
thu_tu = list(range(16))
r.shuffle(thu_tu)
for k in thu_tu:
    if xoa == toi_da_xoa:
        break
    i, j = divmod(k, 4)
    giu, o[i][j] = o[i][j], 0
    if so_cach([h[:] for h in o]) == 1:
        xoa += 1
    else:
        o[i][j] = giu
for h in o:
    print("".join("." if x == 0 else str(x) for x in h))
