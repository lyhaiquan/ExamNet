import viz

n = int(input())
ds = viz.DSLK.tu(map(int, input().split()), ten="ds")
# Đảo ngược bằng ba biến trỏ:
#   prev = None, cur = ds.dau
#   khi cur khác None: nxt = cur.tiep; cur.tiep = prev; prev = cur; cur = nxt
#   cuối cùng: ds.dau = prev

print(*ds)
