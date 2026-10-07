-- LabCast — lược đồ cơ sở dữ liệu (spec §15)
--
-- SQLite trên máy server. Mọi câu có dữ liệu người dùng đi qua PreparedStatement.
-- Mỗi nhóm bảng ghi rõ chủ (spec §17.1). Đổi bảng của ai thì người đó duyệt PR.
-- Người 2 giữ phần mở kết nối và chạy file này (server.store).
--
-- Thời gian lưu dạng INTEGER: số mili giây kể từ epoch, theo đồng hồ server.

PRAGMA foreign_keys = ON;

-- WAL cho phép đọc và ghi đồng thời: nhiều luồng ghi CODE_DELTA trong khi sơ đồ
-- lớp liên tục đọc tiến độ.
PRAGMA journal_mode = WAL;

-- ---------------------------------------------------------------------
-- Tài khoản, phiên, lớp học, câu hỏi nhanh, nhật ký — chủ: Người 2
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    full_name     TEXT    NOT NULL,
    role          TEXT    NOT NULL CHECK (role IN ('TEACHER', 'STUDENT')),
    -- Không bao giờ lưu mật khẩu thô. PBKDF2-HMAC-SHA256, salt riêng từng người.
    pwd_hash      BLOB    NOT NULL,
    salt          BLOB    NOT NULL,
    iterations    INTEGER NOT NULL,
    created_at    INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sessions (
    token         TEXT    PRIMARY KEY,
    user_id       INTEGER NOT NULL REFERENCES users(id),
    device        TEXT,                 -- máy đang dùng; đăng nhập máy khác thì phiên cũ bị đóng
    created_at    INTEGER NOT NULL,
    last_seen     INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS classes (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL,
    teacher_id    INTEGER NOT NULL REFERENCES users(id),
    created_at    INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS enrollments (
    class_id      INTEGER NOT NULL REFERENCES classes(id),
    user_id       INTEGER NOT NULL REFERENCES users(id),
    PRIMARY KEY (class_id, user_id)
);

CREATE TABLE IF NOT EXISTS quizzes (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    class_id      INTEGER NOT NULL REFERENCES classes(id),
    lesson_id     TEXT    REFERENCES lessons(id),
    question_json TEXT    NOT NULL,
    answer_json   TEXT    NOT NULL,     -- chỉ nằm trên server
    opens_at      INTEGER NOT NULL,
    closes_at     INTEGER NOT NULL
);

-- est_sent_at = arrived_at − min(srtt_ms/2, 150): cơ sở để quyết định accepted (ĐG4)
CREATE TABLE IF NOT EXISTS quiz_answers (
    quiz_id       INTEGER NOT NULL REFERENCES quizzes(id),
    user_id       INTEGER NOT NULL REFERENCES users(id),
    answer_json   TEXT    NOT NULL,
    arrived_at    INTEGER NOT NULL,
    est_sent_at   INTEGER NOT NULL,
    srtt_ms       INTEGER NOT NULL,
    accepted      INTEGER NOT NULL CHECK (accepted IN (0, 1)),
    correct       INTEGER NOT NULL CHECK (correct IN (0, 1)),
    PRIMARY KEY (quiz_id, user_id)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    ts            INTEGER NOT NULL,
    actor         TEXT,                 -- username, hoặc địa chỉ IP khi chưa đăng nhập
    action        TEXT    NOT NULL,
    detail        TEXT
);

-- ---------------------------------------------------------------------
-- Bài học, tiến độ, đề biến thể, bài nộp — chủ: Người 3
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lessons (
    id            TEXT    PRIMARY KEY,  -- ví dụ dsa.sap-xep.noi-bot
    mon           TEXT    NOT NULL CHECK (mon IN ('dsa', 'sql')),
    chuong        TEXT    NOT NULL,
    ten           TEXT    NOT NULL,
    loai          TEXT    NOT NULL CHECK (loai IN ('giang-giai', 'bai-tap', 'luyen-tap')),
    path          TEXT    NOT NULL,     -- thư mục trong content/
    content_hash  TEXT    NOT NULL      -- đổi nội dung thì sinh lại đề biến thể và test ẩn
);

-- cooldown_until và wrong_submits lưu ở đây nên khởi động lại app hay đổi máy
-- cũng không thoát được thời gian chờ (spec §8.4)
CREATE TABLE IF NOT EXISTS progress (
    user_id       INTEGER NOT NULL REFERENCES users(id),
    lesson_id     TEXT    NOT NULL REFERENCES lessons(id),
    tier          TEXT    NOT NULL CHECK (tier IN ('xem', 'du-doan', 'mo-phong', 'code')),
    status        TEXT    NOT NULL CHECK (status IN ('LOCKED', 'OPEN', 'MASTERED')),
    streak        INTEGER NOT NULL DEFAULT 0,  -- số đề dự đoán đúng liên tiếp
    wrong_submits INTEGER NOT NULL DEFAULT 0,
    score         INTEGER,
    cooldown_until INTEGER,
    updated_at    INTEGER NOT NULL,
    PRIMARY KEY (user_id, lesson_id)
);

-- Mã đề dùng một lần: used_at khác NULL thì mọi câu trả lời kèm nonce này bị từ chối
CREATE TABLE IF NOT EXISTS variants (
    nonce         TEXT    PRIMARY KEY,  -- 128 bit, dạng hex
    user_id       INTEGER NOT NULL REFERENCES users(id),
    lesson_id     TEXT    NOT NULL REFERENCES lessons(id),
    kind          TEXT    NOT NULL CHECK (kind IN ('PREDICT', 'SIM')),
    question_json TEXT    NOT NULL,
    answer_json   TEXT    NOT NULL,     -- chỉ nằm trên server
    issued_at     INTEGER NOT NULL,
    expires_at    INTEGER NOT NULL,
    used_at       INTEGER
);

CREATE TABLE IF NOT EXISTS submissions (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER NOT NULL REFERENCES users(id),
    lesson_id     TEXT    NOT NULL REFERENCES lessons(id),
    code_seq      INTEGER NOT NULL,     -- chạy đúng phiên bản code đã được xác nhận (ĐG2)
    kind          TEXT    NOT NULL CHECK (kind IN ('RUN', 'SUBMIT', 'HINT')),
    verdict       TEXT    NOT NULL,     -- OK, WRONG, TIME_LIMIT, MEMORY_LIMIT, RUNTIME_ERROR, …
    detail_json   TEXT,
    score         INTEGER,
    created_at    INTEGER NOT NULL
);

-- ---------------------------------------------------------------------
-- Dòng code và trợ giúp — chủ: Người 1
-- ---------------------------------------------------------------------
-- Khoá chính (user_id, lesson_id, seq): gửi lại bao nhiêu lần cũng chỉ một dòng (ĐG2)
CREATE TABLE IF NOT EXISTS code_deltas (
    user_id       INTEGER NOT NULL REFERENCES users(id),
    lesson_id     TEXT    NOT NULL REFERENCES lessons(id),
    seq           INTEGER NOT NULL,
    pos           INTEGER NOT NULL,
    del_len       INTEGER NOT NULL,
    ins_text      TEXT    NOT NULL,
    client_ts     INTEGER NOT NULL,
    server_ts     INTEGER NOT NULL,
    PRIMARY KEY (user_id, lesson_id, seq)
);

CREATE TABLE IF NOT EXISTS checkpoints (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER NOT NULL REFERENCES users(id),
    lesson_id     TEXT    NOT NULL REFERENCES lessons(id),
    upto_seq      INTEGER NOT NULL,     -- code đầy đủ sau khi áp mọi delta tới seq này
    code          TEXT    NOT NULL,
    reason        TEXT    NOT NULL CHECK (reason IN ('RUN', 'SUBMIT', 'TIMER')),
    verdict       TEXT,
    created_at    INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS hand_raises (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER NOT NULL REFERENCES users(id),
    lesson_id     TEXT    NOT NULL REFERENCES lessons(id),
    raised_at     INTEGER NOT NULL,
    handled_by    INTEGER REFERENCES users(id),
    handled_at    INTEGER
);

CREATE TABLE IF NOT EXISTS comments (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id    INTEGER NOT NULL REFERENCES users(id),
    lesson_id     TEXT    NOT NULL REFERENCES lessons(id),
    line          INTEGER NOT NULL,
    author_id     INTEGER NOT NULL REFERENCES users(id),
    text          TEXT    NOT NULL,
    created_at    INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_checkpoints_latest ON checkpoints (user_id, lesson_id, upto_seq);
CREATE INDEX IF NOT EXISTS idx_submissions_user   ON submissions (user_id, lesson_id, created_at);
