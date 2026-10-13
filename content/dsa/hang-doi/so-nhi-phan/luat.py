"""Luật soi trace bài số nhị phân (spec §8.2 bước 3): các số phải lần lượt được lấy ra khỏi hàng đợi q.

Bắt được: in thẳng bằng cách tự đổi cơ số (chia 2 lấy dư) mà không dùng hàng đợi, hoặc dùng hàng đợi sai thứ
tự (lấy ở cuối như ngăn xếp). Cấm `bin`/`format` thì kiem_cam.py đã bắt.
"""


def kiem(su_kien, dau_vao):
    """Trả None nếu đạt, hoặc một câu giải thích vi phạm."""
    n = int(dau_vao.split()[0])
    ra = [e.get("x") for e in su_kien if e.get("t") == "deq" and e.get("v") == "q"]
    if len(ra) < n:
        return (f"Chỉ lấy {len(ra)} xâu ra khỏi hàng đợi q, cần {n}: hãy sinh từng số bằng "
                "q.deq() rồi q.enq(x + \"0\"), q.enq(x + \"1\")")
    for k in range(1, n + 1):
        dung = bin(k)[2:]
        if str(ra[k - 1]) != dung:
            return f"Xâu thứ {k} lấy ra khỏi hàng đợi là {ra[k - 1]!r}, phải là {dung!r}"
    return None
