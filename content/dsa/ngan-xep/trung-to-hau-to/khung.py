import viz

UU_TIEN = {"+": 1, "-": 1, "*": 2, "/": 2}

s = input().strip()
st = viz.NganXep(ten="st")  # toán tử và ngoặc mở chưa xử lý
ra = viz.HangDoi(ten="ra")  # biểu thức hậu tố, đưa vào bằng ra.enq(x)
for c in s:
    # chữ cái → ra.enq(c);  "(" → st.push(c)
    # ")" → lấy toán tử trên st đưa vào ra tới khi gặp "(", rồi bỏ "("
    # toán tử → khi st.top() là toán tử có UU_TIEN >= UU_TIEN[c] thì đưa nó sang ra; rồi st.push(c)
    pass
# hết biểu thức: đưa hết toán tử còn trên st sang ra
print("".join(ra))
