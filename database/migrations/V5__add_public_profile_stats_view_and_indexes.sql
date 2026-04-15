BEGIN;

-- Support case-insensitive user lookup for profile/search flows.
CREATE INDEX IF NOT EXISTS idx_users_username_lower
    ON users ((lower(username)));

CREATE INDEX IF NOT EXISTS idx_users_full_name_lower
    ON users ((lower(full_name)));

-- Aggregate stats used by public user profile header.
CREATE OR REPLACE VIEW v_user_profile_stats AS
SELECT
    u.user_id,
    u.username,
    u.full_name,
    u.avatar_url,
    u.follower_count,
    u.following_count,
    COALESCE(p_stats.post_count, 0)::INT AS post_count,
    COALESCE(p_stats.total_received_likes, 0)::INT AS total_received_likes
FROM users u
LEFT JOIN (
    SELECT
        p.user_id,
        COUNT(p.post_id)::INT AS post_count,
        COALESCE(SUM(p.like_count), 0)::INT AS total_received_likes
    FROM posts p
    GROUP BY p.user_id
) p_stats ON p_stats.user_id = u.user_id;

COMMIT;
