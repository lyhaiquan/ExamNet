-- ExamNet — lược đồ cơ sở dữ liệu
--
-- Thiết kế theo bốn bước của giáo trình: trích thực thể → xét quan hệ
-- (1-1 gộp, 1-n giữ nguyên, n-n đẻ bảng trung gian) → bảng phía n giữ khóa
-- ngoại → rà soát thuộc tính.
--
-- Mỗi nhóm bảng ghi rõ chủ (docs/PHAN-CONG.md). Đổi bảng của ai thì người đó
-- duyệt PR. Người 3 giữ phần mở kết nối và chạy file này (service.db).

PRAGMA foreign_keys = ON;

-- Bật WAL: cho phép đọc và ghi đồng thời thay vì khóa toàn bộ file.
-- Quan trọng với ExamNet vì nhiều luồng cùng ghi ANSWER_DELTA trong khi
-- dashboard giám thị liên tục đọc trạng thái.
PRAGMA journal_mode = WAL;

-- ---------------------------------------------------------------------
-- Tài khoản — chủ: Người 2
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    -- Không bao giờ lưu mật khẩu thô. PBKDF2-HMAC-SHA256, salt riêng từng
    -- người để hai tài khoản cùng mật khẩu vẫn ra hash khác nhau.
    password_hash BLOB    NOT NULL,
    salt          BLOB    NOT NULL,
    iterations    INTEGER NOT NULL,
    full_name     TEXT    NOT NULL,
    role          INTEGER NOT NULL,   -- 0=CANDIDATE, 1=PROCTOR, 2=ADMIN
    created_at    INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);

-- ---------------------------------------------------------------------
-- Kỳ thi — chủ: Người 3
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exams (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    title           TEXT    NOT NULL,
    -- 0=DRAFT, 1=SCHEDULED, 2=RUNNING, 3=CLOSED, 4=GRADED
    state           INTEGER NOT NULL DEFAULT 0,
    duration_sec    INTEGER NOT NULL,
    -- Mốc thời gian dùng đồng hồ SERVER. Client không bao giờ được ghi vào đây.
    scheduled_start INTEGER,
    started_at      INTEGER,
    deadline_at     INTEGER,
    shuffle         INTEGER NOT NULL DEFAULT 1,
    created_by      INTEGER NOT NULL REFERENCES users (id),
    created_at      INTEGER NOT NULL
);

-- ---------------------------------------------------------------------
-- Ngân hàng câu hỏi — chủ: Người 3
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS questions (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    exam_id    INTEGER NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
    -- 0 = một đáp án đúng, 1 = nhiều đáp án đúng
    type       INTEGER NOT NULL DEFAULT 0,
    content    TEXT    NOT NULL,
    points     REAL    NOT NULL DEFAULT 1.0,
    topic      TEXT,
    difficulty INTEGER NOT NULL DEFAULT 1,
    ord        INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_questions_exam ON questions (exam_id);

-- Quan hệ questions–options là 1-n, nên khóa ngoại nằm ở phía options (phía n).
CREATE TABLE IF NOT EXISTS options (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    question_id INTEGER NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    content     TEXT    NOT NULL,
    -- CỘT NÀY KHÔNG BAO GIỜ ĐƯỢC GỬI XUỐNG CLIENT.
    -- Server là bên duy nhất giữ đáp án đúng; đó là lý do việc chấm điểm phải
    -- xảy ra ở server, và là lý do thí sinh không thể đọc đáp án từ RAM máy thi.
    is_correct  INTEGER NOT NULL DEFAULT 0,
    ord         INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_options_question ON options (question_id);

-- ---------------------------------------------------------------------
-- Thí sinh dự thi — bảng trung gian n-n giữa users và exams — chủ: Người 2
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS candidates (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id  INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exam_id  INTEGER NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
    seat_no  TEXT,
    UNIQUE (user_id, exam_id)
);

-- ---------------------------------------------------------------------
-- Phiên thi — chủ: Người 2
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sessions (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    token           TEXT    NOT NULL UNIQUE,
    user_id         INTEGER NOT NULL REFERENCES users (id),
    exam_id         INTEGER NOT NULL REFERENCES exams (id),
    state           INTEGER NOT NULL,
    started_at      INTEGER NOT NULL,
    last_seq        INTEGER NOT NULL DEFAULT 0,
    clock_offset_ms INTEGER NOT NULL DEFAULT 0,
    submitted_at    INTEGER,
    UNIQUE (user_id, exam_id)
);

-- ---------------------------------------------------------------------
-- Bài làm — trung tâm của ĐG1 — chủ: Người 1
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS answers (
    session_id INTEGER NOT NULL REFERENCES sessions (id) ON DELETE CASCADE,
    question_id INTEGER NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    -- Danh sách id đáp án đã chọn, ngăn cách bởi dấu phẩy. Rỗng = bỏ trống.
    option_ids TEXT    NOT NULL DEFAULT '',
    -- Seq của ANSWER_DELTA đã tạo ra bản ghi này.
    -- Khóa chính (session_id, question_id) cộng với cột này là toàn bộ cơ chế
    -- khử trùng: gửi lại cùng một delta chỉ ghi đè bằng đúng giá trị cũ, nên
    -- thao tác là idempotent. Đây là thứ cho phép protocol chạy ngữ nghĩa
    -- at-least-once trên đường truyền mà vẫn exactly-once về trạng thái.
    seq        INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (session_id, question_id)
);

-- ---------------------------------------------------------------------
-- Kết quả — chủ: Người 3
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS results (
    session_id INTEGER PRIMARY KEY REFERENCES sessions (id) ON DELETE CASCADE,
    score      REAL    NOT NULL,
    max_score  REAL    NOT NULL,
    correct    INTEGER NOT NULL,
    total      INTEGER NOT NULL,
    graded_at  INTEGER NOT NULL
);

-- ---------------------------------------------------------------------
-- Nhật ký hoạt động — "Ghi log hoạt động", "Centralized Logging" — chủ: Người 4
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_log (
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    -- Luôn dùng đồng hồ SERVER. Log mà trộn giờ của nhiều máy khách thì không
    -- dựng lại được trình tự sự kiện khi cần điều tra sự cố.
    ts     INTEGER NOT NULL,
    actor  TEXT,
    action TEXT    NOT NULL,
    detail TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_ts ON audit_log (ts);
