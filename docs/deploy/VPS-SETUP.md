# Dựng VPS để deploy tự động

Server trên VPS **chỉ để nhóm thử phần TCP từ xa** và để thấy mỗi lần merge đã lên được một máy thật. Lớp học thật chạy server trên máy giáo viên trong phòng máy (spec §5.1), vì multicast không đi qua Internet.

Làm **một lần**, do một người trong nhóm làm. Sau đó, mỗi lần một PR được merge vào `main`, GitHub Actions tự:

1. build + test, đóng Docker image, đẩy lên GHCR với tag là mã commit;
2. SSH vào VPS, kéo đúng image đó, khởi động lại;
3. gọi `http://<IP VPS>:8080/version` để chắc chắn VPS đang chạy đúng commit vừa merge.

Chưa làm xong hướng dẫn này thì bước 2–3 **tự bỏ qua** (không báo đỏ), bước 1 vẫn chạy.

---

## 1. Thuê VPS

Cần một máy Ubuntu 22.04 hoặc 24.04, tối thiểu 1 GB RAM. Image đã đóng cho cả hai kiến trúc nên chọn loại nào cũng được:

| Nhà cung cấp | Ghi chú |
| --- | --- |
| **Oracle Cloud Always Free** | Miễn phí vĩnh viễn. Chọn máy Ampere A1 (ARM). Phải mở cổng ở **hai** chỗ — xem bước 3 |
| **DigitalOcean** | Có $200 credit qua [GitHub Student Pack](https://education.github.com/pack). Droplet Basic 1 GB là đủ |
| Azure for Students | $100 credit, không cần thẻ |

Ghi lại **địa chỉ IP công khai** của máy.

## 2. Cài Docker và tạo người dùng `deploy`

SSH vào VPS bằng tài khoản quản trị nhà cung cấp cấp sẵn (`ubuntu` trên Oracle, `root` trên DigitalOcean), rồi chạy:

```bash
sudo adduser --disabled-password --gecos "" deploy
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker deploy
```

`deploy` là tài khoản GitHub Actions dùng để SSH vào. Tài khoản này chỉ cần quyền chạy Docker, không cần `sudo`.

## 3. Mở cổng

Cần mở **TCP 7000, 8080** từ Internet (22 cho SSH thường đã mở sẵn).

| Cổng | Dùng cho |
| --- | --- |
| 7000 | LCP/1.0 — app giáo viên và học viên kết nối vào (có từ giai đoạn 1) |
| 8080 | `/version` — bước deploy kiểm tra đúng commit |

**Firewall của nhà cung cấp — bắt buộc:**

- *DigitalOcean:* Networking → Firewalls → Create Firewall → Inbound Rules thêm TCP 7000, 8080 → gán vào droplet.
- *Oracle:* Networking → Virtual Cloud Networks → chọn VCN → Security Lists → Default Security List → Add Ingress Rules: Source CIDR `0.0.0.0/0`, IP Protocol TCP, Destination Port Range `7000,8080`.

**Firewall trong máy — chỉ khi đã mở ở trên mà vẫn không vào được.** Ảnh Ubuntu của Oracle có sẵn luật iptables chặn mọi cổng chưa khai báo, đây là chỗ hay bị kẹt nhất:

```bash
sudo iptables -I INPUT -p tcp -m multiport --dports 7000,8080 -m conntrack --ctstate NEW -j ACCEPT
sudo netfilter-persistent save
```

Máy bật `ufw` thì dùng: `sudo ufw allow 7000,8080/tcp`.

## 4. Tạo khoá SSH riêng cho GitHub Actions

Trên **máy của bạn** (Git Bash), ở một thư mục **ngoài repo**:

```bash
ssh-keygen -t ed25519 -C "github-actions-labcast" -f labcast-deploy -N ""
```

Được hai file: `labcast-deploy` (khoá riêng, **không bao giờ commit**) và `labcast-deploy.pub` (khoá công khai).

> Đã tạo khoá `examnet-deploy` từ hồi dự án còn tên ExamNet thì **dùng tiếp**, không cần tạo lại: tên file không ảnh hưởng gì.

Đưa khoá công khai lên VPS. Trên VPS:

```bash
sudo mkdir -p /home/deploy/.ssh
echo "DÁN NỘI DUNG labcast-deploy.pub VÀO ĐÂY" | sudo tee -a /home/deploy/.ssh/authorized_keys
sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh && sudo chmod 600 /home/deploy/.ssh/authorized_keys
```

Thử từ máy của bạn — phải chạy được mà không hỏi mật khẩu:

```bash
ssh -i labcast-deploy deploy@<IP> docker ps
```

## 5. Lấy known_hosts

GitHub Actions cần biết trước "vân tay" của VPS để không bị ai đó chen giữa giả làm VPS. Trên máy của bạn:

```bash
ssh-keyscan -t ed25519 <IP>
```

Đối chiếu vân tay cho chắc. Hai lệnh dưới phải in ra **cùng một chuỗi** `SHA256:...`:

```bash
# trên máy bạn
ssh-keyscan -t ed25519 <IP> | ssh-keygen -lf -
# trên VPS
ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```

## 6. Khai báo trên GitHub

Chủ repo làm, trong **Settings** của repo:

**Biến (không bí mật)** — Secrets and variables → Actions → tab *Variables* → New repository variable:

| Tên | Giá trị |
| --- | --- |
| `VPS_HOST` | IP công khai của VPS |
| `VPS_USER` | `deploy` |

**Bí mật** — Environments → New environment → đặt tên `production` → Add environment secret:

| Tên | Giá trị |
| --- | --- |
| `VPS_SSH_KEY` | Toàn bộ nội dung file khoá riêng (`labcast-deploy` hoặc `examnet-deploy`), gồm cả dòng `-----BEGIN…` và `-----END…` |
| `VPS_KNOWN_HOSTS` | Dòng in ra ở bước 5 (`<IP> ssh-ed25519 AAAA…`) |

Nên bật thêm trong environment `production`: *Deployment branches and tags* → *Selected branches* → `main`, để chỉ main được deploy.

Repo công khai nhưng secrets vẫn an toàn: GitHub không đưa secrets cho PR mở từ fork, và job deploy chỉ chạy khi `main` thay đổi.

## 7. Lần deploy đầu tiên

1. Vào tab **Actions** → workflow **Deploy** → **Run workflow** (hoặc merge một PR bất kỳ).
2. Nếu bước *Kéo image mới* báo `denied` hoặc `unauthorized`: image trên GHCR đang để riêng tư. Vào trang cá nhân GitHub → **Packages** → `labcast-server` → **Package settings** → **Change visibility** → **Public**. Chạy lại workflow.
3. Mở `http://<IP>:8080/version` trên trình duyệt. Trường `commit` phải trùng mã commit mới nhất trên `main`.

Từ đây mỗi lần merge, trang này sẽ đổi mã commit sau khoảng vài phút.

---

## Vận hành hằng ngày

SSH vào VPS bằng tài khoản `deploy`, rồi:

```bash
cd ~/labcast
docker compose ps                    # đang chạy không
docker compose logs -f --tail 100    # xem log
cat .env                             # đang chạy commit nào
```

**Quay về bản cũ** khi bản mới hỏng:

```bash
cd ~/labcast
sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=<mã commit cũ>/' .env
docker compose pull && docker compose up -d
```

Lần merge tiếp theo sẽ ghi đè `.env` và đưa VPS về bản mới nhất. Cách chữa bền vững là sửa lỗi bằng một PR mới.

**Database** nằm trong volume Docker tên `labcast_labcast-data`, không mất khi deploy lại. Sao lưu:

```bash
docker run --rm -v labcast_labcast-data:/data -v "$PWD":/backup alpine \
  tar czf /backup/labcast-data.tgz -C /data .
```

## Giới hạn

UDP multicast (chiếu animation, cổng 7001) và tìm server bằng UDP broadcast (cổng 7002) không đi qua Internet, nên bản trên VPS gửi mọi gói chiếu bằng TCP. Đây đúng là nhánh tự chuyển sang TCP của ĐG1 (spec §12). Thí nghiệm 1 và 3 phải chạy trong **LAN thật**, ví dụ phòng máy hoặc laptop của nhóm cắm dây chung một switch.

Dự án đổi tên từ ExamNet: nếu VPS còn bản cũ ở `~/examnet`, bước deploy tự dừng nó để trả lại cổng 8080. Volume dữ liệu cũ vẫn giữ; xoá khi chắc không cần bằng `docker volume rm examnet_examnet-data`.
