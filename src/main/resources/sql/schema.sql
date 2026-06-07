-- pixtagarc データベーススキーマ
-- SQLite 3.x 対応

PRAGMA foreign_keys = ON;

-- 作者テーブル
CREATE TABLE IF NOT EXISTS authors (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT    NOT NULL UNIQUE
);

-- タグテーブル
CREATE TABLE IF NOT EXISTS tags (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT    NOT NULL UNIQUE,
    star INTEGER NOT NULL DEFAULT 0
);

-- 作品テーブル
CREATE TABLE IF NOT EXISTS works (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    title       TEXT    NOT NULL,
    author_id   INTEGER REFERENCES authors(id) ON DELETE SET NULL,
    external_id TEXT,
    total_pages INTEGER,
    created_at  TEXT    NOT NULL,
    updated_at  TEXT    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_works_title ON works(title);
CREATE INDEX IF NOT EXISTS idx_works_author_id ON works(author_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_works_external_id ON works(external_id);

-- 画像テーブル
CREATE TABLE IF NOT EXISTS images (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    file_path   TEXT    NOT NULL UNIQUE,
    file_name   TEXT    NOT NULL,
    file_size   INTEGER,
    width       INTEGER,
    height      INTEGER,
    media_type  TEXT    NOT NULL,
    author_id   INTEGER REFERENCES authors(id) ON DELETE SET NULL,
    work_id     INTEGER REFERENCES works(id) ON DELETE SET NULL,
    page_number INTEGER,
    star        INTEGER NOT NULL DEFAULT 0,
    is_hidden   INTEGER NOT NULL DEFAULT 0,
    created_at  TEXT    NOT NULL,
    imported_at TEXT    NOT NULL
);

-- 画像-タグ中間テーブル
CREATE TABLE IF NOT EXISTS image_tags (
    image_id INTEGER NOT NULL REFERENCES images(id) ON DELETE CASCADE,
    tag_id   INTEGER NOT NULL REFERENCES tags(id)   ON DELETE CASCADE,
    PRIMARY KEY (image_id, tag_id)
);

-- 全文検索用仮想テーブル（FTS5）
CREATE VIRTUAL TABLE IF NOT EXISTS image_fts USING fts5 (
    file_name,
    tags_text,
    author_name,
    content='images',
    content_rowid='id'
);

-- 保存済み検索条件テーブル
CREATE TABLE IF NOT EXISTS saved_searches (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    name           TEXT    NOT NULL UNIQUE,
    keyword        TEXT,
    tag_ids        TEXT,
    author_ids     TEXT,
    date_from      TEXT,
    date_to        TEXT,
    exclude_hidden INTEGER NOT NULL DEFAULT 1,
    min_star       INTEGER NOT NULL DEFAULT 0,
    sort_column    TEXT    NOT NULL DEFAULT 'created_at',
    sort_order     TEXT    NOT NULL DEFAULT 'DESC',
    created_at     TEXT    NOT NULL,
    updated_at     TEXT    NOT NULL
);

-- インデックス
CREATE INDEX IF NOT EXISTS idx_images_file_name  ON images(file_name);
CREATE INDEX IF NOT EXISTS idx_images_media_type ON images(media_type);
CREATE INDEX IF NOT EXISTS idx_images_created_at ON images(created_at);
CREATE INDEX IF NOT EXISTS idx_images_author_id  ON images(author_id);
CREATE INDEX IF NOT EXISTS idx_images_is_hidden  ON images(is_hidden);
CREATE INDEX IF NOT EXISTS idx_images_work_id    ON images(work_id);
CREATE INDEX IF NOT EXISTS idx_images_work_page  ON images(work_id, page_number);
CREATE INDEX IF NOT EXISTS idx_images_star       ON images(star);
CREATE INDEX IF NOT EXISTS idx_image_tags_tag_id ON image_tags(tag_id);
