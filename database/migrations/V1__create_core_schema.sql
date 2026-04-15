BEGIN;

CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tags (
    tag_id SERIAL PRIMARY KEY,
    tag_name VARCHAR(50) NOT NULL UNIQUE,
    tag_type VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS dishes (
    dish_id SERIAL PRIMARY KEY,
    dish_name VARCHAR(100) NOT NULL,
    image_url TEXT,
    description TEXT,
    ingredients TEXT,
    instructions TEXT,
    prep_time INT,
    cook_time INT,
    serving INT
);

CREATE TABLE IF NOT EXISTS dish_tags (
    dish_id INT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (dish_id, tag_id),
    CONSTRAINT fk_dish_tags_dish
        FOREIGN KEY (dish_id) REFERENCES dishes(dish_id) ON DELETE CASCADE,
    CONSTRAINT fk_dish_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_dish_tags_tag_id ON dish_tags(tag_id);

CREATE TABLE IF NOT EXISTS personal_dishes (
    personal_dish_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    original_dish_id INT,
    dish_name VARCHAR(100) NOT NULL,
    image_url TEXT,
    description TEXT,
    ingredients TEXT,
    instructions TEXT,
    prep_time INT,
    cook_time INT,
    serving INT,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_personal_dishes_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_personal_dishes_original
        FOREIGN KEY (original_dish_id) REFERENCES dishes(dish_id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_personal_dishes_user_created_at
    ON personal_dishes(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_personal_dishes_original_dish_id
    ON personal_dishes(original_dish_id);

CREATE TABLE IF NOT EXISTS personal_dish_tags (
    personal_dish_id INT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (personal_dish_id, tag_id),
    CONSTRAINT fk_personal_dish_tags_personal_dish
        FOREIGN KEY (personal_dish_id) REFERENCES personal_dishes(personal_dish_id) ON DELETE CASCADE,
    CONSTRAINT fk_personal_dish_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_personal_dish_tags_tag_id
    ON personal_dish_tags(tag_id);

CREATE TABLE IF NOT EXISTS posts (
    post_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    post_type VARCHAR(20) NOT NULL,
    title VARCHAR(200),
    content TEXT,
    image_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_posts_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_posts_user_created_at
    ON posts(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_posts_created_at
    ON posts(created_at DESC);

CREATE TABLE IF NOT EXISTS saved_posts (
    user_id INT NOT NULL,
    post_id INT NOT NULL,
    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id),
    CONSTRAINT fk_saved_posts_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_posts_post
        FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_saved_posts_post_id ON saved_posts(post_id);
CREATE INDEX IF NOT EXISTS idx_saved_posts_saved_at ON saved_posts(saved_at DESC);

CREATE TABLE IF NOT EXISTS favorite_dishes (
    user_id INT NOT NULL,
    dish_id INT NOT NULL,
    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, dish_id),
    CONSTRAINT fk_favorite_dishes_user
        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_dishes_dish
        FOREIGN KEY (dish_id) REFERENCES dishes(dish_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_favorite_dishes_dish_id ON favorite_dishes(dish_id);
CREATE INDEX IF NOT EXISTS idx_favorite_dishes_saved_at ON favorite_dishes(saved_at DESC);

COMMIT;
