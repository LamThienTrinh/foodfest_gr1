-- Phase 3.2: per-family nickname for members.
ALTER TABLE family_members
    ADD COLUMN IF NOT EXISTS nickname VARCHAR(60);
