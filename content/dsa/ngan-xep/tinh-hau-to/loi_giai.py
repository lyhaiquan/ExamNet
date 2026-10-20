import viz

st = viz.NganXep(ten="st")
viz.key("Đọc từ trái sang phải: số thì cất vào ngăn xếp, toán tử thì lấy hai số ra tính")
for x in input().split():
    if x.isdigit():
        viz.say(f"{x} là số: đẩy vào ngăn xếp")
        st.push(int(x))
        continue
    viz.say(f"'{x}': lấy hai giá trị trên cùng ra, giá trị lấy ra trước là toán hạng bên phải")
    b = st.pop()
    a = st.pop()
    kq = a + b if x == "+" else a - b if x == "-" else a * b
    viz.say(f"{a} {x} {b} = {kq}: đẩy kết quả vào ngăn xếp")
    st.push(kq)
kq = st.pop()
viz.key(f"Hết biểu thức, ngăn xếp còn một giá trị: {kq}")
print(kq)
