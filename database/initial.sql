-- Active: 1763946630811@@127.0.0.1@5432@foodfest

-- =============================================
-- 1. USER & SYSTEM DATA (Dữ liệu gốc)
-- =============================================

DROP TABLE IF EXISTS saved_posts CASCADE;
DROP TABLE IF EXISTS personal_dishes CASCADE;
DROP TABLE IF EXISTS dish_tags CASCADE;
DROP TABLE IF EXISTS posts CASCADE;
DROP TABLE IF EXISTS personal_dish_tags CASCADE;
DROP TABLE IF EXISTS dishes CASCADE;
DROP TABLE IF EXISTS tags CASCADE;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    avatar_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tags (
    tag_id SERIAL PRIMARY KEY,
    tag_name VARCHAR(50) NOT NULL UNIQUE,
    tag_type VARCHAR(50)
);

CREATE TABLE dishes (
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


CREATE TABLE dish_tags (
    dish_id INT,
    tag_id INT,
    PRIMARY KEY (dish_id, tag_id),
    CONSTRAINT fk_dt_dish FOREIGN KEY (dish_id) REFERENCES dishes(dish_id) ON DELETE CASCADE,
    CONSTRAINT fk_dt_tag FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);

CREATE TABLE personal_dishes (
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pd_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_pd_origin FOREIGN KEY (original_dish_id) REFERENCES dishes(dish_id) ON DELETE SET NULL
);

CREATE TABLE personal_dish_tags (
    personal_dish_id INT,
    tag_id INT,
    PRIMARY KEY (personal_dish_id, tag_id),
    -- Xóa món cá nhân -> Xóa luôn liên kết tag
    CONSTRAINT fk_pdt_dish FOREIGN KEY (personal_dish_id) REFERENCES personal_dishes(personal_dish_id) ON DELETE CASCADE,
    -- Xóa tag khỏi hệ thống -> Xóa luôn liên kết trong món cá nhân
    CONSTRAINT fk_pdt_tag FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);

CREATE TABLE posts (
    post_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    post_type VARCHAR(20) NOT NULL,
    title VARCHAR(200),
    content TEXT,
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_p_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE saved_posts (
    user_id INT,
    post_id INT,
    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id),
    CONSTRAINT fk_sp_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_sp_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);
CREATE TABLE follows (
    follower_id INT NOT NULL, -- Người đi follow (Ví dụ: Tôi)
    following_id INT NOT NULL, -- Người được follow (Ví dụ: Idol)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, following_id), -- Một người chỉ follow người kia 1 lần
    CONSTRAINT fk_f_follower FOREIGN KEY (follower_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_f_following FOREIGN KEY (following_id) REFERENCES users(user_id) ON DELETE CASCADE,
    -- Chặn việc tự mình follow chính mình (Optional nhưng nên có)
    CONSTRAINT check_self_follow CHECK (follower_id <> following_id)
);
CREATE TABLE favorite_dishes (
    user_id INT NOT NULL,
    dish_id INT NOT NULL,
    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Lưu thời điểm thích để sắp xếp (Mới thích xếp trên)
    -- Khóa chính kép: Đảm bảo 1 người chỉ thích 1 món đúng 1 lần (không bị trùng)
    PRIMARY KEY (user_id, dish_id),
    -- Khóa ngoại
    CONSTRAINT fk_fd_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_fd_dish FOREIGN KEY (dish_id) REFERENCES dishes(dish_id) ON DELETE CASCADE
);

ALTER TABLE users 
ADD COLUMN follower_count INT DEFAULT 0, -- Số người theo dõi mình
ADD COLUMN following_count INT DEFAULT 0; -- Số người mình đang theo dõi

CREATE TABLE post_likes (
    user_id INT NOT NULL,
    post_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Khóa chính kép: Đảm bảo 1 người chỉ like 1 bài viết 1 lần
    PRIMARY KEY (user_id, post_id),
    
    CONSTRAINT fk_pl_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_pl_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

CREATE TABLE comments (
    comment_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    post_id INT NOT NULL,
    content TEXT NOT NULL, -- Nội dung bình luận
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_c_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_c_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);
ALTER TABLE posts 
ADD COLUMN like_count INT DEFAULT 0,    -- Tổng số lượt like
ADD COLUMN comment_count INT DEFAULT 0; -- Tổng số bình luận



CREATE TABLE follows (
    follower_id INT NOT NULL, -- Người đi follow (Ví dụ: Tôi)
    following_id INT NOT NULL, -- Người được follow (Ví dụ: Idol)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (follower_id, following_id), -- Một người chỉ follow người kia 1 lần
    
    CONSTRAINT fk_f_follower FOREIGN KEY (follower_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_f_following FOREIGN KEY (following_id) REFERENCES users(user_id) ON DELETE CASCADE,

    -- Chặn việc tự mình follow chính mình (Optional nhưng nên có)
    CONSTRAINT check_self_follow CHECK (follower_id <> following_id)
);

-- =============================================
-- SAMPLE DATA
-- =============================================

-- Xóa dữ liệu cũ để tránh conflict khi chạy lại
-- Sử dụng TRUNCATE để reset sequence (auto-increment counter)
TRUNCATE TABLE dish_tags, dishes, tags RESTART IDENTITY CASCADE;

-- Insert Tags (Nhãn cho món ăn)
INSERT INTO tags (tag_name, tag_type) VALUES
('Món mặn', 'TYPE'),
('Món chiên', 'TYPE'),
('Món xào', 'TYPE'),
('Món kho', 'TYPE'),
('Món rang', 'TYPE'),
('Món canh', 'TYPE'),
('Món khai vị', 'TYPE'),
('Đặc sản', 'TYPE'),
('Món chay', 'TYPE'),
('Cay', 'TASTE'),
('Ngọt', 'TASTE'),
('Chua', 'TASTE'),
('Mặn', 'TASTE'),
('Đậm đà', 'TASTE'),
('Thanh mát', 'TASTE'),
('Thịt heo', 'INGREDIENT'),
('Thịt gà', 'INGREDIENT'),
('Cá', 'INGREDIENT'),
('Hải sản', 'INGREDIENT'),
('Đậu phụ', 'INGREDIENT'),
('Rau củ', 'INGREDIENT'),
('Trứng', 'INGREDIENT'),
('Đậu', 'INGREDIENT'),
('Mọi mùa', 'SEASON'),
('Ngày lễ', 'SEASON'),
('Cơm gia đình', 'SEASON'),
('Món rim', 'TYPE'),
('Món luộc', 'TYPE'),
('Món rán', 'TYPE'),
('Món nhồi', 'TYPE'),
('Thịt bò', 'INGREDIENT'),
('Tôm', 'INGREDIENT'),
('Bí đao', 'INGREDIENT')
ON CONFLICT (tag_name) DO NOTHING;

-- Insert Dishes (10 món ăn Việt Nam truyền thống)
INSERT INTO dishes (dish_name, image_url, description, ingredients, instructions, prep_time, cook_time, serving) VALUES
('Thịt rang cháy cạnh', 
'/images/thit-rang-chay-canh.jpg',
'Món thịt rang cháy cạnh là món ăn dân dã, phổ biến trong bữa cơm gia đình Việt Nam. Thịt ba chỉ được rang đến khi cháy cạnh, thơm lừng, đậm đà với màu nâu caramel bắt mắt. Món này ăn kèm với cơm nóng và rau sống rất ngon.',
'- Thịt ba chỉ: 500g
- Nước mắm: 3 muỗng canh
- Đường: 2 muỗng canh
- Tỏi: 3 tép
- Ớt: 2 quả (tuỳ khẩu vị)
- Hành tím: 2 củ
- Hạt tiêu: 1/2 muỗng cà phê
- Dầu ăn: 1 muỗng canh
- Nước dừa tươi hoặc nước lọc: 50ml',
'Bước 1: Sơ chế thịt
- Thịt ba chỉ rửa sạch, cắt miếng vừa ăn (khoảng 2-3cm). Để ráo nước.
- Tỏi bóc vỏ, băm nhỏ. Hành tím bóc vỏ, thái lát mỏng. Ớt thái khoanh.

Bước 2: Ướp thịt
- Cho thịt vào tô lớn, thêm 1 muỗng canh nước mắm, 1/2 muỗng canh đường, tỏi băm, hạt tiêu. Trộn đều, ướp 15-20 phút.

Bước 3: Rang thịt
- Bắc chảo lên bếp, cho dầu ăn vào, đợi nóng thì cho hành tím vào phi thơm.
- Cho thịt đã ướp vào, đảo đều trên lửa vừa đến khi thịt săn lại.
- Thêm 1.5 muỗng canh nước mắm, 1.5 muỗng canh đường vào đảo đều.

Bước 4: Kho thịt
- Thêm nước dừa (hoặc nước lọc) vào, đun sôi rồi hạ lửa nhỏ.
- Kho khoảng 20-25 phút, thỉnh thoảng đảo đều để thịt thấm gia vị.
- Khi nước cạn, tăng lửa lớn, đảo liên tục đến khi thịt có màu nâu sẫm, cháy cạnh, bóng đẹp.
- Cho ớt vào đảo cùng 1-2 phút.

Bước 5: Hoàn thành
- Tắt bếp, cho thịt ra đĩa. Ăn nóng với cơm và rau sống.',
20, 35, 4),
--2. Đậu phụ sốt cà chua
('Đậu phụ sốt cà chua',
'/images/dau-phu-sot-ca-chua.jpg',
'Món ăn chay đơn giản, bổ dưỡng và thơm ngon. Đậu phụ chiên giòn bên ngoài, mềm bên trong kết hợp với sốt cà chua chua ngọt, đậm đà. Món này có thể ăn chay hoặc ăn mặn đều hợp.',
'- Đậu phụ non: 3 miếng (400g)
- Cà chua: 3 quả to
- Tỏi: 2 tép
- Hành lá: 2 cây
- Ớt: 1 quả
- Đường: 1 muỗng canh
- Nước mắm/tương ớt chay: 2 muỗng canh
- Dầu ăn để chiên
- Bột năng: 1 muỗng cà phê (pha với 2 muỗng nước)
- Hạt tiêu',
'Bước 1: Sơ chế nguyên liệu
- Đậu phụ rửa sạch, cắt miếng vuông vừa ăn (3x3cm), để ráo nước hoặc thấm khô bằng giấy.
- Cà chua rửa sạch, cắt múi cau hoặc hạt lựu.
- Tỏi băm nhỏ, hành lá thái khúc 3cm, ớt thái khoanh.

Bước 2: Chiên đậu phụ
- Bắc chảo lên bếp, cho dầu ăn vào đun nóng (lửa vừa).
- Cho từng miếng đậu phụ vào chiên đến khi vàng giòn 4 mặt.
- Vớt ra, để ráo dầu trên giấy thấm dầu.

Bước 3: Làm sốt cà chua
- Dùng lại chảo vừa chiên đậu, chỉ để lại khoảng 2 muỗng dầu.
- Cho tỏi băm vào phi thơm.
- Cho cà chua vào xào trên lửa vừa đến khi cà chua nhũn, ra nước.
- Nêm 2 muỗng nước mắm (hoặc tương ớt chay), 1 muỗng đường, 1/3 muỗng hạt tiêu.
- Thêm khoảng 100ml nước, đun sôi.

Bước 4: Hoàn thành món
- Cho đậu phụ đã chiên vào sốt, đảo nhẹ để đậu thấm sốt (khoảng 3-5 phút).
- Pha bột năng với nước, rót vào đảo đều để sốt đặc lại.
- Cho hành lá, ớt vào, đảo đều rồi tắt bếp.
- Múc ra đĩa, ăn nóng với cơm.',
15, 20, 3),
-- 3. Rau muống xào tỏi
('Rau muống xào tỏi',
'/images/rau-muong-xao-toi.jpg',
'Món rau xào đơn giản, nhanh gọn nhưng cực kỳ phổ biến trong bữa cơm Việt Nam. Rau muống giòn xanh, thơm mùi tỏi phi, ăn với cơm nóng rất ngon miệng.',
'- Rau muống: 500g
- Tỏi: 4-5 tép
- Dầu ăn: 2 muỗng canh
- Nước mắm: 1 muỗng canh
- Muối: 1/3 muỗng cà phê
- Đường: 1/4 muỗng cà phê
- Hạt nêm: 1/2 muỗng cà phê (tuỳ chọn)',
'Bước 1: Sơ chế rau muống
- Rau muống nhặt bỏ phần gốc già, lá vàng.
- Rửa sạch qua 2-3 lần nước, để ráo.
- Cắt khúc khoảng 5cm, tách riêng phần thân và phần ngọn lá.
- Tỏi bóc vỏ, đập dập, băm nhỏ hoặc thái lát mỏng.

Bước 2: Xào rau muống
- Bắc chảo/wok lên bếp, đun nóng trên lửa lớn.
- Cho dầu ăn vào, đợi dầu sôi bốc khói nhẹ.
- Cho tỏi vào phi thơm (khoảng 10-15 giây).
- Cho phần thân rau muống vào trước, đảo nhanh tay khoảng 30 giây.
- Tiếp tục cho phần ngọn lá vào, đảo đều.

Bước 3: Nêm nếm
- Nêm nước mắm, muối, đường, hạt nêm (nếu dùng).
- Đảo nhanh tay trong 1-2 phút cho rau chín vừa, vẫn giữ được độ giòn và màu xanh.

Bước 4: Hoàn thành
- Tắt bếp ngay khi rau vừa chín.
- Múc ra đĩa, ăn ngay khi còn nóng.',
10, 5, 3),

-- 4. Trứng đúc thịt
('Trứng đúc thịt',
'/images/trung-duc-thit.jpg',
'Món ăn dân dã, đậm đà hương vị Bắc Bộ. Trứng được đánh tan, trộn với thịt băm, mộc nhĩ, miến, sau đó đúc chín và cắt miếng ăn kèm cơm. Món này vừa bổ dưỡng, vừa dễ làm.',
'- Trứng gà: 4 quả
- Thịt băm: 150g
- Mộc nhĩ khô: 5g
- Miến (bún tàu): 30g
- Hành tím: 2 củ
- Nước mắm: 1.5 muỗng canh
- Hạt tiêu: 1/2 muỗng cà phê
- Dầu ăn: 2 muỗng canh
- Hành lá, rau mùi để trang trí',
'Bước 1: Sơ chế nguyên liệu
- Mộc nhĩ ngâm nước ấm cho nở, rửa sạch, thái nhỏ.
- Miến ngâm nước cho mềm, cắt khúc ngắn.
- Hành tím bóc vỏ, băm nhỏ.
- Hành lá, rau mùi rửa sạch, thái nhỏ.

Bước 2: Ướp thịt
- Cho thịt băm vào tô, thêm hành băm, 1/2 muỗng nước mắm, hạt tiêu.
- Trộn đều, ướp khoảng 10 phút.

Bước 3: Chuẩn bị hỗn hợp trứng
- Đánh tan 4 quả trứng vào tô lớn.
- Thêm thịt đã ướp, mộc nhĩ, miến vào trộn đều.
- Nêm thêm 1 muỗng nước mắm, hạt tiêu. Trộn đều.

Bước 4: Đúc trứng
- Bắc chảo chống dính lên bếp, cho 2 muỗng dầu vào.
- Đợi chảo nóng, rót hỗn hợp trứng vào, dàn đều.
- Nấu trên lửa nhỏ, đậy nắp lại khoảng 5-7 phút cho trứng chín.
- Lật mặt, nấu thêm 3-5 phút cho chín đều.

Bước 5: Hoàn thành
- Cho trứng đúc ra thớt, cắt miếng vừa ăn.
- Rắc hành lá, rau mùi lên trên.
- Ăn nóng với cơm và nước mắm ớt.',
15, 15, 3),

-- 5. Sườn xào chua ngọt
('Sườn xào chua ngọt',
'/images/suon-xao-chua-ngot.jpg',
'Món sườn xào chua ngọt với sốt đỏ bắt mắt, vị chua ngọt hài hoà, thịt sườn mềm, thấm đậm gia vị. Món này rất được ưa chuộng, đặc biệt là trong các bữa tiệc và họp mặt gia đình.',
'- Sườn non: 500g
- Dứa tươi: 150g
- Cà chua: 2 quả
- Ớt chuông đỏ/vàng: 1 quả
- Hành tây: 1/2 củ
- Tỏi: 3 tép
- Gừng: 1 lát nhỏ

Gia vị ướp:
- Nước mắm: 1 muỗng canh
- Hạt tiêu: 1/2 muỗng cà phê
- Bột năng: 1 muỗng canh

Sốt chua ngọt:
- Tương cà: 3 muỗng canh
- Giấm gạo: 2 muỗng canh
- Đường: 2.5 muỗng canh
- Nước lọc: 150ml
- Bột năng: 1 muỗng cà phê (pha với nước)',
'Bước 1: Sơ chế sườn
- Sườn non rửa sạch, chặt miếng vừa ăn.
- Ướp sườn với nước mắm, hạt tiêu, bột năng trong 20 phút.

Bước 2: Sơ chế rau củ
- Dứa bổ múi cau, cà chua cắt múi.
- Ớt chuông, hành tây cắt miếng vuông.
- Tỏi băm, gừng băm nhỏ.

Bước 3: Chiên sườn
- Bắc chảo lên bếp, cho dầu ăn vào đun nóng.
- Chiên sườn đã ướp đến khi vàng đều các mặt.
- Vớt ra để ráo dầu.

Bước 4: Làm sốt chua ngọt
- Trộn tương cà, giấm, đường, nước lọc trong 1 tô nhỏ.

Bước 5: Xào sườn
- Dùng lại chảo vừa chiên sườn, để lại 1 muỗng dầu.
- Phi thơm tỏi, gừng.
- Cho hành tây vào xào thơm.
- Thêm sườn đã chiên vào, đảo đều.
- Rót hỗn hợp sốt chua ngọt vào, đun sôi.
- Cho dứa, cà chua, ớt chuông vào, đảo đều.
- Khi sốt sôi, cho bột năng đã pha nước vào để sốt đặc lại.

Bước 6: Hoàn thành
- Đảo đều trong 2-3 phút rồi tắt bếp.
- Múc ra đĩa, ăn nóng với cơm.',
25, 25, 4),

-- 6. Canh cua mồng tơi
('Canh cua mồng tơi',
'/images/canh-cua-mong-toi.jpg',
'Món canh thanh mát, bổ dưỡng với vị ngọt tự nhiên từ cua và rau mồng tơi. Đây là món canh truyền thống của miền Bắc, thường xuất hiện trong các bữa cơm gia đình hàng ngày.',
'- Cua đồng: 300g (hoặc cua biển)
- Rau mồng tơi: 300g
- Thịt băm: 100g (tuỳ chọn)
- Cà chua: 2 quả
- Hành tím: 2 củ
- Tỏi: 2 tép
- Trứng gà: 1 quả (tuỳ chọn)
- Mắm tôm: 1 muỗng cà phê
- Nước mắm: 1 muỗng canh
- Hạt nêm: 1 muỗng cà phê
- Dầu ăn
- Hành lá, rau mùi',
'Bước 1: Sơ chế cua
- Cua rửa sạch, chải kỹ vỏ.
- Bẻ mai cua, bỏ mang, mật cua.
- Chặt cua thành 4-6 miếng (cua nhỏ có thể để nguyên).
- Đập nhẹ càng cua để khi nấu dễ thấm gia vị.

Bước 2: Sơ chế rau củ
- Rau mồng tơi nhặt bỏ lá già, rửa sạch, cắt khúc 5cm.
- Cà chua cắt múi cau.
- Hành tím, tỏi băm nhỏ.
- Hành lá, rau mùi thái nhỏ.

Bước 3: Nấu canh
- Bắc nồi lên bếp, cho 1 muỗng dầu ăn vào.
- Phi thơm hành tím, tỏi băm.
- Cho cà chua vào xào đến khi nhũn.
- Nếu có thịt băm, cho vào xào cùng.
- Thêm khoảng 1 lít nước vào, đun sôi.

Bước 4: Cho cua vào nấu
- Khi nước sôi, cho cua vào nấu khoảng 10 phút.
- Nêm nếm với nước mắm, hạt nêm, mắm tôm (nếm vừa ăn).
- Vớt bọt để canh trong.

Bước 5: Cho rau mồng tơi
- Cho rau mồng tơi vào, đun sôi trở lại.
- Nếu dùng trứng: đánh tan trứng, rưới vào canh khi sôi, đảo nhẹ.
- Nấu thêm 2-3 phút cho rau chín vừa.

Bước 6: Hoàn thành
- Cho hành lá, rau mùi vào, tắt bếp.
- Múc ra tô, ăn nóng với cơm.',
20, 20, 4),

-- 7. Gà rang gừng
('Gà rang gừng',
'/images/ga-rang-gung.jpg',
'Món gà rang gừng thơm nồng, cay nồng từ gừng, thịt gà săn chắc, đậm đà. Đây là món ăn giúp sưởi ấm cơ thể, rất phù hợp cho mùa đông hoặc khi trời lạnh.',
'- Thịt gà: 500g (đùi hoặc ức)
- Gừng tươi: 100g
- Tỏi: 5 tép
- Ớt sừng: 2 quả
- Sả: 2 cây
- Lá chanh: 5-6 lá
- Nước mắm: 3 muỗng canh
- Đường: 1 muỗng canh
- Hạt tiêu: 1 muỗng cà phê
- Dầu ăn
- Hành lá: 2 cây',
'Bước 1: Sơ chế gà
- Thịt gà rửa sạch, chặt miếng vừa ăn.
- Ướp gà với 1 muỗng nước mắm, 1/2 muỗng hạt tiêu trong 15 phút.

Bước 2: Sơ chế gia vị
- Gừng gọt vỏ, thái lát mỏng hoặc sợi nhỏ.
- Tỏi đập dập, bóc vỏ (có thể để nguyên tép).
- Sả đập dập, cắt khúc 3cm.
- Ớt cắt khoanh, lá chanh rửa sạch.
- Hành lá cắt khúc 3cm.

Bước 3: Rang gà
- Bắc chảo lên bếp, cho 2 muỗng dầu vào đun nóng.
- Cho gừng vào phi thơm trên lửa vừa.
- Cho tỏi, sả vào phi cùng.
- Cho thịt gà vào, xào trên lửa lớn đến khi gà săn lại, chín bên ngoài.

Bước 4: Kho gà
- Giảm lửa xuống vừa.
- Nêm 2 muỗng nước mắm, 1 muỗng đường, 1/2 muỗng hạt tiêu.
- Đảo đều, kho gà khoảng 15-20 phút, thỉnh thoảng đảo để gà thấm gia vị.
- Nếu khô, thêm chút nước, nấu đến khi gà chín, nước cạn, có màu nâu đẹp.

Bước 5: Hoàn thành
- Cho ớt, lá chanh vào, đảo đều 1 phút.
- Cho hành lá vào, đảo nhanh rồi tắt bếp.
- Múc ra đĩa, ăn nóng với cơm.',
20, 30, 4),

-- 8. Thịt kho tàu (Thịt kho trứng)
('Thịt kho tàu',
'/images/thit-kho-tau.jpg',
'Món ăn truyền thống không thể thiếu trong dịp Tết và các ngày lễ quan trọng của người Việt. Thịt ba chỉ và trứng được kho với nước dừa tươi, tạo nên vị ngọt đậm đà, màu nâu caramel đẹp mắt.',
'- Thịt ba chỉ: 700g
- Trứng gà/vịt: 6-8 quả
- Nước dừa tươi: 500ml
- Đường: 3 muỗng canh (hoặc đường phên)
- Nước mắm: 4 muỗng canh
- Tỏi: 5 tép
- Hành tím: 3 củ
- Hạt tiêu: 1 muỗng cà phê
- Ớt khô: 2-3 quả (tuỳ khẩu vị)
- Nước dừa non (hoặc nước lọc) để kho',
'Bước 1: Sơ chế thịt
- Thịt ba chỉ rửa sạch, luộc sơ qua nước sôi khoảng 5 phút để loại bỏ mùi hôi.
- Vớt ra, rửa lại với nước lạnh, để ráo.
- Cắt thịt thành miếng vuông vừa ăn (3x3cm).

Bước 2: Luộc và làm trứng
- Trứng luộc chín, ngâm nước lạnh rồi bóc vỏ.
- Dùng nĩa chích nhẹ lên bề mặt trứng để khi kho dễ thấm gia vị.

Bước 3: Pha nước màu
- Bắc chảo nhỏ lên bếp, cho 3 muỗng đường vào.
- Đun trên lửa nhỏ, khuấy đều cho đường tan và chuyển màu nâu caramel.
- Khi đường sánh lại và có màu nâu đậm, rót từ từ 100ml nước dừa vào (cẩn thận vì nước sẽ sôi bùng lên).
- Khuấy đều tạo thành nước màu, tắt bếp.

Bước 4: Kho thịt
- Tỏi, hành tím bóc vỏ, băm nhỏ hoặc để nguyên (đập dập).
- Bắc nồi lên bếp, cho 1 muỗng dầu vào, phi thơm tỏi, hành.
- Cho thịt vào xào săn lại.
- Rót nước màu vừa pha vào, đảo đều.
- Thêm nước mắm, hạt tiêu, ớt khô.
- Rót nước dừa vào ngập thịt.

Bước 5: Kho
- Đun sôi trên lửa lớn, sau đó hạ lửa nhỏ, đậy nắp kho khoảng 45 phút.
- Thỉnh thoảng mở nắp đảo đều để thịt chín đều và thấm gia vị.
- Cho trứng vào, kho thêm 20-30 phút cho đến khi nước cạn, sệt lại, thịt và trứng có màu nâu bóng đẹp.

Bước 6: Hoàn thành
- Nêm nếm lại cho vừa khẩu vị.
- Tắt bếp, để yên 10 phút cho thịt ngấm gia vị.
- Múc ra tô, ăn kèm cơm nóng và dưa chua.',
30, 70, 6),

-- 9. Cá kho tộ
('Cá kho tộ',
'/images/ca-kho-to.jpg',
'Đặc sản miền Nam Việt Nam, đặc biệt là của người dân vùng đất Vũ Đại (Tân Phú, TP.HCM). Cá được kho trong tộ đất nung với nước dừa, thịt ba chỉ, tạo nên vị ngọt đậm đà, đặc trưng khó quên.',
'- Cá lóc/cá basa/cá thu: 500g
- Thịt ba chỉ: 150g
- Nước dừa tươi: 300ml
- Đường: 2 muỗng canh
- Nước mắm: 4 muỗng canh
- Tỏi: 5 tép
- Ớt: 3 quả
- Tiêu xay: 1 muỗng cà phê
- Dầu ăn
- Hành lá, rau răm',
'Bước 1: Sơ chế cá
- Cá rửa sạch, cắt khúc vừa ăn (dày khoảng 3cm).
- Ướp cá với 1 muỗng nước mắm, 1/2 muỗng tiêu, để 15 phút.

Bước 2: Chiên cá
- Bắc chảo lên bếp, cho dầu ăn vào đun nóng.
- Chiên cá đến khi vàng 2 mặt, vớt ra để ráo dầu.
(Chiên cá giúp cá không bị nát khi kho)

Bước 3: Sơ chế thịt và gia vị
- Thịt ba chỉ rửa sạch, cắt lát mỏng.
- Tỏi bóc vỏ, băm nhỏ hoặc đập dập.
- Ớt cắt khoanh.
- Hành lá, rau răm rửa sạch, thái khúc.

Bước 4: Pha nước màu
- Bắc chảo nhỏ, cho 2 muỗng đường vào.
- Đun lửa nhỏ cho đường tan và chuyển màu nâu caramel.
- Rót từ từ 50ml nước vào, khuấy đều.

Bước 5: Kho cá
- Lấy tộ đất (hoặc nồi) đặt lên bếp.
- Lót thịt ba chỉ xuống đáy (để cá không bị dính đáy).
- Xếp cá lên trên.
- Rắc tỏi, ớt lên cá.
- Rót nước màu, nước mắm, nước dừa vào.
- Đun sôi trên lửa lớn, sau đó hạ lửa nhỏ.
- Kho khoảng 30-40 phút, thỉnh thoảng lấy nước kho rưới lên cá.

Bước 6: Hoàn thành
- Khi nước cạn, sệt lại, cá có màu nâu đỏ đẹp thì tắt bếp.
- Rắc hành lá, rau răm lên trên.
- Ăn nóng với cơm, dưa chua và canh.',
25, 45, 4),

-- 10. Lạc rang muối
('Lạc rang muối',
'/images/lac-rang-muoi.jpg',
'Món ăn vặt truyền thống, đơn giản nhưng cực kỳ phổ biến trong văn hoá ẩm thực Việt Nam. Lạc rang muối giòn tan, thơm bùi, mặn vừa phải, là món ăn chơi tuyệt vời hoặc dùng làm nhân cho các món bánh.',
'- Đậu phộng tươi (còn vỏ lụa): 500g
- Muối hạt: 2 muỗng canh
- Nước: 200ml
- Lá chanh: 5-6 lá (tuỳ chọn)
- Ớt khô: 2 quả (tuỳ chọn)',
'Bước 1: Sơ chế đậu phộng
- Đậu phộng rửa sạch qua vài lần nước.
- Để ráo nước.

Bước 2: Luộc đậu phộng với muối
- Cho đậu phộng vào nồi.
- Thêm 200ml nước, 2 muỗng muối hạt.
- Nếu dùng lá chanh, bỏ vào luộc cùng.
- Đun trên lửa vừa, đảo đều để muối tan và thấm vào đậu.
- Khi nước sôi và cạn gần hết, tắt bếp.

Bước 3: Rang đậu phộng
Cách 1: Rang trên bếp
- Bắc chảo rộng lên bếp (không cần dầu).
- Cho đậu phộng đã luộc vào, rang trên lửa nhỏ.
- Đảo đều liên tục trong khoảng 20-30 phút cho đến khi đậu khô ráo, giòn và có màu vàng nâu.
- Lưu ý: Phải đảo đều tay để đậu chín đều, không bị cháy.

Cách 2: Rang bằng lò nướng
- Làm nóng lò ở 160°C.
- Trải đậu phộng ra khay nướng.
- Nướng khoảng 25-30 phút, thỉnh thoảng lấy ra lắc đều.

Bước 4: Hoàn thành
- Khi đậu phộng đã giòn, vỏ dễ bong ra, tắt bếp.
- Để nguội hoàn toàn.
- Nếu thích cay, trộn thêm chút ớt bột hoặc ớt khô rang giã nhỏ.
- Bảo quản trong hộp kín để giữ độ giòn.

Lưu ý: Đậu phộng sẽ giòn hơn khi nguội hoàn toàn.',
10, 35, 6),
-- 11. Canh rau ngót thịt băm
('Canh rau ngót thịt băm',
'/images/canh-rau-ngot-thit-bam.jpg',
'Món canh thanh mát, giàu dinh dưỡng với rau ngót có vị ngọt tự nhiên kết hợp với thịt băm đậm đà. Đây là món canh truyền thống miền Bắc, rất phổ biến trong bữa cơm gia đình hàng ngày.',
'- Rau ngót: 300g
- Thịt nạc vai băm: 150g
- Tỏi: 2 tép
- Hành tím: 2 củ
- Nước mắm: 1.5 muỗng canh
- Hạt nêm: 1 muỗng cà phê
- Dầu ăn: 1 muỗng canh
- Hạt tiêu: 1/4 muỗng cà phê
- Hành lá, rau mùi để trang trí',
'Bước 1: Sơ chế rau ngót
- Rau ngót nhặt bỏ lá già, thân già.
- Rửa sạch qua 2-3 lần nước, để ráo.
- Cắt khúc khoảng 5cm.

Bước 2: Sơ chế và ướp thịt
- Thịt băm rửa sạch, vắt ráo nước.
- Tỏi, hành tím bóc vỏ, băm nhỏ.
- Ướp thịt với 1/2 muỗng nước mắm, hạt tiêu, 1/2 tép tỏi băm trong 10 phút.

Bước 3: Nấu canh
- Bắc nồi lên bếp, cho 1 muỗng dầu ăn vào.
- Phi thơm hành tím và tỏi còn lại.
- Cho thịt băm vào xào săn lại.
- Thêm khoảng 800ml nước, đun sôi.
- Vớt bọt cho canh trong.

Bước 4: Cho rau vào
- Khi nước sôi, nêm nếm với nước mắm, hạt nêm cho vừa ăn.
- Cho rau ngót vào, đun sôi trở lại.
- Nấu thêm 2-3 phút cho rau chín vừa.

Bước 5: Hoàn thành
- Cho hành lá, rau mùi vào, tắt bếp.
- Múc ra tô, ăn nóng với cơm.',
15, 15, 3),

-- 12. Tôm rim thịt ba chỉ
('Tôm rim thịt ba chỉ',
'/images/tom-rim-thit-ba-chi.jpg',
'Món rim đậm đà với sự kết hợp hoàn hảo giữa tôm tươi ngọt và thịt ba chỉ béo ngậy. Món ăn có màu đỏ nâu bắt mắt, vị mặn ngọt cân bằng, rất hợp để ăn với cơm nóng.',
'- Tôm sú/tôm càng: 300g
- Thịt ba chỉ: 200g
- Nước mắm: 3 muỗng canh
- Đường: 2 muỗng canh
- Nước dừa tươi: 150ml
- Tỏi: 4 tép
- Hành tím: 3 củ
- Ớt: 2 quả
- Hạt tiêu: 1/2 muỗng cà phê
- Dầu ăn: 2 muỗng canh',
'Bước 1: Sơ chế tôm
- Tôm rửa sạch, cắt bỏ râu, chân tôm.
- Dùng tăm lấy chỉ lưng tôm ra.
- Để ráo nước.

Bước 2: Sơ chế thịt
- Thịt ba chỉ rửa sạch, cắt miếng vuông vừa ăn (2x2cm).
- Luộc sơ qua nước sôi khoảng 3 phút, vớt ra để ráo.

Bước 3: Sơ chế gia vị
- Tỏi, hành tím bóc vỏ, băm nhỏ.
- Ớt thái khoanh.

Bước 4: Rim thịt trước
- Bắc chảo lên bếp, cho 1 muỗng dầu vào.
- Phi thơm hành tím, tỏi.
- Cho thịt ba chỉ vào xào săn.
- Nêm 1.5 muỗng nước mắm, 1 muỗng đường.
- Thêm nước dừa, đun sôi rồi hạ lửa nhỏ.
- Rim thịt khoảng 15 phút.

Bước 5: Rim tôm
- Cho tôm vào rim cùng thịt.
- Nêm thêm 1.5 muỗng nước mắm, 1 muỗng đường.
- Rim trên lửa vừa khoảng 10-12 phút.
- Đảo đều để tôm và thịt chín đều, thấm gia vị.
- Khi nước cạn, sệt lại, có màu đỏ nâu đẹp thì cho ớt vào.

Bước 6: Hoàn thành
- Đảo đều 1-2 phút rồi tắt bếp.
- Múc ra đĩa, ăn nóng với cơm.',
20, 30, 4),

-- 13. Thịt luộc cà pháo
('Thịt luộc cà pháo',
'/images/thit-luoc-ca-phao.jpg',
'Món ăn dân dã, thanh đạm với thịt luộc mềm ngọt ăn kèm cà pháo chua chua, cay cay đặc trưng. Đây là món ăn truyền thống của người Việt, thường xuất hiện trong các bữa cơm gia đình hoặc ngày giỗ, tết.',
'- Thịt ba chỉ/thịt nạc vai: 500g
- Cà pháo: 5-6 quả
- Hành lá: 2 cây
- Rau thơm (rau răm, húng quế): 1 bó nhỏ

Nước chấm:
- Nước mắm: 2 muỗng canh
- Đường: 1 muỗng canh
- Nước cốt chanh: 1 muỗng canh
- Tỏi: 2 tép
- Ớt: 2 quả
- Nước lọc: 3 muỗng canh',
'Bước 1: Sơ chế thịt
- Thịt rửa sạch, để nguyên miếng hoặc cuộn tròn, dùng dây buộc lại.
- Chà muối lên bề mặt, để 5 phút rồi rửa lại.

Bước 2: Luộc thịt
- Đun sôi nồi nước lớn.
- Cho thịt vào, thêm 1 muỗng cà phê muối, 2 củ hành tím đập dập, 1 lát gừng.
- Luộc trên lửa vừa khoảng 30-40 phút (tuỳ độ dày của miếng thịt).
- Thỉnh thoảng vớt bọt cho nước trong.
- Dùng đũa đâm vào, nếu không còn máu chảy ra là thịt đã chín.
- Vớt thịt ra, ngâm ngay vào nước đá lạnh khoảng 10 phút (để thịt săn lại, da giòn).

Bước 3: Sơ chế cà pháo và rau
- Cà pháo rửa sạch, thái lát mỏng.
- Hành lá, rau thơm rửa sạch.
- Hành lá thái khúc, rau thơm xé nhỏ.

Bước 4: Làm nước chấm
- Tỏi, ớt băm nhỏ.
- Trộn nước mắm, đường, nước cốt chanh, nước lọc, tỏi băm, ớt băm.
- Khuấy đều cho đường tan.

Bước 5: Hoàn thành
- Vớt thịt ra khỏi nước đá, lau khô.
- Thái lát mỏng vừa ăn.
- Xếp thịt ra đĩa, rắc hành lá lên trên.
- Ăn kèm cà pháo, rau sống và nước chấm.',
20, 40, 4),

-- 14. Cá rán giòn
('Cá rán giòn',
'/images/ca-ran-gion.jpg',
'Món cá rán giòn tan bên ngoài, thịt mềm ngọt bên trong. Cá được tẩm ướp gia vị, tẩm bột rồi rán vàng ròn. Món này ăn nóng với cơm và nước mắm ớt tỏi rất ngon.',
'- Cá (cá điêu hồng, cá chim, cá rô phi): 2 con (khoảng 500g)
- Bột chiên giòn/bột chiên xù: 150g
- Nước mắm: 2 muỗng canh
- Tỏi: 3 tép
- Gừng: 2 lát
- Hạt tiêu: 1/2 muỗng cà phê
- Dầu ăn để rán
- Rau sống, chanh, ớt để ăn kèm',
'Bước 1: Sơ chế cá
- Cá làm sạch, cạo vảy, rửa bụng, bỏ mang.
- Rửa sạch, để ráo.
- Rạch đường chéo 2-3 nhát trên thân cá (giúp cá thấm gia vị và chín đều).

Bước 2: Ướp cá
- Tỏi, gừng đập dập, băm nhỏ.
- Chà muối lên cá, rửa lại.
- Ướp cá với nước mắm, tỏi, gừng, hạt tiêu.
- Để ướp ít nhất 20-30 phút.

Bước 3: Tẩm bột cá
- Cho bột chiên giòn vào tô.
- Lần lượt lăn từng con cá trong bột, tẩm đều khắp mình cá.
- Vỗ nhẹ bỏ bột thừa.

Bước 4: Rán cá
- Bắc chảo lên bếp, cho dầu ăn vào (nhiều dầu, ngập cá).
- Đun dầu nóng khoảng 170-180°C (thả que tre vào, nếu có bọt khí là đủ nóng).
- Cho cá vào rán trên lửa vừa.
- Rán đến khi cá vàng giòn 2 mặt (khoảng 10-12 phút).
- Vớt ra, để ráo dầu trên giấy thấm dầu.

Bước 5: Hoàn thành
- Xếp cá ra đĩa.
- Ăn nóng với cơm, rau sống, nước mắm ớt tỏi.',
30, 15, 3),

-- 15. Bắp cải xào
('Bắp cải xào',
'/images/bap-cai-xao.jpg',
'Món rau xào đơn giản, nhanh gọn với bắp cải giòn ngọt. Có thể xào chay hoặc thêm tôm, thịt tùy thích. Món ăn thanh mát, bổ dưỡng cho bữa cơm hàng ngày.',
'- Bắp cải: 1/2 củ (khoảng 400g)
- Cà rót: 2 quả (tuỳ chọn)
- Tỏi: 3 tép
- Dầu ăn: 2 muỗng canh
- Nước mắm: 1 muỗng canh
- Muối: 1/3 muỗng cà phê
- Đường: 1/4 muỗng cà phê
- Hạt nêm: 1/2 muỗng cà phê',
'Bước 1: Sơ chế bắp cải
- Bắp cải rửa sạch.
- Bỏ lõi, tách từng lá.
- Thái sợi hoặc cắt miếng vừa ăn.
- Ngâm nước muối loãng 5 phút, rửa lại, để ráo.

Bước 2: Sơ chế cà rốt
- Cà rốt gọt vỏ, rửa sạch.
- Thái lát mỏng hoặc sợi.

Bước 3: Sơ chế tỏi
- Tỏi bóc vỏ, đập dập hoặc băm nhỏ.

Bước 4: Xào bắp cải
- Bắc chảo/wok lên bếp, đun nóng trên lửa lớn.
- Cho dầu ăn vào, đợi dầu sôi.
- Cho tỏi vào phi thơm (khoảng 10 giây).
- Cho cà rốt vào xào nhanh 1 phút.
- Cho bắp cải vào, xào nhanh tay.

Bước 5: Nêm nếm
- Nêm nước mắm, muối, đường, hạt nêm.
- Xào đều trong 3-4 phút cho bắp cải chín vừa, vẫn giòn và giữ được màu xanh.

Bước 6: Hoàn thành
- Tắt bếp khi bắp cải vừa chín.
- Múc ra đĩa, ăn ngay khi còn nóng.',
10, 5, 3),

-- 16. Canh sườn chua
('Canh sườn chua',
'/images/canh-suon-chua.jpg',
'Món canh chua Nam Bộ với sườn non mềm ngọt, nước dùng chua chua thanh mát từ me, cà chua, dứa. Đây là món canh đậm đà hương vị miền Nam, rất kích thích vị giác.',
'- Sườn non: 400g
- Dứa tươi: 150g
- Cà chua: 2 quả
- Đậu bắp: 5 cây
- Giá đỗ: 100g
- Rau thơm (ngò gai, rau om, húng quế): 1 bó
- Me: 50g (hoặc 2 muỗng nước me)
- Đường: 1.5 muỗng canh
- Nước mắm: 2 muỗng canh
- Mắm tôm: 1/2 muỗng cà phê
- Hạt nêm: 1 muỗng cà phê
- Tỏi, hành tím: 2 tép mỗi loại
- Ớt: 2 quả',
'Bước 1: Sơ chế sườn
- Sườn non chặt miếng vừa ăn.
- Rửa sạch, chần qua nước sôi để loại bỏ tạp chất.
- Rửa lại với nước lạnh.

Bước 2: Sơ chế rau củ
- Dứa, cà chua cắt múi vừa ăn.
- Đậu bắp cắt khúc 3cm.
- Giá đỗ rửa sạch.
- Rau thơm nhặt rửa sạch.
- Tỏi, hành tím băm nhỏ.
- Ớt thái khoanh.

Bước 3: Pha nước me
- Me ngâm nước ấm, vò lấy nước, lọc bỏ bã.

Bước 4: Nấu canh
- Bắc nồi lên bếp, cho 1 muỗng dầu vào.
- Phi thơm hành tím, tỏi.
- Cho cà chua vào xào nhuyễn.
- Cho sườn vào xào săn.
- Thêm khoảng 1.2 lít nước, đun sôi.
- Vớt bọt, hạ lửa nhỏ.
- Nấu sườn khoảng 20 phút cho mềm.

Bước 5: Nêm nếm và cho rau
- Cho nước me, dứa vào, đun sôi.
- Nêm đường, nước mắm, mắm tôm, hạt nêm cho vừa ăn (vị chua ngọt cân bằng).
- Cho đậu bắp vào, nấu thêm 5 phút.
- Cho giá đỗ, rau thơm vào, đun sôi là tắt bếp.

Bước 6: Hoàn thành
- Cho ớt vào.
- Múc ra tô, ăn nóng với cơm.',
25, 30, 4),

-- 17. Chả lá lốt
('Chả lá lốt',
'/images/cha-la-lot.jpg',
'Món chả lá lốt thơm nức mũi với hương vị đặc trưng từ lá lốt kết hợp thịt băm ướp gia vị đậm đà. Đây là món ăn phổ biến trong nhà hàng và cũng là món ăn yêu thích của nhiều gia đình Việt.',
'- Thịt nạc vai băm: 300g
- Thịt mỡ băm nhỏ: 100g
- Lá lốt: 30-40 lá
- Tỏi: 4 tép
- Hành tím: 3 củ
- Sả: 1 cây
- Nước mắm: 2 muỗng canh
- Đường: 1 muỗng cà phê
- Hạt tiêu: 1/2 muỗng cà phê
- Dầu ăn để nướng/chiên
- Đồ nướng: que xiên (nếu nướng)',
'Bước 1: Sơ chế nguyên liệu
- Lá lốt rửa sạch, để ráo nước.
- Tỏi, hành tím bóc vỏ, băm nhỏ.
- Sả bóc lớp ngoài, thái lát mỏng hoặc băm nhỏ.

Bước 2: Ướp thịt
- Trộn thịt nạc và thịt mỡ trong tô lớn.
- Thêm tỏi băm, hành băm, sả băm.
- Nêm nước mắm, đường, hạt tiêu.
- Trộn đều, ướp ít nhất 30 phút (hoặc để tủ lạnh 1-2 giờ cho thấm).

Bước 3: Gói chả
- Lấy 1 lá lốt, đặt mặt nhẵn úp xuống.
- Để 1 muỗng cà phê hỗn hợp thịt vào giữa lá.
- Gấp 2 bên lá vào, sau đó cuộn tròn từ dưới lên.
- Làm tương tự với các lá còn lại.

Bước 4: Nướng/chiên chả
Cách 1: Nướng than/bếp
- Xiên chả vào que tre.
- Nướng trên than hồng hoặc vỉ nướng.
- Phết dầu ăn lên bề mặt chả trong khi nướng.
- Nướng khoảng 10-15 phút, lật đều các mặt cho chín vàng.

Cách 2: Chiên chảo
- Bắc chảo lên bếp, cho dầu vào.
- Xếp chả vào chiên trên lửa vừa.
- Chiên đến khi chả vàng đều các mặt (khoảng 8-10 phút).

Bước 5: Hoàn thành
- Vớt chả ra, để ráo dầu.
- Xếp ra đĩa.
- Ăn nóng với bún, rau sống, nước mắm ớt tỏi.',
40, 15, 4),

-- 18. Mướp đắng nhồi thịt
('Mướp đắng nhồi thịt',
'/images/muop-dang-nhoi-thit.jpg',
'Món ăn dân dã với vị đắng thanh mát của mướp đắng kết hợp với nhân thịt đậm đà. Món này vừa bổ dưỡng, vừa có tác dụng giải nhiệt, mát gan rất tốt.',
'- Mướp đắng (khổ qua): 2-3 trái
- Thịt băm: 200g
- Mộc nhĩ: 5g
- Miến: 20g
- Tỏi: 2 tép
- Hành tím: 2 củ
- Nước mắm: 2 muỗng canh
- Hạt tiêu: 1/2 muỗng cà phê
- Hạt nêm: 1 muỗng cà phê
- Dầu ăn
- Hành lá, rau mùi',
'Bước 1: Sơ chế mướp đắng
- Mướp đắng rửa sạch.
- Cắt khúc dài khoảng 4-5cm.
- Dùng muỗng nạo bỏ ruột và hạt bên trong.
- Ngâm mướp trong nước muối loãng 15-20 phút để giảm vị đắng (tuỳ khẩu vị).
- Rửa lại, để ráo.

Bước 2: Làm nhân thịt
- Mộc nhĩ ngâm nở, rửa sạch, thái nhỏ.
- Miến ngâm nở, cắt khúc ngắn.
- Tỏi, hành tím băm nhỏ.
- Trộn thịt băm với mộc nhĩ, miến, tỏi, hành.
- Nêm 1 muỗng nước mắm, hạt tiêu. Trộn đều.

Bước 3: Nhồi thịt vào mướp
- Dùng tay hoặc muỗng nhồi nhân thịt vào từng khúc mướp đắng.
- Nhồi chặt tay, đầy khúc mướp.

Bước 4: Nấu mướp nhồi thịt
Cách 1: Luộc
- Đun sôi nồi nước.
- Cho mướp nhồi thịt vào luộc khoảng 15-20 phút đến khi chín.
- Vớt ra, để ráo.

Cách 2: Nấu canh
- Bắc nồi lên bếp, cho 1 muỗng dầu vào.
- Phi thơm tỏi, hành băm.
- Thêm nước, đun sôi.
- Cho mướp nhồi thịt vào, nấu khoảng 20 phút.
- Nêm nước mắm, hạt nêm cho vừa ăn.

Bước 5: Hoàn thành
- Vớt mướp ra đĩa (nếu luộc) hoặc múc canh ra tô.
- Rắc hành lá, rau mùi lên trên.
- Ăn nóng với cơm và nước mắm.',
30, 20, 3),

-- 19. Bò xào cần tây
('Bò xào cần tây',
'/images/bo-xao-can-tay.jpg',
'Món ăn hiện đại, bổ dưỡng với thịt bò mềm, thơm kết hợp cùng cần tây giòn ngọt. Món này dễ làm, nhanh gọn, phù hợp cho bữa cơm bận rộn nhưng vẫn đầy đủ dinh dưỡng.',
'- Thịt bò thăn: 300g
- Cần tây: 200g
- Tỏi: 3 tép
- Hành tây: 1/2 củ
- Dầu ăn: 2 muỗng canh

Gia vị ướp bò:
- Nước tương: 1 muỗng canh
- Dầu hào: 1 muỗng cà phê
- Bột năng: 1 muỗng cà phê
- Hạt tiêu: 1/3 muỗng cà phê
- Dầu ăn: 1 muỗng cà phê

Gia vị nêm:
- Nước tương: 1/2 muỗng canh
- Dầu hào: 1 muỗng cà phê
- Đường: 1/4 muỗng cà phê
- Hạt nêm: 1/2 muỗng cà phê',
'Bước 1: Sơ chế thịt bò
- Thịt bò rửa sạch, thấm khô.
- Thái lát mỏng, ngang thớ thịt.

Bước 2: Ướp thịt bò
- Cho thịt bò vào tô.
- Thêm nước tương, dầu hào, bột năng, hạt tiêu, dầu ăn.
- Trộn đều, ướp ít nhất 15-20 phút.

Bước 3: Sơ chế rau củ
- Cần tây rửa sạch, cắt khúc xiên khoảng 4cm.
- Tỏi bóc vỏ, băm nhỏ hoặc thái lát.
- Hành tây bóc vỏ, thái múi cau.

Bước 4: Xào bò
- Bắc chảo/wok lên bếp, đun rất nóng.
- Cho 1 muỗng dầu vào.
- Cho thịt bò vào xào nhanh trên lửa lớn đến khi bò chín tái (khoảng 1-2 phút).
- Vớt bò ra đĩa riêng.

Bước 5: Xào cần tây
- Dùng lại chảo vừa xào bò.
- Cho thêm 1 muỗng dầu, phi thơm tỏi.
- Cho hành tây vào xào thơm.
- Cho cần tây vào, xào nhanh khoảng 2 phút.

Bước 6: Hoàn thành
- Cho thịt bò đã xào trở lại chảo.
- Nêm nước tương, dầu hào, đường, hạt nêm.
- Đảo nhanh tay khoảng 1 phút cho ngấm gia vị.
- Tắt bếp, múc ra đĩa.
- Ăn nóng với cơm.',
20, 8, 3),

-- 20. Canh bí xanh nấu tôm
('Canh bí xanh nấu tôm',
'/images/canh-bi-xanh-nau-tom.jpg',
'Món canh thanh mát, ngọt tự nhiên từ bí xanh và tôm tươi. Đây là món canh gia đình đơn giản, bổ dưỡng, thích hợp cho mọi lứa tuổi.',
'- Bí xanh (bí đao): 300g
- Tôm tươi: 150g
- Tỏi: 2 tép
- Hành tím: 2 củ
- Nước mắm: 1.5 muỗng canh
- Hạt nêm: 1 muỗng cà phê
- Dầu ăn: 1 muỗng canh
- Hạt tiêu
- Hành lá, rau mùi',
'Bước 1: Sơ chế bí xanh
- Bí xanh gọt vỏ, bỏ ruột.
- Rửa sạch, cắt miếng vuông vừa ăn hoặc hình quạt.

Bước 2: Sơ chế tôm
- Tôm rửa sạch, cắt bỏ râu, chân.
- Dùng tăm lấy chỉ lưng tôm.
- Để ráo nước.

Bước 3: Sơ chế gia vị
- Tỏi, hành tím bóc vỏ, băm nhỏ.
- Hành lá, rau mùi rửa sạch, thái nhỏ.

Bước 4: Nấu canh
- Bắc nồi lên bếp, cho 1 muỗng dầu vào.
- Phi thơm hành tím, tỏi.
- Cho tôm vào xào sơ đến khi tôm chuyển màu.
- Thêm khoảng 800ml nước, đun sôi.

Bước 5: Cho bí vào nấu
- Khi nước sôi, cho bí xanh vào.
- Nấu khoảng 10-12 phút cho bí chín mềm.
- Nêm nước mắm, hạt nêm cho vừa ăn.
- Nêm thêm chút hạt tiêu.

Bước 6: Hoàn thành
- Cho hành lá, rau mùi vào, tắt bếp.
- Múc ra tô, ăn nóng với cơm.',
15, 15, 3);

-- Mapping Dishes với Tags
INSERT INTO dish_tags (dish_id, tag_id) VALUES
-- Thịt rang cháy cạnh (dish_id = 1)
(1, 1),  -- Món mặn
(1, 5),  -- Món rang
(1, 14), -- Đậm đà
(1, 16), -- Thịt heo
(1, 24), -- Mọi mùa
(1, 26), -- Cơm gia đình
-- Đậu phụ sốt cà chua (dish_id = 2)
(2, 3),  -- Món xào
(2, 9),  -- Món chay
(2, 12), -- Chua
(2, 11), -- Ngọt
(2, 20), -- Đậu phụ
(2, 21), -- Rau củ
(2, 15), -- Thanh mát
(2, 24), -- Mọi mùa
(2, 26), -- Cơm gia đình
-- Rau muống xào tỏi (dish_id = 3)
(3, 3),  -- Món xào
(3, 9),  -- Món chay
(3, 21), -- Rau củ
(3, 15), -- Thanh mát
(3, 24), -- Mọi mùa
(3, 26), -- Cơm gia đình
-- Trứng đúc thịt (dish_id = 4)
(4, 1),  -- Món mặn
(4, 14), -- Đậm đà
(4, 16), -- Thịt heo
(4, 22), -- Trứng
(4, 24), -- Mọi mùa
(4, 26), -- Cơm gia đình
-- Sườn xào chua ngọt (dish_id = 5)
(5, 1),  -- Món mặn
(5, 3),  -- Món xào
(5, 12), -- Chua
(5, 11), -- Ngọt
(5, 16), -- Thịt heo
(5, 24), -- Mọi mùa
(5, 26), -- Cơm gia đình
-- Canh cua mồng tơi (dish_id = 6)
(6, 6),  -- Món canh
(6, 15), -- Thanh mát
(6, 19), -- Hải sản
(6, 21), -- Rau củ
(6, 24), -- Mọi mùa
(6, 26), -- Cơm gia đình
-- Gà rang gừng (dish_id = 7)
(7, 1),  -- Món mặn
(7, 5),  -- Món rang
(7, 10), -- Cay
(7, 14), -- Đậm đà
(7, 17), -- Thịt gà
(7, 24), -- Mọi mùa
(7, 26), -- Cơm gia đình
-- Thịt kho tàu (dish_id = 8)
(8, 1),  -- Món mặn
(8, 4),  -- Món kho
(8, 8),  -- Đặc sản
(8, 14), -- Đậm đà
(8, 16), -- Thịt heo
(8, 22), -- Trứng
(8, 25), -- Ngày lễ
-- Cá kho tộ (dish_id = 9)
(9, 1),  -- Món mặn
(9, 4),  -- Món kho
(9, 8),  -- Đặc sản
(9, 14), -- Đậm đà
(9, 18), -- Cá
(9, 25), -- Ngày lễ
-- Lạc rang muối (dish_id = 10)
(10, 2),  -- Món chiên
(10, 5),  -- Món rang
(10, 7),  -- Món khai vị
(10, 23), -- Đậu
(10, 24); -- Mọi mùa


-- Mapping 10 món mới với Tags
INSERT INTO dish_tags (dish_id, tag_id) VALUES
-- Canh rau ngót thịt băm (dish_id = 11)
(11, 6),  -- Món canh
(11, 15), -- Thanh mát
(11, 16), -- Thịt heo
(11, 21), -- Rau củ
(11, 24), -- Mọi mùa
(11, 26), -- Cơm gia đình
-- Tôm rim thịt ba chỉ (dish_id = 12)
(12, 1),  -- Món mặn
(12, 27), -- Món rim
(12, 14), -- Đậm đà
(12, 16), -- Thịt heo
(12, 32), -- Tôm
(12, 24), -- Mọi mùa
(12, 26), -- Cơm gia đình
-- Thịt luộc cà pháo (dish_id = 13)
(13, 1),  -- Món mặn
(13, 28), -- Món luộc
(13, 15), -- Thanh mát
(13, 16), -- Thịt heo
(13, 24), -- Mọi mùa
(13, 25), -- Ngày lễ
-- Cá rán giòn (dish_id = 14)
(14, 1),  -- Món mặn
(14, 29), -- Món rán
(14, 18), -- Cá
(14, 24), -- Mọi mùa
(14, 26), -- Cơm gia đình
-- Bắp cải xào (dish_id = 15)
(15, 3),  -- Món xào
(15, 9),  -- Món chay
(15, 21), -- Rau củ
(15, 15), -- Thanh mát
(15, 24), -- Mọi mùa
(15, 26), -- Cơm gia đình
-- Canh sườn chua (dish_id = 16)
(16, 6),  -- Món canh
(16, 12), -- Chua
(16, 15), -- Thanh mát
(16, 16), -- Thịt heo
(16, 24), -- Mọi mùa
(16, 26), -- Cơm gia đình
-- Chả lá lốt (dish_id = 17)
(17, 1),  -- Món mặn
(17, 7),  -- Món khai vị
(17, 14), -- Đậm đà
(17, 16), -- Thịt heo
(17, 24), -- Mọi mùa
(17, 25), -- Ngày lễ
-- Mướp đắng nhồi thịt (dish_id = 18)
(18, 1),  -- Món mặn
(18, 30), -- Món nhồi
(18, 15), -- Thanh mát
(18, 16), -- Thịt heo
(18, 21), -- Rau củ
(18, 24), -- Mọi mùa
(18, 26), -- Cơm gia đình
-- Bò xào cần tây (dish_id = 19)
(19, 1),  -- Món mặn
(19, 3),  -- Món xào
(19, 14), -- Đậm đà
(19, 31), -- Thịt bò
(19, 21), -- Rau củ
(19, 24), -- Mọi mùa
(19, 26), -- Cơm gia đình
-- Canh bí xanh nấu tôm (dish_id = 20)
(20, 6),  -- Món canh
(20, 15), -- Thanh mát
(20, 32), -- Tôm
(20, 21), -- Rau củ
(20, 33), -- Bí đao
(20, 24), -- Mọi mùa
(20, 26); -- Cơm gia đình


