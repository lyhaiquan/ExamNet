import viz

m, q = map(int, input().split())
T = viz.BangBam(m, ten="T")  # T[h] là dây chuyền ở ô h
for _ in range(q):
    lenh, x = input().split()
    x = int(x)
    h = x % m
    if lenh == "them":
        # x chưa có trong T[h] thì T[h].append(x)
        pass
    elif lenh == "xoa":
        # x có trong T[h] thì T[h].remove(x)
        pass
    else:
        # in YES nếu x có trong T[h], ngược lại NO
        pass
