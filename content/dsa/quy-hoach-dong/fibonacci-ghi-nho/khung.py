import viz

n = int(input())
nho = viz.Mang([-1] * (n + 1), ten="nho")  # nho[i] = F(i) đã tính; -1 là chưa tính


def fib(i):
    # nho[i] != -1: đã tính rồi → viz.cat("…") rồi trả nho[i] luôn
    # i < 2: F(i) = i; ngược lại F(i) = fib(i - 1) + fib(i - 2)
    # Ghi vào nho[i] rồi mới trả về
    return 0


print(fib(n))
