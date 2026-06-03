BEGIN;

CREATE TABLE IF NOT EXISTS family_saved_meals (
    family_saved_meal_id SERIAL PRIMARY KEY,
    family_id INT NOT NULL,
    preset_name VARCHAR(120) NOT NULL,
    created_by_user_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_family_saved_meals_family
        FOREIGN KEY (family_id) REFERENCES family_groups(family_id) ON DELETE CASCADE,
    CONSTRAINT fk_family_saved_meals_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_family_saved_meals_name_not_blank
        CHECK (length(trim(preset_name)) > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_family_saved_meals_name
    ON family_saved_meals(family_id, preset_name);

CREATE INDEX IF NOT EXISTS idx_family_saved_meals_family_id
    ON family_saved_meals(family_id);

CREATE TABLE IF NOT EXISTS family_saved_meal_items (
    family_saved_meal_item_id SERIAL PRIMARY KEY,
    family_saved_meal_id INT NOT NULL,
    dish_id INT,
    personal_dish_id INT,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_family_saved_meal_items_saved_meal
        FOREIGN KEY (family_saved_meal_id) REFERENCES family_saved_meals(family_saved_meal_id) ON DELETE CASCADE,
    CONSTRAINT fk_family_saved_meal_items_dish
        FOREIGN KEY (dish_id) REFERENCES dishes(dish_id) ON DELETE SET NULL,
    CONSTRAINT fk_family_saved_meal_items_personal_dish
        FOREIGN KEY (personal_dish_id) REFERENCES personal_dishes(personal_dish_id) ON DELETE SET NULL,
    CONSTRAINT chk_family_saved_meal_items_source
        CHECK (
            (dish_id IS NOT NULL AND personal_dish_id IS NULL)
            OR
            (dish_id IS NULL AND personal_dish_id IS NOT NULL)
        )
);

CREATE INDEX IF NOT EXISTS idx_family_saved_meal_items_saved_meal_id
    ON family_saved_meal_items(family_saved_meal_id);

COMMIT;
