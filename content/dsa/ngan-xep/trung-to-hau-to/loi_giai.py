import viz

UU_TIEN = {"+": 1, "-": 1, "*": 2, "/": 2}

s = input().strip()
st = viz.NganXep(ten="st")
ra = viz.HangDoi(ten="ra")
viz.key(f"Đọc {s} từ trái sang phải")
for c in s:
    if c.isalpha():
        viz.say(f"'{c}' là toán hạng: đưa thẳng ra kết quả")
        ra.enq(c)
    elif c == "(":
        viz.say("Ngoặc mở: đẩy vào ngăn xếp")
        st.push(c)
    elif c == ")":
        viz.say("Ngoặc đóng: đưa các toán tử ra kết quả tới khi gặp ngoặc mở")
        while st.top() != "(":
            ra.enq(st.pop())
        st.pop()
    else:
        while st and st.top() != "(" and UU_TIEN[st.top()] >= UU_TIEN[c]:
            viz.say(f"'{st.top()}' trên đỉnh ưu tiên không kém '{c}': tính trước, đưa ra kết quả")
            ra.enq(st.pop())
        viz.say(f"Đẩy '{c}' vào ngăn xếp, chờ toán hạng bên phải")
        st.push(c)
if st:
    viz.key("Hết biểu thức: đưa các toán tử còn lại ra kết quả")
while st:
    ra.enq(st.pop())
viz.key(f"Hậu tố: {''.join(ra)}")
print("".join(ra))
