import viz

n, S = map(int, input().split())
a = viz.Mang(sorted(map(int, input().split())), ten="a")
con_lai = [0] * (n + 1)
for i in range(n - 1, -1, -1):
    con_lai[i] = con_lai[i + 1] + a[i]
viz.key(f"Đã sắp a tăng dần, cần tổng S = {S}")


def thu(i, tong):
    if tong == S:
        viz.say(f"Tổng đã đúng bằng {S}: được một cách")
        return 1
    if tong + con_lai[i] < S:
        viz.cat(f"Tổng {tong}, lấy hết các số còn lại cũng chỉ được {tong + con_lai[i]} < {S}: cắt")
        return 0
    a.con_tro("i", i)
    if tong + a[i] > S:
        viz.cat(f"Tổng {tong} + a[{i}] = {tong + a[i]} > {S}, các số sau còn lớn hơn: cắt")
        return 0
    viz.say(f"Lấy a[{i}] = {a[i]}: tổng thành {tong + a[i]}")
    lay = thu(i + 1, tong + a[i])
    a.con_tro("i", i)
    viz.say(f"Bỏ a[{i}] = {a[i]}: tổng vẫn là {tong}")
    bo = thu(i + 1, tong)
    return lay + bo


kq = thu(0, 0)
viz.key(f"Có {kq} cách chọn tập con tổng bằng {S}")
print(kq)
