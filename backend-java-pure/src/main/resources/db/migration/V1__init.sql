CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS articles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    image VARCHAR(1024),
    author VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    category_id BIGINT,
    CONSTRAINT fk_articles_category
        FOREIGN KEY (category_id)
            REFERENCES categories(id)
            ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_articles_published_created_at ON articles (published, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_articles_category_id ON articles (category_id);

CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    author VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_comments_article
        FOREIGN KEY (article_id)
            REFERENCES articles(id)
            ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_comments_article_id_approved_created_at ON comments (article_id, approved, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comments_approved_created_at ON comments (approved, created_at DESC);
