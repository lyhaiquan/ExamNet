import heapq

k = int(input())
day = []
for _ in range(k):
    dong = list(map(int, input().split()))
    day.append(dong[1:])
dong = []  # (giá trị, số thứ tự dãy, vị trí trong dãy)
kq = []
print(*kq)
