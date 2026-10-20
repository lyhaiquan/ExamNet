import viz

n = int(input())
nho = viz.Mang([-1] * (n + 1), ten="nho")
viz.key(f"Tính F({n}); mảng nho ghi các giá trị đã tính, -1 là chưa tính")


def fib(i):
    if nho[i] != -1:
        viz.cat(f"nho[{i}] = {nho[i]} đã có: lấy ra dùng, không gọi tiếp")
        return nho[i]
    kq = i if i < 2 else fib(i - 1) + fib(i - 2)
    viz.say(f"Tính xong F({i}) = {kq}: ghi vào nho[{i}]")
    nho[i] = kq
    return kq


kq = fib(n)
viz.key(f"F({n}) = {kq}. Các ô của nho được điền từ nhỏ tới lớn: điền bảng bằng vòng lặp cũng được")
print(kq)
