# FoodFest - Mạng xã hội ẩm thực và quản lý bữa ăn gia đình

FoodFest là ứng dụng chia sẻ ẩm thực được xây dựng bằng Kotlin Multiplatform. Dự án kết hợp mạng xã hội món ăn, kho công thức, món cá nhân và không gian gia đình để người dùng có thể tìm món, lưu công thức, lên thực đơn, tạo danh sách đi chợ và chia sẻ trải nghiệm nấu ăn.

![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-purple?logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-Multiplatform-blue?logo=jetpackcompose)
![Ktor](https://img.shields.io/badge/Ktor-Server-orange)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue?logo=postgresql)
![Cloudinary](https://img.shields.io/badge/Cloudinary-Image%20CDN-lightblue)

---

## Scope hiện tại

| Nhóm chức năng | Nội dung đã triển khai |
|---|---|
| Xác thực và hồ sơ | Đăng ký, đăng nhập JWT, tự động đăng nhập bằng token, cập nhật hồ sơ, đổi mật khẩu, đổi avatar, xem hồ sơ công khai |
| Mạng xã hội | Feed tất cả bài viết, feed người đang theo dõi, tìm kiếm bài viết/người dùng, lọc loại bài viết, bài viết xu hướng, tạo/sửa/xóa bài viết |
| Tương tác bài viết | Like/unlike, lưu/bỏ lưu bài viết, danh sách bài đã lưu, bình luận 2 cấp, xóa bình luận của chính mình |
| Follow và profile | Theo dõi/hủy theo dõi người dùng, xem follower/following, xem bài viết của mình, mở profile tác giả từ bài viết |
| Kho món ăn | Danh sách món hệ thống, chi tiết món, tìm kiếm, lọc theo tag loại món/vị/nguyên liệu/mùa, random món |
| Món yêu thích | Thêm/bỏ món yêu thích, kiểm tra trạng thái yêu thích, danh sách món yêu thích |
| Món cá nhân | Tạo, xem, sửa, xóa món của tôi; lưu phiên bản cá nhân từ món hệ thống; dùng món cá nhân trong BlindBox |
| BlindBox | Random món ăn từ kho hệ thống hoặc món cá nhân, có lọc theo loại món/vị/nguyên liệu |
| Không gian gia đình | Tạo/đổi tên gia đình, mời/thêm/xóa thành viên, rời nhóm, đặt nickname trong gia đình |
| Thực đơn gia đình | Tạo thực đơn theo ngày/tuần, thêm/xóa món, lưu bữa ăn mẫu, áp dụng bữa đã lưu vào thực đơn |
| Bình chọn bữa ăn | Vote/unvote món trong bữa, xem tổng hợp vote, xem lịch sử vote gần đây |
| Tủ đồ và đi chợ | Quản lý pantry, xóa đồ hết hạn, tạo shopping list từ thực đơn, tick đồ đã mua, mark all purchased, đồng bộ đồ đã mua vào pantry |
| Ghi chú gia đình | Tạo và xem ghi chú nội bộ theo từng gia đình |
| Thông báo | Danh sách thông báo, số thông báo chưa đọc, đánh dấu đã đọc, đánh dấu tất cả đã đọc, đăng ký/hủy device token |
| Upload ảnh | Upload ảnh Base64 lên Cloudinary cho bài viết, món ăn, món cá nhân và avatar |

---

## Luồng sử dụng chính

1. Người dùng đăng ký/đăng nhập, cập nhật hồ sơ và avatar.
2. Người dùng xem feed, đăng bài, like, lưu bài, bình luận và theo dõi tác giả.
3. Người dùng khám phá kho món ăn, lọc theo tag, lưu món yêu thích hoặc tạo món cá nhân.
4. BlindBox gợi ý món ngẫu nhiên từ món hệ thống hoặc món cá nhân.
5. Gia đình cùng tạo thực đơn tuần, thêm món, vote món, lưu bữa ăn mẫu.
6. Từ thực đơn tuần, gia đình tạo danh sách đi chợ và đồng bộ đồ đã mua vào pantry.
7. Hệ thống hiển thị thông báo liên quan đến gia đình, lời mời và pantry.

---

## Tech stack

### Mobile app

| Công nghệ | Mục đích |
|---|---|
| Kotlin Multiplatform | Chia sẻ logic Android/iOS |
| Compose Multiplatform | UI declarative dùng chung |
| Material 3 | Component và theme giao diện |
| Ktor Client | Gọi REST API |
| Kotlin Coroutines | Xử lý bất đồng bộ |
| Multiplatform Settings | Lưu token đăng nhập |
| Coil | Tải ảnh phía Android |
| MPFilePicker | Chọn ảnh từ thiết bị |

### Backend

| Công nghệ | Mục đích |
|---|---|
| Ktor Server | REST API |
| PostgreSQL | Cơ sở dữ liệu quan hệ |
| Exposed ORM | Truy vấn database |
| HikariCP | Connection pooling |
| JWT + BCrypt | Xác thực và bảo mật mật khẩu |
| Koin | Dependency injection |
| Cloudinary | Lưu trữ ảnh |
| dotenv | Đọc cấu hình môi trường |

---

## Cấu trúc project

```text
foodfest_gr1/
├── composeApp/          # Ứng dụng Kotlin Multiplatform
│   └── src/
│       ├── commonMain/  # UI, state, repository dùng chung
│       ├── androidMain/ # Android-specific code
│       └── iosMain/     # iOS-specific code
├── server/              # Backend Ktor
│   └── src/main/kotlin/com/foodfest/app/
│       ├── core/        # Database, response, exception
│       ├── features/    # auth, post, dish, family, notification...
│       ├── plugins/     # Ktor plugins
│       ├── services/    # Cloudinary service
│       └── di/          # Koin module
├── shared/              # Module shared dùng chung
├── database/            # Schema, seed, migrations và script chạy migration
└── iosApp/              # iOS host project
```

---

## Cài đặt và chạy

### Yêu cầu

- JDK 17+
- Android Studio hoặc IntelliJ IDEA
- PostgreSQL 15+
- Gradle wrapper đi kèm project
- Tài khoản Cloudinary nếu muốn chạy upload ảnh với cấu hình riêng

### 1. Chuẩn bị database

```powershell
createdb foodfest

.\database\run-migrations.ps1 -DbHost 127.0.0.1 -Database foodfest -User postgres -Password postgres
```

Nếu chỉ muốn tạo schema, không seed dữ liệu mẫu:

```powershell
.\database\run-migrations.ps1 -DbHost 127.0.0.1 -Database foodfest -User postgres -Password postgres -SkipSeed
```

### 2. Cấu hình backend

Tạo file `server/.env` từ `server/.env.example` và cập nhật thông tin local:

```powershell
Copy-Item server\.env.example server\.env
```

Các biến quan trọng:

```env
DATABASE_URL=jdbc:postgresql://localhost:5432/foodfest
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres
JWT_SECRET=your-secret-key
JWT_ISSUER=foodfest-api
JWT_AUDIENCE=foodfest-users
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
```

### 3. Chạy backend

```powershell
.\gradlew.bat :server:run
```

Server chạy tại:

```text
http://localhost:8080
```

Health check:

```text
GET http://localhost:8080/health
```

### 4. Chạy Android app

```powershell
.\gradlew.bat :composeApp:installDebug
```

Hoặc mở project bằng Android Studio và chạy configuration của `composeApp`.

### 5. Chạy iOS app

Mở `iosApp/iosApp.xcodeproj` bằng Xcode, chọn simulator hoặc thiết bị thật rồi Run.

---

## Nhóm API chính

| Nhóm | Endpoint tiêu biểu |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`, `PUT /api/auth/profile`, `PUT /api/auth/avatar` |
| User/Profile | `GET /api/users/{userId}/profile`, `GET /api/users/{userId}/posts`, `POST /api/users/{userId}/follow` |
| Posts | `GET /api/posts`, `GET /api/posts/feed/following`, `POST /api/posts`, `PUT /api/posts/{postId}`, `DELETE /api/posts/{postId}` |
| Post interactions | `POST /api/posts/{postId}/like`, `POST /api/posts/{postId}/save`, `GET /api/posts/saved` |
| Comments | `POST /api/posts/{postId}/comments`, `GET /api/posts/{postId}/comments`, `GET /api/comments/{commentId}/replies`, `DELETE /api/comments/{commentId}` |
| Dishes/Tags | `GET /api/dishes`, `GET /api/dishes/search`, `GET /api/dishes/random`, `GET /api/dishes/{id}`, `GET /api/tags` |
| Favorites | `GET /api/favorites`, `POST /api/favorites/toggle/{dishId}`, `DELETE /api/favorites/remove/{dishId}` |
| My dishes | `GET /api/my-dishes`, `POST /api/my-dishes`, `PUT /api/my-dishes/{id}`, `DELETE /api/my-dishes/{id}` |
| BlindBox | `GET /api/blind-box/random` |
| Families | `GET /api/families`, `POST /api/families`, `PUT /api/families/{familyId}`, `POST /api/families/{familyId}/invites` |
| Family menu | `GET /api/families/{familyId}/menus/week`, `POST /api/families/{familyId}/menus`, `POST /api/families/{familyId}/menus/{menuId}/items` |
| Family vote | `POST /api/families/{familyId}/menus/{menuId}/items/{itemId}/vote`, `GET /api/families/{familyId}/votes/recent` |
| Pantry/shopping | `GET /api/families/{familyId}/pantry`, `POST /api/families/{familyId}/shopping-lists/generate`, `POST /api/families/{familyId}/shopping-lists/{shoppingListId}/sync-pantry` |
| Notes/notifications | `GET /api/families/{familyId}/notes`, `GET /api/notifications`, `PUT /api/notifications/read-all` |
| Upload | `POST /api/upload/image` |

---

## Database

Database dùng migrations tăng dần trong `database/migrations`. Scope hiện tại có các nhóm bảng chính:

- `users`, `follows`
- `posts`, `post_likes`, `saved_posts`, `post_comments`
- `dishes`, `tags`, `dish_tags`, `favorite_dishes`, `personal_dishes`
- `family_groups`, `family_members`, `family_invites`
- `family_menus`, `family_menu_items`, `family_menu_votes`
- `family_saved_meals`, `family_pantry_items`, `family_shopping_lists`, `family_notes`
- `notifications`, `push_device_tokens`

---

## Build và kiểm tra nhanh

```powershell
.\gradlew.bat :server:build
.\gradlew.bat :composeApp:assembleDebug
```

---

## Ghi chú nộp bài

- Repo đã tách phần app, server và database rõ ràng để thầy có thể xem source theo từng module.
- Các file cấu hình local như `.env`, `local.properties`, `.gradle`, `build`, `.idea`, `.vscode`, `.agents`, `.codex` không nên commit lên GitHub.
- Nếu chạy trên máy khác, cần tạo database PostgreSQL và cập nhật `server/.env` theo môi trường local.

---

## License

MIT License
