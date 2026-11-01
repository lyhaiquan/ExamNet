import csv, sys, os
from collections import defaultdict
import statistics as st
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

R = sys.argv[1]  # thư mục statics/results
C = ["#2a78d6", "#eb6834", "#1baf7a", "#eda100"]
INK, INK2, GRID = "#0b0b0b", "#52514e", "#e4e3df"
plt.rcParams.update({"font.family": "Times New Roman", "font.size": 11, "axes.edgecolor": INK2,
                     "axes.labelcolor": INK, "xtick.color": INK2, "ytick.color": INK2,
                     "axes.spines.top": False, "axes.spines.right": False, "figure.dpi": 200})

def rows(n): return list(csv.DictReader(open(os.path.join(R, n), encoding="utf-8")))
def save(fig, n): fig.tight_layout(); fig.savefig(os.path.join(R, n), facecolor="white"); plt.close(fig)

# ---------- TN1 ----------
t1 = rows("exp1_dong-bo-gio.csv")
cfg = ["0/0", "25/25", "100/100", "20/180"]
skews = ["0", "30000", "-60000", "300000"]
sk_lab = {"0": "lệch 0", "30000": "lệch +30 s", "-60000": "lệch −60 s", "300000": "lệch +5 phút"}
by = defaultdict(list)
for r in t1:
    by[(r["tre_len_ms"] + "/" + r["tre_xuong_ms"], r["lech_gio_ms"])].append(r)

# Hình: sai số tuyệt đối trung bình (log), baseline vs LabCast theo cấu hình trễ, lệch +5 phút
fig, ax = plt.subplots(figsize=(6.4, 3.4))
w = 0.38
for i, c in enumerate(cfg):
    g = by[(c, "300000")]
    b = st.mean(abs(float(x["sai_so_baseline_ms"])) for x in g)
    l = max(st.mean(abs(float(x["sai_so_labcast_ms"])) for x in g), 0.1)
    ax.bar(i - w / 2 - 0.01, b, w, color=C[1], label="Giờ máy (không đồng bộ)" if i == 0 else None)
    ax.bar(i + w / 2 + 0.01, l, w, color=C[0], label="LabCast (ClockSync)" if i == 0 else None)
    ax.text(i - w / 2, b * 1.25, f"{b/1000:.0f} s", ha="center", fontsize=9, color=INK)
    ax.text(i + w / 2, l * 1.25, f"{l:.1f} ms", ha="center", fontsize=9, color=INK)
ax.set_yscale("log"); ax.set_ylim(0.05, 3e7)
ax.set_xticks(range(4), [f"trễ {c} ms" for c in cfg])
ax.set_ylabel("Sai số tuyệt đối TB (ms, thang log)")
ax.yaxis.grid(True, color=GRID, lw=0.6); ax.set_axisbelow(True)
ax.legend(frameon=False, loc="upper center", ncol=2, fontsize=9)
save(fig, "tn1_saiso_baseline_vs_labcast.png")

# Hình: sai số LabCast theo cấu hình trễ, từng mức lệch giờ (điểm), kèm đường lý thuyết (lên−xuống)/2
fig, ax = plt.subplots(figsize=(6.4, 3.4))
for j, s in enumerate(skews):
    for i, c in enumerate(cfg):
        ys = [float(x["sai_so_labcast_ms"]) for x in by[(c, s)]]
        xs = [i + (j - 1.5) * 0.12] * len(ys)
        ax.scatter(xs, ys, s=14, color=C[j], alpha=0.75, lw=0, label=sk_lab[s] if i == 0 else None)
theo = [0, 0, 0, -80]
ax.plot(range(4), theo, ls="--", lw=1.2, color=INK2, label="Lý thuyết (lên − xuống)/2")
ax.set_xticks(range(4), [f"trễ {c} ms" for c in cfg])
ax.set_ylabel("Sai số LabCast (ms)")
ax.axhline(0, color=GRID, lw=0.8); ax.yaxis.grid(True, color=GRID, lw=0.6); ax.set_axisbelow(True)
ax.legend(frameon=False, fontsize=8.5, loc="lower left")
save(fig, "tn1_saiso_theo_tre.png")

# ---------- TN3 ----------
t3 = rows("exp3_mat-mang.csv")
# Hình: số phím mất TB theo tình huống
groups = [("rut-day", o) for o in ["2000", "5000", "10000", "30000"]] + \
         [("tat-app-khi-mat-mang", o) for o in ["2000", "5000", "10000"]] + [("tat-app-khi-co-mang", "0")]
lab = {"rut-day": "Rút dây", "tat-app-khi-mat-mang": "Tắt app khi mất mạng", "tat-app-khi-co-mang": "Tắt app khi có mạng"}
col = {"rut-day": C[0], "tat-app-khi-mat-mang": C[1], "tat-app-khi-co-mang": C[2]}
fig, ax = plt.subplots(figsize=(6.4, 3.4))
seen = set()
for i, (sc, o) in enumerate(groups):
    g = [r for r in t3 if r["tinh_huong"] == sc and r["mat_mang_ms"] == o]
    m = st.mean(int(r["phim_mat"]) for r in g)
    ok = sum(int(r["phim_mat"]) == 0 for r in g)
    ax.bar(i, max(m, 0.6), 0.7, color=col[sc], label=lab[sc] if sc not in seen else None); seen.add(sc)
    ax.text(i, max(m, 0.6) + 4, f"{m:.0f}\n{ok}/{len(g)}", ha="center", fontsize=8, color=INK)
ax.set_xticks(range(len(groups)), [f"{int(o)//1000} s" if o != "0" else "—" for _, o in groups])
ax.set_xlabel("Thời gian mất mạng"); ax.set_ylabel("Số phím mất trung bình")
ax.text(1.0, 1.02, "nhãn: TB phím mất / số lần không mất phím", transform=ax.transAxes, ha="right", fontsize=8, color=INK2)
ax.set_ylim(0, 200); ax.yaxis.grid(True, color=GRID, lw=0.6); ax.set_axisbelow(True)
ax.legend(frameon=False, fontsize=9, loc="upper left")
save(fig, "tn3_phim_mat.png")

# Hình: thời gian đồng bộ lại (box) theo thời lượng rút dây, bỏ lần kẹt (-1)
fig, ax = plt.subplots(figsize=(6.4, 3.2))
outs = ["2000", "5000", "10000", "30000"]
data = [[int(r["dong_bo_lai_ms"]) / 1000 for r in t3 if r["tinh_huong"] == "rut-day"
         and r["mat_mang_ms"] == o and int(r["dong_bo_lai_ms"]) >= 0] for o in outs]
bp = ax.boxplot(data, widths=0.45, patch_artist=True, medianprops=dict(color=INK, lw=1.5),
                whiskerprops=dict(color=INK2), capprops=dict(color=INK2), flierprops=dict(markeredgecolor=INK2))
for b in bp["boxes"]: b.set(facecolor=C[0] + "55", edgecolor=C[0])
for i, d in enumerate(data):
    ax.scatter([i + 1] * len(d), d, s=12, color=C[0], zorder=3)
    ax.text(i + 1.3, st.median(d), f"trung vị {st.median(d):.1f} s\nn={len(d)}", fontsize=8, va="center", color=INK)
ax.set_xticks(range(1, 5), [f"{int(o)//1000} s" for o in outs])
ax.set_xlabel("Thời gian rút dây"); ax.set_ylabel("Đồng bộ lại sau khi cắm (s)")
ax.yaxis.grid(True, color=GRID, lw=0.6); ax.set_axisbelow(True)
save(fig, "tn3_dong_bo_lai.png")
print("ok")
