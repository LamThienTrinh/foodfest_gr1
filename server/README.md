# FoodFest Backend API

Backend API cho ứng dụng FoodFest - Nền tảng chia sẻ công thức nấu ăn Việt Nam.

## 🏗️ Kiến trúc

```
server/src/main/kotlin/com/foodfest/app/
├── Application.kt              # Entry point
├── config/
│   └── DatabaseConfig.kt       # Cấu hình database
├── data/
│   ├── models/
│   │   ├── Tables.kt          # Exposed table definitions
│   │   └── DTOs.kt            # Request/Response DTOs
│   └── repositories/
│       ├── DishRepository.kt   # Data access layer cho Dishes
│       ├── TagRepository.kt    # Data access layer cho Tags
│       └── UserRepository.kt   # Data access layer cho Users
├── plugins/
│   ├── CORS.kt                # CORS configuration
│   ├── Routing.kt             # Route configuration
│   └── Serialization.kt       # JSON serialization
└── routes/
    ├── DishRoutes.kt          # Dish endpoints
    └── TagRoutes.kt           # Tag endpoints
```

## 🚀 Tech Stack

- **Framework**: Ktor 2.3.7
- **Database**: PostgreSQL 15+ với Exposed ORM
- **Connection Pool**: HikariCP
- **Serialization**: kotlinx.serialization
- **Security**: BCrypt cho password hashing

## 📋 Yêu cầu

- JDK 17+
- PostgreSQL 15+
- Gradle 8+

## ⚙️ Cấu hình

File `src/main/resources/application.conf`:

```hocon
database {
    url = "jdbc:postgresql://localhost:5432/foodfest"
    user = "postgres"
    password = "postgres"
    maxPoolSize = 10
}
```

Hoặc dùng environment variables:
- `DATABASE_URL`
- `DATABASE_USER`
- `DATABASE_PASSWORD`

## 🎯 API Endpoints

### Health Check
```
GET /health
```

### Dishes

#### Lấy danh sách món ăn (có phân trang)
```
GET /api/dishes?page=1&pageSize=20

Response:
{
  "success": true,
  "data": {
    "items": [...],
    "page": 1,
    "pageSize": 20,
    "totalItems": 100,
    "totalPages": 5
  }
}
```

#### Lấy chi tiết món ăn
```
GET /api/dishes/{id}

Response:
{
  "success": true,
  "data": {
    "id": 1,
    "dishName": "Thịt rang cháy cạnh",
    "imageUrl": "/images/thit-rang-chay-canh.jpg",
    "description": "...",
    "ingredients": "...",
    "instructions": "...",
    "prepTime": 20,
    "cookTime": 35,
    "serving": 4,
    "tags": [
      {
        "id": 1,
        "tagName": "Món mặn",
        "tagType": "TYPE"
      },
      ...
    ]
  }
}
```

#### Tìm kiếm món ăn
```
GET /api/dishes/search?q=thịt&tags=1,5,17

Parameters:
- q: từ khóa tìm kiếm (tìm theo tên món)
- tags: danh sách tag IDs (phân cách bằng dấu phẩy)
```

#### Lấy món ăn theo tags
```
GET /api/dishes/by-tags?tags=1,5,17
```

### Tags

#### Lấy tất cả tags
```
GET /api/tags

Response:
{
  "success": true,
  "data": [
    {
      "id": 1,
      "tagName": "Món mặn",
      "tagType": "TYPE"
    },
    ...
  ]
}
```

#### Lấy tags theo loại
```
GET /api/tags/by-type/{type}

Types: TYPE, TASTE, INGREDIENT, SEASON
```

## 🏃 Chạy ứng dụng

### Development
```bash
./gradlew :server:run
```

### Build
```bash
./gradlew :server:build
```

### Run JAR
```bash
java -jar server/build/libs/server-all.jar
```

Server sẽ chạy tại: `http://localhost:8080`

## 📊 Database Schema

### Tables
- **users**: Người dùng
- **tags**: Nhãn cho món ăn (TYPE, TASTE, INGREDIENT, SEASON)
- **dishes**: Món ăn hệ thống (chỉ đọc)
- **dish_tags**: Mapping giữa dishes và tags
- **posts**: Bài viết của người dùng
- **saved_posts**: Bài viết đã lưu
- **personal_dishes**: Món ăn cá nhân (có thể clone từ dishes)

## 🔐 Security (Coming Soon)

- JWT Authentication
- Password hashing với BCrypt
- Role-based access control

## 📝 Response Format

### Success Response
```json
{
  "success": true,
  "data": {...},
  "message": "Optional message"
}
```

### Error Response
```json
{
  "success": false,
  "error": "Error message",
  "data": null
}
```

## 🧪 Testing

```bash
./gradlew :server:test
```

## 📦 Dependencies

Xem file `build.gradle.kts` để biết chi tiết các dependencies.

## 🤝 Contributing

1. Fork the project
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## 📄 License

MIT License
