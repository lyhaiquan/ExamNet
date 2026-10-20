"""Luật soi trace bài chèn, xoá trên danh sách liên kết: sửa chính danh sách ds, không dựng lại.

Mỗi lệnh chen tạo đúng một nút, nên số nút tạo ra phải bằng n + số lệnh chen. Rồi dựng lại danh sách từ các
lần đổi mũi tên trong trace (ảnh chụp đầu, node, link, set): đi từ nút đầu phải ra đúng danh sách sau mọi lệnh.
Làm trên list Python rồi dựng danh sách mới ở cuối thì sai một trong hai điều này.
"""


def kiem(su_kien, dau_vao):
    dong = dau_vao.split("\n")
    n, q = map(int, dong[0].split())
    dung = dong[1].split()
    chen = 0
    for d in dong[2 : 2 + q]:
        c = d.split()
        if c[0] == "chen":
            dung.insert(int(c[1]), c[2])
            chen += 1
        else:
            del dung[int(c[1])]

    sai = ("hãy sửa mũi tên trên chính danh sách ds (moi.tiep = p.tiep; p.tiep = moi; p.tiep = p.tiep.tiep), "
           "không dựng lại danh sách mới")
    tao = sum(1 for e in su_kien if e.get("t") == "node" and e.get("v") == "ds")
    if tao != n + chen:
        return f"Tạo {tao} nút, đúng ra là {n + chen} ({n} nút ban đầu, mỗi lệnh chen một nút): {sai}"

    dau, gia_tri, tiep = None, {}, {}
    for e in su_kien:
        if e.get("t") == "snap" and isinstance(e.get("state"), dict) and "ds" in e["state"]:
            s = e["state"]["ds"]
            dau = s.get("dau")
            for nid, x, sau, _ in s.get("nut", []):
                gia_tri[nid], tiep[nid] = x, sau
            break
    for e in su_kien:
        if e.get("v") != "ds":
            continue
        if e.get("t") == "node":
            if e["id"] in gia_tri:
                return f"Nút số {e['id']} được tạo hai lần: {sai}"
            gia_tri[e["id"]], tiep[e["id"]] = e.get("x"), None
        elif e.get("t") == "link":
            if e.get("id") is None:
                dau = e.get("w")
            else:
                tiep[e["id"]] = e.get("w")
        elif e.get("t") == "set":
            gia_tri[e["id"]] = e.get("x")
    ra, p, da_qua = [], dau, set()
    while p is not None and p not in da_qua:
        da_qua.add(p)
        ra.append(str(gia_tri.get(p)))
        p = tiep.get(p)
    if ra != dung:
        return f"Đi từ nút đầu của ds được {' '.join(ra) or '(rỗng)'}, đúng ra là {' '.join(dung) or '(rỗng)'}: {sai}"
    return None
