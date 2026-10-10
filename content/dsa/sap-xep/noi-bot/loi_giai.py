import viz


def sap_xep(a):
    n = len(a)
    for i in range(n - 1):
        for j in range(n - 1 - i):
            x, y = a[j], a[j + 1]
            if x > y:
                a[j], a[j + 1] = y, x
                viz.say(f"{x} > {y} nên đổi chỗ")
        # list(a) chép mảng mà không sinh sự kiện đọc, để thuyết minh không làm rối animation
        viz.key(f"Hết lượt {i + 1}: {list(a)[n - 1 - i]} đã về đúng chỗ")


a = viz.Mang(list(map(int, input().split())), ten="a")
sap_xep(a)
print(*a)
