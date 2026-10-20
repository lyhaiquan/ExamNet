"""Danh sách 34 bài luyện tập (đề tự viết, phỏng theo bài kinh điển trên LeetCode). gen.py đọc file này."""

import random


def mang(a, dau=None):
    """Input dạng: dòng 1 là n (và các số kèm), dòng 2 là mảng."""
    dong1 = " ".join(map(str, [len(a)] + (dau or [])))
    return f"{dong1}\n{' '.join(map(str, a))}\n"


def R(seed):
    return random.Random(seed)


BAI = []


def bai(**k):
    BAI.append(k)


VAO_MANG = "Dòng 1 là *n*. Dòng 2 là *n* số nguyên cách nhau một dấu cách."
KHUNG_MANG = "n = int(input())\na = list(map(int, input().split()))\n"

# ============================================================ EASY

bai(muc="easy", slug="hai-so-tong", ten="Hai số có tổng bằng k", lc="1. Two Sum",
    mo_ta="Cho mảng *a* gồm *n* số nguyên và số *k*. Tìm hai vị trí *i* < *j* sao cho `a[i] + a[j] = k`. "
          "Đề bảo đảm có **đúng một** cặp như vậy. Vị trí đánh số từ 0.",
    vao="Dòng 1 là *n* và *k*. Dòng 2 là *n* số nguyên.", ra="Hai số *i* và *j*, cách nhau một dấu cách.",
    rang_buoc="2 ≤ *n* ≤ 10⁴, |a[i]|, |k| ≤ 10⁹.",
    khung="n, k = map(int, input().split())\na = list(map(int, input().split()))\n\n"
          "# Viết code của em ở đây: tìm i < j để a[i] + a[j] == k\ni, j = -1, -1\n\nprint(i, j)\n",
    giai="n, k = map(int, input().split())\na = list(map(int, input().split()))\nda_gap = {}\n"
         "for j in range(n):\n    can = k - a[j]\n    if can in da_gap:\n        print(da_gap[can], j)\n        break\n"
         "    da_gap[a[j]] = j\n",
    tests=lambda: ["4 9\n2 7 11 15\n", "3 6\n3 2 4\n", "2 6\n3 3\n", "5 -8\n-3 -1 0 -5 4\n",
                   "6 0\n5 -2 9 2 7 1\n", _hai_so(1)])


def _hai_so(seed):
    r = R(seed)
    while True:
        a = r.sample(range(-10**6, 10**6), 3000)
        i, j = sorted(r.sample(range(3000), 2))
        k = a[i] + a[j]
        s = set()
        dem = 0
        for x in a:
            if k - x in s:
                dem += 1
            s.add(x)
        if dem == 1:
            return f"3000 {k}\n{' '.join(map(str, a))}\n"


bai(muc="easy", slug="so-doi-xung", ten="Số đối xứng", lc="9. Palindrome Number",
    mo_ta="Cho số nguyên *x*. Số *x* là **số đối xứng** nếu đọc từ trái sang phải và từ phải sang trái như nhau "
          "(121 đối xứng, -121 không đối xứng vì đọc ngược là 121-). Hãy làm **không đổi sang chuỗi**: dùng `% 10` "
          "và `// 10`.",
    vao="Một số nguyên *x*.", ra="`YES` nếu *x* đối xứng, ngược lại `NO`.", rang_buoc="|x| ≤ 2³¹.",
    khung="x = int(input())\n\n# Viết code của em ở đây: đảo ngược các chữ số của x rồi so sánh\nla_doi_xung = False\n\n"
          "print(\"YES\" if la_doi_xung else \"NO\")\n",
    giai="x = int(input())\nif x < 0:\n    print(\"NO\")\nelse:\n    goc, dao = x, 0\n    while x > 0:\n"
         "        dao = dao * 10 + x % 10\n        x //= 10\n    print(\"YES\" if dao == goc else \"NO\")\n",
    tests=lambda: ["121\n", "-121\n", "10\n", "0\n", "1234554321\n", "2147447412\n"])

bai(muc="easy", slug="mua-ban-co-phieu", ten="Mua bán cổ phiếu một lần", lc="121. Best Time to Buy and Sell Stock",
    mo_ta="`gia[i]` là giá một cổ phiếu ở ngày *i*. Em được mua đúng một lần rồi bán đúng một lần ở một ngày **sau** "
          "ngày mua. Lãi lớn nhất là bao nhiêu? Nếu ngày nào cũng lỗ thì không giao dịch, lãi là 0.",
    vao=VAO_MANG, ra="Một số: lãi lớn nhất.", rang_buoc="1 ≤ *n* ≤ 10⁵, 0 ≤ gia[i] ≤ 10⁴.",
    khung="n = int(input())\ngia = list(map(int, input().split()))\n\n"
          "# Viết code của em ở đây: nhớ giá thấp nhất đã gặp\nlai = 0\n\nprint(lai)\n",
    giai="n = int(input())\ngia = list(map(int, input().split()))\nthap = gia[0]\nlai = 0\nfor g in gia:\n"
         "    if g < thap:\n        thap = g\n    elif g - thap > lai:\n        lai = g - thap\nprint(lai)\n",
    tests=lambda: [mang([7, 1, 5, 3, 6, 4]), mang([7, 6, 4, 3, 1]), mang([5]), mang([2, 4, 1]),
                   mang([3, 3, 3]), mang([R(2).randint(0, 10**4) for _ in range(20000)])])

bai(muc="easy", slug="co-phan-tu-trung", ten="Có phần tử trùng không", lc="217. Contains Duplicate",
    mo_ta="Cho mảng *a*. In `YES` nếu có một giá trị xuất hiện ít nhất hai lần, `NO` nếu mọi phần tử khác nhau.",
    vao=VAO_MANG, ra="`YES` hoặc `NO`.", rang_buoc="1 ≤ *n* ≤ 10⁵, |a[i]| ≤ 10⁹.",
    khung=KHUNG_MANG + "\n# Viết code của em ở đây\nco_trung = False\n\nprint(\"YES\" if co_trung else \"NO\")\n",
    giai=KHUNG_MANG + "da_gap = set()\nco_trung = False\nfor x in a:\n    if x in da_gap:\n        co_trung = True\n"
         "        break\n    da_gap.add(x)\nprint(\"YES\" if co_trung else \"NO\")\n",
    tests=lambda: [mang([1, 2, 3, 1]), mang([1, 2, 3, 4]), mang([5]), mang([-1, -1]),
                   mang(R(3).sample(range(10**9), 50000)), mang(R(4).sample(range(10**9), 49999) + [7, 7])])

bai(muc="easy", slug="don-so-0", ten="Dồn số 0 về cuối", lc="283. Move Zeroes",
    mo_ta="Cho mảng *a*. Dồn mọi số 0 về cuối mảng, các số khác 0 giữ nguyên thứ tự trước sau. Làm **ngay trên "
          "mảng a** (không tạo mảng mới): dùng một con trỏ ghi.",
    vao=VAO_MANG, ra="Mảng sau khi dồn, các số cách nhau một dấu cách.", rang_buoc="1 ≤ *n* ≤ 10⁵.",
    khung=KHUNG_MANG + "\n# Viết code của em ở đây: ghi = vị trí sẽ ghi số khác 0 tiếp theo\n\nprint(*a)\n",
    giai=KHUNG_MANG + "ghi = 0\nfor i in range(n):\n    if a[i] != 0:\n        a[ghi], a[i] = a[i], a[ghi]\n"
         "        ghi += 1\nprint(*a)\n",
    tests=lambda: [mang([0, 1, 0, 3, 12]), mang([0]), mang([1, 2, 3]), mang([0, 0, 1]),
                   mang([4, 0, 0, -2, 0, 9, 0]), mang([R(5).choice([0, 0, 1, 2, 3]) for _ in range(30000)])])

bai(muc="easy", slug="so-xuat-hien-mot-lan", ten="Số xuất hiện một lần", lc="136. Single Number",
    mo_ta="Trong mảng *a*, mọi giá trị xuất hiện đúng hai lần, trừ một giá trị xuất hiện đúng một lần. Tìm giá trị "
          "đó. Gợi ý: phép XOR `^` có `x ^ x = 0` và `x ^ 0 = x`.",
    vao=VAO_MANG, ra="Giá trị xuất hiện một lần.", rang_buoc="1 ≤ *n* ≤ 3·10⁴, *n* lẻ.",
    khung=KHUNG_MANG + "\n# Viết code của em ở đây\nket_qua = 0\n\nprint(ket_qua)\n",
    giai=KHUNG_MANG + "ket_qua = 0\nfor x in a:\n    ket_qua ^= x\nprint(ket_qua)\n",
    tests=lambda: [mang([2, 2, 1]), mang([4, 1, 2, 1, 2]), mang([7]), mang([-3, 5, 5]),
                   mang([9, -9, 9]), _mot_lan(6)])


def _mot_lan(seed):
    r = R(seed)
    v = r.sample(range(-30000, 30000), 15000)
    a = v[1:] * 2 + [v[0]]
    r.shuffle(a)
    return mang(a)


bai(muc="easy", slug="dao-chu", ten="Hai chuỗi đảo chữ", lc="242. Valid Anagram",
    mo_ta="Chuỗi *t* là **đảo chữ** của chuỗi *s* nếu *t* dùng đúng các chữ cái của *s*, mỗi chữ đúng số lần, chỉ "
          "khác thứ tự (`listen` và `silent`). Kiểm tra *t* có phải đảo chữ của *s* không. Hãy đếm bằng dict.",
    vao="Dòng 1 là *s*, dòng 2 là *t* (chữ thường a–z).", ra="`YES` hoặc `NO`.", rang_buoc="1 ≤ |s|, |t| ≤ 5·10⁴.",
    khung="s = input().strip()\nt = input().strip()\n\n# Viết code của em ở đây: đếm số lần mỗi chữ\nla_dao_chu = False\n\n"
          "print(\"YES\" if la_dao_chu else \"NO\")\n",
    giai="s = input().strip()\nt = input().strip()\ndem = {}\nfor c in s:\n    dem[c] = dem.get(c, 0) + 1\n"
         "for c in t:\n    dem[c] = dem.get(c, 0) - 1\nla_dao_chu = len(s) == len(t) and all(v == 0 for v in dem.values())\n"
         "print(\"YES\" if la_dao_chu else \"NO\")\n",
    tests=lambda: ["listen\nsilent\n", "rat\ncar\n", "a\na\n", "ab\nabb\n", "aacc\nccac\n",
                   _dao("".join(R(7).choice("abcdef") for _ in range(40000)), 8)])


def _dao(s, seed):
    t = list(s)
    R(seed).shuffle(t)
    return f"{s}\n{''.join(t)}\n"


bai(muc="easy", slug="leo-cau-thang", ten="Leo cầu thang", lc="70. Climbing Stairs",
    mo_ta="Cầu thang có *n* bậc. Mỗi lần em bước lên 1 bậc hoặc 2 bậc. Có bao nhiêu cách khác nhau để lên tới bậc "
          "*n*? Gợi ý: số cách tới bậc *i* bằng số cách tới bậc *i*−1 cộng số cách tới bậc *i*−2.",
    vao="Một số *n*.", ra="Số cách.", rang_buoc="1 ≤ *n* ≤ 80.",
    khung="n = int(input())\n\n# Viết code của em ở đây\nso_cach = 0\n\nprint(so_cach)\n",
    giai="n = int(input())\ntruoc, nay = 1, 1\nfor i in range(2, n + 1):\n    truoc, nay = nay, truoc + nay\nprint(nay)\n",
    tests=lambda: ["3\n", "5\n", "1\n", "2\n", "45\n", "80\n"])

bai(muc="easy", slug="so-con-thieu", ten="Số còn thiếu", lc="268. Missing Number",
    mo_ta="Mảng *a* có *n* số **khác nhau** lấy từ các số 0, 1, …, *n* (tức là đủ *n* + 1 số trừ đúng một số). "
          "Tìm số bị thiếu. Gợi ý: tổng 0 + 1 + … + *n* là `n * (n + 1) // 2`.",
    vao=VAO_MANG, ra="Số bị thiếu.", rang_buoc="1 ≤ *n* ≤ 10⁵.",
    khung=KHUNG_MANG + "\n# Viết code của em ở đây\nthieu = -1\n\nprint(thieu)\n",
    giai=KHUNG_MANG + "thieu = n * (n + 1) // 2 - sum(a)\nprint(thieu)\n",
    tests=lambda: [mang([3, 0, 1]), mang([0, 1]), mang([1]), mang([0]), mang([9, 6, 4, 2, 3, 5, 7, 0, 1]),
                   _thieu(9)])


def _thieu(seed):
    r = R(seed)
    a = list(range(80001))
    a.pop(r.randrange(len(a)))
    r.shuffle(a)
    return mang(a)


bai(muc="easy", slug="chuoi-doi-xung", ten="Câu đối xứng", lc="125. Valid Palindrome",
    mo_ta="Một câu là **đối xứng** nếu sau khi bỏ hết ký tự không phải chữ cái hay chữ số và đổi chữ hoa thành chữ "
          "thường, nó đọc xuôi ngược như nhau. Dùng hai con trỏ từ hai đầu đi vào, `c.isalnum()` và `c.lower()`.",
    vao="Một dòng chứa câu (có thể có dấu cách, dấu câu).", ra="`YES` hoặc `NO`.", rang_buoc="1 ≤ độ dài ≤ 2·10⁵.",
    khung="s = input()\n\n# Viết code của em ở đây: hai con trỏ trai, phai\nla_doi_xung = False\n\n"
          "print(\"YES\" if la_doi_xung else \"NO\")\n",
    giai="s = input()\ntrai, phai = 0, len(s) - 1\nla_doi_xung = True\nwhile trai < phai:\n"
         "    if not s[trai].isalnum():\n        trai += 1\n    elif not s[phai].isalnum():\n        phai -= 1\n"
         "    elif s[trai].lower() != s[phai].lower():\n        la_doi_xung = False\n        break\n    else:\n"
         "        trai += 1\n        phai -= 1\nprint(\"YES\" if la_doi_xung else \"NO\")\n",
    tests=lambda: ["A man, a plan, a canal: Panama\n", "race a car\n", "!!\n", "0P\n", "Was it a car or a cat I saw?\n",
                   "ab" * 50000 + "ba" * 50000 + "\n"])

# ============================================================ INTERMEDIATE

bai(muc="intermediate", slug="chuoi-con-khong-lap", ten="Chuỗi con dài nhất không lặp ký tự",
    lc="3. Longest Substring Without Repeating Characters",
    mo_ta="Tìm độ dài đoạn liên tiếp dài nhất của chuỗi *s* mà trong đoạn không có ký tự nào xuất hiện hai lần. "
          "Gợi ý: cửa sổ trượt, nhớ vị trí gần nhất của từng ký tự.",
    vao="Một dòng là chuỗi *s* (chữ thường, chữ số).", ra="Độ dài lớn nhất.", rang_buoc="1 ≤ |s| ≤ 10⁵.",
    khung="s = input().strip()\n\n# Viết code của em ở đây\ndai_nhat = 0\n\nprint(dai_nhat)\n",
    giai="s = input().strip()\ncuoi = {}\ndau = 0\ndai_nhat = 0\nfor i, c in enumerate(s):\n"
         "    if c in cuoi and cuoi[c] >= dau:\n        dau = cuoi[c] + 1\n    cuoi[c] = i\n"
         "    dai_nhat = max(dai_nhat, i - dau + 1)\nprint(dai_nhat)\n",
    tests=lambda: ["abcabcbb\n", "bbbbb\n", "pwwkew\n", "a\n", "abba\n",
                   "".join(R(10).choice("abcdefghijklmnopqrstuvwxyz0123456789") for _ in range(100000)) + "\n"])

bai(muc="intermediate", slug="tong-doan-lon-nhat", ten="Đoạn con có tổng lớn nhất", lc="53. Maximum Subarray",
    mo_ta="Tìm tổng lớn nhất của một đoạn **liên tiếp, khác rỗng** trong mảng *a*. Gợi ý (Kadane): tổng tốt nhất "
          "kết thúc tại *i* là `max(a[i], tốt nhất tại i-1 + a[i])`.",
    vao=VAO_MANG, ra="Tổng lớn nhất.", rang_buoc="1 ≤ *n* ≤ 10⁵, |a[i]| ≤ 10⁴.",
    khung=KHUNG_MANG + "\n# Viết code của em ở đây\ntot_nhat = a[0]\n\nprint(tot_nhat)\n",
    giai=KHUNG_MANG + "tai_day = tot_nhat = a[0]\nfor i in range(1, n):\n    tai_day = max(a[i], tai_day + a[i])\n"
         "    tot_nhat = max(tot_nhat, tai_day)\nprint(tot_nhat)\n",
    tests=lambda: [mang([-2, 1, -3, 4, -1, 2, 1, -5, 4]), mang([5, 4, -1, 7, 8]), mang([-3]), mang([-5, -2, -9]),
                   mang([1, -1, 1, -1, 1]), mang([R(11).randint(-10**4, 10**4) for _ in range(100000)])])

bai(muc="intermediate", slug="tich-tru-chinh-no", ten="Tích các phần tử trừ chính nó", lc="238. Product of Array Except Self",
    mo_ta="Với mỗi *i*, tính tích mọi phần tử của *a* **trừ** `a[i]`. Không dùng phép chia. Gợi ý: tích tiền tố "
          "bên trái nhân tích hậu tố bên phải.",
    vao=VAO_MANG, ra="*n* số kết quả, cách nhau một dấu cách.", rang_buoc="2 ≤ *n* ≤ 10⁴, |a[i]| ≤ 30.",
    khung=KHUNG_MANG + "\n# Viết code của em ở đây\nkq = [0] * n\n\nprint(*kq)\n",
    giai=KHUNG_MANG + "kq = [1] * n\ntrai = 1\nfor i in range(n):\n    kq[i] = trai\n    trai *= a[i]\nphai = 1\n"
         "for i in range(n - 1, -1, -1):\n    kq[i] *= phai\n    phai *= a[i]\nprint(*kq)\n",
    tests=lambda: [mang([1, 2, 3, 4]), mang([-1, 1, 0, -3, 3]), mang([2, 3]), mang([0, 0, 5]),
                   mang([1, -1, 2, -2, 1]), mang([R(12).choice([1, -1, 1, 2, -1, 1]) for _ in range(2000)])])

bai(muc="intermediate", slug="ba-so-tong-0", ten="Ba số có tổng bằng 0", lc="15. 3Sum",
    mo_ta="Tìm mọi bộ ba **giá trị** (x, y, z) lấy ở ba vị trí khác nhau của *a* mà x + y + z = 0. Mỗi bộ ghi "
          "tăng dần x ≤ y ≤ z, không in hai bộ giống nhau. Gợi ý: sắp xếp rồi dùng hai con trỏ.",
    vao=VAO_MANG, ra="Dòng đầu là số bộ ba *m*. Tiếp theo *m* dòng, mỗi dòng một bộ x y z, các bộ theo thứ tự tăng dần "
                     "(so x trước, rồi y).", rang_buoc="3 ≤ *n* ≤ 1500, |a[i]| ≤ 10⁵.",
    khung=KHUNG_MANG + "\n# Viết code của em ở đây\nbo_ba = []\n\nprint(len(bo_ba))\nfor x, y, z in bo_ba:\n"
                       "    print(x, y, z)\n",
    giai=KHUNG_MANG + "a.sort()\nbo_ba = []\nfor i in range(n - 2):\n    if i > 0 and a[i] == a[i - 1]:\n        continue\n"
         "    trai, phai = i + 1, n - 1\n    while trai < phai:\n        tong = a[i] + a[trai] + a[phai]\n        if tong < 0:\n"
         "            trai += 1\n        elif tong > 0:\n            phai -= 1\n        else:\n"
         "            bo_ba.append((a[i], a[trai], a[phai]))\n            trai += 1\n"
         "            while trai < phai and a[trai] == a[trai - 1]:\n                trai += 1\n"
         "print(len(bo_ba))\nfor x, y, z in bo_ba:\n    print(x, y, z)\n",
    tests=lambda: [mang([-1, 0, 1, 2, -1, -4]), mang([0, 1, 1]), mang([0, 0, 0, 0]), mang([-2, 0, 1, 1, 2]),
                   mang([3, -1, -1, -2, 5, 0]), mang([R(13).randint(-50, 50) for _ in range(1500)])])

bai(muc="intermediate", slug="binh-chua-nhieu-nuoc", ten="Bình chứa nhiều nước nhất", lc="11. Container With Most Water",
    mo_ta="Có *n* cột thẳng đứng, cột *i* cao `h[i]`, đứng ở hoành độ *i*. Chọn hai cột làm thành bình: lượng nước "
          "là `(j - i) * min(h[i], h[j])`. Tìm lượng nước lớn nhất. Gợi ý: hai con trỏ ở hai đầu, dời cột thấp hơn.",
    vao=VAO_MANG, ra="Lượng nước lớn nhất.", rang_buoc="2 ≤ *n* ≤ 10⁵, 0 ≤ h[i] ≤ 10⁴.",
    khung="n = int(input())\nh = list(map(int, input().split()))\n\n# Viết code của em ở đây\nlon_nhat = 0\n\nprint(lon_nhat)\n",
    giai="n = int(input())\nh = list(map(int, input().split()))\ntrai, phai = 0, n - 1\nlon_nhat = 0\nwhile trai < phai:\n"
         "    lon_nhat = max(lon_nhat, (phai - trai) * min(h[trai], h[phai]))\n    if h[trai] < h[phai]:\n"
         "        trai += 1\n    else:\n        phai -= 1\nprint(lon_nhat)\n",
    tests=lambda: [mang([1, 8, 6, 2, 5, 4, 8, 3, 7]), mang([1, 1]), mang([4, 3, 2, 1, 4]), mang([1, 2, 1]),
                   mang([0, 0, 5]), mang([R(14).randint(0, 10**4) for _ in range(100000)])])

bai(muc="intermediate", slug="nhom-dao-chu", ten="Gom nhóm các từ đảo chữ", lc="49. Group Anagrams",
    mo_ta="Cho *n* từ. Gom các từ là đảo chữ của nhau (cùng các chữ cái, cùng số lần) vào một nhóm. Gợi ý: khoá "
          "của một từ là `''.join(sorted(tu))`.",
    vao="Dòng 1 là *n*. Dòng 2 là *n* từ (chữ thường) cách nhau một dấu cách.",
    ra="Mỗi nhóm một dòng: các từ trong nhóm sắp tăng dần, cách nhau một dấu cách. Các nhóm sắp tăng dần theo từ đầu "
       "tiên. Từ lặp lại thì in đủ số lần.", rang_buoc="1 ≤ *n* ≤ 10⁴, mỗi từ dài 1–10.",
    khung="n = int(input())\ntu = input().split()\n\n# Viết code của em ở đây: dict khoá → danh sách từ\nnhom = []\n\n"
          "for g in nhom:\n    print(*g)\n",
    giai="n = int(input())\ntu = input().split()\ntheo_khoa = {}\nfor w in tu:\n    khoa = ''.join(sorted(w))\n"
         "    theo_khoa.setdefault(khoa, []).append(w)\nnhom = sorted(sorted(g) for g in theo_khoa.values())\n"
         "for g in nhom:\n    print(*g)\n",
    tests=lambda: ["6\neat tea tan ate nat bat\n", "1\na\n", "3\nab ba ab\n", "4\nabc def fed cab\n",
                   "5\nx y x z y\n", _tu(15)])


def _tu(seed):
    r = R(seed)
    goc = ["".join(r.choice("abcde") for _ in range(r.randint(1, 6))) for _ in range(300)]
    ds = []
    for _ in range(8000):
        w = list(r.choice(goc))
        r.shuffle(w)
        ds.append("".join(w))
    return f"{len(ds)}\n{' '.join(ds)}\n"


bai(muc="intermediate", slug="k-phan-tu-nhieu-nhat", ten="k giá trị xuất hiện nhiều nhất", lc="347. Top K Frequent Elements",
    mo_ta="In *k* giá trị xuất hiện nhiều lần nhất trong *a*, theo số lần xuất hiện giảm dần; hai giá trị cùng số lần "
          "thì giá trị nhỏ hơn đứng trước.",
    vao="Dòng 1 là *n* và *k*. Dòng 2 là *n* số nguyên.", ra="*k* giá trị, cách nhau một dấu cách.",
    rang_buoc="1 ≤ *k* ≤ số giá trị khác nhau ≤ *n* ≤ 10⁵.",
    khung="n, k = map(int, input().split())\na = list(map(int, input().split()))\n\n# Viết code của em ở đây\nkq = []\n\n"
          "print(*kq)\n",
    giai="n, k = map(int, input().split())\na = list(map(int, input().split()))\ndem = {}\nfor x in a:\n"
         "    dem[x] = dem.get(x, 0) + 1\nthu_tu = sorted(dem, key=lambda x: (-dem[x], x))\nprint(*thu_tu[:k])\n",
    tests=lambda: [mang([1, 1, 1, 2, 2, 3], [2]), mang([1], [1]), mang([4, 4, 5, 5, 6], [2]), mang([3, 1, 2], [3]),
                   mang([-1, -1, 2, 2, 2, 7], [1]), mang([int(R(16).gauss(0, 30)) for _ in range(100000)], [10])])

bai(muc="intermediate", slug="ke-trom-nha", ten="Kẻ trộm và dãy nhà", lc="198. House Robber",
    mo_ta="Dãy *n* ngôi nhà, nhà *i* có `tien[i]`. Kẻ trộm không được lấy ở **hai nhà kề nhau**. Lấy được nhiều nhất "
          "bao nhiêu? Gợi ý: `tot[i] = max(tot[i-1], tot[i-2] + tien[i])`.",
    vao=VAO_MANG, ra="Số tiền lớn nhất.", rang_buoc="1 ≤ *n* ≤ 10⁵, 0 ≤ tien[i] ≤ 400.",
    khung="n = int(input())\ntien = list(map(int, input().split()))\n\n# Viết code của em ở đây\ntot_nhat = 0\n\n"
          "print(tot_nhat)\n",
    giai="n = int(input())\ntien = list(map(int, input().split()))\ntruoc, nay = 0, 0\nfor x in tien:\n"
         "    truoc, nay = nay, max(nay, truoc + x)\nprint(nay)\n",
    tests=lambda: [mang([1, 2, 3, 1]), mang([2, 7, 9, 3, 1]), mang([5]), mang([2, 1, 1, 2]), mang([0, 0, 0]),
                   mang([R(17).randint(0, 400) for _ in range(100000)])])

bai(muc="intermediate", slug="dem-dao", ten="Đếm số hòn đảo", lc="200. Number of Islands",
    mo_ta="Bản đồ *R* × *C*, ô `1` là đất, ô `0` là nước. Một **đảo** là nhóm ô đất nối nhau theo 4 hướng (trên, dưới, "
          "trái, phải). Đếm số đảo. Gợi ý: gặp ô đất chưa thăm thì loang (BFS/DFS) đánh dấu cả đảo.",
    vao="Dòng 1 là *R* và *C*. *R* dòng tiếp theo, mỗi dòng *C* ký tự `0`/`1` viết liền.", ra="Số đảo.",
    rang_buoc="1 ≤ *R*, *C* ≤ 300.",
    khung="R, C = map(int, input().split())\nluoi = [list(input().strip()) for _ in range(R)]\n\n"
          "# Viết code của em ở đây\nso_dao = 0\n\nprint(so_dao)\n",
    giai="R, C = map(int, input().split())\nluoi = [list(input().strip()) for _ in range(R)]\nso_dao = 0\n"
         "for r in range(R):\n    for c in range(C):\n        if luoi[r][c] == '1':\n            so_dao += 1\n"
         "            ngan = [(r, c)]\n            luoi[r][c] = '0'\n            while ngan:\n                x, y = ngan.pop()\n"
         "                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):\n                    u, v = x + dx, y + dy\n"
         "                    if 0 <= u < R and 0 <= v < C and luoi[u][v] == '1':\n                        luoi[u][v] = '0'\n"
         "                        ngan.append((u, v))\nprint(so_dao)\n",
    tests=lambda: ["3 4\n1100\n1001\n0011\n", "2 3\n111\n010\n", "1 1\n0\n", "3 3\n101\n010\n101\n",
                   "4 5\n11000\n11000\n00100\n00011\n", _ban_do(18)])


def _ban_do(seed):
    r = R(seed)
    return "300 300\n" + "".join("".join(r.choice("0001") for _ in range(300)) + "\n" for _ in range(300))


bai(muc="intermediate", slug="gop-doan", ten="Gộp các đoạn chồng nhau", lc="56. Merge Intervals",
    mo_ta="Cho *n* đoạn [l, r]. Gộp mọi đoạn chồng nhau (kể cả chỉ chạm đầu mút, như [1, 3] và [3, 5]) thành một. "
          "Gợi ý: sắp theo đầu trái, đi một lượt.",
    vao="Dòng 1 là *n*. *n* dòng tiếp theo, mỗi dòng hai số *l* ≤ *r*.",
    ra="Các đoạn sau khi gộp, sắp tăng dần, mỗi đoạn một dòng `l r`.", rang_buoc="1 ≤ *n* ≤ 10⁴, 0 ≤ l ≤ r ≤ 10⁶.",
    khung="n = int(input())\ndoan = [list(map(int, input().split())) for _ in range(n)]\n\n"
          "# Viết code của em ở đây\nkq = []\n\nfor l, r in kq:\n    print(l, r)\n",
    giai="n = int(input())\ndoan = [list(map(int, input().split())) for _ in range(n)]\ndoan.sort()\nkq = []\n"
         "for l, r in doan:\n    if kq and l <= kq[-1][1]:\n        kq[-1][1] = max(kq[-1][1], r)\n    else:\n"
         "        kq.append([l, r])\nfor l, r in kq:\n    print(l, r)\n",
    tests=lambda: ["4\n1 3\n2 6\n8 10\n15 18\n", "2\n1 4\n4 5\n", "1\n5 5\n", "3\n1 10\n2 3\n4 5\n",
                   "3\n6 8\n1 2\n3 4\n", _doan(19)])


def _doan(seed):
    r = R(seed)
    ds = []
    for _ in range(10000):
        l = r.randint(0, 10**6 - 1)
        ds.append(f"{l} {min(10**6, l + r.randint(0, 120))}")
    return f"{len(ds)}\n" + "\n".join(ds) + "\n"


bai(muc="intermediate", slug="tim-trong-mang-xoay", ten="Tìm trong mảng tăng đã bị xoay", lc="33. Search in Rotated Sorted Array",
    mo_ta="Mảng *a* gồm các số **khác nhau**, vốn tăng dần nhưng đã bị xoay ở một vị trí nào đó (ví dụ "
          "[4 5 6 7 0 1 2]). Tìm vị trí của *x* trong *a*, không có thì in -1. Yêu cầu O(log n): tìm kiếm nhị phân, "
          "mỗi lần xét nửa nào đang tăng.",
    vao="Dòng 1 là *n* và *x*. Dòng 2 là mảng *a*.", ra="Vị trí của *x* (đánh số từ 0) hoặc -1.",
    rang_buoc="1 ≤ *n* ≤ 10⁵.",
    khung="n, x = map(int, input().split())\na = list(map(int, input().split()))\n\n# Viết code của em ở đây\nvi_tri = -1\n\n"
          "print(vi_tri)\n",
    giai="n, x = map(int, input().split())\na = list(map(int, input().split()))\ntrai, phai = 0, n - 1\nvi_tri = -1\n"
         "while trai <= phai:\n    giua = (trai + phai) // 2\n    if a[giua] == x:\n        vi_tri = giua\n        break\n"
         "    if a[trai] <= a[giua]:\n        if a[trai] <= x < a[giua]:\n            phai = giua - 1\n        else:\n"
         "            trai = giua + 1\n    else:\n        if a[giua] < x <= a[phai]:\n            trai = giua + 1\n"
         "        else:\n            phai = giua - 1\nprint(vi_tri)\n",
    tests=lambda: [mang([4, 5, 6, 7, 0, 1, 2], [0]), mang([4, 5, 6, 7, 0, 1, 2], [3]), mang([1], [0]), mang([3, 1], [1]),
                   mang([5, 1, 3], [5]), _xoay(20)])


def _xoay(seed):
    r = R(seed)
    a = sorted(r.sample(range(-10**6, 10**6), 100000))
    k = r.randrange(100000)
    a = a[k:] + a[:k]
    return mang(a, [a[r.randrange(100000)]])


bai(muc="intermediate", slug="chuoi-con-doi-xung-dai-nhat", ten="Chuỗi con đối xứng dài nhất", lc="5. Longest Palindromic Substring",
    mo_ta="Tìm đoạn liên tiếp dài nhất của *s* mà đọc xuôi ngược như nhau. Có nhiều đoạn cùng độ dài lớn nhất thì in "
          "đoạn **bắt đầu sớm nhất**. Gợi ý: lấy từng vị trí (và từng khe giữa hai ký tự) làm tâm rồi nới ra hai bên.",
    vao="Một dòng là chuỗi *s* (chữ thường).", ra="Đoạn đối xứng dài nhất.", rang_buoc="1 ≤ |s| ≤ 2000.",
    khung="s = input().strip()\n\n# Viết code của em ở đây\ntot = s[0]\n\nprint(tot)\n",
    giai="s = input().strip()\nn = len(s)\nbd, dai = 0, 1\nfor tam in range(2 * n - 1):\n    l, r = tam // 2, (tam + 1) // 2\n"
         "    while l >= 0 and r < n and s[l] == s[r]:\n        l -= 1\n        r += 1\n"
         "    if r - l - 1 > dai or (r - l - 1 == dai and l + 1 < bd):\n        bd, dai = l + 1, r - l - 1\n"
         "print(s[bd:bd + dai])\n",
    tests=lambda: ["babad\n", "cbbd\n", "a\n", "ac\n", "forgeeksskeegfor\n",
                   "".join(R(21).choice("ab") for _ in range(2000)) + "\n"])

bai(muc="intermediate", slug="dem-doan-tong-k", ten="Đếm đoạn con có tổng bằng k", lc="560. Subarray Sum Equals K",
    mo_ta="Đếm số đoạn liên tiếp (khác rỗng) của *a* có tổng đúng bằng *k*. Mảng có thể có số âm. Gợi ý: tổng tiền tố "
          "và dict đếm số lần mỗi tổng tiền tố đã gặp.",
    vao="Dòng 1 là *n* và *k*. Dòng 2 là mảng *a*.", ra="Số đoạn.", rang_buoc="1 ≤ *n* ≤ 10⁵, |a[i]| ≤ 1000.",
    khung="n, k = map(int, input().split())\na = list(map(int, input().split()))\n\n# Viết code của em ở đây\ndem = 0\n\n"
          "print(dem)\n",
    giai="n, k = map(int, input().split())\na = list(map(int, input().split()))\nso_lan = {0: 1}\ntong = 0\ndem = 0\n"
         "for x in a:\n    tong += x\n    dem += so_lan.get(tong - k, 0)\n    so_lan[tong] = so_lan.get(tong, 0) + 1\n"
         "print(dem)\n",
    tests=lambda: [mang([1, 1, 1], [2]), mang([1, 2, 3], [3]), mang([5], [5]), mang([1, -1, 0], [0]),
                   mang([3, 4, 7, 2, -3, 1, 4, 2], [7]), mang([R(22).randint(-3, 3) for _ in range(100000)], [4])])

bai(muc="intermediate", slug="nhiet-do-am-hon", ten="Bao lâu nữa thì ấm hơn", lc="739. Daily Temperatures",
    mo_ta="`t[i]` là nhiệt độ ngày *i*. Với mỗi ngày, cần chờ bao nhiêu ngày nữa mới có ngày **ấm hơn hẳn**? Không có "
          "thì ghi 0. Gợi ý: ngăn xếp giữ các ngày chưa tìm được ngày ấm hơn.",
    vao=VAO_MANG, ra="*n* số kết quả, cách nhau một dấu cách.", rang_buoc="1 ≤ *n* ≤ 10⁵, 30 ≤ t[i] ≤ 100.",
    khung="n = int(input())\nt = list(map(int, input().split()))\n\n# Viết code của em ở đây\ncho = [0] * n\n\nprint(*cho)\n",
    giai="n = int(input())\nt = list(map(int, input().split()))\ncho = [0] * n\nngan = []\nfor i in range(n):\n"
         "    while ngan and t[ngan[-1]] < t[i]:\n        j = ngan.pop()\n        cho[j] = i - j\n    ngan.append(i)\n"
         "print(*cho)\n",
    tests=lambda: [mang([73, 74, 75, 71, 69, 72, 76, 73]), mang([30, 40, 50, 60]), mang([90]), mang([50, 50, 50]),
                   mang([60, 50, 40, 70]), mang([R(23).randint(30, 100) for _ in range(100000)])])

bai(muc="intermediate", slug="xoay-ma-tran", ten="Xoay ma trận 90 độ", lc="48. Rotate Image",
    mo_ta="Xoay ma trận vuông *n* × *n* đi 90 độ theo chiều kim đồng hồ, **ngay trên ma trận** (không tạo ma trận mới). "
          "Gợi ý: chuyển vị (đổi `m[i][j]` với `m[j][i]`) rồi đảo từng hàng.",
    vao="Dòng 1 là *n*. *n* dòng tiếp theo, mỗi dòng *n* số.", ra="Ma trận sau khi xoay, *n* dòng.",
    rang_buoc="1 ≤ *n* ≤ 200.",
    khung="n = int(input())\nm = [list(map(int, input().split())) for _ in range(n)]\n\n# Viết code của em ở đây\n\n"
          "for hang in m:\n    print(*hang)\n",
    giai="n = int(input())\nm = [list(map(int, input().split())) for _ in range(n)]\nfor i in range(n):\n"
         "    for j in range(i + 1, n):\n        m[i][j], m[j][i] = m[j][i], m[i][j]\nfor hang in m:\n    hang.reverse()\n"
         "for hang in m:\n    print(*hang)\n",
    tests=lambda: ["3\n1 2 3\n4 5 6\n7 8 9\n", "2\n1 2\n3 4\n", "1\n5\n", "4\n5 1 9 11\n2 4 8 10\n13 3 6 7\n15 14 12 16\n",
                   "2\n-1 0\n0 -1\n", "200\n" + "".join(" ".join(str(R(24 + i).randint(-99, 99)) for _ in range(200)) + "\n"
                                                    for i in range(200))])

bai(muc="intermediate", slug="ma-tran-xoan-oc", ten="Đọc ma trận theo hình xoắn ốc", lc="54. Spiral Matrix",
    mo_ta="Đọc các phần tử ma trận *R* × *C* theo hình xoắn ốc: bắt đầu ở góc trên trái, đi sang phải, xuống, sang "
          "trái, lên, rồi vào vòng trong. Gợi ý: bốn biên tren, duoi, trai, phai thu dần vào.",
    vao="Dòng 1 là *R* và *C*. *R* dòng tiếp theo, mỗi dòng *C* số.", ra="*R*·*C* số theo thứ tự đọc, một dòng.",
    rang_buoc="1 ≤ *R*, *C* ≤ 100.",
    khung="R, C = map(int, input().split())\nm = [list(map(int, input().split())) for _ in range(R)]\n\n"
          "# Viết code của em ở đây\nkq = []\n\nprint(*kq)\n",
    giai="R, C = map(int, input().split())\nm = [list(map(int, input().split())) for _ in range(R)]\nkq = []\n"
         "tren, duoi, trai, phai = 0, R - 1, 0, C - 1\nwhile tren <= duoi and trai <= phai:\n"
         "    for j in range(trai, phai + 1):\n        kq.append(m[tren][j])\n    tren += 1\n"
         "    for i in range(tren, duoi + 1):\n        kq.append(m[i][phai])\n    phai -= 1\n    if tren <= duoi:\n"
         "        for j in range(phai, trai - 1, -1):\n            kq.append(m[duoi][j])\n        duoi -= 1\n"
         "    if trai <= phai:\n        for i in range(duoi, tren - 1, -1):\n            kq.append(m[i][trai])\n"
         "        trai += 1\nprint(*kq)\n",
    tests=lambda: ["3 3\n1 2 3\n4 5 6\n7 8 9\n", "3 4\n1 2 3 4\n5 6 7 8\n9 10 11 12\n", "1 1\n7\n", "1 3\n1 2 3\n",
                   "3 1\n1\n2\n3\n", "100 100\n" + "".join(" ".join(str(100 * i + j) for j in range(100)) + "\n"
                                                     for i in range(100))])

bai(muc="intermediate", slug="so-duong-di-luoi", ten="Số đường đi trên lưới", lc="62. Unique Paths",
    mo_ta="Robot đứng ở ô trên trái của lưới *m* × *n*, mỗi bước chỉ đi **sang phải** hoặc **xuống dưới**. Có bao nhiêu "
          "đường khác nhau tới ô dưới phải? Gợi ý: `dp[i][j] = dp[i-1][j] + dp[i][j-1]`.",
    vao="Hai số *m* và *n*.", ra="Số đường đi.", rang_buoc="1 ≤ *m*, *n* ≤ 100.",
    khung="m, n = map(int, input().split())\n\n# Viết code của em ở đây\nso_duong = 0\n\nprint(so_duong)\n",
    giai="m, n = map(int, input().split())\ndp = [[1] * n for _ in range(m)]\nfor i in range(1, m):\n"
         "    for j in range(1, n):\n        dp[i][j] = dp[i - 1][j] + dp[i][j - 1]\nprint(dp[m - 1][n - 1])\n",
    tests=lambda: ["3 3\n", "3 2\n", "1 1\n", "3 7\n", "1 10\n", "100 100\n"])

bai(muc="intermediate", slug="tro-choi-nhay", ten="Trò chơi nhảy", lc="55. Jump Game",
    mo_ta="Em đứng ở ô 0 của mảng *a*. Đứng ở ô *i* thì nhảy được tới bất kỳ ô nào trong *i*+1 … *i*+`a[i]`. Có tới "
          "được ô cuối không? Gợi ý: nhớ ô xa nhất tới được.",
    vao=VAO_MANG, ra="`YES` hoặc `NO`.", rang_buoc="1 ≤ *n* ≤ 10⁵, 0 ≤ a[i] ≤ 10⁵.",
    khung=KHUNG_MANG + "\n# Viết code của em ở đây\ntoi_duoc = False\n\nprint(\"YES\" if toi_duoc else \"NO\")\n",
    giai=KHUNG_MANG + "xa_nhat = 0\nfor i in range(n):\n    if i > xa_nhat:\n        break\n"
         "    xa_nhat = max(xa_nhat, i + a[i])\nprint(\"YES\" if xa_nhat >= n - 1 else \"NO\")\n",
    tests=lambda: [mang([2, 3, 1, 1, 4]), mang([3, 2, 1, 0, 4]), mang([0]), mang([0, 1]), mang([1, 0, 1]),
                   mang([R(25).randint(0, 3) for _ in range(100000)])])

bai(muc="intermediate", slug="tach-tu", ten="Tách chuỗi thành các từ có sẵn", lc="139. Word Break",
    mo_ta="Cho chuỗi *s* và một từ điển. Có tách được *s* thành các từ trong từ điển (mỗi từ dùng bao nhiêu lần cũng "
          "được) không? Gợi ý: `dp[i]` = tách được *i* ký tự đầu.",
    vao="Dòng 1 là *s*. Dòng 2 là số từ *m*. Dòng 3 là *m* từ cách nhau một dấu cách.", ra="`YES` hoặc `NO`.",
    rang_buoc="1 ≤ |s| ≤ 300, 1 ≤ *m* ≤ 1000, mỗi từ dài 1–20, chữ thường.",
    khung="s = input().strip()\nm = int(input())\ntu_dien = input().split()\n\n# Viết code của em ở đây\ntach_duoc = False\n\n"
          "print(\"YES\" if tach_duoc else \"NO\")\n",
    giai="s = input().strip()\nm = int(input())\ntu_dien = set(input().split())\nn = len(s)\ndp = [False] * (n + 1)\n"
         "dp[0] = True\nfor i in range(1, n + 1):\n    for j in range(max(0, i - 20), i):\n"
         "        if dp[j] and s[j:i] in tu_dien:\n            dp[i] = True\n            break\n"
         "print(\"YES\" if dp[n] else \"NO\")\n",
    tests=lambda: ["leetcode\n2\nleet code\n", "catsandog\n5\ncats dog sand and cat\n", "a\n1\na\n", "ab\n1\na\n",
                   "applepenapple\n2\napple pen\n", "a" * 299 + "b\n3\na aa aaa\n"])

bai(muc="intermediate", slug="sinh-ngoac-dung", ten="Sinh mọi dãy ngoặc đúng", lc="22. Generate Parentheses",
    mo_ta="In mọi dãy gồm *n* cặp ngoặc `(` `)` viết đúng (mỗi tiền tố có số `(` không ít hơn số `)`, và tổng hai loại "
          "bằng nhau). Gợi ý: quay lui, thêm `(` khi còn, thêm `)` khi số `)` đang ít hơn số `(`.",
    vao="Một số *n*.", ra="Mỗi dãy một dòng, theo thứ tự từ điển (`(` đứng trước `)`).", rang_buoc="1 ≤ *n* ≤ 10.",
    khung="n = int(input())\n\n# Viết code của em ở đây\nkq = []\n\nfor d in kq:\n    print(d)\n",
    giai="n = int(input())\nkq = []\n\n\ndef sinh(dang, mo, dong):\n    if len(dang) == 2 * n:\n        kq.append(dang)\n"
         "        return\n    if mo < n:\n        sinh(dang + '(', mo + 1, dong)\n    if dong < mo:\n"
         "        sinh(dang + ')', mo, dong + 1)\n\n\nsinh('', 0, 0)\nfor d in kq:\n    print(d)\n",
    tests=lambda: ["2\n", "1\n", "3\n", "4\n", "6\n", "10\n"])

# ============================================================ ADVANCED

bai(muc="advanced", slug="hung-nuoc-mua", ten="Hứng nước mưa", lc="42. Trapping Rain Water",
    mo_ta="Các cột rộng 1 đứng sát nhau, cột *i* cao `h[i]`. Sau mưa, nước đọng giữa các cột. Tổng lượng nước đọng là "
          "bao nhiêu? Ở mỗi vị trí, mực nước là `min(cao nhất bên trái, cao nhất bên phải)`. Thử làm O(1) bộ nhớ bằng "
          "hai con trỏ.",
    vao=VAO_MANG, ra="Tổng lượng nước.", rang_buoc="1 ≤ *n* ≤ 2·10⁴, 0 ≤ h[i] ≤ 10⁵.",
    khung="n = int(input())\nh = list(map(int, input().split()))\n\n# Viết code của em ở đây\nnuoc = 0\n\nprint(nuoc)\n",
    giai="n = int(input())\nh = list(map(int, input().split()))\ntrai, phai = 0, n - 1\nmax_trai = max_phai = 0\nnuoc = 0\n"
         "while trai < phai:\n    if h[trai] < h[phai]:\n        max_trai = max(max_trai, h[trai])\n"
         "        nuoc += max_trai - h[trai]\n        trai += 1\n    else:\n        max_phai = max(max_phai, h[phai])\n"
         "        nuoc += max_phai - h[phai]\n        phai -= 1\nprint(nuoc)\n",
    tests=lambda: [mang([0, 1, 0, 2, 1, 0, 1, 3, 2, 1, 2, 1]), mang([4, 2, 0, 3, 2, 5]), mang([3]), mang([1, 2, 3]),
                   mang([5, 0, 5, 0, 5]), mang([R(26).randint(0, 10**5) for _ in range(20000)])])

bai(muc="advanced", slug="khoang-cach-sua-chuoi", ten="Khoảng cách sửa chuỗi", lc="72. Edit Distance",
    mo_ta="Mỗi thao tác là **chèn**, **xoá** hoặc **thay** một ký tự. Cần ít nhất bao nhiêu thao tác để biến *s* thành "
          "*t*? Gợi ý: `dp[i][j]` = số thao tác ít nhất biến *i* ký tự đầu của *s* thành *j* ký tự đầu của *t*.",
    vao="Dòng 1 là *s*, dòng 2 là *t* (chữ thường).", ra="Số thao tác ít nhất.", rang_buoc="1 ≤ |s|, |t| ≤ 500.",
    khung="s = input().strip()\nt = input().strip()\n\n# Viết code của em ở đây\nso_buoc = 0\n\nprint(so_buoc)\n",
    giai="s = input().strip()\nt = input().strip()\nm, n = len(s), len(t)\ndp = [[0] * (n + 1) for _ in range(m + 1)]\n"
         "for i in range(m + 1):\n    dp[i][0] = i\nfor j in range(n + 1):\n    dp[0][j] = j\nfor i in range(1, m + 1):\n"
         "    for j in range(1, n + 1):\n        if s[i - 1] == t[j - 1]:\n            dp[i][j] = dp[i - 1][j - 1]\n"
         "        else:\n            dp[i][j] = 1 + min(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])\nprint(dp[m][n])\n",
    tests=lambda: ["horse\nros\n", "abc\nabc\n", "a\nb\n", "intention\nexecution\n", "kitten\nsitting\n",
                   "".join(R(27).choice("abc") for _ in range(500)) + "\n" + "".join(R(28).choice("abc") for _ in range(480))
                   + "\n"])

bai(muc="advanced", slug="hinh-chu-nhat-lon-nhat", ten="Hình chữ nhật lớn nhất trong biểu đồ cột",
    lc="84. Largest Rectangle in Histogram",
    mo_ta="Biểu đồ có *n* cột rộng 1 đứng sát nhau, cột *i* cao `h[i]`. Tìm diện tích hình chữ nhật lớn nhất nằm trọn "
          "trong biểu đồ. Gợi ý: ngăn xếp tăng dần; khi lấy một cột ra, cột đó là cột thấp nhất của hình chữ nhật kéo "
          "dài tới hai cột thấp hơn hai bên.",
    vao=VAO_MANG, ra="Diện tích lớn nhất.", rang_buoc="1 ≤ *n* ≤ 10⁵, 0 ≤ h[i] ≤ 10⁴.",
    khung="n = int(input())\nh = list(map(int, input().split()))\n\n# Viết code của em ở đây\nlon_nhat = 0\n\nprint(lon_nhat)\n",
    giai="n = int(input())\nh = list(map(int, input().split()))\nngan = []\nlon_nhat = 0\nfor i in range(n + 1):\n"
         "    cao = h[i] if i < n else 0\n    while ngan and h[ngan[-1]] >= cao:\n        cot = ngan.pop()\n"
         "        trai = ngan[-1] + 1 if ngan else 0\n        lon_nhat = max(lon_nhat, h[cot] * (i - trai))\n"
         "    ngan.append(i)\nprint(lon_nhat)\n",
    tests=lambda: [mang([2, 1, 5, 6, 2, 3]), mang([2, 4]), mang([7]), mang([3, 3, 3]), mang([0, 2, 0]),
                   mang([R(29).randint(0, 10**4) for _ in range(100000)])])

bai(muc="advanced", slug="max-cua-so-truot", ten="Giá trị lớn nhất của cửa sổ trượt", lc="239. Sliding Window Maximum",
    mo_ta="Cửa sổ dài *k* trượt từ đầu tới cuối mảng *a*, mỗi lần dời một ô. In giá trị lớn nhất trong cửa sổ ở mỗi "
          "vị trí. Yêu cầu O(n): dùng hàng đợi hai đầu (`collections.deque`) giữ các vị trí có giá trị giảm dần.",
    vao="Dòng 1 là *n* và *k*. Dòng 2 là mảng *a*.", ra="*n* − *k* + 1 số, cách nhau một dấu cách.",
    rang_buoc="1 ≤ *k* ≤ *n* ≤ 10⁵, |a[i]| ≤ 10⁴.",
    khung="from collections import deque\n\nn, k = map(int, input().split())\na = list(map(int, input().split()))\n\n"
          "# Viết code của em ở đây\nkq = []\n\nprint(*kq)\n",
    giai="from collections import deque\n\nn, k = map(int, input().split())\na = list(map(int, input().split()))\n"
         "q = deque()\nkq = []\nfor i in range(n):\n    while q and a[q[-1]] <= a[i]:\n        q.pop()\n    q.append(i)\n"
         "    if q[0] <= i - k:\n        q.popleft()\n    if i >= k - 1:\n        kq.append(a[q[0]])\nprint(*kq)\n",
    tests=lambda: [mang([1, 3, -1, -3, 5, 3, 6, 7], [3]), mang([1], [1]), mang([9, 11], [2]), mang([4, -2], [1]),
                   mang([7, 2, 4], [2]), mang([R(30).randint(-10**4, 10**4) for _ in range(100000)], [500])])
