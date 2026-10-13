import viz

n = int(input())
h = viz.Mang([], ten="h")
for x in map(int, input().split()):
    viz.key(f"Thêm {x} vào cuối heap")
    h.append(x)
    i = len(h) - 1
    while i > 0:
        cha = (i - 1) // 2
        if h[cha] <= h[i]:
            viz.say("Cha không lớn hơn con: dừng vun lên")
            break
        viz.say("Cha lớn hơn con: đổi chỗ, đi lên một tầng")
        h[cha], h[i] = h[i], h[cha]
        i = cha
print(*h)
