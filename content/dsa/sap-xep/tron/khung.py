import viz


def tron(trai, phai):
    # Đoạn một phần tử (trai >= phai): trả về
    # giua = (trai + phai) // 2; tron(trai, giua); tron(giua + 1, phai)
    # Trộn a[trai..giua] với a[giua+1..phai] vào list tạm (lấy số nhỏ hơn trước, bằng thì lấy bên trái),
    # rồi chép tạm đè lên a[trai..phai]
    pass


a = viz.Mang(list(map(int, input().split())), ten="a")
tron(0, len(a) - 1)
print(*a)
