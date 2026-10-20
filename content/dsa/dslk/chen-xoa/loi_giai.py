import viz

n, q = map(int, input().split())
ds = viz.DSLK.tu(map(int, input().split()), ten="ds")
for _ in range(q):
    lenh = input().split()
    i = int(lenh[1])
    if lenh[0] == "chen":
        x = int(lenh[2])
        viz.key(f"Chèn {x} vào vị trí {i}")
        moi = ds.Nut(x)
        if i == 0:
            viz.say("Vị trí 0: nút mới trỏ tới nút đầu cũ, rồi thành nút đầu")
            moi.tiep = ds.dau
            ds.dau = moi
            continue
        p = ds.dau
        for _ in range(i - 1):
            p = p.tiep
        viz.say(f"p đứng ở nút thứ {i - 1}: nút mới trỏ tới nút sau p trước, rồi p mới trỏ tới nút mới")
        moi.tiep = p.tiep
        p.tiep = moi
    else:
        viz.key(f"Xoá nút ở vị trí {i}")
        if i == 0:
            viz.say("Vị trí 0: nút đầu chuyển sang nút thứ hai")
            ds.dau = ds.dau.tiep
            continue
        p = ds.dau
        for _ in range(i - 1):
            p = p.tiep
        viz.say(f"p đứng ở nút thứ {i - 1}: cho p trỏ vượt qua nút cần xoá")
        p.tiep = p.tiep.tiep
print(*ds) if ds.dau else print("RONG")
