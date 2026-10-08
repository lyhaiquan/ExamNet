# Giai đoạn 1 — Xương sống (tuần 2–3)

**Mục tiêu:** một học viên tự tìm thấy server, đăng nhập, mở bài nổi bọt, gõ code, bấm chạy thử và thấy **chính code mình vừa gõ** chạy thành animation — mọi thứ đi qua LCP/1.0.

**Kiến trúc của giai đoạn:** leader dựng tầng mạng chung (frame, kết nối, router, phiên) và cách cắm module vào server. Ba người còn lại làm phần không cần mạng trước (trình phát, runner, ô soạn code), rồi nối vào router khi PR khung của leader đã merge.

**Công nghệ:** Java 17 (`java.net`, `java.util.concurrent`), JavaFX 21, RichTextFX, Jackson, SnakeYAML, sqlite-jdbc, Python 3.12, Docker.

Quy ước test, lệnh chạy, bảng mã thông điệp và ký hiệu payload: xem [README.md](README.md).

---

## Thứ tự và phụ thuộc

```text
Ngày 1–2   Người 2: task 2.1 → 2.4 (frame, mã, payload, router + module)  ──► PR "khung" merge sớm nhất có thể
           Người 4: task 4.1 → 4.2 (trace, trình phát với view giả)       ┐
           Người 3: task 3.1 → 3.2 (runner Python chạy độc lập)            ├ không cần mạng, làm song song
           Người 1: task 1.1 → 1.2 (TextOps, gộp phím gõ)                  ┘
Sau khi PR khung merge: mọi người nối handler của mình vào router (các task còn lại)
Cuối tuần 3: task 2.10 lắp màn bài học → nghiệm thu M1
```

---

## Người 2 (leader) — tầng mạng chung

### Task 2.1: `Frame` và `FrameCodec`

**Files:**
- Create: `source/common/src/main/java/labcast/common/protocol/Frame.java`
- Create: `source/common/src/main/java/labcast/common/protocol/FrameCodec.java`
- Create: `source/common/src/main/java/labcast/common/protocol/ProtocolException.java`
- Test: `source/common/src/test/java/labcast/common/protocol/FrameCodecTest.java`

**Hợp đồng:**

```java
public record Frame(MessageType type, byte flags, int seq, byte[] payload) {
    public static final byte MAGIC_0 = 'L', MAGIC_1 = 'C', VERSION = 1;
    public static final int HEADER_SIZE = 13;
    public static final int MAX_PAYLOAD = 8 * 1024 * 1024;
    public static final byte FLAG_COMPRESSED = 1, FLAG_JSON = 2, FLAG_REQUIRES_ACK = 4, FLAG_VIA_TCP = 8;

    public static Frame of(MessageType type, byte[] payload);      // flags 0, seq 0 (codec gán seq khi gửi)
    public boolean has(byte flag);
    public PayloadReader reader();
    public Frame withSeq(int seq);
}

public final class FrameCodec {
    public static void write(OutputStream out, Frame f) throws IOException;      // không tự flush
    public static Frame read(InputStream in) throws IOException;                  // null nếu đóng sạch ở ranh giới frame
    public static byte[] toBytes(Frame f);                                         // cho UDP
    public static Frame fromBytes(byte[] buf, int off, int len) throws ProtocolException;
}

public final class ProtocolException extends IOException {
    public ProtocolException(ErrorCode code, String message);
    public ErrorCode code();
}
```

**Test — viết trước:**

```java
class FrameCodecTest {

    private static Frame mau() {
        return new Frame(MessageType.HELLO, Frame.FLAG_JSON, 42, "xin chào".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void ghiRoiDoc_raDungFrame() throws Exception {
        var out = new ByteArrayOutputStream();
        FrameCodec.write(out, mau());
        Frame f = FrameCodec.read(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(MessageType.HELLO, f.type());
        assertEquals(42, f.seq());
        assertTrue(f.has(Frame.FLAG_JSON));
        assertArrayEquals(mau().payload(), f.payload());
    }

    /** TCP được phép trả về từng byte một — codec phải đọc lặp cho đủ. */
    @Test
    void docTungByteMot_vanRaDungFrame() throws Exception {
        byte[] bytes = FrameCodec.toBytes(mau());
        InputStream nhoGiot = new InputStream() {
            int i = 0;
            @Override public int read() { return i < bytes.length ? bytes[i++] & 0xFF : -1; }
            @Override public int read(byte[] b, int off, int len) {
                if (i >= bytes.length) return -1;
                b[off] = bytes[i++];
                return 1;
            }
        };
        assertArrayEquals(mau().payload(), FrameCodec.read(nhoGiot).payload());
    }

    @Test
    void haiFrameDinhLien_docDuocCaHai() throws Exception {
        var out = new ByteArrayOutputStream();
        FrameCodec.write(out, mau());
        FrameCodec.write(out, Frame.of(MessageType.BYE, new byte[0]));
        var in = new ByteArrayInputStream(out.toByteArray());
        assertEquals(MessageType.HELLO, FrameCodec.read(in).type());
        assertEquals(MessageType.BYE, FrameCodec.read(in).type());
        assertNull(FrameCodec.read(in));
    }

    @Test
    void saiMagic_nemProtocolException() {
        byte[] bytes = FrameCodec.toBytes(mau());
        bytes[0] = 'G'; // ví dụ ai đó gửi "GET / HTTP/1.1"
        var ex = assertThrows(ProtocolException.class, () -> FrameCodec.read(new ByteArrayInputStream(bytes)));
        assertEquals(ErrorCode.BAD_FRAME, ex.code());
    }

    /** LEN khổng lồ phải bị từ chối TRƯỚC khi cấp phát mảng. */
    @Test
    void lenVuotTran_nemTruocKhiCapPhat() {
        byte[] header = FrameCodec.toBytes(Frame.of(MessageType.HELLO, new byte[0]));
        ByteBuffer.wrap(header).putInt(9, Integer.MAX_VALUE);
        assertThrows(ProtocolException.class, () -> FrameCodec.read(new ByteArrayInputStream(header)));
    }

    @Test
    void dutGiuaFrame_nemEOFException() {
        byte[] bytes = FrameCodec.toBytes(mau());
        byte[] cut = Arrays.copyOf(bytes, bytes.length - 3);
        assertThrows(EOFException.class, () -> FrameCodec.read(new ByteArrayInputStream(cut)));
    }

    @Test
    void fromBytes_choDatagramUdp() throws Exception {
        byte[] bytes = FrameCodec.toBytes(mau());
        assertEquals(42, FrameCodec.fromBytes(bytes, 0, bytes.length).seq());
        assertThrows(ProtocolException.class, () -> FrameCodec.fromBytes(bytes, 0, 5));
    }
}
```

**Chỗ khó — đọc frame** (tham khảo bản cũ `FrameCodec`):

```java
public static Frame read(InputStream in) throws IOException {
    byte[] h = new byte[Frame.HEADER_SIZE];
    int first = in.read();
    if (first == -1) return null;                    // đóng sạch ở ranh giới frame
    h[0] = (byte) first;
    readFully(in, h, 1, h.length - 1);
    if (h[0] != Frame.MAGIC_0 || h[1] != Frame.MAGIC_1) throw new ProtocolException(ErrorCode.BAD_FRAME, "Sai MAGIC");
    if (h[2] != Frame.VERSION) throw new ProtocolException(ErrorCode.BAD_FRAME, "Sai VER " + h[2]);
    MessageType type = MessageType.fromCode(h[3] & 0xFF);  // mã lạ → ProtocolException
    int seq = ByteBuffer.wrap(h, 5, 4).getInt();
    int len = ByteBuffer.wrap(h, 9, 4).getInt();
    if (len < 0 || len > Frame.MAX_PAYLOAD)          // KIỂM TRƯỚC khi new byte[len]
        throw new ProtocolException(ErrorCode.BAD_FRAME, "LEN không hợp lệ: " + len);
    byte[] payload = new byte[len];
    readFully(in, payload, 0, len);
    return new Frame(type, h[4], seq, payload);
}

private static void readFully(InputStream in, byte[] b, int off, int len) throws IOException {
    while (len > 0) {
        int n = in.read(b, off, len);
        if (n < 0) throw new EOFException("Kết nối đóng giữa frame");
        off += n;
        len -= n;
    }
}
```

**Commit:** `feat(common): thêm Frame và FrameCodec đọc lặp cho đủ LEN`

---

### Task 2.2: `MessageType`, `ErrorCode`, `Role`

**Files:**
- Create: `source/common/src/main/java/labcast/common/protocol/MessageType.java`
- Create: `source/common/src/main/java/labcast/common/protocol/ErrorCode.java`
- Create: `source/common/src/main/java/labcast/common/protocol/Role.java`
- Test: `source/common/src/test/java/labcast/common/protocol/MessageTypeTest.java`

**Hợp đồng:**

```java
public enum MessageType {
    HELLO(0x01), HELLO_ACK(0x02), /* … đủ mọi mã ở bảng trong README.md … */ CAST_REPAIR(0x74);
    public int code();
    public static MessageType fromCode(int code) throws ProtocolException; // mã lạ → BAD_FRAME
}

public enum ErrorCode {
    BAD_FRAME(1), AUTH_REQUIRED(2), FORBIDDEN(3), LOCKED(4), COOLDOWN(5), RATE_LIMITED(6),
    NONCE_INVALID(7), SQL_NOT_ALLOWED(8), SANDBOX_DOWN(9), INTERNAL(99);
    public int code();
    public static ErrorCode fromCode(int code);   // mã lạ → INTERNAL
}

public enum Role { TEACHER, STUDENT }
```

**Test:**

```java
@Test
void maKhongTrungNhau() {
    var seen = new HashSet<Integer>();
    for (MessageType t : MessageType.values()) assertTrue(seen.add(t.code()), "Trùng mã: " + t);
}

@Test
void fromCode_khuHoiDuocMoiLoai() throws Exception {
    for (MessageType t : MessageType.values()) assertEquals(t, MessageType.fromCode(t.code()));
}

@Test
void maLa_nemProtocolException() {
    assertThrows(ProtocolException.class, () -> MessageType.fromCode(0xFE));
}
```

Điền **đủ** mọi mã ngay bây giờ, kể cả của giai đoạn sau, để ba người kia không phải sửa file chung.

**Commit:** `feat(common): thêm bảng mã thông điệp và mã lỗi LCP/1.0`

---

### Task 2.3: `PayloadWriter`, `PayloadReader`, `JsonPayload`

**Files:**
- Create: `source/common/src/main/java/labcast/common/protocol/PayloadWriter.java`
- Create: `source/common/src/main/java/labcast/common/protocol/PayloadReader.java`
- Create: `source/common/src/main/java/labcast/common/protocol/JsonPayload.java`
- Modify: `source/common/pom.xml` — thêm `jackson-databind` (không ghi version)
- Test: `source/common/src/test/java/labcast/common/protocol/PayloadTest.java`

**Hợp đồng:**

```java
public final class PayloadWriter {
    public PayloadWriter u8(int v); u16(int v); i32(int v); i64(long v); bool(boolean v);
    public PayloadWriter str(String s);      // [u16 độ dài][UTF-8], > 65 535 byte → IllegalArgumentException
    public PayloadWriter blob(byte[] b);     // [i32 độ dài][byte]
    public byte[] toBytes();
}

public final class PayloadReader {
    public PayloadReader(byte[] payload);
    public int u8(); int u16(); int i32(); long i64(); boolean bool(); String str(); byte[] blob();
    // mọi hàm ném ProtocolException(BAD_FRAME) khi thiếu byte — không bao giờ ArrayIndexOutOfBounds
    public boolean hasRemaining();
}

public final class JsonPayload {
    public static Frame frame(MessageType type, Object value);           // đặt FLAG_JSON
    public static <T> T read(Frame f, Class<T> type) throws ProtocolException;
    public static JsonNode tree(Frame f) throws ProtocolException;
}
```

**Test:**

```java
@Test
void ghiRoiDoc_dungThuTuVaGiaTri() throws Exception {
    byte[] b = new PayloadWriter().u8(200).u16(65_000).i32(-5).i64(1L << 40).bool(true).str("Lê Hải")
            .blob(new byte[] {1, 2}).toBytes();
    var r = new PayloadReader(b);
    assertEquals(200, r.u8());
    assertEquals(65_000, r.u16());
    assertEquals(-5, r.i32());
    assertEquals(1L << 40, r.i64());
    assertTrue(r.bool());
    assertEquals("Lê Hải", r.str());
    assertArrayEquals(new byte[] {1, 2}, r.blob());
    assertFalse(r.hasRemaining());
}

@Test
void thieuByte_nemProtocolException() {
    var r = new PayloadReader(new byte[] {0, 5, 'a'}); // khai báo chuỗi 5 byte nhưng chỉ có 1
    assertThrows(ProtocolException.class, r::str);
}

@Test
void json_khuHoi() throws Exception {
    record Bai(String id, int so) {}
    Frame f = JsonPayload.frame(MessageType.LESSON_DATA, new Bai("dsa.x", 3));
    assertTrue(f.has(Frame.FLAG_JSON));
    assertEquals(new Bai("dsa.x", 3), JsonPayload.read(f, Bai.class));
}
```

**Commit:** `feat(common): thêm PayloadWriter, PayloadReader và JsonPayload`

---

### Task 2.4: Hàng đợi ghi có ưu tiên, `Connection`, `Router`, cách cắm module

Đây là PR **"khung"** — ba người còn lại chờ nó để nối handler. Mở PR ngay khi 2.1–2.4 xong.

**Files:**
- Create: `source/server/src/main/java/labcast/server/net/Priority.java`
- Create: `source/server/src/main/java/labcast/server/net/OutboundQueue.java`
- Create: `source/server/src/main/java/labcast/server/net/Connection.java`
- Create: `source/server/src/main/java/labcast/server/net/TcpServer.java`
- Create: `source/server/src/main/java/labcast/server/net/Router.java`
- Create: `source/server/src/main/java/labcast/server/net/Ctx.java`
- Create: `source/server/src/main/java/labcast/server/net/FramePolicy.java`
- Create: `source/server/src/main/java/labcast/server/ServerModule.java`
- Create: `source/server/src/main/java/labcast/server/ServerContext.java`
- Create: `source/server/src/main/java/labcast/server/LabCastServer.java`
- Create: `source/common/src/main/java/labcast/common/transport/SocketOptions.java`
- Test: `OutboundQueueTest`, `RouterTest`, `TcpServerTest` trong `source/server/src/test/java/labcast/server/net/`

**Hợp đồng:**

```java
public enum Priority { CRITICAL, NORMAL, CONFLATABLE, DROPPABLE }   // spec §11

public final class OutboundQueue {
    public OutboundQueue(int capacity);                               // 256
    /** CRITICAL: chen lên đầu (sau các CRITICAL khác), không bao giờ bỏ, được vượt capacity.
     *  NORMAL: đầy thì chờ tối đa normalWait, hết giờ trả false.
     *  CONFLATABLE: đang có frame cùng key chờ gửi → thay tại chỗ; không có → như NORMAL.
     *  DROPPABLE: đầy thì bỏ DROPPABLE cũ nhất rồi thêm vào. */
    public boolean offer(Frame f, Priority p, String key, Duration normalWait) throws InterruptedException;
    public Frame take() throws InterruptedException;                  // luồng ghi gọi
    public int pending(String key);                                   // cho mirror ở giai đoạn 3
    public int dropPending(String key);
    public int size();
}

public final class Connection implements AutoCloseable {
    public long id();
    public String remoteAddress();
    public boolean send(Frame f, Priority p);                         // gán SEQ tăng dần rồi đưa vào hàng đợi
    public boolean send(Frame f, Priority p, String conflateKey);
    public OutboundQueue queue();
    public void close();                                              // đóng socket, dừng hai luồng
}

@FunctionalInterface
public interface Handler { void handle(Ctx ctx, Frame f) throws Exception; }

public final class Router {
    /** Không truyền role = chỉ cần đã đăng nhập. HELLO, AUTH, RESUME_REQ, TIME_SYNC_REQ,
     *  HEARTBEAT_ACK, BYE được phép trước khi đăng nhập. */
    public Router on(MessageType t, Handler h);
    public Router on(MessageType t, Set<Role> roles, Handler h);
    public void setPolicy(FramePolicy p);                             // mặc định FramePolicy.ALLOW_ALL
    public void route(Ctx ctx, Frame f);                              // mọi ngoại lệ → ERROR(INTERNAL), không làm chết kết nối
}

public interface FramePolicy {
    record Decision(boolean allowed, ErrorCode code, int retryAfterMs) {
        public static final Decision ALLOW = new Decision(true, null, 0);
    }
    Decision check(Session s, MessageType t);                          // s có thể null trước khi đăng nhập
    FramePolicy ALLOW_ALL = (s, t) -> Decision.ALLOW;
}

public final class Ctx {
    public Connection connection();
    public Session session();                                         // null trước khi đăng nhập
    public void bindSession(Session s);
    public long receivedAt();                                         // thời điểm luồng đọc nhận frame (ms)
    public void reply(MessageType t, byte[] payload);                 // NORMAL
    public void reply(Frame f, Priority p);
    public void error(ErrorCode c, String message, int retryAfterMs);
}

public interface ServerModule { void install(ServerContext ctx); }

public record ServerContext(Router router, SessionManager sessions, Database db, Presence presence,
                            ScheduledExecutorService scheduler, ServerConfig config) {}
```

**Cách cắm module:** mỗi người viết một lớp `XxxModule implements ServerModule` trong package của mình, đăng ký handler trong `install`, rồi thêm **một dòng** vào danh sách của `LabCastServer`:

```java
private static final List<ServerModule> MODULES = List.of(
        new AccountModule(),      // Người 2
        new SyncModule(),         // Người 3
        new LessonModule(),       // Người 3
        new RunModule(),          // Người 3
        new JournalModule()       // Người 1
        // thêm module mới ở đây, mỗi module một dòng
);
```

**Luồng của một kết nối:**

```text
luồng đọc:  FrameCodec.read ─► ghi receivedAt ─► FramePolicy.check ─► pool nghiệp vụ ─► Router.route
                                                   │ từ chối → ERROR(RATE_LIMITED…) ngay, không vào pool
luồng ghi:  OutboundQueue.take ─► FrameCodec.write ─► flush khi hàng đợi rỗng
```

- Chỉ **luồng ghi** được gọi `write` trên socket. Không giữ khoá nào khi ghi.
- `ProtocolException` ở luồng đọc → gửi `ERROR(BAD_FRAME)` (CRITICAL), ghi `audit_log`, đóng kết nối.
- `SocketOptions.applyTcp(socket)`: `TCP_NODELAY=true`, `SO_KEEPALIVE=true`, `SO_TIMEOUT=15000`.

**Test:**

```java
class OutboundQueueTest {
    private final Duration w = Duration.ofMillis(50);
    private Frame f(MessageType t) { return Frame.of(t, new byte[0]); }

    @Test
    void critical_chenLenDauHang() throws Exception {
        var q = new OutboundQueue(10);
        q.offer(f(MessageType.LESSON_DATA), Priority.NORMAL, null, w);
        q.offer(f(MessageType.QUIZ_OPEN), Priority.CRITICAL, null, w);
        assertEquals(MessageType.QUIZ_OPEN, q.take().type());
    }

    @Test
    void conflatable_thayTaiChoKhiCungKey() throws Exception {
        var q = new OutboundQueue(10);
        q.offer(new Frame(MessageType.CLASS_STATE, (byte) 0, 0, new byte[] {1}), Priority.CONFLATABLE, "lop", w);
        q.offer(new Frame(MessageType.CLASS_STATE, (byte) 0, 0, new byte[] {2}), Priority.CONFLATABLE, "lop", w);
        assertEquals(1, q.size());
        assertArrayEquals(new byte[] {2}, q.take().payload());
    }

    @Test
    void droppable_hangDay_boCaiCuNhat() throws Exception {
        var q = new OutboundQueue(2);
        q.offer(new Frame(MessageType.CAST_DATA, (byte) 0, 1, new byte[0]), Priority.DROPPABLE, null, w);
        q.offer(new Frame(MessageType.CAST_DATA, (byte) 0, 2, new byte[0]), Priority.DROPPABLE, null, w);
        q.offer(new Frame(MessageType.CAST_DATA, (byte) 0, 3, new byte[0]), Priority.DROPPABLE, null, w);
        assertEquals(2, q.take().seq());
    }

    @Test
    void normal_hangDay_hetGioTraFalse() throws Exception {
        var q = new OutboundQueue(1);
        assertTrue(q.offer(f(MessageType.LESSON_DATA), Priority.NORMAL, null, w));
        assertFalse(q.offer(f(MessageType.LESSON_DATA), Priority.NORMAL, null, w));
    }

    @Test
    void dropPending_xoaDungKey() throws Exception {
        var q = new OutboundQueue(10);
        for (int i = 0; i < 5; i++) q.offer(f(MessageType.MIRROR_DELTA), Priority.NORMAL, "mirror:7", w);
        q.offer(f(MessageType.LESSON_DATA), Priority.NORMAL, null, w);
        assertEquals(5, q.dropPending("mirror:7"));
        assertEquals(1, q.size());
    }
}
```

`RouterTest`: (a) handler được gọi đúng loại; (b) gọi `QUIZ_OPEN` khi chưa đăng nhập → `ERROR(AUTH_REQUIRED)`; (c) học viên gọi handler chỉ cho `TEACHER` → `ERROR(FORBIDDEN)`; (d) handler ném ngoại lệ → `ERROR(INTERNAL)`, router không ném ra ngoài. Dùng `Ctx` giả ghi lại frame trả về.

`TcpServerTest` (socket thật, cổng 0): (a) gửi `HELLO` → nhận `HELLO_ACK`; (b) gửi 13 byte rác → nhận `ERROR(BAD_FRAME)` rồi kết nối bị đóng (`read` trả `-1`); (c) 20 client cùng gửi `HELLO` → cả 20 nhận `HELLO_ACK`.

**Commit:** `feat(server): thêm kết nối, hàng đợi ghi có ưu tiên, router và cách cắm module`

---

### Task 2.5: `Database` chạy `schema.sql`

**Files:**
- Create: `source/server/src/main/java/labcast/server/store/Database.java`
- Create: `source/server/src/main/java/labcast/server/store/SqlScript.java`
- Test: `source/server/src/test/java/labcast/server/store/DatabaseTest.java`

**Hợp đồng:**

```java
public final class Database implements AutoCloseable {
    public static Database open(String path) throws SQLException;     // ":memory:" cho test; chạy schema.sql
    public Connection connection();                                     // java.sql.Connection, đã bật foreign_keys
    public <T> T tx(SqlWork<T> work) throws SQLException;               // BEGIN … COMMIT, lỗi thì ROLLBACK
}

final class SqlScript {
    /** Bỏ chú thích "--" tới cuối dòng, rồi tách theo ';'. schema.sql có ';' nằm trong chú thích
     *  (ví dụ cột sessions.device), nên PHẢI bỏ chú thích trước khi tách. */
    static List<String> split(String script);
}
```

SQLite: mỗi `Database` giữ **một** `java.sql.Connection`, mọi thao tác ghi đi qua `synchronized` hoặc một luồng ghi riêng — SQLite chỉ cho một người ghi một lúc.

**Test:** `open(":memory:")` rồi đếm `sqlite_master` có đúng 15 bảng; `SqlScript.split("a; -- x; y\nb;")` ra `["a", "b"]`; chạy `open` hai lần trên cùng file không lỗi (`IF NOT EXISTS`).

**Commit:** `feat(server): mở SQLite và chạy schema.sql lúc khởi động`

---

### Task 2.6: Tài khoản và đăng nhập

**Files:**
- Create: `source/server/src/main/java/labcast/server/account/PasswordHasher.java`
- Create: `source/server/src/main/java/labcast/server/account/UserRepository.java`
- Create: `source/server/src/main/java/labcast/server/account/RosterImporter.java`
- Create: `source/server/src/main/java/labcast/server/account/AccountModule.java`
- Create: `source/server/src/main/java/labcast/server/session/Session.java`
- Create: `source/server/src/main/java/labcast/server/session/SessionManager.java`
- Test: `PasswordHasherTest`, `RosterImporterTest`, `AuthFlowTest`

**Payload:**

| Thông điệp | Payload |
| --- | --- |
| `HELLO` | `str clientVersion, str device` |
| `HELLO_ACK` | `i64 serverTime, str className` |
| `AUTH` | `str username, str password` |
| `AUTH_OK` | `str token, str role, i64 userId, str fullName, i64 classId` |
| `AUTH_FAIL` | `str reason` |
| `RESUME_REQ` | `str token, str lessonId` (rỗng nếu chưa mở bài), `i64 lastAckedSeq` |
| `RESUME_STATE` | `bool ok, i64 serverLastSeq` (−1 nếu không có) |
| `ERROR` | `u16 code, str message, i32 retryAfterMs` |
| `BYE` | `str reason` |
| `ADMIN_ROSTER_IMPORT` | `str csv` (mỗi dòng `username,họ tên,mật khẩu`) |
| `ADMIN_OK` | `str message` |

**Hợp đồng:**

```java
public final class PasswordHasher {                       // PBKDF2WithHmacSHA256, salt 16 byte, hash 32 byte
    public static final int ITERATIONS = 120_000;
    public record Hashed(byte[] hash, byte[] salt, int iterations) {}
    public static Hashed hash(String password);
    public static boolean verify(String password, Hashed stored);   // so bằng MessageDigest.isEqual
}

public final class SessionManager {
    public Session login(User u, Connection c);           // đang có phiên khác của u → gửi BYE(LOGGED_IN_ELSEWHERE) cho phiên cũ
    public Optional<Session> resume(String token, Connection c);
    public Optional<Session> byUser(long userId);
    public Collection<Session> all();
    public void onDisconnect(Connection c);               // giữ phiên để chờ RESUME, không xoá
    public void setCodeSeqLookup(CodeSeqLookup l);        // Người 1 gắn ở giai đoạn 2
}

@FunctionalInterface
public interface CodeSeqLookup { long lastSeq(long userId, String lessonId); }  // mặc định trả −1
```

- Token: 16 byte từ `SecureRandom`, mã hex, lưu bảng `sessions`.
- Đăng nhập sai: `AUTH_FAIL("Sai tên đăng nhập hoặc mật khẩu")` — **không** nói rõ sai tên hay sai mật khẩu. Ghi `audit_log`.
- Tài khoản giáo viên đầu tiên tạo bằng tham số dòng lệnh `--tao-giao-vien <user>:<mật khẩu>:<họ tên>` khi khởi động (task 2.9).

**Test:**
- `PasswordHasherTest`: cùng mật khẩu hai lần ra hai salt khác nhau; `verify` đúng/sai; đổi một byte hash → sai.
- `RosterImporterTest`: CSV 3 dòng tạo 3 học viên, dòng trùng `username` bị bỏ và báo trong kết quả; dòng thiếu cột bị bỏ.
- `AuthFlowTest` (socket thật): đăng nhập đúng → `AUTH_OK` có role `STUDENT`; sai mật khẩu → `AUTH_FAIL`; đăng nhập cùng tài khoản ở kết nối thứ hai → kết nối thứ nhất nhận `BYE("LOGGED_IN_ELSEWHERE")`; `RESUME_REQ` với token cũ trên kết nối mới → `RESUME_STATE(ok=true)`.

**Commit:** `feat(server): thêm đăng nhập PBKDF2, phiên và nhập danh sách lớp`

---

### Task 2.7: Heartbeat cơ bản và trạng thái có mặt

Bản giai đoạn 1 dùng chu kỳ **cố định 2 s**; giai đoạn 2 đổi sang thích nghi.

**Files:**
- Create: `source/server/src/main/java/labcast/server/presence/Presence.java`
- Create: `source/server/src/main/java/labcast/server/presence/PresenceState.java`
- Test: `source/server/src/test/java/labcast/server/presence/PresenceTest.java`

**Payload:** `HEARTBEAT` (server → app) `i64 serverTime`. `HEARTBEAT_ACK` (app → server) `i64 echoServerTime, list<{str key, i64 value}> fields`.

Danh sách `fields` là **trường mở rộng** (spec §17.3): app thêm bằng `heartbeat.addField(...)`, server đọc bằng `presence.onHeartbeat(...)`. Ví dụ Người 4 gửi `lastCastSeq`.

**Hợp đồng:**

```java
public enum PresenceState { ALIVE, SUSPECT, DISCONNECTED }

public final class Presence {
    public void start();                                           // mỗi 2 s gửi HEARTBEAT cho mọi phiên đang có kết nối
    public long srtt(long userId);                                  // −1 nếu chưa đo được
    public PresenceState state(long userId);
    public void onHeartbeat(BiConsumer<Session, Map<String, Long>> listener);
    public void onStateChange(BiConsumer<Session, PresenceState> listener);
    void ack(Session s, long echoServerTime, Map<String, Long> fields, long now);  // handler HEARTBEAT_ACK gọi
}
```

- RTT mẫu `= now − echoServerTime`. `srtt = 7/8·srtt + 1/8·rtt` (mẫu đầu: `srtt = rtt`).
- Không có ACK trong 2 chu kỳ → `SUSPECT`; 5 chu kỳ → `DISCONNECTED` và đóng kết nối.

**Test** (đồng hồ giả, không ngủ thật): mẫu RTT 100, 100, 200 → `srtt` ra đúng công thức; bỏ lỡ 2 chu kỳ → `SUSPECT`, 5 → `DISCONNECTED`; ACK lại sau `SUSPECT` → `ALIVE`; listener `onHeartbeat` nhận đúng map `fields`.

**Commit:** `feat(server): thêm heartbeat chu kỳ cố định và trạng thái có mặt`

---

### Task 2.8: Tìm server bằng UDP

**Files:**
- Create: `source/server/src/main/java/labcast/server/discovery/LanInterface.java`
- Create: `source/server/src/main/java/labcast/server/discovery/DiscoveryResponder.java`
- Create: `source/client/src/main/java/labcast/client/net/DiscoveryClient.java`
- Test: `LanInterfaceTest`, `DiscoveryTest`

**Payload:** `DISCOVER` `str clientVersion`. `DISCOVER_REPLY` `str className, str teacherName, str host, u16 port, bool tls, str certFingerprint`.

**Hợp đồng:**

```java
public record NicInfo(String name, String displayName, List<InetAddress> ipv4, boolean up, boolean loopback) {}

public final class LanInterface {
    /** Chọn card LAN: đang bật, không phải loopback, có IPv4 riêng (10.x, 172.16–31.x, 192.168.x),
     *  tên không chứa vEthernet, WSL, docker, VirtualBox, VMware, Hyper-V, vboxnet, br-.
     *  override (tên card hoặc IP) từ --lan được ưu tiên. */
    static Optional<NicInfo> pick(List<NicInfo> all, String override);
    public static NicInfo pickOrThrow(String override) throws SocketException;   // đọc NetworkInterface thật
}

public final class DiscoveryResponder implements AutoCloseable {
    public DiscoveryResponder(int port, Supplier<byte[]> replyPayload);   // port 7002; 0 khi test
    public void start();
    public int port();
}

public final class DiscoveryClient {
    public record ServerInfo(String className, String teacherName, String host, int port, boolean tls, String fingerprint) {}
    /** Gửi DISCOVER tới 255.255.255.255 và địa chỉ broadcast của từng card, gom trả lời trong timeout. */
    public static List<ServerInfo> find(int port, Duration timeout) throws IOException;
}
```

**Test:**
- `LanInterfaceTest`: danh sách giả gồm `vEthernet (WSL)` 172.25.x, `Ethernet` 192.168.1.10, `Loopback` → chọn `Ethernet`; có override `"vEthernet (WSL)"` → chọn nó; không card nào hợp lệ → `Optional.empty()`.
- `DiscoveryTest`: responder cổng 0 trên `127.0.0.1`, gửi `DISCOVER` tới `127.0.0.1:<port>` → nhận `DISCOVER_REPLY` đúng `className`; datagram rác → responder bỏ qua, không chết.

**Commit:** `feat(server): tìm server bằng UDP broadcast, chọn đúng card LAN`

---

### Task 2.9: `ServerMain` thật

**Files:**
- Modify: `source/server/src/main/java/labcast/server/ServerMain.java`
- Create: `source/server/src/main/java/labcast/server/ServerConfig.java`
- Test: `ServerConfigTest`

**Tham số dòng lệnh:**

| Tham số | Mặc định |
| --- | --- |
| `--port` | 7000 |
| `--db` | `labcast.db` (biến môi trường `LABCAST_DB` được ưu tiên) |
| `--content` | `content` |
| `--lop` | `Lớp thực hành` |
| `--lan` | tự chọn (task 2.8) |
| `--tao-giao-vien user:mật khẩu:họ tên` | không |
| `--khong-sandbox` | tắt (chỉ để phát triển, spec §8.1) |

`main` vẫn **giữ** `StatusEndpoint.start(...)` cho bước deploy. In ra địa chỉ card LAN đã chọn để giáo viên kiểm.

**Test:** `ServerConfig.parse(...)` đọc đúng từng tham số; tham số lạ → `IllegalArgumentException` có thông báo dễ hiểu.

**Commit:** `feat(server): ServerMain mở cổng 7000, tìm server và các module`

---

### Task 2.10: Client mạng, đăng nhập và lắp màn bài học

**Files:**
- Create: `source/client/src/main/java/labcast/client/net/LcpClient.java`
- Create: `source/client/src/main/java/labcast/client/net/Heartbeat.java`
- Create: `source/client/src/main/java/labcast/client/app/LabCastApp.java` (Launcher đã có từ giai đoạn 0)
- Create: `source/client/src/main/java/labcast/client/ui/LoginController.java`, `LoginView.java`
- Create: `source/client/src/main/java/labcast/client/ui/LessonScreen.java`
- Test: `LcpClientTest` (dùng server thật — `client` đã có `labcast-server` ở scope test)

**Hợp đồng:**

```java
public final class LcpClient implements AutoCloseable {
    public void connect(String host, int port) throws IOException;
    public void send(MessageType t, byte[] payload);
    public void send(Frame f);
    public void on(MessageType t, Consumer<Frame> listener);          // gọi trên luồng đọc mạng
    public CompletableFuture<Frame> expect(MessageType... types);     // đăng ký TRƯỚC khi send yêu cầu
    public Heartbeat heartbeat();                                      // tự trả HEARTBEAT_ACK
    public void onStateChange(Consumer<State> l);                      // CONNECTED, RECONNECTING, CLOSED
    public enum State { CONNECTED, RECONNECTING, CLOSED }
}

public final class Heartbeat {
    public void addField(String key, LongSupplier value);             // ví dụ ("lastCastSeq", cast::lastSeq)
}
```

- Mất kết nối → tự nối lại sau 0,5 / 1 / 2 / 4 s, tối đa 5 s một lần; nối được thì gửi `HELLO` rồi `RESUME_REQ(token…)`.
- Listener chạy trên luồng mạng. Ai cập nhật giao diện thì tự bọc `Platform.runLater`.

**Màn bài học** (`LessonScreen`) chỉ **lắp ghép** ba phần của ba người, không chứa nghiệp vụ:

```text
┌──────────────────────────────┬──────────────────────────────┐
│ PlayerPane (Người 4)          │ CodeEditor (Người 1)          │
│ animation + thanh tua         │ ô soạn code                   │
├──────────────────────────────┴──────────────────────────────┤
│ RunPanel (Người 3): nút Chạy thử, kết quả từng test           │
└─────────────────────────────────────────────────────────────┘
```

**Test** (`LcpClientTest`): kết nối, `HELLO` → `HELLO_ACK`; đăng nhập qua `expect(AUTH_OK, AUTH_FAIL)`; server gửi `HEARTBEAT` → client tự trả `HEARTBEAT_ACK` có trường mở rộng đã `addField`; server đóng socket → state thành `RECONNECTING` rồi `CONNECTED` lại và phiên được khôi phục.

**Commit:** `feat(client): thêm client mạng tự nối lại, đăng nhập và màn bài học`

---

## Người 4 — trace và trình phát

### Task 4.1: Đọc trace

**Files:**
- Create: `source/common/src/main/java/labcast/common/trace/TraceEvent.java`
- Create: `source/common/src/main/java/labcast/common/trace/Trace.java`
- Create: `source/common/src/main/java/labcast/common/trace/TraceReader.java`
- Create: `source/common/src/test/resources/trace/noi-bot-4.jsonl` (trace mẫu, lấy đúng ví dụ ở spec §7.3)
- Test: `source/common/src/test/java/labcast/common/trace/TraceReaderTest.java`

**Hợp đồng:**

```java
public record TraceEvent(String t, ObjectNode data) {         // data = toàn bộ object JSON của dòng
    public String var();                                       // trường "v", null nếu không có
    public int i(String field);
}

public final class Trace {
    public JsonNode header();                                  // sự kiện "hdr"
    public List<TraceEvent> events();                          // không gồm hdr, end
    public int snapshotBefore(int k);                          // chỉ số sự kiện "snap" gần nhất ≤ k, −1 nếu không có
    public boolean truncated();
    public List<Integer> keySteps();                           // chỉ số các sự kiện "key"
}

public final class TraceReader {
    public static Trace read(Reader r) throws IOException;     // dòng trống bỏ qua; dòng hỏng → IOException có số dòng
    public static Trace append(Trace t, List<String> lines);   // nhận thêm khúc từ TRACE_CHUNK
}
```

**Test:** đọc file mẫu → đúng số sự kiện, `header().get("lesson")`; `snapshotBefore(5)` trả đúng chỉ số; file có `"truncated":true` ở `end` → `truncated()`; dòng JSON hỏng → `IOException` chứa "dòng 3".

**Commit:** `feat(common): đọc trace JSON từng dòng và chỉ mục keyframe`

### Task 4.2: Trình phát

**Files:**
- Create: `source/client/src/main/java/labcast/client/player/View.java` (đúng chữ ký spec §7.4)
- Create: `source/client/src/main/java/labcast/client/player/Player.java`
- Create: `source/client/src/main/java/labcast/client/player/PlayerPane.java` (JavaFX: vùng vẽ + nút + thanh tua)
- Test: `source/client/src/test/java/labcast/client/player/PlayerTest.java`

**Hợp đồng:**

```java
public final class Player {
    public Player(Trace trace, Map<String, View> viewsByVar, LongSupplier nowMillis);
    public void play(); void pause(); void speed(double x);    // 0.25 … 8
    public void seek(int k);                                   // reset từ snapshot rồi áp tới k, KHÔNG animation
    public void nextKeyStep();
    public void tick();                                        // AnimationTimer gọi mỗi khung hình
    public int position();
    public static final Duration STEP = Duration.ofMillis(400); // 1 bước ở tốc độ 1×
}
```

- Sự kiện có `v` → chuyển cho view của biến đó. Sự kiện `say` → dòng thuyết minh. `line`, `vars` → view `code-bien` nếu có.
- `seek(k)`: `s = snapshotBefore(k)`; mọi view `reset(state)`; áp các sự kiện `s+1 … k` với `animate=false`.

**Test** (view giả ghi lại lời gọi, đồng hồ giả): `seek(450)` với snapshot ở 400 → mỗi view nhận 1 `reset` và đúng 50 `apply(…, false)`; `play()` rồi tiến đồng hồ 2 000 ms ở tốc độ 1× → vị trí tăng 5; tốc độ 2× → tăng 10; `nextKeyStep()` nhảy đúng sự kiện `key` kế tiếp.

**Commit:** `feat(client): thêm trình phát trace có tua qua keyframe`

### Task 4.3: Bộ vẽ `mang`

**Files:**
- Create: `source/client/src/main/java/labcast/client/views/mang/ArrayModel.java` (thuần Java)
- Create: `source/client/src/main/java/labcast/client/views/mang/ArrayView.java` (JavaFX, `implements View`)
- Test: `source/client/src/test/java/labcast/client/views/mang/ArrayModelTest.java`

**Hợp đồng:**

```java
public final class ArrayModel {
    public void reset(List<Integer> values);
    /** Trả về thay đổi cần vẽ: READ (tô vàng ô i), SET (đổi giá trị ô i), SWAP (hai SET liên tiếp đổi giá trị cho nhau). */
    public List<Change> apply(TraceEvent e);
    public List<Integer> values();
    public sealed interface Change permits Read, Set, Swap {}
}
```

- `ArrayView` vẽ mỗi phần tử một `Rectangle` cao theo giá trị. `SWAP` dùng `TranslateTransition` cho hai cột chạy cùng lúc (`ParallelTransition`). Màu theo quy ước spec §7.4.
- Phát hiện đổi chỗ: giữ lại `SET` vừa nhận; nếu `SET` kế tiếp ghi vào ô khác giá trị cũ của ô trước và ngược lại → gộp thành `SWAP`.

**Test:** `reset([5,1,4])`, áp `read 0`, `read 1`, `set 0 → 1`, `set 1 → 5` → nhận `Read, Read, Swap(0,1)`, `values() = [1,5,4]`; `set` đơn lẻ → `Set`.

**Commit:** `feat(client): thêm bộ vẽ mảng có hiệu ứng đổi chỗ`

---

## Người 3 — runner, chạy code, bài học, đồng hồ

### Task 3.1: `viz.Mang` và bộ ghi trace (Python)

**Files:**
- Create: `runner/viz/__init__.py`
- Create: `runner/viz/recorder.py`
- Create: `runner/viz/mang.py`
- Test: `runner/tests/test_mang.py`

**Hợp đồng và code chính:**

```python
# runner/viz/recorder.py
import json

class TraceLimit(Exception):
    """Vượt trần số sự kiện hoặc dung lượng — dừng chương trình học viên."""

class Recorder:
    SNAP_EVERY = 200

    def __init__(self, out, max_events=10_000, max_bytes=2_000_000, enabled=True):
        self.out, self.max_events, self.max_bytes, self.enabled = out, max_events, max_bytes, enabled
        self.count, self.bytes, self.structures = 0, 0, {}

    def register(self, name, structure):
        self.structures[name] = structure
        if self.enabled:
            self._write({"t": "snap", "n": self.count, "state": self._state()})

    def emit(self, event):
        if not self.enabled:
            return
        self._write(event)
        self.count += 1
        if self.count % self.SNAP_EVERY == 0:
            self._write({"t": "snap", "n": self.count, "state": self._state()})

    def _state(self):
        return {name: list(s) for name, s in self.structures.items()}

    def _write(self, obj):
        line = json.dumps(obj, ensure_ascii=False)
        self.bytes += len(line) + 1
        if self.count >= self.max_events or self.bytes > self.max_bytes:
            raise TraceLimit()
        self.out.write(line + "\n")

current = Recorder(out=None, enabled=False)   # run.py thay bằng bộ ghi thật

def say(text):
    current.emit({"t": "say", "s": str(text)})

def key(text):
    current.emit({"t": "key", "s": str(text)})
```

```python
# runner/viz/mang.py
from . import recorder

class Mang(list):
    """Mảng "gắn camera": ghi lại mọi lần đọc và ghi theo chỉ số."""

    def __init__(self, values, ten="a"):
        super().__init__(values)
        self.ten = ten
        recorder.current.register(ten, self)

    def __getitem__(self, i):
        if isinstance(i, int):
            recorder.current.emit({"t": "read", "v": self.ten, "i": i % len(self)})
        return super().__getitem__(i)

    def __setitem__(self, i, x):
        super().__setitem__(i, x)
        if isinstance(i, int):
            recorder.current.emit({"t": "set", "v": self.ten, "i": i % len(self), "x": x})
```

`runner/viz/__init__.py` xuất `Mang`, `say`, `key`, `recorder`.

**Test (pytest):**

```python
import io, json
from viz import recorder
from viz.mang import Mang

def ghi(**kw):
    buf = io.StringIO()
    recorder.current = recorder.Recorder(out=buf, **kw)
    return buf

def dong(buf):
    return [json.loads(x) for x in buf.getvalue().splitlines()]

def test_doi_cho_sinh_hai_lenh_set():
    buf = ghi()
    a = Mang([5, 1], ten="a")
    a[0], a[1] = a[1], a[0]
    t = [e["t"] for e in dong(buf)]
    assert t == ["snap", "read", "read", "set", "set"]
    assert list(a) == [1, 5]

def test_vuot_tran_thi_dung():
    buf = ghi(max_events=3)
    a = Mang([1, 2, 3], ten="a")
    try:
        for _ in range(10):
            a[0]
        assert False, "phải dừng"
    except recorder.TraceLimit:
        pass

def test_tat_ghi_thi_khong_sinh_su_kien():
    recorder.current = recorder.Recorder(out=None, enabled=False)
    a = Mang([3, 2], ten="a")
    a[0] = 9
    assert list(a) == [9, 2]
```

**Commit:** `feat(runner): thêm mảng gắn camera và bộ ghi trace`

### Task 3.2: `run.py` — chạy một công việc

**Files:**
- Create: `runner/run.py`
- Test: `runner/tests/test_run.py`

**Công việc đọc từ stdin** (một object JSON):

```json
{"code": "…", "input": "5 1 4 2\n", "trace": true, "time_limit_ms": 1000,
 "max_events": 10000, "max_bytes": 2000000}
```

**Ra stdout** (JSON từng dòng): các sự kiện trace (nếu `trace`), rồi **một** dòng kết quả cuối:

```json
{"t": "result", "ok": true, "stdout": "1 2 4 5\n", "error": null, "truncated": false, "time_ms": 3}
```

- Trước khi chạy code học viên: lưu `sys.stdout` thật để ghi trace; thay `sys.stdout` bằng `io.StringIO()` để gom phần học viên `print`; `sys.stdin = io.StringIO(job["input"])`.
- `exec(compile(code, "bai_lam.py", "exec"), {"__name__": "__main__"})`.
- Bắt `TraceLimit` → `truncated: true, ok: true`; bắt ngoại lệ khác → `ok: false, error: traceback rút gọn` (chỉ giữ các khung của `bai_lam.py`).
- `resource.setrlimit(RLIMIT_CPU, …)` khi chạy trên Linux (trong Docker); trên Windows bỏ qua.

**Test:** chạy `run.py` bằng `subprocess` với code nổi bọt dùng `viz.Mang` → dòng cuối `ok: true`, stdout `"1 2 4 5\n"`, có sự kiện `set`; code `raise ValueError` → `ok: false`, `error` chứa `ValueError` và `bai_lam.py`; vòng lặp vô hạn đọc `a[0]` → `truncated: true`.

**Commit:** `feat(runner): run.py chạy bài làm, gom stdout và trace`

### Task 3.3: Chạy code từ server

**Files:**
- Create: `source/server/src/main/java/labcast/server/run/Job.java`
- Create: `source/server/src/main/java/labcast/server/run/Sandbox.java`, `DockerSandbox.java`, `LocalPythonSandbox.java`
- Create: `source/server/src/main/java/labcast/server/run/RunService.java`, `RunModule.java`
- Test: `LocalPythonSandboxTest`, `DockerSandboxTest`

**Hợp đồng:**

```java
public record Job(String code, String input, boolean trace, int timeLimitMs) {}
public record Outcome(boolean ok, String stdout, String error, boolean truncated, long timeMs, boolean killed) {}

public interface Sandbox {
    /** Chạy job; mỗi dòng trace gọi onLine (không gồm dòng "result"). */
    Outcome run(Job job, Consumer<String> onLine) throws IOException, InterruptedException;
}

public final class RunService {                                     // spec §17.3
    public CompletableFuture<Outcome> run(long userId, Job job, Consumer<String> onLine);
}
```

- `DockerSandbox` dựng lệnh đúng các cờ ở spec §8.1, `ProcessBuilder`, ghi job ra stdin rồi đóng stdin, đọc stdout từng dòng. Quá `timeLimitMs + 1500` → `docker kill <tên container>` (đặt tên container `labcast-run-<uuid>` để kill được) và trả `killed=true`.
- `LocalPythonSandbox` chạy `python runner/run.py` — chỉ khi server có `--khong-sandbox`.
- `RunService` giai đoạn 1: `Executors.newFixedThreadPool(max(1, nhân − 1))`. Hàng đợi công bằng làm ở giai đoạn 2.

**Payload:**

| Thông điệp | Payload |
| --- | --- |
| `RUN_REQ` | `str lessonId, i64 codeSeq, str testId` (rỗng = test xem đầu tiên) |
| `RUN_QUEUED` | `i64 runId, u16 position` |
| `TRACE_CHUNK` | `i64 runId, i32 chunkIndex, bool last, blob lines` (UTF-8, tối đa 200 dòng một khúc) |
| `RUN_RESULT` | `JSON{runId, ok, stdout, error, truncated, timeMs, tests:[{id, verdict}]}` |

Handler `RUN_REQ`: lấy code bằng `CodeStore.current(userId, lessonId)` (Người 1, task 1.3); nếu `codeSeq` của server nhỏ hơn yêu cầu thì chờ tối đa 1 s rồi chạy bản mới nhất có.

**Test:** `LocalPythonSandboxTest` (bỏ qua nếu không có `python`): chạy nổi bọt → `ok`, nhận > 0 dòng trace. `DockerSandboxTest` (bỏ qua nếu `docker info` lỗi): job `import socket; socket.create_connection(("1.1.1.1", 80), 2)` → `ok=false` (không có mạng); `while True: pass` với `timeLimitMs=500` → `killed=true` trong < 3 s.

**Commit:** `feat(server): chạy bài làm trong Docker sandbox và gửi trace về`

### Task 3.4: Nạp bài học và bài mẫu nổi bọt

**Files:**
- Create: `source/server/src/main/java/labcast/server/lesson/Lesson.java`, `LessonRepository.java`, `LessonModule.java`
- Modify: `source/server/pom.xml` — thêm `snakeyaml`, `jackson-databind`
- Create: `content/dsa/sap-xep/noi-bot/` với `lesson.yaml`, `de.md`, `khung.py`, `loi_giai.py`, `demo.in`, `tests/nho-1.in`, `tests/nho-1.out`
- Test: `LessonRepositoryTest` (thư mục `content` giả trong `src/test/resources`)

**Hợp đồng:**

```java
public record Lesson(String id, String mon, String chuong, String ten, String loai, List<String> bac,
                     List<ViewSpec> khungNhin, List<String> camDung, int thoiGianMs, List<String> testXem,
                     Path dir) {}
public record ViewSpec(String bien, String kieu) {}

public final class LessonRepository {
    public static LessonRepository load(Path contentRoot);   // bài lỗi: bỏ qua và log, không làm hỏng cả server
    public Optional<Lesson> get(String id);
    public List<Lesson> all();
    public List<String> errors();                             // để CI in ra
}
```

**Payload:** `LESSON_LIST` trống. `LESSON_FETCH` `str lessonId`. `LESSON_DATA` `JSON{kind: "list", lessons:[{id, mon, chuong, ten, loai}]}` hoặc `JSON{kind: "lesson", id, ten, de, khung, khungNhin, trace}`, trong đó `trace` là trace giảng giải (chạy `loi_giai.py` với `demo.in` qua sandbox lần đầu được hỏi, rồi giữ trong bộ nhớ).

**`khung.py` của bài nổi bọt:**

```python
import viz


def sap_xep(a):
    # Viết sắp xếp nổi bọt ở đây. Chỉ đổi chỗ hai phần tử kề nhau.
    pass


a = viz.Mang(list(map(int, input().split())), ten="a")
sap_xep(a)
print(*a)
```

`loi_giai.py` giống `khung.py` nhưng có thân hàm và `viz.say`/`viz.key` sau mỗi lượt. `demo.in` là `5 1 4 2 8`. `tests/nho-1.in` là `3 2 1`, `nho-1.out` là `1 2 3`.

**Test:** nạp thư mục giả có 1 bài đúng và 1 bài thiếu `lesson.yaml` → `all()` có 1 bài, `errors()` có 1 dòng nhắc tên thư mục hỏng.

**Commit:** `feat(server): nạp bài học từ content/ và thêm bài nổi bọt`

### Task 3.5: Đồng bộ đồng hồ bản đầu

**Files:**
- Create: `source/server/src/main/java/labcast/server/sync/SyncModule.java`
- Create: `source/client/src/main/java/labcast/client/clock/Clock.java`, `ClockSync.java`
- Test: `source/client/src/test/java/labcast/client/clock/ClockSyncTest.java`

**Payload:** `TIME_SYNC_REQ` `i64 t1`. `TIME_SYNC_RESP` `i64 t1, i64 t2, i64 t3`.

- Server: `t2 = ctx.receivedAt()` (thời điểm **luồng đọc** nhận frame, không phải lúc handler chạy), `t3` = ngay trước khi gửi; trả với `Priority.CRITICAL` để không phải xếp hàng.
- App: khi kết nối gửi 5 yêu cầu cách nhau 100 ms; `t4` ghi ngay khi nhận.

```java
public interface Clock { long serverNow(); }                       // spec §17.3

public final class ClockSync implements Clock {
    public synchronized boolean addSample(long t1, long t2, long t3, long t4); // giữ mẫu delay nhỏ nhất
    public long offsetMs(); long bestDelayMs(); boolean isSynced();
    public long serverNow();                                       // System.currentTimeMillis() + offset
}
```

`offset = ((t2 − t1) + (t3 − t4)) / 2`, `delay = (t4 − t1) − (t3 − t2)`; delay âm thì bỏ mẫu (tham khảo bản cũ `ClockSync`).

**Test:**

```java
@Test
void giuMauDelayNhoNhat() {
    var c = new ClockSync();
    c.addSample(1000, 6050, 6051, 1101);   // delay 100, offset 5000
    c.addSample(2000, 7010, 7011, 2021);   // delay 20, offset 5000 — tốt hơn
    c.addSample(3000, 8300, 8301, 3401);   // delay 400 — bỏ
    assertEquals(20, c.bestDelayMs());
    assertEquals(5000, c.offsetMs());
}

@Test
void delayAm_boMau() {
    var c = new ClockSync();
    assertFalse(c.addSample(1000, 500, 600, 900));
    assertFalse(c.isSynced());
}
```

**Commit:** `feat(server): đồng bộ đồng hồ kiểu NTP, giữ mẫu RTT nhỏ nhất`

---

## Người 1 — dòng code

### Task 1.1: `TextOps`

**Files:**
- Create: `source/common/src/main/java/labcast/common/protocol/CodeDelta.java` (record dùng chung hai phía — PR nhỏ, nhờ leader duyệt vì nằm trong `common.protocol`)
- Create: `source/server/src/main/java/labcast/server/journal/TextOps.java`
- Test: `TextOpsTest`

```java
public record CodeDelta(String lessonId, long seq, int pos, int delLen, String insText, long clientTs) {
    public byte[] toPayload();                       // str lessonId, i64 seq, i32 pos, i32 delLen, str insText, i64 clientTs
    public static CodeDelta from(Frame f) throws ProtocolException;
}

final class TextOps {
    /** Xoá delLen ký tự từ pos rồi chèn insText. pos/delLen ngoài phạm vi → IllegalArgumentException. */
    static String apply(String text, int pos, int delLen, String ins);
}
```

**Test:** chèn đầu, giữa, cuối; xoá; thay thế; `pos` âm hoặc `pos + delLen > length` → ngoại lệ; chuỗi có ký tự tiếng Việt (đếm theo `char` của Java, hai phía cùng quy ước).

**Commit:** `feat(server): thêm CodeDelta và phép áp thay đổi văn bản`

### Task 1.2: Gộp phím gõ thành delta

**Files:**
- Create: `source/client/src/main/java/labcast/client/editor/DeltaBatcher.java`
- Test: `DeltaBatcherTest`

```java
public final class DeltaBatcher {
    public DeltaBatcher(String lessonId, LongSupplier nowMillis, Consumer<CodeDelta> out);
    public void onChange(int pos, String removed, String inserted);   // RichTextFX báo mỗi thay đổi
    public void tick();                                               // gọi mỗi 50 ms; quá 100 ms từ thay đổi đầu → xả
    public void flush();                                              // xả ngay (trước khi chạy thử)
    public long lastSeq();
}
```

Gộp: các lần **chèn liền nhau** (lần sau bắt đầu đúng ở cuối lần trước) thành một delta; các lần xoá lùi liền nhau thành một delta. Thay đổi không liền nhau → xả delta đang gộp trước rồi mở delta mới. `seq` tăng 1 mỗi delta, bắt đầu từ `lastSeq` server trả về.

**Test** (đồng hồ giả): gõ "a","b","c" liền nhau trong 60 ms → sau `tick()` ở 110 ms ra **1** delta `insText="abc"`; gõ "a" rồi nhảy về đầu gõ "x" → 2 delta; `flush()` xả ngay; `seq` liên tiếp 1, 2, 3.

**Commit:** `feat(client): gộp phím gõ mỗi 100 ms thành delta`

### Task 1.3: Lưu dòng code trên server

**Files:**
- Create: `source/server/src/main/java/labcast/server/journal/CodeStore.java`, `JournalModule.java`
- Test: `CodeStoreTest`

```java
public final class CodeStore {                                         // spec §17.3
    public record Snapshot(String code, long seq) {}
    /** Trả seq liền mạch cuối cùng sau khi xử lý (dùng cho CODE_ACK). */
    public long accept(long userId, CodeDelta d);
    public Snapshot current(long userId, String lessonId);              // code đầy đủ tại seq liền mạch cuối
    public long lastSeq(long userId, String lessonId);                  // nối vào SessionManager.setCodeSeqLookup ở giai đoạn 2
}
```

- `accept`: `INSERT OR IGNORE` vào `code_deltas` (khoá chính `user_id, lesson_id, seq`). Chỉ **áp** delta khi `seq == lastSeq + 1`; delta đến sớm thì lưu bảng nhưng chưa áp; mỗi lần áp xong kiểm tiếp các delta đã lưu có `seq` kế tiếp.
- `current`: giữ bản đệm trong bộ nhớ; lần đầu thì dựng lại bằng cách áp lần lượt các delta trong bảng (giai đoạn 2 thêm mốc lưu để không phải áp từ đầu).
- Code ban đầu của một bài = nội dung `khung.py` (lấy qua `LessonRepository` — gọi API của Người 3, không đọc file trực tiếp).

**Payload:** `CODE_DELTA` như `CodeDelta.toPayload()`. `CODE_ACK` `str lessonId, i64 lastSeq`.

**Test:** áp 1, 2, 3 theo thứ tự → `current` đúng, `lastSeq=3`; gửi trùng 2 → bỏ qua, ACK vẫn 3; nhận 5 trước 4 → ACK 3, nhận 4 → ACK 5 và code có cả 4 lẫn 5; tạo `CodeStore` mới trên cùng database → `current` dựng lại đúng.

**Commit:** `feat(server): lưu dòng code, áp delta liền mạch, bỏ trùng`

### Task 1.4: Ô soạn code và gửi delta

**Files:**
- Create: `source/client/src/main/java/labcast/client/editor/CodeEditor.java` (RichTextFX `CodeArea`, tô màu từ khoá Python bằng regex)
- Create: `source/client/src/main/java/labcast/client/journal/JournalClient.java`
- Modify: `source/client/pom.xml` — `richtextfx` đã thêm ở giai đoạn 0
- Test: `JournalClientTest`

```java
public final class JournalClient {
    public JournalClient(LcpClient client);
    public void submit(CodeDelta d);              // giữ trong danh sách chưa ACK rồi gửi
    public int unacked();                         // giai đoạn 2 thay danh sách trong bộ nhớ bằng nhật ký trên đĩa
}
```

Nhận `CODE_ACK(lastSeq)` → xoá mọi delta có `seq ≤ lastSeq`.

**Test** (với server thật): gửi 3 delta → nhận ACK → `unacked() == 0` và `CodeStore.current` khớp văn bản đã gõ.

**Commit:** `feat(client): ô soạn code Python gửi delta lên server`

---

## Nghiệm thu M1

Hai máy cắm dây chung switch; máy A chạy server (có Docker), máy B chạy app.

```bash
# máy A — đứng ở thư mục gốc repo
docker build -t labcast-runner:py3.12 runner
cd source && ./mvnw -q package -DskipTests
java -jar server/target/labcast-server.jar --tao-giao-vien gv:matkhau:Giáo viên --content ../content
```

- [ ] Máy B mở app: danh sách lớp tự hiện tên lớp của máy A (không gõ IP).
- [ ] Đăng nhập học viên (nhập bằng `ADMIN_ROSTER_IMPORT` từ app giáo viên hoặc test) thành công; sai mật khẩu bị từ chối.
- [ ] Mở bài nổi bọt: animation giảng giải chạy, tua tới lui được.
- [ ] Gõ code nổi bọt vào khung, bấm **Chạy thử**: animation của **chính code vừa gõ** chạy, kết quả test `nho-1` đúng.
- [ ] Wireshark trên máy A, lọc `tcp.port == 7000`: thấy frame bắt đầu bằng `4C 43` (`LC`).
- [ ] `./mvnw verify` và `python -m pytest runner` xanh trên CI.
