A = input().strip()
B = input().strip()
truoc = [0] * (len(B) + 1)
dai_nhat = 0
for a in A:
    nay = [0] * (len(B) + 1)
    for j, b in enumerate(B, 1):
        if a == b:
            nay[j] = truoc[j - 1] + 1
    dai_nhat = max(dai_nhat, max(nay))
    truoc = nay
print(dai_nhat)
