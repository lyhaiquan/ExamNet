import viz

n = int(input())
ds = viz.DSLK.tu(map(int, input().split()), ten="ds")
viz.key("Bắt đầu: prev = None, cur ở nút đầu")
prev = None
cur = ds.dau
while cur is not None:
    nxt = cur.tiep
    viz.say("Giữ nút sau bằng nxt, rồi quay mũi tên của cur về prev")
    cur.tiep = prev
    prev = cur
    cur = nxt
    viz.say("prev và cur cùng tiến lên một nút")
viz.key("cur đã đi hết danh sách: prev là nút đầu mới")
ds.dau = prev
print(*ds)
