import viz

nho = {}


def fib(n):
    # F(0) = 0, F(1) = 1, F(n) = fib(n - 1) + fib(n - 2)
    # Ghi nhớ: n đã có trong nho thì trả nho[n] ngay
    #   (gọi viz.cat("đã có trong bảng nhớ") trước đó để thấy nhánh bị cắt trên cây lời gọi)
    # Tính xong thì lưu nho[n] rồi mới trả về.
    return 0


n = int(input())
print(fib(n))
