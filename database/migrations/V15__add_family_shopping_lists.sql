CREATE TABLE IF NOT EXISTS family_shopping_lists (
    family_shopping_list_id SERIAL PRIMARY KEY,
    family_id INTEGER NOT NULL,
    menu_week DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_family_shopping_lists_family
        FOREIGN KEY (family_id) REFERENCES family_groups(family_id) ON DELETE CASCADE,
    CONSTRAINT uq_family_shopping_lists_family_week
        UNIQUE (family_id, menu_week)
);

CREATE TABLE IF NOT EXISTS family_shopping_list_items (
    family_shopping_list_item_id SERIAL PRIMARY KEY,
    family_shopping_list_id INTEGER NOT NULL,
    ingredient_name VARCHAR(120) NOT NULL,
    required_qty DOUBLE PRECISION NOT NULL DEFAULT 1,
    unit VARCHAR(30),
    category VARCHAR(40) NOT NULL DEFAULT 'Khác',
    note TEXT,
    is_purchased BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_to_user_id INTEGER,
    used_qty DOUBLE PRECISION,
    purchased_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_family_shopping_list_items_list
        FOREIGN KEY (family_shopping_list_id)
        REFERENCES family_shopping_lists(family_shopping_list_id) ON DELETE CASCADE,
    CONSTRAINT fk_family_shopping_list_items_assigned_user
        FOREIGN KEY (assigned_to_user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT chk_family_shopping_list_item_name_not_blank
        CHECK (length(trim(ingredient_name)) > 0),
    CONSTRAINT chk_family_shopping_list_item_qty_positive
        CHECK (required_qty > 0),
    CONSTRAINT chk_family_shopping_list_item_used_qty_non_negative
        CHECK (used_qty IS NULL OR used_qty >= 0)
);

CREATE TABLE IF NOT EXISTS family_shopping_list_activity (
    family_shopping_list_activity_id SERIAL PRIMARY KEY,
    family_shopping_list_id INTEGER NOT NULL,
    family_shopping_list_item_id INTEGER,
    actor_user_id INTEGER NOT NULL,
    action VARCHAR(40) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_family_shopping_list_activity_list
        FOREIGN KEY (family_shopping_list_id)
        REFERENCES family_shopping_lists(family_shopping_list_id) ON DELETE CASCADE,
    CONSTRAINT fk_family_shopping_list_activity_item
        FOREIGN KEY (family_shopping_list_item_id)
        REFERENCES family_shopping_list_items(family_shopping_list_item_id) ON DELETE SET NULL,
    CONSTRAINT fk_family_shopping_list_activity_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_family_shopping_lists_family_week
    ON family_shopping_lists(family_id, menu_week);

CREATE INDEX IF NOT EXISTS idx_family_shopping_list_items_list
    ON family_shopping_list_items(family_shopping_list_id);

CREATE INDEX IF NOT EXISTS idx_family_shopping_list_activity_list
    ON family_shopping_list_activity(family_shopping_list_id, created_at DESC);
