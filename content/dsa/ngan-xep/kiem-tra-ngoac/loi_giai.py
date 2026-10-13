import viz

CAP = {")": "(", "]": "[", "}": "{"}


def hop_le(s):
    st = viz.NganXep(ten="st")
    for k, c in enumerate(s):
        if c in "([{":
            viz.say(f"Ký tự {k + 1} là '{c}': ngoặc mở, đẩy vào ngăn xếp")
            st.push(c)
        else:
            if not st:
                viz.key(f"Ký tự {k + 1} là '{c}' nhưng ngăn xếp rỗng: không có ngoặc mở nào để ghép")
                return False
            viz.say(f"Ký tự {k + 1} là '{c}': lấy ngoặc mở gần nhất ra để ghép")
            m = st.pop()
            if m != CAP[c]:
                viz.key(f"'{m}' không cùng loại với '{c}': dãy không hợp lệ")
                return False
            viz.say(f"'{m}' ghép đúng với '{c}'")
    if st:
        viz.key(f"Hết dãy mà còn {len(st)} ngoặc mở chưa được đóng: không hợp lệ")
        return False
    viz.key("Hết dãy và ngăn xếp rỗng: dãy hợp lệ")
    return True


s = input().strip()
print("YES" if hop_le(s) else "NO")
