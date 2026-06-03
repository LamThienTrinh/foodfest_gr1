BEGIN;

ALTER TABLE family_menus
    ADD COLUMN IF NOT EXISTS is_saved BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_family_menus_is_saved
    ON family_menus(family_id, is_saved);

COMMIT;
