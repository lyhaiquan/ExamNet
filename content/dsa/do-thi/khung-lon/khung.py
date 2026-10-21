n, m = map(int, input().split())
canh = []
for _ in range(m):
    u, v, w = map(int, input().split())
    canh.append((w, u, v))
cha = list(range(n + 1))


def tim(x):
    # gốc của thành phần chứa x, có nén đường đi
    return x
