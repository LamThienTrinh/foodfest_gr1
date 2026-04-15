BEGIN;

CREATE TABLE IF NOT EXISTS follows (
    follower_id INT NOT NULL,
    following_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, following_id),
    CONSTRAINT fk_follows_follower
        FOREIGN KEY (follower_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_following
        FOREIGN KEY (following_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_follows_not_self
        CHECK (follower_id <> following_id)
);

CREATE INDEX IF NOT EXISTS idx_follows_follower_id ON follows(follower_id);
CREATE INDEX IF NOT EXISTS idx_follows_following_id ON follows(following_id);
CREATE INDEX IF NOT EXISTS idx_follows_created_at ON follows(created_at DESC);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS follower_count INT NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS following_count INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS post_likes (
    user_id INT NOT NULL,
    post_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id),
    CONSTRAINT fk_post_likes_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_post_likes_post
        FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_post_likes_post_id ON post_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_post_likes_created_at ON post_likes(created_at DESC);

CREATE TABLE IF NOT EXISTS comments (
    comment_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    post_id INT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_post
        FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_comments_post_created_at ON comments(post_id, created_at ASC);
CREATE INDEX IF NOT EXISTS idx_comments_user_created_at ON comments(user_id, created_at DESC);

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS like_count INT NOT NULL DEFAULT 0;

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS comment_count INT NOT NULL DEFAULT 0;

UPDATE users u
SET follower_count = f.cnt
FROM (
    SELECT following_id, COUNT(*)::INT AS cnt
    FROM follows
    GROUP BY following_id
) f
WHERE u.user_id = f.following_id;

UPDATE users
SET follower_count = 0
WHERE follower_count IS NULL;

UPDATE users u
SET following_count = f.cnt
FROM (
    SELECT follower_id, COUNT(*)::INT AS cnt
    FROM follows
    GROUP BY follower_id
) f
WHERE u.user_id = f.follower_id;

UPDATE users
SET following_count = 0
WHERE following_count IS NULL;

UPDATE posts p
SET like_count = l.cnt
FROM (
    SELECT post_id, COUNT(*)::INT AS cnt
    FROM post_likes
    GROUP BY post_id
) l
WHERE p.post_id = l.post_id;

UPDATE posts
SET like_count = 0
WHERE like_count IS NULL;

UPDATE posts p
SET comment_count = c.cnt
FROM (
    SELECT post_id, COUNT(*)::INT AS cnt
    FROM comments
    GROUP BY post_id
) c
WHERE p.post_id = c.post_id;

UPDATE posts
SET comment_count = 0
WHERE comment_count IS NULL;

COMMIT;
