"""Bài làm C, C++, Java: biên dịch, chạy, chấm, chạy từng dòng dưới debugger, kiểm tên cấm.

Máy thiếu gcc/g++/gdb/javac thì bỏ qua phần tương ứng (image runner có đủ; CI chạy trong image).
"""

import io
import json
import shutil

import pytest

import kiem_cam
import ngon_ngu
from run import chay

can_gcc = pytest.mark.skipif(shutil.which("gcc") is None or shutil.which("g++") is None, reason="thiếu gcc/g++")
can_gdb = pytest.mark.skipif(shutil.which("gdb") is None or shutil.which("g++") is None, reason="thiếu gdb")
can_java = pytest.mark.skipif(shutil.which("javac") is None, reason="thiếu JDK")

TONG_CPP = """#include <bits/stdc++.h>
using namespace std;

int binh_phuong(int x) {
    int y = x * x;
    return y;
}

int main() {
    int n;
    cin >> n;
    vector<int> a(n);
    for (int i = 0; i < n; i++) cin >> a[i];
    long long tong = 0;
    for (int i = 0; i < n; i++) {
        tong += binh_phuong(a[i]);
    }
    cout << tong << endl;
    return 0;
}
"""

TONG_JAVA = """import java.util.*;

public class Main {
    static int binhPhuong(int x) {
        int y = x * x;
        return y;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextInt();
        }
        long tong = 0;
        List<Integer> ds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            tong += binhPhuong(a[i]);
            ds.add(a[i]);
        }
        System.out.println(tong);
    }
}
"""


def chay_job(ngon_ngu_ma, code, vao="", tung_dong=False, gioi_han=1000):
    out = io.StringIO()
    job = {"code": code, "input": vao, "trace": tung_dong, "line_trace": tung_dong, "time_limit_ms": gioi_han,
           "ngon_ngu": ngon_ngu_ma, "views": []}
    import run

    # run.main() chuyển C/C++/Java sang ngon_ngu; gọi thẳng để không qua stdin
    if ngon_ngu_ma == "python":
        chay(job, out)
    else:
        run.ngon_ngu.chay_job(job, out)
    su_kien = [json.loads(d) for d in out.getvalue().splitlines()]
    return su_kien[:-1], su_kien[-1]


def cac_buoc(trace):
    """[(dòng, {biến: giá trị} sau dòng đó)]: biến là bảng gần nhất tính tới bước đó."""
    buoc, bien = [], {}
    for e in trace:
        if e["t"] == "line":
            buoc.append([e["n"], dict(bien)])
        elif e["t"] == "vars":
            bien = dict(e["d"])
            if buoc:
                buoc[-1][1] = dict(bien)
    return buoc


@can_gcc
def test_cpp_chay_thu_dung():
    _, kq = chay_job("cpp", TONG_CPP, "3\n1 2 3\n")
    assert kq["ok"] and kq["stdout"] == "14\n"


@can_gcc
def test_c_chay_thu_dung():
    code = '#include <stdio.h>\nint main(void){long a,b;scanf("%ld %ld",&a,&b);printf("%ld\\n",a+b);return 0;}\n'
    _, kq = chay_job("c", code, "2 3\n")
    assert kq["ok"] and kq["stdout"] == "5\n"


@can_gcc
def test_loi_bien_dich_co_dong():
    _, kq = chay_job("cpp", "int main() {\n    int x = ;\n}\n")
    assert not kq["ok"]
    assert kq["error_kind"] == "bien-dich" and kq["error_line"] == 2
    assert "main.cpp:2" in kq["error"] and "/tmp" not in kq["error"]


@can_gcc
def test_segfault_va_vong_lap_vo_tan():
    _, kq = chay_job("cpp", "int main() { int *p = nullptr; *p = 5; }\n")
    assert not kq["ok"] and "Segmentation fault" in kq["error"]
    _, kq = chay_job("cpp", "int main() { while (true) {} }\n")
    assert not kq["ok"] and kq["error"].startswith("Quá thời gian")


@can_gdb
def test_cpp_chay_tung_dong():
    trace, kq = chay_job("cpp", TONG_CPP, "3\n1 2 3\n", tung_dong=True)
    assert kq["ok"] and kq["stdout"] == "14\n"
    assert trace[0]["t"] == "hdr" and trace[0]["source"] == TONG_CPP
    assert all(e.get("v") == "__trace_table__" for e in trace if e["t"] in ("line", "vars"))
    buoc = cac_buoc(trace)
    dong = [d for d, _ in buoc]
    # vòng for viết xuống dòng: dòng 16 chạy mỗi vòng, mỗi lần gọi hàm vào dòng 5, 6 của binh_phuong
    assert dong.count(16) == 3 and dong.count(5) == 3
    assert ["a", "{1, 2, 3}"] in [list(x) for _, b in buoc for x in b.items()]
    # tham số đã có giá trị thật ngay dòng đầu của hàm (không dừng ở phần chuẩn bị của hàm)
    assert [b.get("x") for d, b in buoc if d == 5] == ["1", "2", "3"]
    # không bao giờ dừng trong thư viện chuẩn
    assert set(dong) <= set(range(1, TONG_CPP.count("\n") + 1))


@can_gdb
def test_cpp_chay_tung_dong_vong_vo_tan_bi_cat_va_qua_gio():
    code = "int main() {\n    long long x = 0;\n    while (true) {\n        x++;\n    }\n}\n"
    trace, kq = chay_job("cpp", code, tung_dong=True)
    assert len([e for e in trace if e["t"] == "line"]) == ngon_ngu.TRAN_BUOC
    assert trace[-1] == {"t": "end", "truncated": True}
    assert not kq["ok"] and kq["error"].startswith("Quá thời gian")


@can_gdb
def test_cpp_chay_tung_dong_segfault_co_dong():
    code = "int main() {\n    int *p = nullptr;\n    *p = 5;\n    return 0;\n}\n"
    _, kq = chay_job("cpp", code, tung_dong=True)
    assert not kq["ok"] and kq["error_line"] == 3


@can_java
def test_java_chay_va_loi():
    _, kq = chay_job("java", TONG_JAVA, "3\n1 2 3\n")
    assert kq["ok"] and kq["stdout"] == "14\n"
    code = "public class Main {\n    public static void main(String[] a) {\n        int[] x = new int[2];\n" \
           "        x[5] = 1;\n    }\n}\n"
    _, kq = chay_job("java", code)
    assert not kq["ok"] and kq["error_line"] == 4 and "ngoài mảng" in kq["error"]
    _, kq = chay_job("java", "public class Solution {\n    public static void main(String[] a) {}\n}\n")
    assert kq["error_kind"] == "bien-dich" and "public class Main" in kq["error"]


@can_java
def test_java_chay_tung_dong():
    trace, kq = chay_job("java", TONG_JAVA, "3\n1 2 3\n", tung_dong=True)
    assert kq["ok"] and kq["stdout"] == "14\n"
    buoc = cac_buoc(trace)
    assert [b.get("x") for d, b in buoc if d == 5] == ["1", "2", "3"]
    cuoi = buoc[-1][1]
    assert cuoi["ds"] == "[1, 2, 3]" and cuoi["a"] == "[1, 2, 3]" and cuoi["tong"] == "14"
    assert "args" not in cuoi


def _cham(ngon_ngu_ma, code, tests, cam=None):
    import cham

    ra = []
    job = {"k": "K", "code": code, "time_limit_ms": 1000, "ngon_ngu": ngon_ngu_ma, "tests": tests, "cam": cam or {}}
    cham.cham_ngon_ngu(job, ra.append)
    return ra


@can_gcc
def test_cham_bien_dich_mot_lan_chay_tung_test():
    tests = [{"id": "t1", "input": "2 3\n"}, {"id": "t2", "input": "10 -4\n"}]
    ra = _cham("cpp", "#include <iostream>\nint main(){long long a,b;std::cin>>a>>b;std::cout<<a+b;}\n", tests)
    assert [(r["t"], r.get("status"), r.get("stdout")) for r in ra] == [
        ("test", "OK", "5"), ("test", "OK", "6"), ("done", None, None)]
    ra = _cham("cpp", "int main() { int x = ; }\n", tests)
    assert len(ra) == 1 and ra[0]["t"] == "bien_dich" and ra[0]["dong"] == 1


@can_gcc
def test_cham_ten_cam_suy_tu_cam_python():
    code = "#include <bits/stdc++.h>\nusing namespace std;\nint main(){vector<int> a={2,1};sort(a.begin(),a.end());}\n"
    ra = _cham("cpp", code, [{"id": "t1", "input": ""}], {"cam_dung": ["sorted", "sort"]})
    assert ra == [{"t": "cam", "loi": ["Không được dùng sort (dòng 3)"]}]


def test_kiem_ten_cam_bo_chu_thich_chuoi_va_ham_tu_viet():
    code = """// sort(a) trong chú thích
void sort(int a[], int n) { }
int main() {
    const char *s = "std::sort";
    int a[3];
    sort(a, 3);
}
"""
    assert kiem_cam.kiem_ngon_ngu(code, "cpp", ["sort"]) == []
    assert kiem_cam.kiem_ngon_ngu("int main(){ std::sort(a, a+3); }", "cpp", ["sort"]) == [
        "Không được dùng sort (dòng 1)"]


def test_kiem_ten_cam_java():
    code = "class Main {\n void f(List<Integer> l, int[] a) {\n  l.sort(null);\n  Arrays . sort(a);\n }\n}\n"
    ds = kiem_cam.cam_cua("java", ["sort"], [])
    assert set(kiem_cam.kiem_ngon_ngu(code, "java", ds)) == {
        "Không được dùng Arrays.sort (dòng 4)", "Không được dùng .sort() (dòng 3)"}


def test_cam_rieng_cua_bai_thang_cam_suy_ra():
    assert kiem_cam.cam_cua("cpp", ["sort"], ["heapq"], {"cpp": ["std::sort"]}) == ["std::sort"]
    assert "priority_queue" in kiem_cam.cam_cua("cpp", [], ["heapq"])
    assert kiem_cam.cam_cua("c", ["set"], []) == []
