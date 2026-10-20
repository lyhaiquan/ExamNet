import viz

m, q = map(int, input().split())
T = viz.BangBam(m, ten="T")
for _ in range(q):
    lenh, x = input().split()
    x = int(x)
    h = x % m
    if lenh == "them":
        viz.key(f"Thêm {x}: {x} mod {m} = {h}, xem dây chuyền ô {h}")
        if x not in T[h]:
            viz.say(f"Chưa có {x}: thêm vào cuối dây chuyền ô {h}")
            T[h].append(x)
        else:
            viz.say(f"{x} đã có, không thêm nữa")
    elif lenh == "xoa":
        viz.key(f"Xoá {x}: tìm trong dây chuyền ô {h}")
        if x in T[h]:
            T[h].remove(x)
    else:
        viz.key(f"Tìm {x}: chỉ cần so với dây chuyền ô {h}")
        print("YES" if x in T[h] else "NO")
