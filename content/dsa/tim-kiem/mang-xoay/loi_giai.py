import sys


def tim(a, x):
    lo, hi = 0, len(a) - 1
    while lo <= hi:
        giua = (lo + hi) // 2
        if a[giua] == x:
            return giua
        if a[lo] <= a[giua]:  # nửa trái [lo, giua] tăng dần
            if a[lo] <= x < a[giua]:
                hi = giua - 1
            else:
                lo = giua + 1
        else:  # nửa phải [giua, hi] tăng dần
            if a[giua] < x <= a[hi]:
                lo = giua + 1
            else:
                hi = giua - 1
    return -1


data = sys.stdin.read().split()
n, q = int(data[0]), int(data[1])
a = list(map(int, data[2 : 2 + n]))
print("\n".join(str(tim(a, int(x))) for x in data[2 + n : 2 + n + q]))
