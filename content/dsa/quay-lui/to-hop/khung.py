import viz

n, k = map(int, input().split())
ds = viz.Mang([], ten="ds")  # các số đã chọn, tăng dần


def thu(bat_dau, ds):
    # ds đủ k số: in ra (print(*ds)) rồi trả về
    # Cắt nhánh: số còn lại n - bat_dau + 1 ít hơn số còn thiếu k - len(ds) → viz.cat("…") rồi trả về
    # Ngược lại thử x = bat_dau, …, n: ds.append(x), gọi thu(x + 1, ds), rồi ds.pop()
    pass


thu(1, ds)
