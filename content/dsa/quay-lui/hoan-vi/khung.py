import viz

n = int(input())
ds = viz.Mang([], ten="ds")  # hoán vị đang dựng dở
dung = [False] * (n + 1)     # dung[x]: x đã có trong ds chưa


def thu(ds):
    # ds đủ n số: in ra (print(*ds)) rồi trả về
    # Ngược lại thử lần lượt x = 1, 2, …, n mà dung[x] còn False:
    #   ds.append(x), dung[x] = True, gọi thu(ds), rồi gỡ ra: ds.pop(), dung[x] = False
    pass


thu(ds)
