# 🍜 FoodFest - Food Social Network

> Ứng dụng mạng xã hội chia sẻ ẩm thực được xây dựng với Kotlin Multiplatform

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple?logo=kotlin)
![Compose](https://img.shields.io/badge/Compose-Multiplatform-blue?logo=jetpackcompose)
![Ktor](https://img.shields.io/badge/Ktor-2.3.7-orange)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)

---

## 📱 Tính Năng

- ✅ Đăng ký / Đăng nhập (JWT Authentication)
- ✅ Xem feed bài viết với infinite scroll
- ✅ Tìm kiếm theo tiêu đề + lọc theo loại bài
- ✅ Đăng bài viết với hình ảnh (Cloudinary)
- ✅ Like / Unlike bài viết
- ✅ Lưu bài viết yêu thích
- ✅ Xem profile cá nhân

---

## 🛠️ Tech Stack

### Mobile App
| Công nghệ | Mục đích |
|-----------|----------|
| Kotlin Multiplatform | Cross-platform codebase (Android + iOS) |
| Jetpack Compose Multiplatform | Declarative UI |
| Kotlin Coroutines | Async/Await, non-blocking |
| Ktor Client | HTTP networking |
| Coil | Image loading (Android) |
| Multiplatform Settings | Local storage |

### Backend Server
| Công nghệ | Mục đích |
|-----------|----------|
| Ktor Server | RESTful API framework |
| Exposed ORM | Database queries |
| PostgreSQL | Relational database |
| HikariCP | Connection pooling |
| JWT + BCrypt | Authentication |
| Koin | Dependency Injection |
| Cloudinary | Image CDN |

---

## 📁 Cấu Trúc Project

```
foodfest/
├── composeApp/          # Mobile App (KMP)
│   └── src/
│       ├── commonMain/  # Shared code (Android + iOS)
│       ├── androidMain/ # Android-specific
│       └── iosMain/     # iOS-specific
├── server/              # Ktor Backend
│   └── src/main/kotlin/
│       └── com/foodfest/app/
│           ├── features/    # auth, post, user...
│           ├── plugins/     # Ktor plugins
│           └── di/          # Koin modules
├── shared/              # Shared library
└── database/            # SQL scripts
```

---

## 🚀 Cách Chạy

### 1. Database (PostgreSQL)
```bash
# Tạo database
createdb foodfest

# Chạy migrations tăng dần (V1, V2, V3)
./database/run-migrations.ps1 -DbHost 127.0.0.1 -Database foodfest -User postgres -Password postgres

# Chạy migrations nhưng bỏ qua seed data
./database/run-migrations.ps1 -DbHost 127.0.0.1 -Database foodfest -User postgres -Password postgres -SkipSeed

# Hoặc chỉ tạo schema (không seed dữ liệu mẫu)
psql -d foodfest -f database/schema.sql

# Seed dữ liệu mẫu (tuỳ chọn)
psql -d foodfest -f database/seed.sql
```

### 2. Backend Server
```bash
cd foodfest

# Tạo file .env với các biến môi trường
# DB_URL, JWT_SECRET, CLOUDINARY_*

# Chạy server
.\gradlew.bat :server:run
# Server chạy tại http://localhost:8080
```

### 3. Mobile App (Android)
```bash
# Build và cài đặt
.\gradlew.bat :composeApp:installDebug
```

### 4. Mobile App (iOS)
```bash
cd iosApp
pod install
# Mở .xcworkspace trong Xcode
```

---

## 🔌 API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/auth/register` | Đăng ký |
| POST | `/api/auth/login` | Đăng nhập |
| GET | `/api/posts` | Lấy danh sách bài viết |
| GET | `/api/posts?search=...&postType=...` | Tìm kiếm + lọc |
| POST | `/api/posts` | Tạo bài viết mới |
| POST | `/api/posts/{id}/like` | Like/unlike |
| POST | `/api/posts/{id}/save` | Lưu bài viết |
| POST | `/api/upload/image` | Upload ảnh (Base64) |

---

## 📸 Upload Ảnh Flow

```
📱 App                    🖥️ Server                 ☁️ Cloudinary
   │                          │                          │
   │  1. Chọn ảnh             │                          │
   │  2. ByteArray → Base64   │                          │
   │  3. POST /api/upload     │                          │
   │─────────────────────────►│                          │
   │                          │  4. Upload to Cloudinary │
   │                          │─────────────────────────►│
   │                          │  5. Return URL           │
   │                          │◄─────────────────────────│
   │  6. Trả về imageUrl      │                          │
   │◄─────────────────────────│                          │
```

---

## 🏗️ Architecture

```
┌─────────────────┐                    ┌─────────────────┐
│  Mobile App     │ ◄───── REST ─────► │   Ktor Server   │
│  (KMP + Compose)│      + JWT         │                 │
└─────────────────┘                    └────────┬────────┘
                                                │
                    ┌───────────────────────────┼───────────────────────────┐
                    ▼                           ▼                           ▼
            ┌───────────────┐         ┌─────────────────┐         ┌─────────────────┐
            │  PostgreSQL   │         │   Cloudinary    │         │   JWT Auth      │
            └───────────────┘         └─────────────────┘         └─────────────────┘
```

---

---

## 📝 License

MIT License
