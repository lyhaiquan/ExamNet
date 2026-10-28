import sys


def dem(a):
    # Sắp xếp trộn, đếm nghịch thế khi lấy phần tử từ nửa phải trước phần tử của nửa trái
    n = len(a)
    if n < 2:
        return a, 0
    giua = n // 2
    trai, x = dem(a[:giua])
    phai, y = dem(a[giua:])
    kq, i, j, c = [], 0, 0, x + y
    while i < len(trai) and j < len(phai):
        if trai[i] <= phai[j]:
            kq.append(trai[i])
            i += 1
        else:
            kq.append(phai[j])
            c += len(trai) - i
            j += 1
    kq.extend(trai[i:])
    kq.extend(phai[j:])
    return kq, c


data = sys.stdin.read().split()
n = int(data[0])
a = list(map(int, data[1 : n + 1]))
print(dem(a)[1])
