# Giai đoạn 0 — Đề xuất và chuẩn bị (trước T0 → tuần 1)

**Mục tiêu:** thầy duyệt đề tài; thử trước ba rủi ro lớn của phòng máy (multicast, Docker, đóng gói JavaFX); repo sẵn sàng để cả nhóm bắt đầu giai đoạn 1.

**Đã làm sẵn ngày 07/10/2026:** khung Maven `labcast.*`, 46 `package-info.java`, `schema.sql` 15 bảng, `runner/`, `content/`, `packaging/`, tài liệu, CI có pytest và build image runner, deploy đổi sang `labcast-server`. Phần dưới là việc **còn lại**.

Quy ước chung: xem [README.md](README.md).

---

## Task 0.1: Gửi đề xuất cho thầy (Người 2)

- [ ] Chép bảng spec §0 (9 mục theo `Topics.md` §7) và sơ đồ §5.1 vào email hoặc tin nhắn cho thầy.
- [ ] Hỏi kèm ba câu, câu trả lời dùng để chốt giả định A1, A2, A3 ở spec §18:
  1. Đề cương hai môn DSA và SQL (danh sách chương, bài).
  2. Môn SQL dạy trên hệ quản trị nào (SQL Server, MySQL, PostgreSQL)?
  3. Hạn nộp và ngày bảo vệ.
- [ ] Thầy trả lời: ghi lại vào `docs/specs/labcast-design.md` §18 (đổi giả định thành quyết định), sửa §7.7 theo đề cương.

**Commit (sau khi có trả lời):** `docs(docs): chốt đề cương và hệ quản trị SQL theo phản hồi của thầy`

---

## Task 0.2: Thử multicast trong phòng máy thật (Người 4)

Code nháp, **không** đưa vào repo. Mục đích: biết sớm phòng máy có chặn multicast không.

**File nháp** `MulticastThu.java` (chạy thẳng bằng `java MulticastThu.java`, không cần Maven):

```java
import java.net.*;
import java.nio.charset.StandardCharsets;

public class MulticastThu {
    public static void main(String[] a) throws Exception {
        InetAddress group = InetAddress.getByName("239.255.70.1");
        int port = 7001;
        NetworkInterface nic = NetworkInterface.getByName(a[1]);       // tên card, xem bằng: java MulticastThu.java cards x
        if (a[0].equals("cards")) {
            for (var n : java.util.Collections.list(NetworkInterface.getNetworkInterfaces()))
                if (n.isUp()) System.out.println(n.getName() + " | " + n.getDisplayName() + " | " + n.inetAddresses().toList());
            return;
        }
        try (var s = new MulticastSocket(port)) {
            s.setNetworkInterface(nic);
            s.setTimeToLive(1);
            if (a[0].equals("gui")) {
                for (int i = 0; i < 100; i++) {
                    byte[] b = ("goi " + i).getBytes(StandardCharsets.UTF_8);
                    s.send(new DatagramPacket(b, b.length, group, port));
                    Thread.sleep(50);
                }
            } else {
                s.joinGroup(new InetSocketAddress(group, port), nic);
                s.setSoTimeout(10_000);
                byte[] buf = new byte[1500];
                int n = 0;
                while (true) {
                    var p = new DatagramPacket(buf, buf.length);
                    s.receive(p);
                    System.out.println(++n + ": " + new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8));
                }
            }
        }
    }
}
```

**Các bước:**

- [ ] Trên hai máy phòng lab cắm dây: `java MulticastThu.java cards x` để xem tên card LAN thật.
- [ ] Máy nhận: `java MulticastThu.java nhan <tên card>`. Máy gửi: `java MulticastThu.java gui <tên card>`.
- [ ] Lặp lại khi máy **gửi** có cài Docker Desktop (có card vEthernet), lần lượt chọn card LAN và card vEthernet, để thấy chọn sai card thì không ai nhận được.
- [ ] Ghi kết quả vào `docs/thu-truoc/multicast.md`: nhận được bao nhiêu gói trên 100; Windows có hiện hộp thoại tường lửa không; học viên có quyền bấm "Allow" không.

**Xong khi:** biết phòng máy cho multicast đi qua hay không. Không qua thì báo cả nhóm: demo sẽ mang router riêng, và nhánh chuyển sang TCP của ĐG1 phải làm sớm hơn.

**Commit:** `docs(docs): ghi kết quả thử multicast trong phòng máy`

---

## Task 0.3: Thử Docker sandbox (Người 3)

- [ ] Cài Docker Desktop (Windows cần WSL2). `docker info` chạy được.
- [ ] Đóng image runner: `docker build -t labcast-runner:py3.12 runner`.
- [ ] Chạy thử đủ cờ ở spec §8.1 với một đoạn Python:

```bash
echo 'print(sum(range(10)))' > /tmp/thu.py
docker run --rm -i --network none --memory 256m --memory-swap 256m --cpus 1 \
  --pids-limit 64 --read-only --tmpfs /tmp:size=16m --user 65534:65534 \
  --cap-drop ALL --security-opt no-new-privileges \
  --entrypoint python labcast-runner:py3.12 - < /tmp/thu.py
```

- [ ] Đo thời gian khởi động một container: chạy lệnh trên 10 lần, ghi thời gian trung bình (Git Bash: `time`, PowerShell: `Measure-Command`).
- [ ] Thử ba đoạn "phá": mở kết nối mạng (`socket.create_connection(("1.1.1.1", 80), 2)`), ghi file vào `/` (`open("/x","w")`), vòng lặp vô hạn — ghi lại cái nào bị chặn và báo lỗi gì.
- [ ] Ghi vào `docs/thu-truoc/docker.md`.

**Xong khi:** biết một lượt chạy tốn bao lâu. Nếu > 2 s thì cân nhắc làm sớm "container khởi động sẵn" (thí nghiệm 7).

**Commit:** `docs(docs): ghi kết quả thử Docker sandbox`

---

## Task 0.4: JavaFX trong app và lớp `Launcher` (Người 2)

**Files:**
- Modify: `source/client/pom.xml`
- Create: `source/client/src/main/java/labcast/client/app/Launcher.java`
- Create: `source/client/src/main/java/labcast/client/app/LabCastApp.java`

**`source/client/pom.xml`** — thêm hai dependency (không ghi version, lấy từ pom cha) và plugin đóng jar:

```xml
<dependency>
  <groupId>org.openjfx</groupId>
  <artifactId>javafx-controls</artifactId>
</dependency>
<dependency>
  <groupId>org.fxmisc.richtext</groupId>
  <artifactId>richtextfx</artifactId>
</dependency>
```

```xml
<build>
  <finalName>labcast-client</finalName>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-shade-plugin</artifactId>
      <executions>
        <execution>
          <phase>package</phase>
          <goals><goal>shade</goal></goals>
          <configuration>
            <createDependencyReducedPom>false</createDependencyReducedPom>
            <filters>
              <filter>
                <artifact>*:*</artifact>
                <excludes>
                  <exclude>META-INF/MANIFEST.MF</exclude>
                  <exclude>META-INF/*.SF</exclude>
                  <exclude>META-INF/*.DSA</exclude>
                  <exclude>META-INF/*.RSA</exclude>
                  <exclude>module-info.class</exclude>
                </excludes>
              </filter>
            </filters>
            <transformers>
              <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                <mainClass>labcast.client.app.Launcher</mainClass>
                <manifestEntries>
                  <Implementation-Version>${project.version}</Implementation-Version>
                </manifestEntries>
              </transformer>
            </transformers>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

**Vì sao cần `Launcher`:** jar gộp có lớp chính kế thừa `javafx.application.Application` thì Java báo "JavaFX runtime components are missing". Lớp chính phải là một lớp thường gọi sang:

```java
package labcast.client.app;

/** Lớp chính của jar. KHÔNG kế thừa Application — xem docs/plans/giai-doan-0.md task 0.4. */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        LabCastApp.main(args);
    }
}
```

```java
package labcast.client.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public final class LabCastApp extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("LabCast");
        stage.setScene(new Scene(new Label("LabCast — giai đoạn 0"), 480, 240));
        stage.show();
    }
}
```

**Kiểm:**

```bash
cd source && ./mvnw -q package -DskipTests
java -jar client/target/labcast-client.jar        # hiện cửa sổ "LabCast — giai đoạn 0"
./mvnw verify                                     # CI trên Ubuntu không có màn hình vẫn phải xanh: không test nào mở cửa sổ
```

Jar gộp chỉ chứa thư viện JavaFX của **hệ điều hành đang build**. Build trên Windows thì chạy trên Windows; bộ cài cho phòng máy do CI build trên Windows (task 0.5).

**Commit:** `build(client): thêm JavaFX, RichTextFX và lớp Launcher cho jar chạy được`

---

## Task 0.5: Workflow phát hành bộ cài (Người 2)

**Files:**
- Create: `.github/workflows/release.yml`

```yaml
# Gắn tag v* (ví dụ v0.1.0) → build trên Windows → đóng bộ cài .msi và bản chạy thẳng
# → đưa lên GitHub Releases. Máy phòng lab không cần cài Java: bộ cài kèm sẵn.
name: Release

on:
  push:
    tags: ["v*"]

permissions:
  contents: write

jobs:
  windows:
    runs-on: windows-2022          # có sẵn WiX 3, thứ jpackage của JDK 17 cần để tạo .msi
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
          cache: maven

      - name: Build jar
        working-directory: source
        run: ./mvnw.cmd -B -ntp package -DskipTests

      - name: Đóng bộ cài
        shell: bash
        run: |
          ver="${GITHUB_REF_NAME#v}"
          mkdir -p packaging/out/input
          cp source/client/target/labcast-client.jar packaging/out/input/
          jpackage --type app-image --name LabCast --app-version "$ver" \
            --input packaging/out/input --main-jar labcast-client.jar \
            --main-class labcast.client.app.Launcher --dest packaging/out
          jpackage --type msi --name LabCast --app-version "$ver" \
            --app-image packaging/out/LabCast --dest packaging/out \
            --win-menu --win-shortcut --win-dir-chooser
          (cd packaging/out && 7z a "LabCast-$ver-chay-thang.zip" LabCast)

      - name: Đưa lên GitHub Releases
        shell: bash
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          gh release create "$GITHUB_REF_NAME" packaging/out/*.msi packaging/out/*.zip \
            --title "LabCast $GITHUB_REF_NAME" --generate-notes
```

**Kiểm:**

- [ ] `actionlint` không cảnh báo (chạy bằng Docker: `docker run --rm -v "$PWD:/repo" -w /repo rhysd/actionlint:latest`).
- [ ] Gắn tag thử trên một nhánh riêng: `git tag v0.0.1-thu && git push origin v0.0.1-thu`. Workflow xanh, trang Releases có `.msi` và `.zip`.
- [ ] Cài `.msi` trên một máy **không có Java**: app mở được.
- [ ] Xoá release và tag thử sau khi kiểm xong.
- [ ] Nếu bước `jpackage --type msi` báo thiếu WiX: thêm bước `choco install wixtoolset --version 3.14.1 -y` trước bước đóng bộ cài.

**Commit:** `ci(packaging): đóng bộ cài msi và bản chạy thẳng khi gắn tag`

---

## Task 0.6: Thư mục mẫu để soạn bài (Người 3)

**Files:**
- Create: `content/_mau/lesson.yaml`, `de.md`, `khung.py`, `loi_giai.py`, `sinh_test.py`, `demo.in`, `tests/.gitkeep`

`_mau` là bài **mẫu để chép** khi soạn bài mới; `LessonRepository` (giai đoạn 1) bỏ qua thư mục bắt đầu bằng `_`. `lesson.yaml` chép đúng ví dụ ở spec §7.1, mỗi trường kèm một dòng chú thích giải thích. `sinh_test.py` có sẵn khung:

```python
"""Sinh test cho bài. Gọi: python sinh_test.py <cỡ: nho|tb|lon> <seed>
In ra input của một test. Output đúng do lời giải mẫu tính, không viết tay."""
import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
random.seed(seed)
n = {"nho": random.randint(1, 8), "tb": random.randint(20, 50), "lon": random.randint(5_000, 20_000)}[co]
print(*[random.randint(-1000, 1000) for _ in range(n)])
```

Kiểm định dạng `lesson.yaml` trên CI làm ở giai đoạn 3 (task "kiểm nội dung"), dùng lại `LessonRepository` của giai đoạn 1.

**Commit:** `feat(content): thêm thư mục mẫu để soạn bài mới`

---

## Task 0.7: GitHub — đổi tên, thành viên, luật bảo vệ (Người 2)

- [ ] **Settings → General → Repository name** đổi `ExamNet` → `LabCast` (remote trên máy leader đã trỏ sẵn `LabCast.git`).
- [ ] Xoá nhánh cũ `docs/docs-phan-cong-theo-tinh-nang`.
- [ ] **Settings → General → Pull Requests:** chỉ tick *Allow squash merging*; *Default commit message* = *Pull request title*; tick *Automatically delete head branches*.
- [ ] **Settings → Collaborators:** mời 3 bạn, quyền *Write*. Mỗi bạn chạy `git clone https://github.com/lyhaiquan/LabCast.git` rồi `npm install`.
- [ ] Tạo `.github/CODEOWNERS` (điền username GitHub thật của từng người):

```text
# Mỗi package một chủ — spec §17.1. PR chạm vào đây tự động nhờ chủ duyệt.
*                                           @lyhaiquan
/source/server/src/main/java/labcast/server/journal/   @<nguoi-1>
/source/server/src/main/java/labcast/server/mirror/    @<nguoi-1>
/source/client/src/main/java/labcast/client/editor/    @<nguoi-1>
/source/client/src/main/java/labcast/client/journal/   @<nguoi-1>
/source/client/src/main/java/labcast/client/mirror/    @<nguoi-1>
/source/client/src/main/java/labcast/client/views/cay/ @<nguoi-1>
/source/client/src/main/java/labcast/client/views/caygoi/ @<nguoi-1>
/source/client/src/main/java/labcast/client/views/dothi/  @<nguoi-1>
/runner/                                    @<nguoi-3>
/content/                                   @<nguoi-3>
/source/server/src/main/java/labcast/server/sync/      @<nguoi-3>
/source/server/src/main/java/labcast/server/lesson/    @<nguoi-3>
/source/server/src/main/java/labcast/server/practice/  @<nguoi-3>
/source/server/src/main/java/labcast/server/run/       @<nguoi-3>
/source/server/src/main/java/labcast/server/grade/     @<nguoi-3>
/source/server/src/main/java/labcast/server/sqlviz/    @<nguoi-3>
/source/client/src/main/java/labcast/client/clock/     @<nguoi-3>
/source/client/src/main/java/labcast/client/practice/  @<nguoi-3>
/source/client/src/main/java/labcast/client/views/luoi/   @<nguoi-3>
/source/client/src/main/java/labcast/client/views/bangsql/ @<nguoi-3>
/source/common/src/main/java/labcast/common/trace/     @<nguoi-4> @<nguoi-3>
/source/server/src/main/java/labcast/server/cast/      @<nguoi-4>
/source/client/src/main/java/labcast/client/cast/      @<nguoi-4>
/source/client/src/main/java/labcast/client/player/    @<nguoi-4>
/source/client/src/main/java/labcast/client/presenter/ @<nguoi-4>
/source/client/src/main/java/labcast/client/views/mang/     @<nguoi-4>
/source/client/src/main/java/labcast/client/views/nganxep/  @<nguoi-4>
/source/client/src/main/java/labcast/client/views/dslk/     @<nguoi-4>
/source/client/src/main/java/labcast/client/views/bam/      @<nguoi-4>
/source/client/src/main/java/labcast/client/views/codebien/ @<nguoi-4>
/source/bench/                              @<nguoi-4>
```

- [ ] **Settings → Rules → Rulesets:** làm đúng `CONTRIBUTING.md` mục "Thiết lập GitHub lần đầu", bước 4. Thêm *Require review from Code Owners*.

**Commit:** `chore(repo): thêm CODEOWNERS theo bảng phân công`

---

## Task 0.8: Bật deploy lên VPS (Người 2)

Làm theo `docs/deploy/VPS-SETUP.md`. Riêng với VPS hiện có (137.184.78.166, đã cài Docker và user `deploy`):

- [ ] **Settings → Secrets and variables → Actions → Variables:** `VPS_HOST = 137.184.78.166`, `VPS_USER = deploy`.
- [ ] **Environments → production:** secret `VPS_SSH_KEY` (khoá riêng `examnet-deploy` đã tạo, dùng tiếp), `VPS_KNOWN_HOSTS` (dòng `ssh-keyscan` đã đối chiếu vân tay).
- [ ] **Packages → `labcast-server` → Change visibility → Public.**
- [ ] DigitalOcean → Networking → Firewalls: mở TCP 7000 và 8080 (bỏ 5000, 5001 cũ).
- [ ] **Actions → Deploy → Run workflow.** Mở `http://137.184.78.166:8080/version`: `service` là `labcast-server`, `commit` trùng commit mới nhất trên `main`.

---

## Nghiệm thu M0

- [ ] Thầy đã duyệt đề tài; spec §18 cập nhật theo câu trả lời.
- [ ] `docs/thu-truoc/multicast.md` và `docs/thu-truoc/docker.md` có kết quả thật.
- [ ] `./mvnw verify` và `pytest` xanh trên CI.
- [ ] Bộ cài từ GitHub Releases mở được app trên máy không có Java.
- [ ] Repo tên `LabCast`, có ruleset và `CODEOWNERS`; 3 bạn clone và chạy `./mvnw verify` thành công trên máy mình.
- [ ] `http://137.184.78.166:8080/version` trả đúng commit mới nhất.
