"""Thí nghiệm 2 (TN2) — chiếu animation multicast cho cả lớp, đo tự động.

Chép nguyên thư mục tn2/ sang mọi máy (cần JDK 17+ và Python 3.9+, không cần cài thêm gì).

  Máy giáo viên:  python tn2.py giao-vien --hoc-vien 2 --so-lan 10
  Máy học viên:   python tn2.py hoc-vien <IP máy giáo viên>
  Phân tích:      python tn2.py phan-tich            (tự chạy sau khi đo; vẽ hình nếu có matplotlib)

Máy giáo viên bật server LabCast (cổng 7000, multicast 239.255.70.1:7001) và một cổng điều phối HTTP 7900.
Học viên tự nhận tài khoản sv1, sv2, ..., đi theo từng lượt đo, cuối buổi gửi file CSV về máy giáo viên.
"""

import argparse
import csv
import glob
import http.server
import os
import shutil
import socket
import subprocess
import sys
import threading
import time
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
KQ = os.path.join(HERE, "ket-qua")
PORT_DIEU_PHOI = 7900

# tên lượt, tỉ lệ bỏ gói giả lập ở máy học viên, máy bị chặn multicast ("sv1" hoặc "-")
LUOT = [
    ("A-binh-thuong", 0.0, "-"),
    ("B-mat-goi-10", 0.10, "-"),
    ("C-mat-goi-30", 0.30, "-"),
    ("D-chan-multicast", 0.0, "sv1"),
]

sys.stdout.reconfigure(encoding="utf-8", line_buffering=True)


def java():
    j = os.environ.get("JAVA") or shutil.which("java")
    if not j:
        sys.exit("Không thấy lệnh java. Cài JDK 17+ hoặc đặt biến JAVA=đường dẫn java.exe")
    return j


def java_cmd(main, *args):
    cp = os.path.join(HERE, "lib", "*")
    return [java(), "-Dfile.encoding=UTF-8", "-cp", cp, "labcast.bench.exp." + main, *args]


def ip_chinh():
    """IP của card ra mạng (card Wi-Fi đang nối router)."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except OSError:
        return None


# ---------------------------------------------------------------- máy giáo viên
class DieuPhoi:
    def __init__(self):
        self.luot = "CHO"
        self.mat_goi = 0.0
        self.chan = "-"
        self.so_may = 0
        self.da_nop = set()
        self.lock = threading.Lock()


def giao_vien(a):
    os.makedirs(KQ, exist_ok=True)
    dp = DieuPhoi()

    class H(http.server.BaseHTTPRequestHandler):
        def log_message(self, *x):
            pass

        def _tra(self, s):
            b = s.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Length", str(len(b)))
            self.end_headers()
            self.wfile.write(b)

        def do_GET(self):
            u = urllib.parse.urlparse(self.path)
            q = urllib.parse.parse_qs(u.query)
            if u.path == "/dang-ky":
                with dp.lock:
                    dp.so_may += 1
                    ten = "sv%d" % dp.so_may
                print("  + %s vào: %s" % (q.get("may", ["?"])[0], ten))
                self._tra(ten)
            elif u.path == "/luot":
                self._tra("%s %s %s" % (dp.luot, dp.mat_goi, dp.chan))
            else:
                self._tra("LabCast TN2")

        def do_POST(self):
            u = urllib.parse.urlparse(self.path)
            q = urllib.parse.parse_qs(u.query)
            ten = os.path.basename(q.get("ten", ["x"])[0])
            body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
            with open(os.path.join(KQ, "tn2_%s.csv" % ten), "wb") as f:
                f.write(body)
            with dp.lock:
                dp.da_nop.add(ten)
            print("  ← nhận kết quả của %s (%d byte)" % (ten, len(body)))
            self._tra("ok")

    httpd = http.server.ThreadingHTTPServer(("0.0.0.0", PORT_DIEU_PHOI), H)
    threading.Thread(target=httpd.serve_forever, daemon=True).start()

    ip = a.ip or ip_chinh()
    if not ip:
        sys.exit("Không xác định được IP Wi-Fi, chạy lại với --ip <IP máy này>")
    trace = os.path.join(HERE, "tn2.trace")
    p = subprocess.Popen(java_cmd("Exp2Chieu", trace, os.path.join(KQ, "tn2_giao-vien.csv"), ip),
                         stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True, encoding="utf-8", bufsize=1)

    def doi(prefix):
        for line in p.stdout:
            line = line.strip()
            if line.startswith(("LAN ", "SERVER")):
                print("   ", line)
            if line.startswith(prefix):
                return line
        sys.exit("Server Java đã tắt bất ngờ")

    def gui(s):
        p.stdin.write(s + "\n")
        p.stdin.flush()

    doi("SERVER")
    print("=" * 60)
    print("Máy giáo viên sẵn sàng. Trên mỗi máy học viên gõ:")
    print("    python tn2.py hoc-vien %s" % ip)
    print("Đang đợi %d máy học viên..." % a.hoc_vien)
    print("=" * 60)
    gui("CHO %d" % a.hoc_vien)
    doi("SAN_SANG")
    time.sleep(2)

    for ten, mat, chan in LUOT:
        print("\n▶ Lượt %s  (bỏ gói giả lập %.0f%%, chặn multicast: %s) — %d lần chiếu ~%d s"
              % (ten, mat * 100, chan, a.so_lan, a.so_lan * 12))
        if ten.startswith("A") and a.dung_lai:
            input("  Để các máy yên, gần router. Bấm Enter để bắt đầu...")
        dp.luot, dp.mat_goi, dp.chan = ten, mat, chan
        time.sleep(3)  # học viên hỏi lượt mỗi 0,5 s
        gui("LUOT %s %d" % (ten, a.so_lan))
        doi("XONG")
        time.sleep(2)

    print("\nĐo xong, đợi học viên gửi kết quả...")
    dp.luot = "THOAT"
    han = time.time() + 60
    while len(dp.da_nop) < dp.so_may and time.time() < han:
        time.sleep(0.5)
    gui("THOAT")
    doi("DA_THOAT")
    p.wait(10)
    httpd.shutdown()
    print("Đã có kết quả của %d/%d máy trong %s" % (len(dp.da_nop), dp.so_may, KQ))
    phan_tich(a)


# ---------------------------------------------------------------- máy học viên
def hoc_vien(a):
    base = "http://%s:%d" % (a.ip, PORT_DIEU_PHOI)
    may = socket.gethostname()
    try:
        ten = urllib.request.urlopen(base + "/dang-ky?may=" + urllib.parse.quote(may), timeout=5).read().decode()
    except OSError as e:
        sys.exit("Không vào được máy giáo viên %s (%s). Kiểm tra IP, cùng Wi-Fi, tường lửa cho Python/Java." % (a.ip, e))
    os.makedirs(KQ, exist_ok=True)
    out = os.path.join(KQ, "tn2_%s.csv" % ten)
    print("Tài khoản %s, ghi %s" % (ten, out))
    p = subprocess.Popen(java_cmd("Exp2Nghe", a.ip, ten, out, "%s-%s" % (ten, may)),
                         stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True, encoding="utf-8", bufsize=1)
    threading.Thread(target=lambda: [print("   ", l.strip()) for l in p.stdout], daemon=True).start()
    cu = None
    while True:
        try:
            s = urllib.request.urlopen(base + "/luot", timeout=5).read().decode().split()
        except OSError:
            time.sleep(1)
            continue
        if s != cu:
            cu = s
            if s[0] == "THOAT":
                break
            if s[0] != "CHO":
                p.stdin.write("LUOT %s %s %d\n" % (s[0], s[1], 1 if s[2] == ten else 0))
                p.stdin.flush()
        time.sleep(0.5)
    p.stdin.write("THOAT\n")
    p.stdin.flush()
    p.wait(15)
    with open(out, "rb") as f:
        body = f.read()
    req = urllib.request.Request(base + "/nop?ten=" + ten, data=body, method="POST")
    urllib.request.urlopen(req, timeout=10).read()
    print("Đã gửi kết quả về máy giáo viên. Xong!")


# ---------------------------------------------------------------- phân tích
def pct(xs, q):
    if not xs:
        return float("nan")
    xs = sorted(xs)
    k = (len(xs) - 1) * q
    i = int(k)
    return xs[i] if i + 1 >= len(xs) else xs[i] + (xs[i + 1] - xs[i]) * (k - i)


def phan_tich(a):
    gv_file = os.path.join(KQ, "tn2_giao-vien.csv")
    files = [f for f in glob.glob(os.path.join(KQ, "tn2_sv*.csv"))]
    if not os.path.exists(gv_file) or not files:
        sys.exit("Chưa có kết quả trong %s" % KQ)
    gv = list(csv.DictReader(open(gv_file, encoding="utf-8")))
    sv = []
    for f in files:
        sv += list(csv.DictReader(open(f, encoding="utf-8")))
    # Lệnh tới muộn (qua TCP sau khi rớt mạng) có thể tới khi học viên đã sang lượt sau: gán lượt theo giờ hẹn
    # at của server, không theo nhãn lúc nhận.
    moc = sorted((int(r["gio_server_gui"]), r["luot"]) for r in gv if r["lenh"] == "LOAD")
    for r in sv:
        if r["loai"] == "CONTROL":
            at = int(r["gio_hen_at"])
            r["luot"] = max((m for m in moc if m[0] <= at), default=moc[0])[1]
    may_list = sorted({r["may"] for r in sv})
    luot_list = [l for l, _, _ in LUOT if any(r["luot"] == l for r in gv)]

    tong = []
    print("\n" + "=" * 78)
    print("KẾT QUẢ TN2 — %d máy học viên: %s" % (len(may_list), ", ".join(may_list)))
    for l in luot_list:
        g = [r for r in gv if r["luot"] == l]
        lenh = len(g)
        t0 = int(g[0]["gio_server_gui"])
        t1 = int(g[-1]["gio_server_gui"])
        prev = [r for r in gv if luot_list.index(r["luot"]) < luot_list.index(l)]
        b0 = int(prev[-1]["byte_da_phat"]) if prev else 0
        bytes_ = int(g[-1]["byte_da_phat"]) - b0
        thoi_gian = (t1 - t0) / 1000 + 1
        kbps = bytes_ * 8 / 1000 / thoi_gian
        def d(k):
            return int(g[-1][k]) - (int(prev[-1][k]) if prev else 0)
        for m in may_list:
            rows = [r for r in sv if r["may"] == m and r["luot"] == l]
            ctl = [r for r in rows if r["loai"] == "CONTROL"]
            nhan = len({r["gio_hen_at"] for r in ctl})
            som = [int(r["som_ms"]) for r in ctl]
            tre = sum(1 for x in som if x < 0)
            qua_tcp = sum(1 for r in ctl if r["duong"] == "tcp")
            snap_all = [r for r in sv if r["may"] == m and r["loai"] in ("CONTROL", "READY", "TONG")]
            idx = [i for i, r in enumerate(snap_all) if r["luot"] == l]
            def cnt(k):
                if not idx:
                    return 0
                end = int(snap_all[idx[-1]][k])
                start = int(snap_all[idx[0] - 1][k]) if idx[0] > 0 else 0
                return end - start
            ready = sum(1 for r in rows if r["loai"] == "READY")
            loads = sum(1 for r in g if r["lenh"] == "LOAD")
            rtt = int(ctl[-1]["rtt_ms"]) if ctl else -1
            tong.append(dict(luot=l, may=m, lenh_gui=lenh, lenh_nhan=nhan, tre_han=tre, qua_tcp=qua_tcp,
                             som_p50=round(pct(som, .5)), som_p05=round(pct(som, .05)), som_min=min(som) if som else "",
                             trace_du=ready, lan_load=loads, goi_bo_gia_lap=cnt("goi_bo_gia_lap"), nack=cnt("nack"),
                             goi_tcp=cnt("goi_tcp"), rtt_ms=rtt, kbps_server=round(kbps, 2),
                             nack_server=d("nack_nhan"), sua_rieng=d("sua_rieng"), phat_lai=d("phat_lai")))
    # độ lệch giữa các máy: cùng một lệnh (cùng gio_hen_at), máy nào nhận muộn hơn giờ hẹn thì bắt đầu muộn
    lech = {}
    for l in luot_list:
        by_at = {}
        for r in sv:
            if r["luot"] == l and r["loai"] == "CONTROL":
                by_at.setdefault(r["gio_hen_at"], {})[r["may"]] = (int(r["som_ms"]), int(r["rtt_ms"]))
        ds = []
        for at, mm in by_at.items():
            if len(mm) < 2:
                continue
            bat_dau = [max(0, -s) for s, _ in mm.values()]
            rtts = sorted(rt for _, rt in mm.values())
            bien = (rtts[-1] + rtts[-2]) / 2  # mỗi đồng hồ sai ≤ RTT/2: hai máy lệch thêm tối đa tổng hai nửa
            ds.append(max(bat_dau) - min(bat_dau) + bien)
        lech[l] = ds

    w = csv.DictWriter(open(os.path.join(KQ, "tn2_tong-hop.csv"), "w", newline="", encoding="utf-8"),
                       fieldnames=list(tong[0].keys()))
    w.writeheader()
    w.writerows(tong)
    print("lệnh: nhận/gửi · trễ hạn: số lệnh tới sau giờ hẹn · TCP: số lệnh nhận qua TCP · sớm50: trung vị ms tới trước giờ hẹn")
    print("%-17s %-14s %7s %7s %5s %6s %6s %6s %8s" % ("lượt", "máy", "lệnh", "trễ hạn", "TCP", "sớm50", "bỏ", "NACK", "kbit/s"))
    for t in tong:
        print("%-17s %-14s %3d/%-3d %7d %5d %6s %6d %6d %8.2f" % (
            t["luot"], t["may"][:14], t["lenh_nhan"], t["lenh_gui"], t["tre_han"], t["qua_tcp"], t["som_p50"],
            t["goi_bo_gia_lap"], t["nack"], t["kbps_server"]))
    print("\nĐộ lệch bắt đầu giữa các máy (cận trên, ms): ")
    for l, ds in lech.items():
        if ds:
            print("  %-17s p50 %5.1f  p95 %5.1f  max %5.1f  (n=%d)" % (l, pct(ds, .5), pct(ds, .95), max(ds), len(ds)))
    with open(os.path.join(KQ, "tn2_lech-giua-may.csv"), "w", encoding="utf-8") as f:
        f.write("luot,lech_ms\n")
        for l, ds in lech.items():
            for x in ds:
                f.write("%s,%.1f\n" % (l, x))
    print("Đã ghi %s" % KQ)
    ve(tong, lech, sv, luot_list)


def ve(tong, lech, sv, luot_list):
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
    except ImportError:
        print("(Không có matplotlib — bỏ qua vẽ hình. Gửi thư mục ket-qua để vẽ ở máy khác.)")
        return
    C = ["#2a78d6", "#eb6834", "#1baf7a", "#eda100"]
    INK2, GRID = "#52514e", "#e4e3df"
    plt.rcParams.update({"font.family": "Times New Roman", "font.size": 11, "axes.spines.top": False,
                         "axes.spines.right": False, "figure.dpi": 200})
    ten = {"A-binh-thuong": "A: bình thường", "B-mat-goi-10": "B: bỏ 10% gói", "C-mat-goi-30": "C: bỏ 30% gói",
           "D-chan-multicast": "D: chặn multicast\n(1 máy)"}
    nhan = [ten.get(l, l) for l in luot_list]

    fig, ax = plt.subplots(figsize=(6.4, 3.3))
    data = [[int(r["som_ms"]) for r in sv if r["luot"] == l and r["loai"] == "CONTROL"] for l in luot_list]
    bp = ax.boxplot(data, widths=0.45, patch_artist=True, medianprops=dict(color="#0b0b0b", lw=1.5))
    for b in bp["boxes"]:
        b.set(facecolor=C[0] + "55", edgecolor=C[0])
    ax.axhline(0, color=C[1], lw=1.2, ls="--")
    ax.text(0.55, 6, "giờ hẹn", color=C[1], fontsize=9, va="bottom")
    ax.set_xticks(range(1, len(luot_list) + 1), nhan, fontsize=9)
    ax.set_ylabel("Lệnh tới sớm hơn giờ hẹn (ms)")
    ax.yaxis.grid(True, color=GRID, lw=0.6)
    ax.set_axisbelow(True)
    fig.tight_layout()
    fig.savefig(os.path.join(KQ, "tn2_som_hon_gio_hen.png"), facecolor="white")
    plt.close(fig)

    fig, ax = plt.subplots(figsize=(6.4, 3.3))
    ds = [lech[l] for l in luot_list]
    if all(ds):
        bp = ax.boxplot(ds, widths=0.45, patch_artist=True, medianprops=dict(color="#0b0b0b", lw=1.5))
        for b in bp["boxes"]:
            b.set(facecolor=C[2] + "55", edgecolor=C[2])
        ax.axhline(50, color=C[1], lw=1.2, ls="--")
        ax.text(0.55, 52, "mục tiêu 50 ms", color=C[1], fontsize=9, va="bottom")
        ax.set_xticks(range(1, len(luot_list) + 1), nhan, fontsize=9)
        ax.set_ylabel("Độ lệch giữa các máy, cận trên (ms)")
        ax.yaxis.grid(True, color=GRID, lw=0.6)
        ax.set_axisbelow(True)
        fig.tight_layout()
        fig.savefig(os.path.join(KQ, "tn2_lech_giua_may.png"), facecolor="white")
    plt.close(fig)
    print("Đã vẽ hình vào", KQ)


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="vai", required=True)
    g = sub.add_parser("giao-vien")
    g.add_argument("--hoc-vien", type=int, default=1, help="số máy học viên phải đợi (mặc định 1)")
    g.add_argument("--so-lan", type=int, default=10, help="số lần chiếu mỗi lượt (mặc định 10, ~12 s/lần)")
    g.add_argument("--ip", help="IP Wi-Fi của máy này (mặc định tự dò)")
    g.add_argument("--dung-lai", action="store_true", help="dừng hỏi trước lượt A")
    h = sub.add_parser("hoc-vien")
    h.add_argument("ip", help="IP máy giáo viên")
    sub.add_parser("phan-tich")
    a = ap.parse_args()
    {"giao-vien": giao_vien, "hoc-vien": hoc_vien, "phan-tich": phan_tich}[a.vai](a)


if __name__ == "__main__":
    main()
