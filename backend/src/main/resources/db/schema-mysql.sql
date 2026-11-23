-- MySQL 8.4. Text timestamps use the same format as SQLite.
-- Define indexes inside CREATE TABLE so the script can be rerun on MySQL.
CREATE TABLE IF NOT EXISTS blog_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    nickname TEXT NOT NULL,
    avatar TEXT,
    email TEXT,
    created_at VARCHAR(19) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_category (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug TEXT NOT NULL,
    description TEXT,
    sort_order INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_tag (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    color VARCHAR(32) NOT NULL DEFAULT 'blue'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_article (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title TEXT NOT NULL,
    summary TEXT,
    content LONGTEXT NOT NULL,
    cover TEXT,
    category_id BIGINT,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    pinned INT NOT NULL DEFAULT 0,
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at VARCHAR(19) NOT NULL,
    updated_at VARCHAR(19) NOT NULL,
    published_at VARCHAR(19),
    INDEX idx_article_status_published (status, published_at DESC),
    INDEX idx_article_category (category_id),
    FOREIGN KEY (category_id) REFERENCES blog_category (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_article_tag (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    FOREIGN KEY (article_id) REFERENCES blog_article (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES blog_tag (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_comment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    author TEXT NOT NULL,
    email TEXT,
    website TEXT,
    content TEXT NOT NULL,
    ip TEXT,
    status VARCHAR(16) NOT NULL DEFAULT 'APPROVED',
    created_at VARCHAR(19) NOT NULL,
    INDEX idx_comment_article_status (article_id, status),
    FOREIGN KEY (article_id) REFERENCES blog_article (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_site_setting (
    id BIGINT PRIMARY KEY,
    icp_number TEXT,
    police_number TEXT,
    police_url TEXT,
    updated_at VARCHAR(19)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_typewriter_config (
    id BIGINT PRIMARY KEY CHECK (id = 1),
    entries MEDIUMTEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_site_presentation (
    id BIGINT PRIMARY KEY CHECK (id = 1),
    typewriter_text TEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_attachment (
    id VARCHAR(36) PRIMARY KEY,
    name TEXT NOT NULL,
    media_type VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    created_at VARCHAR(19) NOT NULL,
    file_path VARCHAR(255) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_article_attachment (
    article_id BIGINT NOT NULL,
    attachment_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (article_id, attachment_id),
    INDEX idx_article_attachment_file (attachment_id),
    FOREIGN KEY (article_id) REFERENCES blog_article (id) ON DELETE CASCADE,
    FOREIGN KEY (attachment_id) REFERENCES blog_attachment (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_site_identity (
    id BIGINT PRIMARY KEY CHECK (id = 1),
    name TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_site_about (
    id BIGINT PRIMARY KEY CHECK (id = 1),
    markdown MEDIUMTEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_background_music (
    id VARCHAR(36) PRIMARY KEY,
    name TEXT NOT NULL,
    sort_order INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE IF NOT EXISTS blog_site_avatar (
    id BIGINT PRIMARY KEY CHECK (id = 1),
    location TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
