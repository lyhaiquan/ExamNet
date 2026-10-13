import viz

n, q = map(int, input().split())
ds = viz.DSLK.tu(map(int, input().split()), ten="ds")
for _ in range(q):
    lenh = input().split()
    i = int(lenh[1])
    if lenh[0] == "chen":
        moi = ds.Nut(int(lenh[2]))
        # i == 0: moi.tiep = ds.dau; ds.dau = moi
        # ngược lại: p = ds.dau, đi i - 1 bước (p = p.tiep); moi.tiep = p.tiep; p.tiep = moi
    else:
        # i == 0: ds.dau = ds.dau.tiep
        # ngược lại: p là nút thứ i - 1; p.tiep = p.tiep.tiep
        pass
print(*ds) if ds.dau else print("RONG")
