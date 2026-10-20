"""Luật soi trace bài tìm kiếm nhị phân: mỗi câu hỏi đọc không quá 2·⌈log₂ n⌉ + 2 ô của a.

Tìm tuần tự đọc trung bình n/2 ô mỗi câu (test tb có 30 số, 4 câu hỏi): vượt xa giới hạn.
"""

import math


def kiem(su_kien, dau_vao):
    n, q = map(int, dau_vao.split()[:2])
    doc = sum(1 for e in su_kien if e.get("t") == "read" and e.get("v") == "a")
    toi_da = q * (2 * math.ceil(math.log2(max(n, 2))) + 2)
    if doc > toi_da:
        return (f"Đọc {doc} ô của a cho {q} câu hỏi: tìm kiếm nhị phân mỗi câu chỉ cần khoảng log₂ {n} lần so, "
                f"cả {q} câu không quá {toi_da}")
    return None
