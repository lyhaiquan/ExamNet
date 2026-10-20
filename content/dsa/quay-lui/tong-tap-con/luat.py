"""Luật soi trace bài tập con tổng S: thu phải đệ quy và phải cắt nhánh.

Test `tb` có ba số nhỏ và ba số lớn hơn S. Duyệt hết mọi tập con thì thu được gọi 64 lần (mỗi lời gọi chọn số
kế tiếp) tới 127 lần (mỗi lời gọi lấy hoặc bỏ một số); cắt nhánh thì không quá vài chục lần.
"""


def kiem(su_kien, dau_vao):
    n = int(dau_vao.split()[0])
    goi = sum(1 for e in su_kien if e.get("t") == "call" and e.get("v") == "thu")
    if goi < 2:
        return "Hãy giải bằng hàm đệ quy thu(i, tong): thu phải tự gọi lại chính nó"
    if goi > 5 * n:
        return f"Chưa cắt nhánh: với {n} số, thu được gọi {goi} lần, gần như thử hết mọi tập con"
    return None
