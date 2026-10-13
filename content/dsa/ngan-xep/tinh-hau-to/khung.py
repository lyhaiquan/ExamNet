import viz

st = viz.NganXep(ten="st")  # các giá trị chờ dùng
for x in input().split():
    # x là số → st.push(int(x))
    # x là toán tử → b = st.pop() (lấy ra trước là toán hạng PHẢI), a = st.pop(), st.push(a x b)
    pass
# Hết biểu thức: st còn đúng một giá trị, đó là kết quả
print(st.pop() if st else "?")
