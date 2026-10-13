import viz

n = int(input())
ban = viz.Bang(n, n, ".", ten="ban")  # bàn cờ: "Q" là ô có quân hậu
cot = [False] * n                     # cot[c]: cột c đã có quân
cheo1 = [False] * (2 * n)             # cheo1[r + c]: đường chéo xuống trái đã có quân
cheo2 = [False] * (2 * n)             # cheo2[r - c + n]: đường chéo xuống phải đã có quân
ds = []                               # ds[r]: cột của quân hậu ở hàng r


def dat(ds):
    # r = len(ds) là hàng cần đặt; r == n thì đã đặt đủ: trả 1
    # Thử từng cột c chưa bị cot / cheo1 / cheo2 khống chế:
    #   đặt quân (ban[r][c] = "Q", ds.append(c), đánh dấu ba mảng), cộng thêm dat(ds), rồi nhấc ra
    # Không cột nào cho ra lời giải (tổng 0): gọi viz.loai("…") rồi trả 0
    return 0


print(dat(ds))
