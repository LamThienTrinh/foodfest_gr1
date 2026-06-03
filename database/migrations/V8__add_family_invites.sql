BEGIN;

CREATE TABLE IF NOT EXISTS family_invites (
    family_invite_id SERIAL PRIMARY KEY,
    family_id INT NOT NULL,
    invited_user_id INT NOT NULL,
    invited_by_user_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    CONSTRAINT fk_family_invites_family
        FOREIGN KEY (family_id) REFERENCES family_groups(family_id) ON DELETE CASCADE,
    CONSTRAINT fk_family_invites_invited_user
        FOREIGN KEY (invited_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_family_invites_invited_by
        FOREIGN KEY (invited_by_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_family_invites_status
        CHECK (status IN ('pending', 'accepted', 'declined'))
);

CREATE INDEX IF NOT EXISTS idx_family_invites_invited_user_status
    ON family_invites(invited_user_id, status);

CREATE INDEX IF NOT EXISTS idx_family_invites_family_id
    ON family_invites(family_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_family_invites_pending
    ON family_invites(family_id, invited_user_id)
    WHERE status = 'pending';

COMMIT;
