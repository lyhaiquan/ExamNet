"""Cây lời gọi đệ quy (spec §7.4 `cay-goi`): ghi `call`/`ret` của các hàm khai báo, không cần sửa code học viên.

Bài khai báo trong `khung_nhin` tên HÀM cần vẽ (`- bien: fib` / `kieu: cay-goi`); run.py bật bộ ghi cho từng
hàm. Mỗi lần hàm được gọi thành một nút "fib(4)", con của lời gọi đang chạy gần nhất của cùng hàm; trả về thì
nút ghi kết quả và chuyển xanh lá. Code (lời giải mẫu hay học viên) gọi `viz.cat("…")` để đánh dấu lời gọi
đang chạy là nhánh bị cắt (xám, ví dụ đã có trong bảng nhớ) hoặc `viz.loai("…")` là nhánh quay lui (đỏ).
"""

from . import recorder, theo_doi

TOI_DA = 200  # số lời gọi vẽ tối đa: cây lớn hơn thì xem cũng không rõ, và ảnh chụp thành quá nặng
DAI_NHAN = 26

_cac_cay = []  # các cây đang ghi, để viz.cat()/viz.loai() tìm lời gọi đang chạy


def bat(ten_ham):
    """run.py gọi cho mỗi `kieu: cay-goi` trong khung_nhin, trước khi chạy bài làm."""
    c = CayGoi(ten_ham)
    _cac_cay.append(c)
    return c


def tat():
    _cac_cay.clear()


def cat(ly_do=None):
    """Lời gọi đang chạy là nhánh bị cắt (xám): đã có kết quả trong bảng nhớ, cận không thoả…"""
    _danh_dau("bo", ly_do)


def loai(ly_do=None):
    """Lời gọi đang chạy là nhánh quay lui (đỏ): thử rồi không được."""
    _danh_dau("loai", ly_do)


def _danh_dau(mau, ly_do):
    if ly_do is not None:
        recorder.say(ly_do)
    # lời gọi đang chạy gần nhất (khung sâu nhất) trong mọi cây
    import sys

    f = sys._getframe(2)
    while f is not None:
        for c in _cac_cay:
            if c._ngan and c._ngan[-1][0] is f and c._ngan[-1][1] is not None:
                c._danh_dau(c._ngan[-1][1], mau)
                return
        f = f.f_back


def _ngan_gon(x, toi_da=12):
    s = repr(x)
    return s if len(s) <= toi_da else s[: toi_da - 1] + "…"


class CayGoi:
    def __init__(self, ten_ham):
        self.ten = ten_ham
        self._nut = []  # [id, cha, nhãn, kết quả (chữ) | None, trạng thái]
        self._ngan = []  # (khung, id | None) của các lời gọi đang chạy; None = vượt TOI_DA, không vẽ
        self._bao_tran = False
        recorder.current.register(ten_ham, self)
        theo_doi.them_moc_goi(self._moc)

    def _moc(self, su, frame, gia_tri):
        if frame.f_code.co_name != self.ten:
            return
        if su == "call":
            self._goi(frame)
        else:
            self._tra_ve(frame, gia_tri)

    def _goi(self, frame):
        if len(self._nut) >= TOI_DA:
            self._ngan.append((frame, None))
            if not self._bao_tran:
                self._bao_tran = True
                recorder.say(f"Cây lời gọi quá lớn: chỉ vẽ {TOI_DA} lời gọi đầu")
            return
        cha = next((i for _, i in reversed(self._ngan) if i is not None), None)
        co = frame.f_code
        ten_tham_so = co.co_varnames[: co.co_argcount]
        nhan = f"{self.ten}({', '.join(_ngan_gon(frame.f_locals.get(t)) for t in ten_tham_so)})"
        if len(nhan) > DAI_NHAN:
            nhan = nhan[: DAI_NHAN - 2] + "…)"
        i = len(self._nut)
        self._nut.append([i, cha, nhan, None, "dang"])
        self._ngan.append((frame, i))
        self._ghi({"t": "call", "id": i, "p": cha, "a": nhan})

    def _tra_ve(self, frame, gia_tri):
        # khung trả về phải là lời gọi trên cùng; lệch (khung có từ trước khi bật ghi) thì bỏ qua
        if not self._ngan or self._ngan[-1][0] is not frame:
            return
        _, i = self._ngan.pop()
        if i is None:
            return
        nut = self._nut[i]
        nut[3] = None if gia_tri is None else _ngan_gon(gia_tri, 14)
        if nut[4] == "dang":
            nut[4] = "xong"
        self._ghi({"t": "ret", "id": i, "x": nut[3]})

    def _danh_dau(self, i, mau):
        self._nut[i][4] = mau
        self._ghi({"t": "mark", "id": i, "m": mau})

    def _ghi(self, su_kien):
        su_kien["v"] = self.ten
        recorder.current.emit(su_kien)

    def trang_thai(self):
        return {
            "kieu": "cay-goi",
            "nut": [list(n) for n in self._nut],
            "ngan": [i for _, i in self._ngan if i is not None],
        }
