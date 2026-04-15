-- =============================================
-- FOLLOWER FEATURE
-- =============================================

-- NOTE:
-- This script is legacy and kept for reference only.
-- Use database/migrations/V2__add_social_tables_and_backfill.sql
-- in the new versioned migration flow.

-- Bảng quan hệ follow: ai follow ai + thời điểm follow
CREATE TABLE IF NOT EXISTS follows (
	follower_id INT NOT NULL,
	following_id INT NOT NULL,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

	PRIMARY KEY (follower_id, following_id),

	CONSTRAINT fk_follows_follower
		FOREIGN KEY (follower_id) REFERENCES users(user_id) ON DELETE CASCADE,
	CONSTRAINT fk_follows_following
		FOREIGN KEY (following_id) REFERENCES users(user_id) ON DELETE CASCADE,
	CONSTRAINT chk_follows_not_self
		CHECK (follower_id <> following_id)
);

-- Index hỗ trợ truy vấn nhanh số follower theo user/tháng
CREATE INDEX IF NOT EXISTS idx_follows_following_id ON follows (following_id);
CREATE INDEX IF NOT EXISTS idx_follows_created_at ON follows (created_at);

-- Tổng follower trên bảng users
ALTER TABLE users
	ADD COLUMN IF NOT EXISTS follower_count INT NOT NULL DEFAULT 0;

-- (Tuỳ chọn) Tổng số người đang follow
ALTER TABLE users
	ADD COLUMN IF NOT EXISTS following_count INT NOT NULL DEFAULT 0;

-- Đồng bộ follower_count từ dữ liệu follows hiện có
UPDATE users u
SET follower_count = f.cnt
FROM (
	SELECT following_id, COUNT(*)::INT AS cnt
	FROM follows
	GROUP BY following_id
) f
WHERE u.user_id = f.following_id;

-- Gán 0 cho user chưa có follower
UPDATE users
SET follower_count = 0
WHERE follower_count IS NULL;

-- Đồng bộ following_count từ dữ liệu follows hiện có
UPDATE users u
SET following_count = f.cnt
FROM (
	SELECT follower_id, COUNT(*)::INT AS cnt
	FROM follows
	GROUP BY follower_id
) f
WHERE u.user_id = f.follower_id;

-- Gán 0 cho user chưa follow ai
UPDATE users
SET following_count = 0
WHERE following_count IS NULL;

-- Hàm trigger: tăng count khi có follow mới
CREATE OR REPLACE FUNCTION trg_follows_after_insert()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
	UPDATE users
	SET follower_count = follower_count + 1
	WHERE user_id = NEW.following_id;

	UPDATE users
	SET following_count = following_count + 1
	WHERE user_id = NEW.follower_id;

	RETURN NEW;
END;
$$;

-- Hàm trigger: giảm count khi unfollow
CREATE OR REPLACE FUNCTION trg_follows_after_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
	UPDATE users
	SET follower_count = GREATEST(follower_count - 1, 0)
	WHERE user_id = OLD.following_id;

	UPDATE users
	SET following_count = GREATEST(following_count - 1, 0)
	WHERE user_id = OLD.follower_id;

	RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS follows_after_insert ON follows;
CREATE TRIGGER follows_after_insert
AFTER INSERT ON follows
FOR EACH ROW
EXECUTE FUNCTION trg_follows_after_insert();

DROP TRIGGER IF EXISTS follows_after_delete ON follows;
CREATE TRIGGER follows_after_delete
AFTER DELETE ON follows
FOR EACH ROW
EXECUTE FUNCTION trg_follows_after_delete();
