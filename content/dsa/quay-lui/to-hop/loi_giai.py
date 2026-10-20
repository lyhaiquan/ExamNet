import viz

n, k = map(int, input().split())
ds = viz.Mang([], ten="ds")
viz.key(f"Chọn {k} số trong 1..{n}, số chọn sau luôn lớn hơn số chọn trước")


def thu(bat_dau, ds):
    if len(ds) == k:
        viz.say(f"Đủ {k} số: in {' '.join(map(str, ds))}")
        print(*ds)
        return
    con, thieu = n - bat_dau + 1, k - len(ds)
    if con < thieu:
        viz.cat(f"Chỉ còn {con} số từ {bat_dau} tới {n}, cần thêm {thieu}: không đủ, cắt")
        return
    for x in range(bat_dau, n + 1):
        viz.say(f"Chọn {x}, số sau phải từ {x + 1} trở lên")
        ds.append(x)
        thu(x + 1, ds)
        ds.pop()
        viz.say(f"Quay lui: bỏ {x} ra")


thu(1, ds)
viz.key("Đã in mọi tổ hợp theo thứ tự từ điển")
