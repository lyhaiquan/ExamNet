import viz


def hop_le(s):
    st = viz.NganXep(ten="st")
    # Duyệt từng ký tự c của s:
    #   ngoặc mở  → st.push(c)
    #   ngoặc đóng → st.pop() lấy ngoặc mở gần nhất ra, kiểm tra có cùng loại không
    # Hết dãy mà st vẫn còn phần tử thì cũng không hợp lệ.
    return True


s = input().strip()
print("YES" if hop_le(s) else "NO")
