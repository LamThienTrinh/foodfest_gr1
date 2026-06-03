# FoodFest Family Space Plan — Chi Tiết Theo Phase

## Tổng quan mục tiêu
Xây dựng không gian gia đình (Family Space) giúp gia đình cùng lập kế hoạch bữa ăn, quản lý nguyên liệu, và đóng vòng trải nghiệm: **Discover → Plan → Shop → Cook → Share**.

---

## Phase 3 — Family Space MVP

### 3.1 Màn hình Family Home
**Mục đích**: Dashboard tổng quan nhóm gia đình.

**Nội dung**:
- Header: Tên gia đình, avatar members
- Quick stats:
  - Số thành viên active
  - Menu tuần tới (next 3 days preview)
  - Số preset đã lưu
- Menu shortcuts:
  - "Xem lịch tuần" → Weekly Menu
  - "Thực đơn đã lưu" → **Saved Meals** (mới)
  - "Quản lý thành viên"
  - "Xem vote gần đây"
- Activity feed (optional):
  - "Mẹ tạo menu Thứ 3"
  - "Con trai vote thích Cà pháo"

**Entry points**:
- Main nav → Family tab
- Profile → "My Families"

---

### 3.2 Màn hình Family Members Management
**Mục đích**: Mời/xóa/quản lý role thành viên.

**Các action**:
- Xem danh sách thành viên + role (Owner/Member)
- Mời thành viên (qua link/mã code)
- Xóa thành viên (owner only)
- Chuyển quyền owner (owner only)

---

### 3.3 Màn hình Weekly Menu / Calendar View
**Mục đích**: Lịch tuần để xem & chỉnh sửa menu từng bữa.

**Giao diện**:
- Grid: 7 cột (Thứ 2 - CN), 2 hàng (Sáng / Tối) hoặc 3 hàng (Sáng / Trưa / Tối)
- Mỗi ô menu:
  - Hiển thị tên bữa ăn (nếu có) hoặc "Chưa chọn"
  - Thumbnail ghép từ 2-3 ảnh món ăn
  - Click để mở → **Day Menu Detail**
- Icon lịch góc phải trên cùng:
  - Mở picker chọn **3 tuần kế tiếp** (không tính tuần hiện tại)
  - Cho phép xem/plan/vote menu của tuần được chọn

**Khi bấm vào ô trống (chưa có menu)**:
- Hiện 2 lựa chọn (Quick Options):
  - **Option 1**: "Build from scratch" (Thêm từng món lẻ)
  - **Option 2**: "Pick from Saved Meals" (Chọn từ bữa ăn đã lưu) ← **Mới**

**Rule dọn dẹp dữ liệu (Phase 3)**:
- Menu tuần đã qua sẽ bị xóa khỏi DB **nếu chưa lưu thành preset**
- Menu đã lưu preset thì được giữ lại

---

### 3.4 Màn hình Day Menu / Meal Detail
**Mục đích**: Xem & chỉnh sửa chi tiết các món trong 1 bữa ăn.

**Giao diện**:
- Header: Tên bữa (VD: "Tối Thứ 3"), ngày tháng
- Danh sách các món (scrollable):
  - Ảnh, tên món, description
  - Nút xóa món khỏi bữa
  - Drag to reorder (nếu cần)
- FAB / Nút "Thêm món": Mở Menu Item Picker
- **Nút "Save as Family Preset"** ← **Mới**:
  - Khi bấm → Modal nhập tên bữa ăn mẫu (VD: "Bữa tối thịt nướng cuối tuần")
  - Confirm → Lưu nguyên cụm món này vào **Saved Meals**
  - Toast thành công + Link nhanh tới Saved Meals

**Khi chọn Option 1 (Build from scratch)**:
- Mở "Menu Item Picker" để chọn từng dish/personal dish

**Khi chọn Option 2 (Pick from Saved Meals)**:
- BottomSheet/Modal hiển thị list **Saved Meals** (xem mục 3.6)
- Bấm 1 phát → Add toàn bộ các món từ preset vào bữa hiện tại

---

### 3.5 Màn hình Menu Item Editor / Picker
**Mục đích**: Chọn/thêm các dish vào 1 bữa ăn.

**Giao diện**:
- Tabs:
  - "System Dishes": Danh sách tất cả món từ Dish catalog
  - "My Dishes": Personal dishes của user
  - "Recent": Các món vừa thêm gần đây
- Search / Filter: Tìm kiếm theo tên, tag
- Mỗi item:
  - Thumbnail, tên, brief description
  - Bấm để thêm vào bữa

---

### 3.6 Màn hình Saved Meals / Family Presets (MỚI)
**Mục đích**: Nơi quản lý các "Bữa ăn" (Meal/Combo) đã được lưu lại để dùng cho sau này.

**Ví dụ thực tế**:
- "Combo Mùa Hè 1": Canh cua + Cà pháo + Thịt rang
- "Bữa tối đơn giản": Bắp cải xào + Cơm + Canh

**Giao diện**:
- List / Grid view (toggle)
- Mỗi thẻ (Card):
  - Thumbnail ghép từ 2-3 ảnh món trong preset
  - Tên preset
  - Số lượng món (VD: "3 items")
  - Sub-text: "Created by Mẹ"
  - Buttons:
    - "Use" (Nhanh chóng thêm vào bữa hiện tại hoặc chọn ngày)
    - "Edit" (Chỉnh sửa danh sách món)
    - "Delete" (Xóa preset)

**Khi bấm "Use"**:
- Dialog chọn ngày/bữa: "Thêm vào ngày nào? Sáng/Trưa/Tối?"
- Confirm → Add toàn bộ items vào bữa đó

**Khi bấm "Edit"**:
- Mở menu tương tự **Day Menu Detail** (có thể thêm/xóa/reorder items)
- Save → Update preset

---

### 3.7 Vote Sheet / Modal
**Mục đích**: Nhanh chóng vote chọn món cho bữa ăn.

**Kích hoạt**: 
- Tại **Day Menu Detail**, nút "Show Votes" hoặc tại **Family Home** mục "Recent Votes"

**Giao diện**:
- Modal / BottomSheet (không full screen)
- Header: "Vote cho bữa tối Thứ 3" + Danh sách các món hiện tại
- Vote buttons:
  - Emoji / Thumbs up / Reaction buttons cho từng món
  - Hoặc checkbox "Tôi thích món này"
- Vote count: Hiển thị "👍 2 người, 😍 1 người" dưới mỗi món
- "Xem kết quả" (nếu muốn xem chi tiết vote)

**UX**:
- Quick action, không cần chuyển trang
- Đóng modal sau khi vote
- Toast confirm "Cảm ơn bạn đã vote!"

---

## Phase 4 — Smart Automation MVP

### 4.1 Pantry / Fridge Management
**Mục đích**: Quản lý tồn kho nguyên liệu gia đình.

**Màn hình Pantry**:
- Danh sách các nguyên liệu có sẵn:
  - Tên ingredient, số lượng, đơn vị, ngày hết hạn (nếu có)
- Buttons:
  - "+ Thêm nguyên liệu"
  - "📝 Chỉnh sửa" (bulk edit)
  - "🗑️ Xóa hết hạn" (quick action)
- Search / Filter

**Khi add nguyên liệu**:
- Form: Tên, số lượng, đơn vị, expiry date
- Save → Thêm vào danh sách

---

### 4.2 Recipe Suggestion Engine (Rule-based MVP)
**Mục đích**: Gợi ý các món dựa trên nguyên liệu có sẵn trong Pantry.

**Luận lý**:
1. User xem **Dish Catalog** hoặc trong **Menu Item Picker**
2. Mỗi dish hiển thị:
   - Bình thường: Tên + ảnh
   - **Match Score** (nếu những nguyên liệu khả dụng):
     - 🟢 **Có sẵn 80-100%**: "Có ngay!"
     - 🟡 **Có sẵn 50-79%**: "Thiếu 2 items"
     - 🔴 **Có sẵn < 50%**: "Thiếu nhiều"

**Cách tính**:
```
Dish A cần: [Thịt heo, Rau muống, Mắm tôm]
Pantry có: [Thịt heo ✓, Rau muống ✓]
→ Match score = 2/3 = 66% (Thiếu 1 item: Mắm tôm)
```

---

### 4.3 Shopping List / Checklist Generator
**Mục đích**: Tự động tạo danh sách mua sắm từ menu tuần.

**Luồng**:
1. User xem **Weekly Menu** → Bấm nút "Generate Shopping List"
2. App phân tích tất cả dish trong tuần → Trích nguyên liệu
3. Trừ đi tồn kho từ **Pantry**
4. Hiển thị **Shopping List** với các items cần mua

**Giao diện Shopping List**:
- Grouped by category (Rau/Thịt/Gia vị/...)
- Mỗi item:
  - Checkbox để tick "Đã mua"
  - Tên + số lượng cần + đơn vị
  - Ghi chú (nếu có)
- Buttons:
  - "Chia sẻ" (chia danh sách cho thành viên khác)
  - "Đánh dấu mua hết" (tick all)
  - "Cập nhật Pantry" (sau khi mua về, auto-add vào Pantry)

**Khi bấm "Cập nhật Pantry"**:
- Dialog xác nhận số lượng thực tế mua
- Save → Auto-add vào Pantry

---

### 4.4 Checklist Detail / Realtime Sync Prep
**Mục đích**: Checklist từng bước nấu nướng từ các món trong menu.

**Giao diện**:
- Danh sách các item từ shopping list
- Mỗi item:
  - Checkbox "Đã chuẩn bị / Đã mua"
  - Tên + số lượng
  - Assigned to (member nào mua item này, optional)
  - "Đã dùng bao nhiêu?" (spinner để trừ từ Pantry)
- Activity log: "Mẹ check Thịt heo lúc 9:30AM"

**Realtime UI (Phase 5 enhancement)**:
- Khi thành viên khác tick item → Refresh ngay (polling hoặc WebSocket Phase 5)

---

### 4.5 Pantry Expiry Notifications
**Mục đích**: Báo cho người dùng khi nguyên liệu trong Pantry đã hết hạn hoặc sắp hết hạn.

**MVP hiện tại**:
- Notification dạng **scan-on-open**:
  - Khi user mở màn **Thông báo** hoặc app gọi unread count
  - Backend scan pantry items của các family mà user là thành viên
  - Nếu item đã hết hạn → tạo notification `pantry_expired`
  - Nếu item sắp hết hạn trong 3 ngày → tạo notification `pantry_expiring`
- Không spam duplicate:
  - Unique theo `(user_id, type, related_entity_type, related_entity_id)`
  - Mỗi pantry item chỉ tạo 1 notification cho mỗi loại cảnh báo/user

**Phase 6 enhancement**:
- Vẫn giữ scan-on-open để inbox luôn tự đồng bộ khi user mở app.
- Backend scheduler cũng scan định kỳ toàn hệ thống khi app không mở.
- Nếu user có device token và server có `FCM_SERVER_KEY`, backend gửi push notification.
- Nếu chưa có token/key thì delivery được log `skipped`, không tạo duplicate notification.

---

## Phase 5 — Realtime + Experience Loop Optimization

### 5.1 Realtime Checklist Sync
**Mục đích**: Đồng bộ trạng thái mua sắm giữa các thành viên gia đình.

**Ký thuật (MVP)**:
- Polling 3-5 giây để check update
- Bàn luận: Sau này upgrade → WebSocket room per family

**UX**:
- Khi thành viên A tick "Thịt heo" → Thành viên B thấy tick ngay (hoặc sau 3-5s)
- Notification nhỏ: "Thịt heo đã mua ✓" (nếu bật notify)

---

### 5.2 Vote Modal Enhancement
**Mục đích**: Thay đổi Vote Sheet thành Modal riêng, UX mượt mà.

**Giao diện Vote Modal**:
- Compact modal (không full screen)
- Header: "Bạn chọn gì cho bữa tối Thứ 3?"
- Tabs (nếu có 3+ bữa):
  - "Bữa tối Thứ 3" (active)
  - "Bữa sáng Thứ 4"
- Danh sách các món:
  - Thumbnail + Tên
  - Vote buttons: 👍 ❤️ 😍 (với count live)
- Đóng modal → Cập nhật vote tức thì

---

### 5.3 Family Chat / Notes Integration (Optional)
**Mục đích**: Trao đổi ý kiến ngay tại family space.

**Tích hợp Vote + Chat**:
- Chat widget nhỏ tại **Family Home** mục "Quick Notes"
- VD message:
  > "Mẹ: Nay thèm ăn cá 🐟"
  > "Con: +1, vote cá đi mọi người!"
  > [Vote for this message: 👍 3 | ❤️ 1]
  > "Bố: Ok, thứ 5 tối ăn cá kho tộ"

**Separate Chat Screen** (nếu cần):
- Dedicated chat room cho gia đình
- Message history
- Reaction / emoji
- Pin important messages ("Menu hôm nay: ...")

---

### 5.4 Experience Loop — Đóng vòng tròn trải nghiệm

**Từng bước của user journey**:

```
1. DISCOVER (Home / Catalog)
   ↓ Xem các món mới, theo dõi trending
   
2. PLAN (Family Space - Weekly Menu)
   ↓ Lập kế hoạch bữa ăn tuần (hoặc dùng Saved Meals)
   
3. VOTE (Vote Modal)
   ↓ Gia đình vote chọn món
   
4. CHAT (Family Notes - optional)
   ↓ Trao đổi nhanh ghi chú nấu nướng
   
5. SHOP (Shopping List)
   ↓ Auto-generate & mua sắm
   
6. COOK (Checklist + Pantry)
   ↓ Prepare & cook (realtime sync với thành viên khác)
   
7. SHARE (Post to Feed)
   ↓ Đăng ảnh bữa ăn → Home feed
   
8. ENGAGE (Like / Comment / Follow)
   ↓ Thích & bình luận những bữa ăn của người khác
   ↓ Quay lại bước 1
```

---

## Phase 6 — Background Jobs + Push Notifications

### 6.1 Pantry Expiry Background Scheduler
**Mục đích**: Tự động báo nguyên liệu hết hạn/sắp hết hạn ngay cả khi user không mở app.

**Luồng đã triển khai**:
1. Ktor background scheduler chạy khi backend start và lặp lại theo interval cấu hình
2. Job scan toàn bộ `family_pantry_items` có `expiry_date`
3. Tạo notification cho tất cả member của family:
   - `pantry_expiring`: còn 1-3 ngày
   - `pantry_expired`: đã quá hạn
4. Gửi push notification nếu user có device token
5. Ghi log job run + delivery result
6. Vẫn giữ idempotency để không spam duplicate

**Yêu cầu kỹ thuật**:
- Job scheduler backend:
  - Ktor background coroutine
  - ENV:
    - `PANTRY_EXPIRY_SCHEDULER_ENABLED` (default: true)
    - `PANTRY_EXPIRY_SCHEDULER_INTERVAL_HOURS` (default: 12)
    - `PANTRY_EXPIRY_SCHEDULER_RUN_ON_STARTUP` (default: true)
- Push notification service:
  - Lưu device token per user/device
  - API:
    - `POST /api/notifications/device-tokens`
    - `DELETE /api/notifications/device-tokens`
  - Tích hợp FCM legacy qua `FCM_SERVER_KEY`
  - Nếu chưa có `FCM_SERVER_KEY` hoặc chưa có token thì delivery được log `skipped`, không block job
- Retry/failure logging:
  - Log job run
  - Log notification delivery result
  - Không block app API nếu push fail

**Exit criteria Phase 6**:
- User nhận cảnh báo Pantry expiry dù không mở app nếu backend đang chạy scheduler và device token đã được đăng ký.
- Không tạo duplicate notifications khi job chạy nhiều lần.
- Có thể mark read notification trong inbox như các notification khác.
- Regression: notification inbox + pantry expiry job tests pass.

---

## DB Schema — Family Space Entities

### family_groups
```
id (PK), name, owner_user_id, created_at, updated_at
```

### family_members
```
family_id (FK), user_id (FK), role (enum: owner/member), joined_at
PK: (family_id, user_id)
```

### family_menus
```
id (PK), family_id (FK), menu_date, meal_type (breakfast/lunch/dinner), status (draft/published), is_saved (boolean), created_at
```

### family_menu_items
```
id (PK), family_menu_id (FK), dish_id (FK, nullable), personal_dish_id (FK, nullable), note, display_order
```

### family_menu_votes
```
id (PK), family_menu_item_id (FK), user_id (FK), vote_type (enum: like/love/...), created_at
```

### **family_saved_meals** (NEW)
```
id (PK), family_id (FK), preset_name, created_by_user_id (FK), created_at, updated_at
```

### **family_saved_meal_items** (NEW)
```
id (PK), family_saved_meal_id (FK), dish_id (FK, nullable), personal_dish_id (FK, nullable), display_order
```

### **family_pantry_items** (NEW - Phase 4)
```
id (PK), family_id (FK), ingredient_name, quantity, unit, expiry_date (nullable), created_at, updated_at
```

### **family_shopping_lists** (NEW - Phase 4)
```
id (PK), family_id (FK), menu_week, status (draft/active/completed), created_at, updated_at
```

### **family_shopping_list_items** (NEW - Phase 4)
```
id (PK), family_shopping_list_id (FK), ingredient_name, required_qty, unit, is_purchased, purchased_by_user_id (nullable), created_at, updated_at
```

### **pantry expiry notifications** (Phase 4.5 MVP)
```
notifications.type:
- pantry_expiring
- pantry_expired

notifications.related_entity_type = family_pantry_item
notifications.related_entity_id = family_pantry_item_id
Unique guard: (user_id, type, related_entity_type, related_entity_id)
```

### **push_device_tokens** (NEW - Phase 6)
```
id (PK), user_id (FK), platform, token, is_active, created_at, updated_at
```

### **notification_job_runs** (NEW - Phase 6)
```
id (PK), job_name, started_at, finished_at, status, inserted_count, push_attempted_count, push_sent_count, error_message
```

### **notification_delivery_logs** (NEW - Phase 6)
```
id (PK), notification_id (FK), user_id (FK), push_device_token_id (FK nullable), provider, status, response_message, created_at
```

---

## Exit Criteria Theo Phase

### Phase 3 (Family Space MVP)
- ✅ Tạo gia đình & mời thành viên thành công
- ✅ Xem lịch tuần & chỉnh sửa menu từng ngày
- ✅ **Save bữa ăn thành preset** (Saved Meals)
- ✅ **Pick from Saved Meals** khi tạo menu mới
- ✅ Vote giao diện hoạt động (modal)
- ✅ Regression: Family-related tests pass

### Phase 4 (Smart Automation)
- ✅ Thêm/sửa/xóa nguyên liệu trong Pantry
- ✅ Recipe suggestion engine hoạt động (match score)
- ✅ Auto-generate Shopping List từ menu
- ✅ Pantry được cập nhật tự động khi mua hàng
- ✅ Pantry expiry notification dạng scan-on-open hoạt động
- ✅ Regression: Pantry + Shopping List tests pass

### Phase 5 (Realtime + Loop)
- ✅ Realtime checklist sync (polling 3-5s)
- ✅ Vote modal UX mượt mà & nhanh
- ✅ (Optional) Family Chat hoạt động cơ bản
- ✅ User có thể đi hết 7 bước experience loop trong 1 session
- ✅ Regression: Full E2E scenario tests pass

### Phase 6 (Background Jobs + Push)
- ✅ Scheduler tự scan Pantry expiry định kỳ
- ✅ Push notification gửi được khi user không mở app
- ✅ Không spam duplicate expiry notifications
- ✅ Regression: Scheduler + notification delivery tests pass

---

## Notes

- **Saved Meals** là feature có high impact & low effort → nên prioritize sớm.
- **Pantry + Shopping List** giúp đóng vòng trải nghiệm → rất quan trọng cho UX thể chất.
- **Realtime** ban đầu dùng polling để đơn giản, Phase 6 nâng cấp WebSocket nếu cần.
- **Chat** là optional ở Phase 5, có thể defer sang Phase 6 nếu time-boxed.
- **Pantry expiry notification** hiện có cả scan-on-open và Phase 6 background scheduler. Push thật cần `FCM_SERVER_KEY` + device token được app native đăng ký.

---

## Quy Ước Triển Khai Bắt Buộc (Code + UI)

### 1) Quy ước comment cho function và logic

Áp dụng cho tất cả phase (Phase 3, 4, 5):

- Mỗi function mới đều phải có comment ngắn mô tả:
  - function làm gì,
  - input/output chính,
  - rule nghiệp vụ quan trọng (nếu có).
- Mỗi block logic nghiệp vụ không hiển nhiên đều phải có comment giải thích intent.
- Các luồng đặc biệt bắt buộc có comment:
  - transaction,
  - phân quyền,
  - validate,
  - mapping dữ liệu,
  - fallback/retry.
- Không viết comment dư thừa kiểu mô tả lại code từng dòng; ưu tiên comment để giải thích "vì sao".
- Khi sửa function cũ, phải cập nhật comment đi kèm để tránh lệch với logic thực tế.

Checklist review trước khi merge:

- [ ] Function mới có comment đầy đủ.
- [ ] Logic nghiệp vụ quan trọng có comment rõ ràng.
- [ ] Comment còn đúng sau khi refactor.

### 2) Quy ước UI: bắt buộc tái sử dụng component sẵn có

Áp dụng cho toàn bộ màn hình Family:

- Khi làm UI mới, ưu tiên dùng lại component đã có trong project (buttons, cards, text fields, modal, bottom sheet, top bar, loading, empty state).
- Không tự tạo component mới nếu đã có component tương đương.
- Nếu bắt buộc tạo component mới:
  - phải chứng minh không có component phù hợp,
  - naming theo convention hiện tại,
  - reusable cho ít nhất 2 màn hình.
- Giữ thống nhất design tokens hiện tại:
  - typography,
  - spacing,
  - corner radius,
  - color,
  - elevation.
- Mọi flow mới của Family (Saved Meals, Pick from Saved, Vote Modal, Pantry, Shopping List) phải dùng chung style từ bộ component hiện có.

Checklist review trước khi merge:

- [ ] UI mới đã reuse component sẵn có.
- [ ] Không duplicate component tương đương.
- [ ] Modal/BottomSheet/Buttons cùng style với app hiện tại.

### 3) Gate bắt buộc theo phase

- **Phase 3 Gate**: Không pass nếu thiếu comment ở function mới hoặc tạo UI lệch component system.
- **Phase 4 Gate**: Không pass nếu logic Pantry/Shopping List thiếu comment giải thích rule trừ tồn kho.
- **Phase 5 Gate**: Không pass nếu logic realtime thiếu comment mô tả polling/sync state, hoặc UI realtime không theo component chung.
- **Phase 6 Gate**: Không pass nếu scheduler không idempotent, thiếu job/delivery log, hoặc push failure làm fail app API/job loop.
