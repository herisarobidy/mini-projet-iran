CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE articles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    image VARCHAR(500),
    author VARCHAR(255) NOT NULL,
    category_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_articles_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE INDEX idx_articles_category_id ON articles(category_id);
CREATE INDEX idx_articles_published ON articles(published);

CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL,
    author VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_comments_article FOREIGN KEY (article_id) REFERENCES articles(id)
);

CREATE INDEX idx_comments_article_id ON comments(article_id);
CREATE INDEX idx_comments_approved ON comments(approved);
