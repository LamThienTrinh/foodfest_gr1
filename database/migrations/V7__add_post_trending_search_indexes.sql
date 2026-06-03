BEGIN;

-- Tối ưu sắp xếp feed ALL theo xu hướng + thời gian.
CREATE INDEX IF NOT EXISTS idx_posts_like_comment_score
    ON posts(like_count DESC, comment_count DESC, created_at DESC, post_id DESC);

CREATE INDEX IF NOT EXISTS idx_posts_created_at_post_id
    ON posts(created_at DESC, post_id DESC);

-- Tối ưu tìm kiếm bài theo title/content.
CREATE INDEX IF NOT EXISTS idx_posts_title_lower
    ON posts ((lower(title)));

CREATE INDEX IF NOT EXISTS idx_posts_content_lower
    ON posts ((lower(content)));

-- Bổ sung index user-search nếu DB chưa có từ migration trước.
CREATE INDEX IF NOT EXISTS idx_users_username_lower
    ON users ((lower(username)));

CREATE INDEX IF NOT EXISTS idx_users_full_name_lower
    ON users ((lower(full_name)));

COMMIT;
