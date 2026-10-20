import sys

import viz

sys.setrecursionlimit(5000)  # dãy đã sắp: cây lời gọi sâu tới n tầng


def nhanh(lo, hi):
    # lo >= hi: trả về
    # chot = a[hi]; i = lo
    # j chạy từ lo tới hi - 1: a[j] < chot thì đổi chỗ a[i] với a[j], i += 1
    #   (a.con_tro("i", i), a.con_tro("j", j), a.con_tro("chốt", hi) để thấy con trỏ)
    # Đổi chỗ a[i] với a[hi]; rồi nhanh(lo, i - 1), nhanh(i + 1, hi)
    pass


a = viz.Mang(list(map(int, input().split())), ten="a")
nhanh(0, len(a) - 1)
print(*a)
