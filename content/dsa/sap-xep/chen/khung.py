import viz


def sap_xep(a):
    n = len(a)
    for i in range(1, n):
        j = i
        # Khi j > 0 và a[j - 1] > a[j]: đổi chỗ a[j - 1] với a[j], rồi j -= 1
        # (a.con_tro("j", j) để thấy phần tử đang được chèn)


a = viz.Mang(list(map(int, input().split())), ten="a")
sap_xep(a)
print(*a)
