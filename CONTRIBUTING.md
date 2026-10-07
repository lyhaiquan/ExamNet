# Quy trình làm việc

Bốn quy tắc, máy móc kiểm hết, không cần nhớ:

1. **Không ai push thẳng lên `main`.** Mọi thay đổi đi qua Pull Request.
2. **Mỗi PR cần CI xanh và 1 người khác duyệt** mới merge được.
3. **Commit và tiêu đề PR theo Conventional Commits, bắt buộc có scope:** `feat(server): thêm vòng accept`.
4. **Merge xong là tự deploy lên VPS.** Kiểm tra ở `http://<IP VPS>:8080/version`.

---

## Cài đặt lần đầu (mỗi người)

Cần: **JDK 17**, **Git**, **Node.js 22.12 trở lên** (chỉ để chạy hook kiểm commit — commitlint không chạy trên Node 20), **Docker** (để chạy code học viên trong sandbox). Làm phần `runner/` hoặc soạn `content/` thì cần thêm **Python 3.12**.

```bash
git clone https://github.com/lyhaiquan/ExamNet.git LabCast
cd LabCast
npm install                # bật hook husky — BẮT BUỘC, không có bước này hook không chạy
cd source
./mvnw verify              # Windows (PowerShell/cmd): mvnw.cmd verify
```

Trên Windows nên dùng **Git Bash** cho các lệnh git để hook chạy giống hệt trên CI.

## Làm một việc từ đầu tới cuối

```bash
git switch main && git pull                       # luôn bắt đầu từ main mới nhất
git switch -c feat/common-frame-codec             # nhánh mới cho việc này

# ... code + test ...

git add .
git commit -m "feat(common): thêm Frame và FrameCodec đọc lặp cho đủ LEN"
git push -u origin feat/common-frame-codec
```

Rồi lên GitHub mở Pull Request:

1. **Tiêu đề PR** đúng mẫu commit — nó sẽ là commit duy nhất nằm lại trên `main`.
2. Điền mẫu mô tả có sẵn.
3. Chờ hai check `build` và `pr-title` xanh, nhờ một bạn review.
4. Bấm **Squash and merge**. Nhánh tự xoá, VPS tự cập nhật sau vài phút.

PR nên nhỏ: một tính năng hoặc một bản sửa, lý tưởng dưới 400 dòng. PR nhỏ được review kỹ hơn và ít xung đột hơn.

**Tên nhánh:** `<type>/<scope>-<mô-tả-ngắn>`, ví dụ `feat/server-accept-loop`, `fix/common-partial-read`, `test/bench-exp3-mat-goi`, `feat/content-n-queens`.

## Luật commit

```text
<type>(<scope>): <mô tả ngắn, tiếng Việt hoặc tiếng Anh>

<thân — tuỳ chọn: vì sao thay đổi, không phải thay đổi gì>
```

**type** — loại thay đổi:

| type | Khi nào |
| --- | --- |
| `feat` | Thêm tính năng |
| `fix` | Sửa lỗi |
| `test` | Thêm hoặc sửa test, kịch bản thí nghiệm |
| `perf` | Tăng hiệu năng |
| `refactor` | Sửa cấu trúc code, không đổi hành vi |
| `docs` | Tài liệu, spec, README |
| `build` | Maven, dependency |
| `ci` | GitHub Actions |
| `chore` | Việc lặt vặt khác |
| `revert` | Hoàn tác một commit |

**scope** — phần nào của dự án (bắt buộc):

| scope | Phần |
| --- | --- |
| `common` `server` `client` `bench` | Bốn module trong `source/` |
| `runner` | Python chạy bài làm và ghi trace |
| `content` | Bài học và bài tập trong `content/` |
| `packaging` | Bộ cài cho phòng máy |
| `docs` `report` | Tài liệu, báo cáo |
| `ci` `deploy` `build` `deps` `repo` | Hạ tầng |

Ví dụ đúng:

```text
feat(server): thêm vòng accept và thread pool có hàng đợi giới hạn
fix(common): kiểm LEN trước khi cấp phát mảng payload
test(bench): kịch bản thí nghiệm 3 so sánh heartbeat cố định và thích nghi
docs(report): viết mục 8.9 thiết kế truyền thông
```

Ví dụ bị chặn: `update code`, `fix bug`, `feat: thêm accept` (thiếu scope), `feat(abc): …` (scope lạ).

## Hook chạy tự động trên máy bạn

| Khi | Hook làm gì |
| --- | --- |
| `git commit` | Kiểm định dạng code Java (chỉ khi commit có file `.java`) và kiểm câu commit |
| `git push` | Chạy `./mvnw verify` — giống hệt CI, để biết đỏ trước khi đẩy lên |

Code chưa đúng định dạng thì sửa tự động:

```bash
cd source && ./mvnw spotless:apply
```

`git commit --no-verify` bỏ qua được hook, nhưng CI vẫn kiểm lại y như vậy và PR sẽ không merge được. Đừng dùng.

## Khi review PR của bạn khác

- Có test cho phần mới không? Test có thật sự kiểm điều PR nói không?
- Đổi protocol (thêm hay sửa thông điệp) thì đã cập nhật spec §10 chưa?
- Đổi định dạng trace thì đã có cả Người 3 và Người 4 duyệt chưa (spec §7.3)?
- Có code nào nằm sai module không, ví dụ app học viên giữ đáp án hay tự quyết "đã qua" (vi phạm spec §1 R3)?
- Bài mới trong `content/` đã qua kiểm nội dung trên CI chưa, người soạn đã tự chạy lại chưa?
- Có khoá, mật khẩu hay file `.db` lọt vào không? **Repo công khai**, lọt là lộ vĩnh viễn trong lịch sử git.

## Ranh giới: chỉ sửa phần của mình

Mỗi package có đúng một chủ, ghi trong `package-info.java` và bảng ở [`docs/PHAN-CONG.md`](docs/PHAN-CONG.md).

- Chỉ sửa file trong package của mình. Cần đổi gì ở phần người khác thì nhắn chủ, hoặc mở PR để chính chủ duyệt.
- Gọi phần của người khác qua các hàm đã chốt ở mục "Chỗ giao nhau" trong `PHAN-CONG.md`, không sửa thẳng vào code của họ.
- File dùng chung (`MessageType`, `ErrorCode`, `pom.xml`, `.github/`, `deploy/`, `README.md`…) thuộc leader, sửa qua PR có leader duyệt.
- Mỗi ngày chạy `git pull --rebase origin main` trên nhánh của mình để bắt kịp phần của người khác sớm, đừng để dồn tới cuối.

## Không được làm

- Commit khoá riêng, keystore (`.p12`, `.jks`), mật khẩu, file `.db`. `.gitignore` đã chặn các đuôi phổ biến, nhưng vẫn phải tự để ý.
- Thêm thư viện hay framework cho phần mạng. Đề bài chấm network programming tự viết (spec §1 R1). Thư viện cho phần khác đã khai báo sẵn phiên bản trong `source/pom.xml`; muốn thêm thư viện mới thì bàn với cả nhóm trước.
- Merge PR của chính mình mà chưa ai duyệt. Luật bảo vệ nhánh chặn việc này, kể cả với chủ repo.

---

## Thiết lập GitHub lần đầu (chủ repo làm một lần)

1. Repo đã có: `github.com/lyhaiquan/ExamNet`, **Public**. Đổi tên thành `LabCast` ở **Settings → General → Repository name** (GitHub tự chuyển hướng link cũ), rồi trên máy mỗi người:
   ```bash
   git remote set-url origin https://github.com/lyhaiquan/LabCast.git
   ```
2. **Settings → General → Pull Requests:**
   - Chỉ tick **Allow squash merging**, bỏ tick merge commit và rebase merge.
   - *Default commit message* chọn **Pull request title**.
   - Tick **Automatically delete head branches**.
3. **Settings → Collaborators** → mời 3 bạn với quyền **Write**.
4. **Settings → Rules → Rulesets → New branch ruleset:**
   - *Ruleset name* `main`, *Enforcement status* **Active**, *Target branches* → **Include default branch**.
   - Tick **Restrict deletions** và **Block force pushes**.
   - Tick **Require a pull request before merging** → *Required approvals* **1**, tick *Dismiss stale pull request approvals when new commits are pushed*.
   - Tick **Require status checks to pass** → thêm `build` và `pr-title`. Hai tên này chỉ hiện ra sau khi đã có ít nhất một PR chạy CI, nên làm bước này sau PR đầu tiên.
   - Để trống *Bypass list* — chủ repo cũng phải qua PR như mọi người.
5. Làm theo [`docs/deploy/VPS-SETUP.md`](docs/deploy/VPS-SETUP.md) để bật deploy tự động.
