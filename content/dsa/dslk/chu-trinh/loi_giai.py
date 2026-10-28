import sys

data = sys.stdin.read().split()
n = int(data[0])
tiep = [0] + list(map(int, data[1 : n + 1]))
rua = tho = 1
gap = False
while tho and tiep[tho]:
    rua = tiep[rua]
    tho = tiep[tiep[tho]]
    if rua == tho:
        gap = True
        break
if not gap:
    print(0, 0)
else:
    rua = 1
    while rua != tho:
        rua, tho = tiep[rua], tiep[tho]
    dai, x = 1, tiep[rua]
    while x != rua:
        x = tiep[x]
        dai += 1
    print(rua, dai)
