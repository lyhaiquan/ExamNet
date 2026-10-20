import viz


def sap_xep(a):
    n = len(a)
    for i in range(n - 1):
        m = i  # vị trí phần tử nhỏ nhất đã thấy trong a[i..n-1]
        # Duyệt j từ i + 1 tới n - 1: a[j] < a[m] thì m = j
        # (a.con_tro("i", i), a.con_tro("min", m) để thấy con trỏ)
        # Hết vòng: m khác i thì đổi chỗ a[i] với a[m] (mỗi lượt tối đa một lần)


a = viz.Mang(list(map(int, input().split())), ten="a")
sap_xep(a)
print(*a)
