BEGIN;

CREATE TABLE IF NOT EXISTS family_groups (
    family_id SERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    owner_user_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_family_groups_owner
        FOREIGN KEY (owner_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_family_groups_name_not_blank
        CHECK (length(trim(name)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_family_groups_owner_user_id
    ON family_groups(owner_user_id);

CREATE TABLE IF NOT EXISTS family_members (
    family_id INT NOT NULL,
    user_id INT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'member',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (family_id, user_id),
    CONSTRAINT fk_family_members_family
        FOREIGN KEY (family_id) REFERENCES family_groups(family_id) ON DELETE CASCADE,
    CONSTRAINT fk_family_members_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_family_members_role
        CHECK (role IN ('owner', 'member'))
);

CREATE INDEX IF NOT EXISTS idx_family_members_user_id
    ON family_members(user_id);

CREATE INDEX IF NOT EXISTS idx_family_members_family_role
    ON family_members(family_id, role);

CREATE TABLE IF NOT EXISTS family_menus (
    family_menu_id SERIAL PRIMARY KEY,
    family_id INT NOT NULL,
    menu_date DATE NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_family_menus_family
        FOREIGN KEY (family_id) REFERENCES family_groups(family_id) ON DELETE CASCADE,
    CONSTRAINT chk_family_menus_meal_type
        CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack', 'other')),
    CONSTRAINT chk_family_menus_status
        CHECK (status IN ('draft', 'voting', 'finalized', 'archived'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_family_menus_slot
    ON family_menus(family_id, menu_date, meal_type);

CREATE INDEX IF NOT EXISTS idx_family_menus_family_date
    ON family_menus(family_id, menu_date);

CREATE INDEX IF NOT EXISTS idx_family_menus_family_status
    ON family_menus(family_id, status);

CREATE TABLE IF NOT EXISTS family_menu_items (
    family_menu_item_id SERIAL PRIMARY KEY,
    family_menu_id INT NOT NULL,
    dish_id INT,
    personal_dish_id INT,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_family_menu_items_menu
        FOREIGN KEY (family_menu_id) REFERENCES family_menus(family_menu_id) ON DELETE CASCADE,
    CONSTRAINT fk_family_menu_items_dish
        FOREIGN KEY (dish_id) REFERENCES dishes(dish_id) ON DELETE SET NULL,
    CONSTRAINT fk_family_menu_items_personal_dish
        FOREIGN KEY (personal_dish_id) REFERENCES personal_dishes(personal_dish_id) ON DELETE SET NULL,
    CONSTRAINT chk_family_menu_items_source
        CHECK (
            (dish_id IS NOT NULL AND personal_dish_id IS NULL)
            OR
            (dish_id IS NULL AND personal_dish_id IS NOT NULL)
        )
);

CREATE INDEX IF NOT EXISTS idx_family_menu_items_menu_id
    ON family_menu_items(family_menu_id);

CREATE INDEX IF NOT EXISTS idx_family_menu_items_dish_id
    ON family_menu_items(dish_id);

CREATE INDEX IF NOT EXISTS idx_family_menu_items_personal_dish_id
    ON family_menu_items(personal_dish_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_family_menu_items_dish_per_menu
    ON family_menu_items(family_menu_id, dish_id)
    WHERE dish_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_family_menu_items_personal_dish_per_menu
    ON family_menu_items(family_menu_id, personal_dish_id)
    WHERE personal_dish_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS family_menu_votes (
    family_menu_item_id INT NOT NULL,
    user_id INT NOT NULL,
    vote_type VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (family_menu_item_id, user_id),
    CONSTRAINT fk_family_menu_votes_item
        FOREIGN KEY (family_menu_item_id) REFERENCES family_menu_items(family_menu_item_id) ON DELETE CASCADE,
    CONSTRAINT fk_family_menu_votes_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT chk_family_menu_votes_vote_type
        CHECK (vote_type IN ('up', 'down'))
);

CREATE INDEX IF NOT EXISTS idx_family_menu_votes_user_id
    ON family_menu_votes(user_id);

-- Keep family owner present in family_members with role = owner.
CREATE OR REPLACE FUNCTION trg_family_groups_add_owner_member()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO family_members (family_id, user_id, role, joined_at)
    VALUES (NEW.family_id, NEW.owner_user_id, 'owner', CURRENT_TIMESTAMP)
    ON CONFLICT (family_id, user_id)
    DO UPDATE SET role = 'owner';

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS family_groups_add_owner_member ON family_groups;
CREATE TRIGGER family_groups_add_owner_member
AFTER INSERT ON family_groups
FOR EACH ROW
EXECUTE FUNCTION trg_family_groups_add_owner_member();

-- Keep member roles in sync if owner_user_id changes.
CREATE OR REPLACE FUNCTION trg_family_groups_sync_owner_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.owner_user_id <> OLD.owner_user_id THEN
        UPDATE family_members
        SET role = 'member'
        WHERE family_id = NEW.family_id
          AND user_id = OLD.owner_user_id;

        INSERT INTO family_members (family_id, user_id, role, joined_at)
        VALUES (NEW.family_id, NEW.owner_user_id, 'owner', CURRENT_TIMESTAMP)
        ON CONFLICT (family_id, user_id)
        DO UPDATE SET role = 'owner';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS family_groups_sync_owner_change ON family_groups;
CREATE TRIGGER family_groups_sync_owner_change
AFTER UPDATE OF owner_user_id ON family_groups
FOR EACH ROW
EXECUTE FUNCTION trg_family_groups_sync_owner_change();

-- Protect owner membership row from being removed or demoted.
CREATE OR REPLACE FUNCTION trg_family_members_protect_owner()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    owner_id INT;
BEGIN
    SELECT owner_user_id
    INTO owner_id
    FROM family_groups
    WHERE family_id = COALESCE(NEW.family_id, OLD.family_id);

    IF owner_id IS NULL THEN
        RETURN COALESCE(NEW, OLD);
    END IF;

    IF TG_OP = 'DELETE' THEN
        IF OLD.user_id = owner_id THEN
            RAISE EXCEPTION 'Cannot remove the owner from family_members';
        END IF;
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        IF OLD.user_id = owner_id AND NEW.role <> 'owner' THEN
            RAISE EXCEPTION 'Cannot demote owner role in family_members';
        END IF;
        RETURN NEW;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$;

DROP TRIGGER IF EXISTS family_members_protect_owner_delete ON family_members;
CREATE TRIGGER family_members_protect_owner_delete
BEFORE DELETE ON family_members
FOR EACH ROW
EXECUTE FUNCTION trg_family_members_protect_owner();

DROP TRIGGER IF EXISTS family_members_protect_owner_update ON family_members;
CREATE TRIGGER family_members_protect_owner_update
BEFORE UPDATE OF role ON family_members
FOR EACH ROW
EXECUTE FUNCTION trg_family_members_protect_owner();

COMMIT;