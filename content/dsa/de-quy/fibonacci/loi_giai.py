import viz

nho = {}


def fib(n):
    if n in nho:
        viz.cat(f"fib({n}) đã có trong bảng nhớ: trả ngay {nho[n]}")
        return nho[n]
    if n < 2:
        kq = n
    else:
        kq = fib(n - 1) + fib(n - 2)
    nho[n] = kq
    viz.say(f"Lưu fib({n}) = {kq} vào bảng nhớ")
    return kq


n = int(input())
viz.key(f"Gọi fib({n})")
print(fib(n))
