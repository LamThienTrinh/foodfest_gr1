## Plan: Post CRUD + Trending + Search

Mục tiêu là bổ sung đầy đủ sửa/xóa bài của chính mình, lọc bài theo khoảng ngày ở màn My Posts, đẩy bài xu hướng 7 ngày lên đầu theo ranking dùng window function, và nâng cấp search ngay trong Home để tìm cả người dùng lẫn bài đăng. Triển khai tách riêng Backend/Frontend, ưu tiên giữ tương thích API cũ để không làm vỡ flow hiện tại.

**Steps**
1. Phase A - Chốt contract và scope thực thi (*blocking*)
1.1. Khóa yêu cầu theo quyết định đã chọn:
- Trending score: `score = likeCount*1 + commentCount*2`.
- Trending placement: trộn chung feed (không tạo tab riêng).
- Date range filter: chỉ áp dụng cho My Posts.
- Edit fields: `title`, `content`, `image` (thay vì nhập `imageUrl`, người dùng chọn ảnh từ thiết bị, upload ngầm và lấy url).
- Edit Profile: Hỗ trợ thay đổi và lưu ảnh đại diện (avatar) của tài khoản.
- Search UX: nâng cấp search trong Home (không tạo màn Search riêng).
1.2. Định nghĩa tham số API mới:
- Post list: `searchType=post|user`, `search`, `includeTrending=true|false`.
- My posts: `startDate`, `endDate`.
- Update post: body chỉ cho 3 trường cho phép sửa.

2. Phase B - Backend (*depends on 1*)
2.1. Post update/delete/my-post-date-range APIs
- Thêm endpoint cập nhật bài: `PUT /api/posts/{postId}` (auth required, owner-only).
- Tận dụng endpoint xóa hiện có: `DELETE /api/posts/{postId}`; chuẩn hóa response để frontend dùng ổn định.
- Mở rộng endpoint user posts: `GET /api/users/{userId}/posts?page=&startDate=&endDate=`.
- Validate date range (`startDate <= endDate`, format ISO date/datetime).

2.2. Trending feed dùng window function (7 ngày)
- Mở rộng query list posts cho feed ALL:
- Tạo cột ảo `trend_score = like_count + comment_count*2`.
- Dùng window function (`ROW_NUMBER()` hoặc `DENSE_RANK()`) trên tập bài trong 7 ngày gần nhất để tính `trend_rank`.
- Sắp xếp trộn feed: bài có `trend_rank` sẽ ưu tiên lên trước, sau đó theo `created_at DESC`.
- Đảm bảo bài ngoài 7 ngày vẫn hiển thị sau nhóm trending.

2.3. Search user hoặc post trong Home
- Nếu `searchType=post`: dùng logic tìm bài hiện có (mở rộng thêm content nếu cần).
- Nếu `searchType=user`: join users + posts, lọc theo username/fullName, trả về posts của user khớp keyword.
- Giữ chung endpoint `/api/posts` để frontend không phải quản lý nhiều nguồn dữ liệu.

2.4. Service/repository/data model updates
- Bổ sung DTO request/response cho update post.
- Bổ sung tham số filter/searchType/includeTrending ở service và repository.
- Cập nhật mapping `Post` nếu cần expose trường trend metadata (tùy chọn: `isTrending`, `trendRank`).

2.5. DB support (*parallel with 2.4 after query draft*)
- Tạo migration mới cho index phục vụ query 7 ngày + search (nếu thiếu), ví dụ:
- `(created_at DESC)`, `(like_count, comment_count)`, `(lower(title))`, `(lower(content))`, `(lower(username))`, `(lower(full_name))`.
- Không sửa migration cũ; thêm migration version mới (ví dụ V7).

3. Phase C - Frontend (*depends on 2*)
3.1. Repository & Models
- Cập nhật `PostRepository`: thêm endpoint `updatePost`, tái sử dụng `deletePost`.
- Mở rộng hàm lấy bài (`getPosts`) với `searchType`, `search` và `includeTrending`.
- Mở rộng hàm lấy bài của user (`getUserPosts`) với `startDate` và `endDate`.
- Sửa lại các data model nếu backend trả về để show trending metadata (ex: flag `isTrending`).

3.2. Home Screen (Search & Trending)
- Cập nhật `HomeState` cho `searchType`, `searchText` (với model update tương ứng).
- Bổ sung chọn chế độ Search (bài viết/user) qua UI của `PostSearchBar.kt`.
- `HomeViewModel`: Truyền `includeTrending=true` cho feed ALL. Dispatch tìm kiếm có truyền `searchType`.
- `HomeScreen`: Dựa trên metadata trending hoặc index để render UI *Badge Xu hướng* cho bài viết.

3.3. My Posts Screen (Sửa/Xóa/Lọc Ngày)
- Thêm filter `startDate` - `endDate` trên màn `MyPostsScreen`, trigger reload khi đổi filter.
- Support action Delete: Bật dialog xác nhận -> gọi `deletePost` -> Reload danh sách.
- Support action Edit:
  - Tạo Edit Screen hoặc BottomSheet cho các trường `title`, `content`, và Image Picker cho phép chọn ảnh mới.
  - Xử lý upload ảnh ngầm (background process) tương tự lúc tạo bài viết để lấy URL mới nếu user đổi ảnh.
  - Gọi API `updatePost` với `imageUrl` mới (hoặc giữ cũ nếu không đổi) và xử lý kết quả -> Reload.
  - Sửa `App.kt` để đăng ký route cho Edit Post nếu thiết kế cần màn riêng.

3.4. User Profile (Đổi Avatar)
- Thêm tính năng thay đổi ảnh đại diện (Avatar) cho user.
- Thêm Image Picker ở UserProfileScreen, khi chọn ảnh sẽ upload lên Cloudinary.
- Cập nhật API Backend để cho phép Update User (chủ yếu là update `avatarUrl`).

4. Phase D - Verification & rollout (*depends on 2,3*)
4.1. Backend verification
- Test update post owner/non-owner, delete post owner/non-owner.
- Test my-post date range valid/invalid.
- Test searchType=post và searchType=user.
- Test trending: dữ liệu trong 7 ngày với like/comment cao được đẩy lên trước.

4.2. Frontend verification
- My Posts: edit/delete/date-filter end-to-end.
- Home: search mode toggle hoạt động đúng.
- Home ALL: bài trending hiển thị ưu tiên; Following giữ hành vi cũ.

4.3. Regression
- Mở rộng `test-api.ps1` cho các API mới (PUT post, date filter, searchType, trending order sanity check).
- Smoke test app: Home, MyPosts, UserProfile không bị regression điều hướng.

**Relevant files**
- `server/src/main/kotlin/com/foodfest/app/features/post/PostRoute.kt` — thêm `PUT /api/posts/{postId}`, mở rộng query params cho list/user-posts.
- `server/src/main/kotlin/com/foodfest/app/features/post/PostService.kt` — thêm `updatePost`, xử lý trending/searchType/date-range orchestration.
- `server/src/main/kotlin/com/foodfest/app/features/post/PostTable.kt` — thêm query update post, my-post date range, trending ranking bằng window function, user-search join.
- `server/src/main/kotlin/com/foodfest/app/features/auth/AuthTable.kt` — tái sử dụng điều kiện tìm user (`username/full_name`) nếu cần shared query helper. Bổ sung update avatar.
- `database/migrations/` — thêm migration mới cho index hỗ trợ trending/search.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/home/data/PostRepository.kt` — thêm `updatePost`, `deletePost`, params `searchType`, `includeTrending`, `startDate/endDate`.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/home/presentation/HomeViewModel.kt` — thêm state và xử lý search mode + trending flags.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/home/presentation/models/HomeState.kt` — thêm `searchType` (và metadata trending nếu dùng).
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/home/presentation/HomeScreen.kt` — cập nhật UI search mode và render badge xu hướng.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/home/presentation/components/PostSearchBar.kt` — mở rộng UI cho tìm bài/tìm người.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/features/profile/presentation/MyPostsScreen.kt` — thêm filter ngày, edit/delete actions, dialog xác nhận.
- `composeApp/src/commonMain/kotlin/com/foodfest/app/App.kt` — thêm route màn edit post nếu chọn navigation-based edit.
- `test-api.ps1` — thêm regression case cho edit/delete/date-range/trending/searchType.

**Verification**
1. Backend compile: `./gradlew.bat :server:compileKotlin`.
2. Frontend compile: `./gradlew.bat :composeApp:compileDebugKotlinAndroid`.
3. API regression:
- `PUT /api/posts/{postId}`: owner success, non-owner forbidden.
- `GET /api/users/{userId}/posts?startDate=&endDate=`: đúng range + lỗi format.
- `GET /api/posts?searchType=post&search=...` và `searchType=user`.
- `GET /api/posts?includeTrending=true`: xác nhận ordering ưu tiên trending 7 ngày.
4. Manual QA:
- My Posts: sửa/xóa/lọc ngày chạy end-to-end.
- Home: chuyển tìm bài/tìm người đúng kết quả.
- Feed ALL: bài trend được đẩy lên đầu; Following không đổi behavior.

**Decisions**
- Bao gồm trong scope:
- Edit post (title/content/ảnh file trực tiếp), delete post trong My Posts, date range filter ở My Posts.
- Cập nhật ảnh đại diện (avatar) cho tài khoản.
- Trending 7 ngày trộn chung feed bằng window function.
- Search người hoặc bài ngay trong Home.
- Loại trừ khỏi scope:
- Không sửa `postType` khi edit.
- Không thêm date filter cho Home/Following.
- Không tạo màn Search riêng hoặc tab Trending riêng.

**Further Considerations**
1. Trending score weight có thể chuyển thành config server (env) để chỉnh nhanh sau release.
2. Nếu query user-search join posts nặng, nên giới hạn page size nhỏ hơn feed thường (ví dụ 10).
3. Cần thống nhất timezone khi lọc `startDate/endDate` để tránh lệch ngày giữa client và server.