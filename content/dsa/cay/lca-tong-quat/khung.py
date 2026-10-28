n, q = map(int, input().split())
cha = [0, 0] + list(map(int, input().split()))  # cha[1] = 0
sau = [0] * (n + 1)  # độ sâu; cha[i] < i nên tính theo thứ tự i = 2..n
for _ in range(q):
    u, v = map(int, input().split())
