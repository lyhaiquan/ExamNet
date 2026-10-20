import viz

n = int(input())
q = viz.HangDoi(ten="q")
q.enq("1")
for k in range(1, n + 1):
    x = q.deq()
    viz.say(f"Lấy {x} ra khỏi đầu hàng: đó là số {k}. Đưa {x}0 và {x}1 vào cuối hàng")
    print(x)
    q.enq(x + "0")
    q.enq(x + "1")
