CREATE TABLE IF NOT EXISTS family_notes (
    family_note_id SERIAL PRIMARY KEY,
    family_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_family_notes_family
        FOREIGN KEY (family_id) REFERENCES family_groups(family_id) ON DELETE CASCADE,
    CONSTRAINT fk_family_notes_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_family_notes_message_not_blank
        CHECK (length(trim(message)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_family_notes_family_created_at
    ON family_notes(family_id, created_at DESC);
