from kiem_cam import kiem

SAP_XEP = ["sorted", "sort"]


def test_bat_sorted_va_sort():
    assert kiem("a = [3, 1]\nb = sorted(a)\n", SAP_XEP) == ["Không được dùng sorted (dòng 2)"]
    assert kiem("a = [3, 1]\na.sort()\n", SAP_XEP) == ["Không được dùng .sort() (dòng 2)"]


def test_bat_dung_gian_tiep():
    assert kiem("f = sorted\nprint(f([2, 1]))\n", SAP_XEP)
    assert kiem("a = [2, 1]\nlist.sort(a)\n", SAP_XEP)
    assert kiem("a = [2, 1]\ngetattr(a, 'sort')()\n", SAP_XEP)
    assert kiem("eval('sor' + 'ted([2, 1])')\n", SAP_XEP)
    assert kiem("__builtins__.__dict__['sorted']\n", SAP_XEP)


def test_khong_bat_ten_giong():
    assert kiem("sorted_list = [1]\nprint(sorted_list)\n", SAP_XEP) == []
    assert kiem("def sort(a):\n    return a\nsort([1])\n", SAP_XEP) == []
    assert kiem("print('sorted')\n", SAP_XEP) == []


def test_bat_import_cam():
    assert kiem("from heapq import heappush\n", cam_import=["heapq"]) == ["Không được import heapq (dòng 1)"]
    assert kiem("import heapq as h\n", cam_import=["heapq"])
    assert kiem("import collections.abc\n", cam_import=["collections"])
    assert kiem("import math\n", cam_import=["heapq"]) == []


def test_cu_phap_pow():
    assert kiem("x = 2\ny = x ** 2\n", cam_cu_phap=["Pow"])
    assert kiem("x = 2\nx **= 2\n", cam_cu_phap=["Pow"])
    assert kiem("x = 2\ny = x * 2\n", cam_cu_phap=["Pow"]) == []


def test_cu_phap_dict():
    assert kiem("d = {}\n", cam_cu_phap=["Dict"])
    assert kiem("d = {1: 2}\n", cam_cu_phap=["Dict"])
    assert kiem("s = {1, 2}\n", cam_cu_phap=["Dict"]) == []


def test_loi_cu_phap_khong_tinh_la_cam():
    assert kiem("def f(:\n", SAP_XEP) == []


def test_khong_co_danh_sach_cam_thi_cho_eval():
    assert kiem("print(eval('1 + 1'))\n") == []
