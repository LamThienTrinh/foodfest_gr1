BEGIN;

CREATE UNIQUE INDEX IF NOT EXISTS uq_family_groups_name
    ON family_groups(name);

COMMIT;
