n = int(input())
a = list(map(int, input().split()))
T = sum(a)
co = [False] * (T + 1)
co[0] = True
# với mỗi x, duyệt s từ T về x: co[s] = co[s] or co[s - x]
