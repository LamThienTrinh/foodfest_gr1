BEGIN;

ALTER TABLE comments
    ADD COLUMN IF NOT EXISTS parent_comment_id INT;

ALTER TABLE comments
    ADD COLUMN IF NOT EXISTS reply_count INT NOT NULL DEFAULT 0;

ALTER TABLE comments
    ADD COLUMN IF NOT EXISTS depth INT NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_comments_parent'
    ) THEN
        ALTER TABLE comments
            ADD CONSTRAINT fk_comments_parent
            FOREIGN KEY (parent_comment_id)
            REFERENCES comments(comment_id)
            ON DELETE CASCADE;
    END IF;
END $$;

UPDATE comments
SET depth = 0
WHERE depth IS NULL;

UPDATE comments
SET reply_count = 0
WHERE reply_count IS NULL;

UPDATE comments parent
SET reply_count = counts.reply_total
FROM (
    SELECT parent_comment_id, COUNT(*)::INT AS reply_total
    FROM comments
    WHERE parent_comment_id IS NOT NULL
    GROUP BY parent_comment_id
) counts
WHERE parent.comment_id = counts.parent_comment_id;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_comments_depth_2_levels'
    ) THEN
        ALTER TABLE comments
            ADD CONSTRAINT chk_comments_depth_2_levels
            CHECK (depth IN (0, 1));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_comments_parent_depth_consistency'
    ) THEN
        ALTER TABLE comments
            ADD CONSTRAINT chk_comments_parent_depth_consistency
            CHECK (
                (parent_comment_id IS NULL AND depth = 0)
                OR
                (parent_comment_id IS NOT NULL AND depth = 1)
            );
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_comments_post_parent_created_at
    ON comments(post_id, parent_comment_id, created_at);

CREATE INDEX IF NOT EXISTS idx_comments_parent_created_at
    ON comments(parent_comment_id, created_at);

COMMIT;
