-- 个人博客 SQLite schema（幂等：全部使用 IF NOT EXISTS）
-- 时间字段统一存 'yyyy-MM-dd HH:mm:ss' 文本，便于 SQLite 直接比较与排序。

CREATE TABLE IF NOT EXISTS blog_user (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL,
    nickname      TEXT    NOT NULL,
    avatar        TEXT,
    email         TEXT,
    created_at    TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS blog_category (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL UNIQUE,
    slug        TEXT    NOT NULL,
    description TEXT,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS blog_tag (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name  TEXT NOT NULL UNIQUE,
    color TEXT NOT NULL DEFAULT 'blue'
);

CREATE TABLE IF NOT EXISTS blog_article (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    title        TEXT    NOT NULL,
    summary      TEXT,
    content      TEXT    NOT NULL,
    cover        TEXT,
    category_id  INTEGER,
    status       TEXT    NOT NULL DEFAULT 'DRAFT',
    pinned       INTEGER NOT NULL DEFAULT 0,
    view_count   INTEGER NOT NULL DEFAULT 0,
    created_at   TEXT    NOT NULL,
    updated_at   TEXT    NOT NULL,
    published_at TEXT,
    FOREIGN KEY (category_id) REFERENCES blog_category (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_article_status_published ON blog_article (status, published_at DESC);
CREATE INDEX IF NOT EXISTS idx_article_category ON blog_article (category_id);

CREATE TABLE IF NOT EXISTS blog_article_tag (
    article_id INTEGER NOT NULL,
    tag_id     INTEGER NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    FOREIGN KEY (article_id) REFERENCES blog_article (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES blog_tag (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS blog_comment (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    article_id INTEGER NOT NULL,
    author     TEXT    NOT NULL,
    email      TEXT,
    website    TEXT,
    content    TEXT    NOT NULL,
    ip         TEXT,
    status     TEXT    NOT NULL DEFAULT 'APPROVED',
    created_at TEXT    NOT NULL,
    FOREIGN KEY (article_id) REFERENCES blog_article (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_comment_article_status ON blog_comment (article_id, status);

-- 站点级运营信息（单行表，固定 id=1）：备案号属于运营数据，改值不需要重启。
CREATE TABLE IF NOT EXISTS blog_site_setting (
    id            INTEGER PRIMARY KEY,
    icp_number    TEXT,
    police_number TEXT,
    police_url    TEXT,
    updated_at    TEXT
);

CREATE TABLE IF NOT EXISTS blog_typewriter_config (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    entries TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS blog_site_presentation (
    id              INTEGER PRIMARY KEY CHECK (id = 1),
    typewriter_text TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS blog_attachment (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    media_type TEXT NOT NULL,
    size INTEGER NOT NULL,
    created_at TEXT NOT NULL,
    file_path TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS blog_article_attachment (
    article_id INTEGER NOT NULL REFERENCES blog_article(id) ON DELETE CASCADE,
    attachment_id TEXT NOT NULL REFERENCES blog_attachment(id) ON DELETE RESTRICT,
    PRIMARY KEY (article_id, attachment_id)
);

CREATE INDEX IF NOT EXISTS idx_article_attachment_file ON blog_article_attachment(attachment_id);

CREATE TABLE IF NOT EXISTS blog_site_identity (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    name TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL
);

-- 关于页正文独立存储；未配置时不需要种子记录。
CREATE TABLE IF NOT EXISTS blog_site_about (
    id       INTEGER PRIMARY KEY CHECK (id = 1),
    markdown TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS blog_background_music (
    id         TEXT    NOT NULL PRIMARY KEY,
    name       TEXT    NOT NULL,
    sort_order INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS blog_site_avatar (
    id         INTEGER PRIMARY KEY CHECK (id = 1),
    location   TEXT
);
