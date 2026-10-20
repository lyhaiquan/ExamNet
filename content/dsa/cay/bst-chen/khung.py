import viz

T = viz.Cay(ten="T")


def chen(nut, k):
    # Chèn khoá k vào cây con gốc `nut`, trả về gốc của cây con đó sau khi chèn.
    #   nut là None  → return T.Nut(k)
    #   k < nut.khoa → nut.trai = chen(nut.trai, k)
    #   ngược lại    → nut.phai = chen(nut.phai, k)
    return nut


def duyet_truoc(nut, kq):
    # Duyệt trước: thêm nut.khoa vào kq, rồi duyệt cây con trái, rồi cây con phải.
    pass


n = int(input())
for k in map(int, input().split()):
    T.goc = chen(T.goc, k)
kq = []
duyet_truoc(T.goc, kq)
print(*kq)
