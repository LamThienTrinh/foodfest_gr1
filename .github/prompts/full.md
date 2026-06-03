# FoodFest Family Space - Bản Plan Tổng Quan Dễ Hiểu

Tài liệu này mô tả toàn bộ phần **Family Space** của FoodFest theo cách một người mới vào dự án có thể hiểu mà chưa cần đọc code.

Family Space là khu vực để nhiều thành viên trong một gia đình cùng lập kế hoạch bữa ăn, quản lý nguyên liệu trong nhà, tạo danh sách mua sắm, vote món ăn, ghi chú với nhau và nhận thông báo khi có việc cần xử lý.

---

## 1. Mục Tiêu Sản Phẩm

FoodFest ban đầu có các phần như catalog món ăn, bài viết, món cá nhân và profile. Family Space mở rộng app theo hướng dùng chung trong gia đình.

Mục tiêu chính:

- Một user có thể tạo hoặc tham gia nhiều gia đình.
- Mỗi gia đình có danh sách thành viên riêng.
- Gia đình có thể lên menu theo tuần.
- Mỗi bữa ăn có thể chứa nhiều món từ catalog hoặc món cá nhân.
- Gia đình có thể lưu một bữa ăn thành preset để dùng lại.
- Thành viên có thể vote món trong từng bữa.
- Gia đình có Pantry để quản lý nguyên liệu đang có.
- App gợi ý món dựa trên nguyên liệu trong Pantry.
- App tạo Shopping List từ menu tuần và trừ đi nguyên liệu đã có trong Pantry.
- Shopping List có checklist để nhiều người cùng tick đã mua/đã chuẩn bị.
- App có Notifications chung cho lời mời gia đình, like bài viết, Pantry hết hạn, v.v.
- Phase 6 có scheduler backend để tự scan Pantry expiry và push notification khi user không mở app.

Vòng trải nghiệm chính:

```text
Discover -> Plan -> Vote -> Chat/Notes -> Shop -> Cook -> Share -> Engage
```

Giải thích nhanh:

- Discover: user xem món ở Home/Catalog.
- Plan: gia đình lên thực đơn tuần.
- Vote: thành viên vote món muốn ăn.
- Chat/Notes: gia đình trao đổi nhanh.
- Shop: app tạo shopping list.
- Cook: checklist + Pantry hỗ trợ chuẩn bị nấu.
- Share: user đăng ảnh/bài viết lên feed.
- Engage: user khác like/comment, tạo thông báo.

---

## 2. Các Khái Niệm Cốt Lõi

### User

User là tài khoản đăng nhập vào FoodFest.

User có:

- `username`
- `fullName`
- `avatarUrl`
- profile public
- các món cá nhân
- các bài post

### Family

Family là một nhóm gia đình trong app.

Một user có thể thuộc nhiều Family. Ví dụ:

- Gia đình nhà mình
- Nhóm nấu ăn với bạn bè
- Gia đình bên ngoại

Mỗi Family có:

- tên gia đình
- owner
- danh sách thành viên
- menu tuần
- saved meals
- pantry
- notes/chat nhẹ
- shopping lists

### Family Member

Family Member là quan hệ giữa user và family.

Thông tin này nằm theo từng family, không thay đổi profile public của user.

Một member có:

- `familyId`
- `userId`
- `role`: `owner` hoặc `member`
- `nickname`: biệt danh trong riêng family đó
- `joinedAt`

Cách hiển thị tên member:

```text
nickname -> fullName -> username
```

Ví dụ user public tên là "Nguyễn Văn A", nhưng trong family có nickname là "Bố". Trong Family Space sẽ ưu tiên hiển thị "Bố".

### Dish

Dish là món ăn trong catalog chung của hệ thống.

### Personal Dish

Personal Dish là món user tự tạo hoặc lưu riêng.

### Family Menu

Family Menu là một ô bữa ăn trong lịch tuần.

Một menu có:

- ngày ăn
- loại bữa: sáng, trưa, tối, phụ
- trạng thái
- danh sách món

### Family Saved Meal

Saved Meal là một bữa ăn/preset đã lưu để dùng lại.

Ví dụ:

- "Bữa tối đơn giản"
- "Combo cuối tuần"
- "Mâm hè"

Một Saved Meal chứa nhiều món. Khi dùng lại, app có thể add toàn bộ các món đó vào một bữa trong tuần.

### Pantry

Pantry là kho nguyên liệu của gia đình.

Mỗi item có:

- tên nguyên liệu
- số lượng
- đơn vị
- ngày hết hạn nếu có

### Shopping List

Shopping List là danh sách cần mua, tạo từ menu tuần.

App phân tích món trong tuần, lấy nguyên liệu cần dùng, trừ đi nguyên liệu có trong Pantry, rồi tạo danh sách còn thiếu.

### Notification

Notification là inbox chung của app.

Notification không chỉ dành cho family. Nó được thiết kế để mở rộng cho nhiều loại thông báo:

- lời mời vào family
- like bài viết
- Pantry sắp hết hạn
- Pantry đã hết hạn
- comment/follow sau này

---

## 3. Luồng Người Dùng Chính

### 3.1 Tạo Gia Đình Và Mời Thành Viên

Luồng:

1. User vào Family tab hoặc Profile -> My Families.
2. Nếu chưa có family, user tạo family mới.
3. User tạo family sẽ là owner.
4. Owner mời thành viên bằng username.
5. App tạo `family_invites`.
6. App tạo notification `family_invite` cho người được mời.
7. Người được mời vào màn Notifications hoặc Family Home để accept/decline.
8. Nếu accept, user được thêm vào `family_members`.
9. Notification lời mời được mark read.

Quy tắc:

- Owner có thể mời member.
- Member thường không tự thêm người khác trong version hiện tại.
- Owner không được mời chính mình.
- Không tạo lời mời trùng nếu đã có pending invite.
- User đã là member thì không thể được mời lại.

### 3.2 Đổi Biệt Danh Trong Gia Đình

Luồng:

1. User mở màn Family Members.
2. Trên card của chính user có action "Đổi biệt danh".
3. User nhập nickname mới hoặc xóa nickname.
4. App cập nhật `family_members.nickname`.
5. UI hiển thị lại theo ưu tiên `nickname -> fullName -> username`.

Quy tắc:

- User chỉ được đổi nickname của chính mình.
- Owner cũng không sửa nickname người khác trong version hiện tại.
- Nickname chỉ áp dụng trong family hiện tại.
- Nickname không đổi Auth profile/public profile.
- Nickname không cần unique.

### 3.3 Lên Menu Tuần

Luồng:

1. User mở Weekly Menu.
2. App hiển thị lịch theo tuần.
3. Mỗi ô là một bữa ăn theo ngày + meal type.
4. User bấm vào ô để mở Day Menu.
5. Nếu bữa chưa có món, user thêm món từ picker.
6. User có thể chọn món từ system dish, personal dish hoặc recent.
7. User có thể lưu bữa hiện tại thành Family Saved Meal.
8. User có thể generate Shopping List từ menu tuần.

Quy tắc dọn dẹp:

- Menu tuần đã qua có thể bị xóa nếu chưa được lưu thành preset.
- Menu đã lưu preset thì giữ lại.

### 3.4 Thêm Món Vào Một Bữa

Trong Day Menu, user có thể thêm món bằng Menu Item Picker.

Picker có:

- System Dishes
- My Dishes
- Recent
- Search/filter
- Match score từ Pantry nếu có dữ liệu nguyên liệu

Mỗi món trong picker có thể hiển thị trạng thái Pantry:

- Có ngay: đủ khoảng 80-100% nguyên liệu
- Thiếu vài món: đủ khoảng 50-79%
- Thiếu nhiều: dưới 50%

Ví dụ:

```text
Món A cần: thịt heo, rau muống, mắm tôm
Pantry có: thịt heo, rau muống
Match score = 2/3 = 66%
Hiển thị: Thiếu 1 item: mắm tôm
```

### 3.5 Lưu Và Dùng Lại Saved Meal

Luồng lưu:

1. User mở Day Menu.
2. User bấm "Save as Family Preset".
3. Nhập tên preset.
4. App lưu danh sách món hiện tại vào `family_saved_meals` và `family_saved_meal_items`.

Luồng dùng lại:

1. User mở Saved Meals.
2. Chọn preset.
3. Chọn ngày và bữa muốn áp dụng.
4. App add toàn bộ món từ preset vào Family Menu tương ứng.

Quy tắc:

- Saved Meal thuộc về family.
- Một family có thể có nhiều preset.
- Preset giúp lập menu nhanh hơn.

### 3.6 Vote Món Trong Bữa

Luồng:

1. User mở Day Menu.
2. User bấm icon vote.
3. App mở Vote Modal compact.
4. Mỗi món trong bữa hiển thị số vote up/down.
5. User vote một món.
6. App cập nhật count và đóng modal.

Quy tắc:

- Vote lưu theo `family_menu_item_id` + `user_id`.
- Một user chỉ có một vote trên một món.
- Nếu bấm lại cùng vote type thì có thể bỏ vote.
- Nếu đổi từ up sang down thì cập nhật vote type.

Vote gần đây:

- Family Home có shortcut "Vote gần đây".
- Màn Vote gần đây hiển thị ai vote, vote món gì, vote up/down, ngày/bữa nào.
- Bấm vào một vote sẽ mở đúng bữa ăn đó và tự mở Vote Modal.

### 3.7 Family Notes

Family Notes là chat nhẹ cho gia đình.

Luồng:

1. User mở Family Notes từ Family Home.
2. User nhập ghi chú hoặc tin nhắn ngắn.
3. App lưu vào `family_notes`.
4. Client polling mỗi 5 giây để thấy note mới từ thành viên khác.

Quy tắc:

- Chỉ member của family mới đọc/gửi note.
- Đây là MVP chat nhẹ, chưa có thread/reaction/pin.

### 3.8 Pantry Management

Luồng:

1. User mở Pantry.
2. App hiển thị nguyên liệu trong family.
3. User thêm/sửa/xóa nguyên liệu.
4. User có thể chọn ngày hết hạn bằng date picker.
5. User có thể search/filter.
6. User có quick action xóa item đã hết hạn.

Mỗi Pantry item có:

- tên nguyên liệu
- số lượng
- đơn vị
- ngày hết hạn
- created/updated time

Quy tắc:

- Pantry thuộc family, không thuộc riêng một user.
- Thành viên trong family cùng xem và cập nhật Pantry.
- Ngày hết hạn dùng cho notification expiry.

### 3.9 Shopping List Và Checklist

Luồng generate:

1. User mở Weekly Menu.
2. Bấm "Generate Shopping List".
3. Backend lấy tất cả món trong tuần.
4. Backend trích nguyên liệu từ dish/personal dish.
5. Backend gom nguyên liệu giống nhau.
6. Backend trừ đi số lượng đang có trong Pantry.
7. Backend tạo Shopping List với các item còn thiếu.

Luồng checklist:

1. User mở Shopping List.
2. Mỗi item có checkbox đã mua/đã chuẩn bị.
3. User có thể assign item cho chính mình.
4. User nhập "đã dùng bao nhiêu" để chuẩn bị trừ Pantry sau này.
5. App ghi activity log.
6. Client polling mỗi 5 giây để cập nhật khi thành viên khác tick item.
7. Sau khi mua xong, user bấm cập nhật Pantry.
8. App add các item đã mua vào Pantry.

Quy tắc realtime MVP:

- Chưa dùng WebSocket.
- Dùng polling 5 giây.
- Quiet polling không được làm mất draft input user đang nhập.

### 3.10 Notifications Inbox

Notifications là inbox chung của app.

Hiện hỗ trợ:

- `family_invite`
- `post_like`
- `pantry_expiring`
- `pantry_expired`

Mỗi notification có:

- user nhận
- type
- title
- message
- related entity
- actionUrl
- read/unread
- createdAt

Luồng click notification:

- Bấm notification sẽ mark read.
- Nếu actionUrl là Pantry thì mở Family Pantry đúng family.
- Nếu là lời mời family thì mở Family Home để accept/decline.
- Các route family khác fallback về Family Home.

Pantry expiry notification:

- Scan-on-open: khi user mở Notifications hoặc app gọi unread count.
- Background scheduler: backend scan định kỳ ngay cả khi user không mở app.
- Push: nếu user có device token và server có FCM key.

---

## 4. Phase Theo Lộ Trình

### Phase 3 - Family Space MVP

Mục tiêu: có không gian gia đình cơ bản để lập menu và quản lý member.

Bao gồm:

- Family Home.
- Tạo nhiều family.
- Chọn family hiện tại nếu user thuộc nhiều family.
- Quản lý member.
- Mời thành viên bằng username.
- Notification lời mời.
- Đổi nickname cá nhân trong family.
- Weekly Menu.
- Day Menu.
- Menu Item Picker.
- Saved Meals.
- Vote Modal.
- Vote gần đây.

Exit criteria:

- User tạo family được.
- User mời member được.
- Người được mời nhận notification và accept/decline được.
- User xem và sửa menu tuần được.
- User lưu bữa thành preset được.
- User dùng preset để add vào bữa được.
- Vote hoạt động.
- Vote gần đây mở đúng bữa.

### Phase 4 - Smart Automation MVP

Mục tiêu: thêm tự động hóa dựa trên Pantry và menu.

Bao gồm:

- Pantry management.
- Date picker cho expiry date.
- Recipe suggestion rule-based theo Pantry.
- Match score trong Dish Catalog/Menu Picker.
- Generate Shopping List từ Weekly Menu.
- Trừ tồn kho Pantry khi tạo Shopping List.
- Checklist mua sắm/nấu nướng.
- Activity log checklist.
- Sync item đã mua về Pantry.
- Pantry expiry notification scan-on-open.

Exit criteria:

- User thêm/sửa/xóa Pantry item được.
- Expiry date chọn bằng lịch được.
- Món hiển thị match score theo Pantry.
- Shopping List tạo được từ menu tuần.
- Shopping List trừ nguyên liệu đã có trong Pantry.
- Checklist tick/update được.
- Sync item đã mua về Pantry được.
- Notification hết hạn/sắp hết hạn tạo được khi mở Notifications/unread count.

### Phase 5 - Realtime Và Experience Loop

Mục tiêu: làm flow gia đình mượt hơn và đóng vòng trải nghiệm.

Bao gồm:

- Checklist polling 5 giây.
- Quiet sync không làm mất draft input.
- Vote Modal compact.
- Vote xong cập nhật count và đóng modal.
- Family Notes polling 5 giây.
- Shortcut Family Notes từ Family Home.
- Flow Plan -> Vote -> Notes -> Shop -> Cook -> Share.

Exit criteria:

- Member A tick checklist, member B thấy sau polling.
- User vote món, count cập nhật.
- User gửi Family Note, người khác thấy sau polling.
- User đi được flow Weekly Menu -> Generate Shopping List -> Checklist -> Update Pantry -> Notes.

### Phase 6 - Background Jobs Và Push Notifications

Mục tiêu: Pantry expiry vẫn báo được khi user không mở app.

Bao gồm:

- Backend scheduler chạy nền trong Ktor.
- Scheduler scan toàn bộ Pantry item có expiry date.
- Tạo notification cho toàn bộ member của family.
- Không tạo duplicate notification.
- Lưu device token per user/device.
- Push notification qua FCM legacy nếu có `FCM_SERVER_KEY`.
- Log job run.
- Log từng delivery result.
- Push fail không làm fail app API hoặc làm chết scheduler loop.

Cấu hình runtime:

```env
PANTRY_EXPIRY_SCHEDULER_ENABLED=true
PANTRY_EXPIRY_SCHEDULER_INTERVAL_HOURS=12
PANTRY_EXPIRY_SCHEDULER_RUN_ON_STARTUP=true
FCM_SERVER_KEY=...
FCM_ENDPOINT=https://fcm.googleapis.com/fcm/send
```

Quy tắc:

- Nếu thiếu `FCM_SERVER_KEY`, delivery log là `skipped`.
- Nếu user chưa có active device token, delivery log là `skipped`.
- Inbox notification vẫn được tạo dù push bị skip/fail.
- Push chỉ gửi cho notification mới được insert, tránh spam khi job chạy lại.

Exit criteria:

- Scheduler chạy khi backend start nếu enabled.
- Scheduler chạy lại theo interval.
- Pantry expiry notification được tạo cho tất cả family members.
- Duplicate được chặn bằng unique guard.
- Device token có API đăng ký/hủy.
- Job/delivery có log.
- `./gradlew check --no-daemon` pass.

---

## 5. Kiến Trúc Dữ Liệu

### family_groups

Lưu family.

Các field chính:

- `family_id`
- `name`
- `owner_user_id`
- `created_at`

### family_members

Lưu user thuộc family nào.

Các field chính:

- `family_id`
- `user_id`
- `role`
- `nickname`
- `joined_at`

Primary key:

```text
(family_id, user_id)
```

### family_invites

Lưu lời mời vào family.

Các field chính:

- `family_invite_id`
- `family_id`
- `invited_user_id`
- `invited_by_user_id`
- `status`: pending, accepted, declined
- `created_at`
- `responded_at`

### family_menus

Lưu một bữa ăn theo ngày.

Các field chính:

- `family_menu_id`
- `family_id`
- `menu_date`
- `meal_type`
- `status`
- `is_saved`
- `created_at`

### family_menu_items

Lưu món trong một bữa.

Các field chính:

- `family_menu_item_id`
- `family_menu_id`
- `dish_id`
- `personal_dish_id`
- `note`
- `created_at`

### family_menu_votes

Lưu vote của user cho món trong bữa.

Các field chính:

- `family_menu_item_id`
- `user_id`
- `vote_type`
- `created_at`

Primary key:

```text
(family_menu_item_id, user_id)
```

### family_saved_meals

Lưu preset bữa ăn.

Các field chính:

- `family_saved_meal_id`
- `family_id`
- `preset_name`
- `created_by_user_id`
- `created_at`

### family_saved_meal_items

Lưu món trong preset.

Các field chính:

- `family_saved_meal_item_id`
- `family_saved_meal_id`
- `dish_id`
- `personal_dish_id`
- `note`
- `created_at`

### family_pantry_items

Lưu nguyên liệu trong Pantry.

Các field chính:

- `family_pantry_item_id`
- `family_id`
- `ingredient_name`
- `quantity`
- `unit`
- `expiry_date`
- `created_at`
- `updated_at`

### family_shopping_lists

Lưu shopping list được tạo từ menu tuần.

Các field chính:

- `family_shopping_list_id`
- `family_id`
- `menu_week`
- `status`
- `created_at`
- `updated_at`

### family_shopping_list_items

Lưu item cần mua/cần chuẩn bị.

Các field chính:

- `family_shopping_list_item_id`
- `family_shopping_list_id`
- `ingredient_name`
- `required_qty`
- `unit`
- `category`
- `note`
- `is_purchased`
- `assigned_to_user_id`
- `used_qty`
- `purchased_at`
- `created_at`
- `updated_at`

### family_shopping_list_activity

Lưu activity log của checklist.

Các field chính:

- `family_shopping_list_activity_id`
- `family_shopping_list_id`
- `family_shopping_list_item_id`
- `actor_user_id`
- `action`
- `message`
- `created_at`

### family_notes

Lưu chat/notes nhẹ của family.

Các field chính:

- `family_note_id`
- `family_id`
- `user_id`
- `message`
- `created_at`

### notifications

Lưu inbox notification chung.

Các field chính:

- `notification_id`
- `user_id`
- `type`
- `title`
- `message`
- `related_entity_type`
- `related_entity_id`
- `action_url`
- `is_read`
- `created_at`

Unique guard quan trọng:

```text
(user_id, type, related_entity_type, related_entity_id)
```

Dùng để tránh spam duplicate cho:

- family invite
- pantry expiry

### push_device_tokens

Lưu token push của từng user/device.

Các field chính:

- `push_device_token_id`
- `user_id`
- `platform`: android, ios, web
- `token`
- `is_active`
- `created_at`
- `updated_at`

### notification_job_runs

Lưu log mỗi lần scheduler chạy.

Các field chính:

- `notification_job_run_id`
- `job_name`
- `started_at`
- `finished_at`
- `status`
- `inserted_count`
- `push_attempted_count`
- `push_sent_count`
- `error_message`

### notification_delivery_logs

Lưu kết quả push từng notification/device.

Các field chính:

- `notification_delivery_log_id`
- `notification_id`
- `user_id`
- `push_device_token_id`
- `provider`
- `status`: sent, failed, skipped
- `response_message`
- `created_at`

---

## 6. API Chính

### Families

```text
GET    /api/families
POST   /api/families
PUT    /api/families/{familyId}
DELETE /api/families/{familyId}/leave
```

### Family Members

```text
GET    /api/families/{familyId}/members
POST   /api/families/{familyId}/members
DELETE /api/families/{familyId}/members/{userId}
PUT    /api/families/{familyId}/members/me/nickname
```

### Family Invites

```text
POST /api/families/{familyId}/invites
GET  /api/families/invites/me
POST /api/families/invites/{inviteId}/respond
```

### Family Menus

```text
GET    /api/families/{familyId}/menus/week
GET    /api/families/{familyId}/menus/recent
POST   /api/families/{familyId}/menus
POST   /api/families/{familyId}/menus/{menuId}/items
DELETE /api/families/{familyId}/menus/{menuId}/items/{itemId}
```

### Votes

```text
POST   /api/families/{familyId}/menus/{menuId}/items/{itemId}/vote
DELETE /api/families/{familyId}/menus/{menuId}/items/{itemId}/vote
GET    /api/families/{familyId}/menus/{menuId}/votes
GET    /api/families/{familyId}/votes/recent
```

### Saved Meals

```text
GET    /api/families/{familyId}/saved-meals
POST   /api/families/{familyId}/menus/{menuId}/saved-meals
GET    /api/families/{familyId}/saved-meals/{savedMealId}
POST   /api/families/{familyId}/saved-meals/{savedMealId}/apply
DELETE /api/families/{familyId}/saved-meals/{savedMealId}
```

### Pantry

```text
GET    /api/families/{familyId}/pantry
POST   /api/families/{familyId}/pantry
PUT    /api/families/{familyId}/pantry/{itemId}
DELETE /api/families/{familyId}/pantry/{itemId}
DELETE /api/families/{familyId}/pantry/expired
```

### Shopping List

```text
POST /api/families/{familyId}/shopping-lists/generate
GET  /api/families/{familyId}/shopping-lists/{shoppingListId}
PUT  /api/families/{familyId}/shopping-lists/{shoppingListId}/items/{itemId}
POST /api/families/{familyId}/shopping-lists/{shoppingListId}/mark-all-purchased
POST /api/families/{familyId}/shopping-lists/{shoppingListId}/sync-pantry
```

### Family Notes

```text
GET  /api/families/{familyId}/notes
POST /api/families/{familyId}/notes
```

### Notifications

```text
GET    /api/notifications
GET    /api/notifications/unread-count
PUT    /api/notifications/{notificationId}/read
PUT    /api/notifications/read-all
POST   /api/notifications/device-tokens
DELETE /api/notifications/device-tokens
```

---

## 7. Quy Tắc Bảo Mật Và Phân Quyền

Tất cả API family cần JWT auth.

Quy tắc chung:

- User phải là member của family thì mới xem/sửa dữ liệu family.
- Owner có quyền mời/xóa member.
- User chỉ tự đổi nickname của mình.
- User chỉ đọc/gửi notes trong family của mình.
- User chỉ xem shopping list/pantry/menu của family mình.
- Notification chỉ trả về notification của chính user đang đăng nhập.
- Device token gắn với user hiện tại từ JWT.

---

## 8. Quy Tắc Notification Và Push

### Idempotency

Không spam duplicate notification.

Pantry expiry dùng unique theo:

```text
user_id + type + related_entity_type + related_entity_id
```

Ví dụ cùng một pantry item sắp hết hạn thì mỗi user chỉ nhận một `pantry_expiring`.

Khi item chuyển từ sắp hết hạn sang đã hết hạn:

- Tạo `pantry_expired` mới nếu chưa có.
- Mark read notification `pantry_expiring` cũ nếu còn unread.

### Push Delivery

Push chỉ là delivery channel. Inbox notification vẫn là source of truth.

Nếu push gửi thất bại:

- Không xóa notification.
- Không rollback notification.
- Ghi delivery log.
- Scheduler vẫn tiếp tục item tiếp theo.

---

## 9. Quy Ước UI

Mọi màn Family nên giữ cùng style app hiện tại:

- dùng màu từ `AppColors`
- dùng card bo góc/elevation thống nhất
- dùng `FoodFestTextField` khi nhập text
- loading/empty/error state rõ ràng
- top bar có back button
- tránh tạo UI component mới nếu component cũ dùng được

Các màn chính:

- FamilyHomeScreen
- FamilyMembersScreen
- FamilyWeeklyMenuScreen
- FamilyDayMenuScreen
- FamilySavedMealsScreen
- FamilyPantryScreen
- FamilyShoppingListScreen
- FamilyNotesScreen
- FamilyRecentVotesScreen
- NotificationScreen

---

## 10. Checklist Test Thủ Công

### Family

- Tạo family mới.
- User thuộc nhiều family và đổi family context.
- Mời user khác bằng username.
- Accept/decline invite.
- Đổi/xóa nickname của chính mình.
- Owner xóa member.

### Menu

- Xem weekly menu.
- Tạo menu slot.
- Thêm system dish.
- Thêm personal dish.
- Xóa item khỏi menu.
- Lưu bữa thành saved meal.
- Apply saved meal vào ngày/bữa khác.

### Vote

- Mở vote modal.
- Vote up/down.
- Vote lại để remove hoặc đổi vote.
- Mở Vote gần đây.
- Bấm vote gần đây và kiểm tra mở đúng bữa.

### Pantry

- Thêm nguyên liệu.
- Sửa số lượng/đơn vị.
- Chọn expiry date từ lịch.
- Search/filter.
- Xóa item hết hạn.
- Kiểm tra match score trong picker.

### Shopping List

- Generate shopping list từ menu tuần.
- Kiểm tra item đã trừ Pantry.
- Tick purchased.
- Assign item cho mình.
- Nhập used qty.
- Xem activity log.
- Sync purchased item về Pantry.
- Mở hai client và kiểm tra polling sync.

### Notes

- Gửi note.
- User khác thấy note sau polling.
- Non-member không đọc/gửi được.

### Notifications

- Family invite tạo notification.
- Post like tạo notification.
- Pantry expiring tạo notification.
- Pantry expired tạo notification.
- Bấm notification mở đúng màn liên quan.
- Mark read.
- Mark all read.

### Phase 6 Scheduler/Push

- Bật backend với scheduler enabled.
- Tạo pantry item hết hạn hoặc sắp hết hạn.
- Chạy job startup hoặc đợi interval.
- Kiểm tra notification được tạo cho tất cả member.
- Chạy job lại và kiểm tra không duplicate.
- Đăng ký device token bằng API.
- Cấu hình `FCM_SERVER_KEY`.
- Kiểm tra delivery log `sent`, `failed` hoặc `skipped`.

---

## 11. Trạng Thái Hiện Tại

Đã hoàn thành theo plan hiện tại:

- Phase 3 Family Space MVP.
- Multiple families.
- Family member nickname.
- Notifications inbox.
- Notification click navigation vào Family/Pantry/Invite.
- Saved Meals.
- Weekly Menu.
- Day Menu.
- Vote Modal.
- Recent Votes screen và deep-link vào đúng bữa.
- Phase 4 Pantry.
- Pantry expiry date picker.
- Recipe suggestion match score.
- Shopping List generator.
- Checklist detail + activity log.
- Update Pantry từ Shopping List.
- Phase 5 polling sync.
- Family Notes.
- Phase 6 background scheduler + push infrastructure.

Lưu ý còn phụ thuộc cấu hình môi trường:

- Push thật cần `FCM_SERVER_KEY`.
- App native cần lấy FCM/APNs token thật rồi gọi API register device token.
- Nếu chưa có token/key, notification vẫn vào inbox nhưng push delivery sẽ log `skipped`.

---

## 12. Bản Đồ File Liên Quan Theo Module

Mục này giúp người mới biết muốn sửa phần nào thì nên mở file nào trước.

Quy ước đọc file:

- Backend family thường nằm trong `server/src/main/kotlin/com/foodfest/app/features/family/`.
- Backend notification/push nằm trong `server/src/main/kotlin/com/foodfest/app/features/notification/`.
- Client Family UI nằm trong `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/`.
- Client Notification UI nằm trong `composeApp/src/commonMain/kotlin/com/foodfest/app/features/notification/`.
- DB migration nằm trong `database/migrations/`.

### 12.1 File Chung Cho Toàn Bộ Family Space

Các file nên đọc trước khi làm bất kỳ phần Family nào:

- `composeApp/src/commonMain/kotlin/com/foodfest/app/App.kt`: khai báo screen navigation, truyền `familyId`, mở Family Home, Weekly Menu, Pantry, Shopping List, Notes, Recent Votes, Notifications.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/data/FamilyModels.kt`: toàn bộ data model phía client cho Family.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/data/FamilyRepository.kt`: toàn bộ API call phía client cho Family.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyTable.kt`: DB table mapping, data class backend, repository logic chính của Family.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyService.kt`: validate request, business rules trước khi gọi repository.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyRoute.kt`: khai báo endpoint HTTP cho Family.
- `server/src/main/kotlin/com/foodfest/app/plugins/Databases.kt`: đăng ký table Exposed để auto-create/schema sync.
- `server/src/main/kotlin/com/foodfest/app/di/MainModule.kt`: khai báo dependency injection cho repository/service.

### 12.2 Family Home Và Multiple Families

Phần này xử lý dashboard gia đình, chọn family hiện tại, shortcut sang các màn khác.

Client:

- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyHomeScreen.kt`: UI Family Home, family selector, shortcut cards.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyHomeViewModel.kt`: load families, load members, load stats, chọn family.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/models/FamilyHomeModels.kt`: state/model riêng cho Family Home.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/App.kt`: route vào Family Home và truyền `initialFamilyId` khi mở từ notification.

Backend:

- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyTable.kt`: `FamilyGroupTable`, `FamilyMemberTable`, `createFamily`, `listFamiliesByUser`.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyService.kt`: validate tạo/rename/list family.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyRoute.kt`: `GET /api/families`, `POST /api/families`.

Migration:

- `database/migrations/V6__add_family_space_core_tables.sql`: core family tables.
- `database/migrations/V9__add_unique_family_name.sql`: unique rule cho family name.

### 12.3 Family Members, Invite Và Nickname

Phần này xử lý danh sách thành viên, mời thành viên, accept/decline invite, đổi biệt danh.

Client:

- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyMembersScreen.kt`: UI danh sách member, invite, nickname dialog.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyMembersViewModel.kt`: load member, invite, remove member, update nickname.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/models/FamilyMembersModels.kt`: state/model riêng cho member screen.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/data/FamilyRepository.kt`: `getFamilyMembers`, `createInvite`, `respondInvite`, `updateMyNickname`.

Backend:

- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyTable.kt`: `FamilyInviteTable`, `FamilyMemberTable`, invite/nickname repository methods.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyService.kt`: validate nickname max length, blank thành null, invite rules.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyRoute.kt`: member/invite/nickname endpoints.
- `server/src/main/kotlin/com/foodfest/app/features/notification/NotificationTable.kt`: tạo notification `family_invite`.

Migration:

- `database/migrations/V8__add_family_invites.sql`: bảng invite.
- `database/migrations/V13__add_family_member_nickname.sql`: cột `nickname` trong `family_members`.
- `database/migrations/V14__add_notifications.sql`: notification inbox dùng cho invite.

### 12.4 Weekly Menu Và Day Menu

Phần này xử lý lịch tuần và chi tiết một bữa.

Client:

- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyWeeklyMenuScreen.kt`: UI lịch tuần, mở từng bữa, generate shopping list.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyWeeklyMenuViewModel.kt`: load week, tạo menu slot, generate shopping list.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/models/FamilyWeeklyMenuModels.kt`: state/model weekly menu.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyDayMenuScreen.kt`: UI chi tiết bữa, picker, save preset, vote modal.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyDayMenuViewModel.kt`: load món trong bữa, add/remove item, load picker, pantry match, vote.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/models/FamilyDayMenuModels.kt`: state/model day menu, picker, vote.

Backend:

- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyTable.kt`: `FamilyMenuTable`, `FamilyMenuItemTable`, menu repository methods.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyService.kt`: validate menu date, meal type, menu item source.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyRoute.kt`: menu endpoints.

Migration:

- `database/migrations/V6__add_family_space_core_tables.sql`: menu/menu item/vote core tables.
- `database/migrations/V11__add_family_menu_saved_flag.sql`: `is_saved` để giữ menu đã lưu preset.

### 12.5 Saved Meals / Family Presets

Phần này xử lý lưu một bữa thành preset và dùng lại preset.

Client:

- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilySavedMealsScreen.kt`: UI danh sách saved meals và apply preset.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilySavedMealsViewModel.kt`: load saved meals, save/apply/delete.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/models/FamilySavedMealsModels.kt`: state/model saved meals.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyDayMenuScreen.kt`: dialog "Save as Family Preset".
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyDayMenuViewModel.kt`: logic save preset từ bữa hiện tại.

Backend:

- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyTable.kt`: `FamilySavedMealTable`, `FamilySavedMealItemTable`, save/apply/delete repository methods.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyService.kt`: validate preset name và apply target menu.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyRoute.kt`: saved meal endpoints.

Migration:

- `database/migrations/V10__add_family_saved_meals.sql`: bảng saved meals và saved meal items.
- `database/migrations/V11__add_family_menu_saved_flag.sql`: đánh dấu menu đã lưu preset.

### 12.6 Vote Modal Và Recent Votes

Phần này xử lý vote trong bữa và danh sách vote gần đây.

Client:

- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyDayMenuScreen.kt`: vote modal compact trong Day Menu.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyDayMenuViewModel.kt`: load vote summary, vote/unvote, cập nhật count.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyRecentVotesScreen.kt`: UI danh sách vote gần đây.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyRecentVotesViewModel.kt`: load recent votes.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/models/FamilyRecentVotesModels.kt`: state/model recent votes.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/App.kt`: deep-link từ recent vote vào đúng Day Menu và tự mở vote modal.

Backend:

- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyTable.kt`: `FamilyMenuVoteTable`, vote repository methods, `listRecentVotes`.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyService.kt`: validate vote type.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyRoute.kt`: vote endpoints và `GET /api/families/{familyId}/votes/recent`.

Migration:

- `database/migrations/V6__add_family_space_core_tables.sql`: bảng `family_menu_votes`.

### 12.7 Pantry Và Recipe Suggestion

Phần này xử lý kho nguyên liệu, expiry date, match score món ăn.

Client:

- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyPantryScreen.kt`: UI Pantry, add/edit/bulk edit, date picker expiry.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyPantryViewModel.kt`: load/create/update/delete Pantry item, bulk edit, search/filter.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/models/FamilyPantryModels.kt`: state/model Pantry.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyDayMenuViewModel.kt`: tính match score dựa trên Pantry cho picker.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyDayMenuScreen.kt`: hiển thị match badge trong picker.

Backend:

- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyTable.kt`: `FamilyPantryItemTable`, pantry repository methods.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyService.kt`: validate ingredient, quantity, expiry date.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyRoute.kt`: pantry endpoints.

Migration:

- `database/migrations/V12__add_family_pantry_items.sql`: bảng Pantry và index expiry.

### 12.8 Shopping List Và Checklist

Phần này xử lý generate shopping list từ menu tuần, checklist mua sắm/nấu nướng, sync Pantry.

Client:

- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyShoppingListScreen.kt`: UI shopping list, checkbox, assign, used qty, activity log, sync Pantry.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyShoppingListViewModel.kt`: load detail, polling quiet sync, update item, mark all, sync Pantry.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/models/FamilyShoppingListModels.kt`: state/model shopping list.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyWeeklyMenuScreen.kt`: button Generate Shopping List.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyWeeklyMenuViewModel.kt`: gọi generate shopping list.

Backend:

- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyTable.kt`: `FamilyShoppingListTable`, `FamilyShoppingListItemTable`, `FamilyShoppingListActivityTable`, extract/trừ Pantry/generate/sync logic.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyService.kt`: validate shopping list request/item update.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyRoute.kt`: shopping list endpoints.

Migration:

- `database/migrations/V15__add_family_shopping_lists.sql`: bảng shopping list, items, activity.

### 12.9 Family Notes / Chat Nhẹ

Phần này xử lý ghi chú/chat nhẹ trong gia đình.

Client:

- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyNotesScreen.kt`: UI notes/chat, polling 5 giây.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyNotesViewModel.kt`: load/send note.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/models/FamilyNotesModels.kt`: state/model notes.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/family/presentation/FamilyHomeScreen.kt`: shortcut Family Notes.

Backend:

- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyTable.kt`: `FamilyNoteTable`, list/create note.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyService.kt`: validate message và member permission.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyRoute.kt`: notes endpoints.

Migration:

- `database/migrations/V16__add_family_notes.sql`: bảng family notes.

### 12.10 Notifications Inbox

Phần này xử lý inbox thông báo trong app.

Client:

- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/notification/presentation/NotificationScreen.kt`: UI danh sách notification, mark read, click navigation.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/notification/presentation/NotificationViewModel.kt`: load notification, unread count, mark read/all read.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/notification/data/NotificationModels.kt`: model notification, unread count, push token request.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/notification/data/NotificationRepository.kt`: API notification và register/deactivate push token.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/App.kt`: xử lý `actionUrl` để mở Family/Pantry/Invite.

Backend:

- `server/src/main/kotlin/com/foodfest/app/features/notification/NotificationTable.kt`: `NotificationTable`, scan Pantry expiry, mark read, device token, job/delivery logs.
- `server/src/main/kotlin/com/foodfest/app/features/notification/NotificationService.kt`: notification use cases, register token, run background job.
- `server/src/main/kotlin/com/foodfest/app/features/notification/NotificationRoute.kt`: notification endpoints.
- `server/src/main/kotlin/com/foodfest/app/features/family/FamilyTable.kt`: tạo notification cho family invite.
- `server/src/main/kotlin/com/foodfest/app/features/post/PostTable.kt`: tạo notification cho post like.

Migration:

- `database/migrations/V14__add_notifications.sql`: bảng notification cơ bản.
- `database/migrations/V17__add_pantry_expiry_notification_unique.sql`: unique guard Pantry expiry.

### 12.11 Phase 6 Scheduler Và Push

Phần này xử lý background job scan Pantry expiry và push notification khi user không mở app.

Backend:

- `server/src/main/kotlin/com/foodfest/app/plugins/BackgroundJobs.kt`: bật scheduler trong Ktor, đọc env, chạy interval.
- `server/src/main/kotlin/com/foodfest/app/Application.kt`: gọi `configureBackgroundJobs()`.
- `server/src/main/kotlin/com/foodfest/app/features/notification/PantryExpiryScheduler.kt`: facade chạy một job Pantry expiry.
- `server/src/main/kotlin/com/foodfest/app/features/notification/PushNotificationService.kt`: gửi push qua FCM legacy nếu có `FCM_SERVER_KEY`.
- `server/src/main/kotlin/com/foodfest/app/features/notification/NotificationService.kt`: `runPantryExpiryBackgroundJob`, push delivery loop, job result.
- `server/src/main/kotlin/com/foodfest/app/features/notification/NotificationTable.kt`: `PushDeviceTokenTable`, `NotificationJobRunTable`, `NotificationDeliveryLogTable`, scan toàn hệ thống.
- `server/src/main/kotlin/com/foodfest/app/di/MainModule.kt`: DI cho `PushNotificationService` và `PantryExpiryScheduler`.
- `server/src/main/kotlin/com/foodfest/app/plugins/Databases.kt`: đăng ký table Phase 6.

Client:

- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/notification/data/NotificationRepository.kt`: `registerPushDeviceToken`, `deactivatePushDeviceToken`.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/notification/data/NotificationModels.kt`: `RegisterPushDeviceTokenRequest`, `PushDeviceToken`.

Migration:

- `database/migrations/V18__add_phase6_push_scheduler_tables.sql`: `push_device_tokens`, `notification_job_runs`, `notification_delivery_logs`.

### 12.12 Routing, Auth, DI Và App Shell

Các file hạ tầng nên biết khi một endpoint/screen không được gọi đúng:

- `server/src/main/kotlin/com/foodfest/app/Application.kt`: boot Ktor app.
- `server/src/main/kotlin/com/foodfest/app/plugins/Routing.kt`: đăng ký route module backend.
- `server/src/main/kotlin/com/foodfest/app/plugins/Security.kt`: JWT auth config.
- `server/src/main/kotlin/com/foodfest/app/plugins/Serialization.kt`: JSON serialization.
- `server/src/main/kotlin/com/foodfest/app/plugins/ErrorHandling.kt`: mapping exception sang API response.
- `server/src/main/kotlin/com/foodfest/app/plugins/Databases.kt`: table registration.
- `server/src/main/kotlin/com/foodfest/app/di/MainModule.kt`: Koin dependency injection.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/App.kt`: app-level navigation.

### 12.13 Tài Liệu Và Plan

Các file tài liệu liên quan:

- `.github/prompts/full.md`: bản plan tổng quan đầy đủ, dễ đọc cho người mới.
- `.github/prompts/plan_fam.md`: bản plan theo phase dùng trong quá trình triển khai.

---

## 13. Quy Ước Code Bắt Buộc

Comment:

- Function mới phải có comment ngắn mô tả mục đích.
- Logic nghiệp vụ quan trọng phải có comment giải thích vì sao làm vậy.
- Các luồng cần comment rõ:
  - transaction
  - authorization
  - validation
  - idempotency
  - fallback/retry
  - scheduler/push failure handling

Không nên comment thừa kiểu mô tả lại từng dòng code.

Gate theo phase:

- Phase 3 không pass nếu thiếu comment ở function mới hoặc UI lệch style app.
- Phase 4 không pass nếu Pantry/Shopping List thiếu comment về rule trừ tồn kho.
- Phase 5 không pass nếu realtime polling/quiet sync thiếu comment.
- Phase 6 không pass nếu scheduler không idempotent, thiếu job/delivery log, hoặc push fail làm fail app API/job loop.

---

## 14. Lệnh Kiểm Tra

Lệnh regression chính:

```powershell
./gradlew check --no-daemon
```

Kỳ vọng:

- Server compile pass.
- Compose app compile pass.
- Lint/check pass.
- Warnings deprecated icon/time có thể còn, nhưng không được làm build fail.


