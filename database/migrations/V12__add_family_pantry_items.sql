BEGIN;

CREATE TABLE IF NOT EXISTS family_pantry_items (
    family_pantry_item_id SERIAL PRIMARY KEY,
    family_id INT NOT NULL,
    ingredient_name VARCHAR(120) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    unit VARCHAR(30),
    expiry_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_family_pantry_items_family
        FOREIGN KEY (family_id) REFERENCES family_groups(family_id) ON DELETE CASCADE,
    CONSTRAINT chk_family_pantry_item_name_not_blank
        CHECK (length(trim(ingredient_name)) > 0),
    CONSTRAINT chk_family_pantry_item_quantity_positive
        CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_family_pantry_family_id
    ON family_pantry_items(family_id);

CREATE INDEX IF NOT EXISTS idx_family_pantry_expiry
    ON family_pantry_items(family_id, expiry_date);

COMMIT;
