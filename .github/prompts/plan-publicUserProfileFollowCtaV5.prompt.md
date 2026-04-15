## Plan: Public User Profile + Follow CTA (V5)

Mục tiêu là thêm luồng bấm vào tác giả bài viết để mở trang cá nhân người khác, có lịch sử bài đăng, số likes tổng, followers, following, và nút Follow hoặc Followed đúng màu.
Mình đã lưu kế hoạch vào session plan (nội bộ), và đây là bản để bạn duyệt.

## Execution Phases

### Phase 1 - Scope lock và chốt UX
1. Thêm section mới ở cuối docs/implementation.md (vùng sau dòng 436) với tên đề xuất: Phase 2.6 - Public User Profile from Post Author Click.
2. Chốt acceptance UX:
- Bấm tác giả bài viết thì mở trang cá nhân của user đó.
- Header có followerCount, followingCount, totalReceivedLikes.
- Có lịch sử bài đăng của user đó (paginate).
- Follow màu xanh khi chưa follow, Followed màu xám khi đã follow.

**Output Phase 1**
1. Scope tài liệu đã lock, không phát sinh yêu cầu ngoài phạm vi.

### Phase 2 - Contract backend và DB Version 5
1. Chốt contract endpoint profile công khai:
- Đề xuất: GET /api/users/{userId}/profile.
- Response: user info + followerCount + followingCount + postCount + totalReceivedLikes + isFollowing (nếu có token).
2. Tạo migration V5:
- Thêm index phục vụ truy vấn profile/user posts.
- Nếu cần, tạo view tổng hợp stats profile để giảm lặp aggregate query.
3. Cập nhật schema bootstrap và database docs để include V5.

**Output Phase 2**
1. Contract API đã chốt.
2. Migration V5 đã sẵn sàng.

### Phase 3 - Implement backend
1. Mở rộng AuthTable/AuthRepository để lấy public profile stats.
2. Mở rộng authService với getPublicProfile(userId, currentUserId?).
3. Thêm route endpoint profile công khai.
4. Tái sử dụng endpoint user posts hiện có để hiển thị lịch sử bài đăng.
5. Chuẩn hóa lỗi:
- userId không hợp lệ -> 400.
- user không tồn tại -> 404.

**Output Phase 3**
1. Endpoint profile công khai chạy ổn định đúng contract.

### Phase 4 - Navigation app và màn UserProfile
1. Thêm Screen.UserProfile trong App navigation.
2. Nối click user từ HomeScreen và SavedPostsScreen sang route UserProfile.
3. Tạo UserProfileScreen + ViewModel:
- load profile header stats,
- load post history theo page,
- state loading/empty/error/pagination đầy đủ.
4. Tận dụng phần list từ MyPostsScreen để giảm rewrite.

**Output Phase 4**
1. Bấm user từ feed hoặc saved posts mở đúng profile người đó.

### Phase 5 - Follow CTA UX trên profile người khác
1. Dùng FollowRepository để check/toggle follow.
2. Render đúng trạng thái:
- Follow: nền xanh.
- Followed: nền xám.
- Loading: disable button + spinner.
3. Sau toggle thành công:
- cập nhật ngay trạng thái nút,
- cập nhật followerCount trên header.
4. Nếu profile của chính mình thì ẩn nút follow.

**Output Phase 5**
1. Follow CTA đúng màu, đúng logic, đúng trạng thái.

### Phase 6 - Regression và Exit gate
1. Build pass:
- server compile,
- compose compile.
2. Migration V5 pass trên DB mới và DB có dữ liệu.
3. Mở rộng test-api.ps1 cho public profile + follow state.
4. Manual QA:
- Home -> bấm user -> profile đúng.
- SavedPosts -> bấm user -> profile đúng.
- Follow xanh khi chưa follow.
- Followed xám khi đã follow.
- Stats và post history cập nhật đúng sau follow/unfollow.

**Output Phase 6**
1. Feature đạt tiêu chí sẵn sàng merge/release.

**Relevant files**
- docs/implementation.md — thêm section mới ở cuối file (sau vùng dòng 436).
- database/migrations/V5__add_public_profile_stats_view_and_indexes.sql — migration mới V5.
- database/schema.sql — include V5.
- database/README.md — cập nhật danh sách migration.
- server/src/main/kotlin/com/foodfest/app/features/auth/AuthTable.kt — query stats profile công khai.
- server/src/main/kotlin/com/foodfest/app/features/auth/authService.kt — logic getPublicProfile.
- server/src/main/kotlin/com/foodfest/app/features/auth/authRoute.kt — route profile công khai.
- server/src/main/kotlin/com/foodfest/app/features/post/PostRoute.kt — tái sử dụng endpoint user posts.
- composeApp/src/commonMain/kotlin/com/foodfest/app/App.kt — thêm route màn UserProfile.
- composeApp/src/commonMain/kotlin/com/foodfest/app/features/auth/data/AuthRepository.kt — gọi API profile công khai.
- composeApp/src/commonMain/kotlin/com/foodfest/app/features/home/presentation/HomeScreen.kt — điều hướng từ click user.
- composeApp/src/commonMain/kotlin/com/foodfest/app/features/savedposts/presentation/SavedPostsScreen.kt — điều hướng từ click user.
- composeApp/src/commonMain/kotlin/com/foodfest/app/features/profile/presentation/MyPostsScreen.kt — tái sử dụng logic list bài đăng.
- composeApp/src/commonMain/kotlin/com/foodfest/app/features/follow/data/FollowRepository.kt — follow toggle/check.
- test-api.ps1 — thêm regression cho public profile + follow state.

**Verification**
1. Build pass: server compile và compose compile.
2. Migration V5 chạy được trên DB mới và DB đang có dữ liệu.
3. Regression API pass cho:
- lấy profile công khai user khác,
- followers/following/likes/posts count,
- follow toggle phản ánh đúng trạng thái.
4. Manual QA pass cho:
- click user từ Home và SavedPosts,
- nút Follow xanh khi chưa follow,
- nút Followed xám khi đã follow.

**Decisions đã chốt trong plan**
- Số lượt thích trên header = tổng likes nhận được trên tất cả bài đăng của user.
- Mọi thay đổi DB được nhét vào migration V5.
- Có thể dùng DB view trong V5 để gom stats profile nếu muốn query gọn và ổn định.

Nếu bạn ok plan này, bước tiếp theo là triển khai lần lượt từ Phase 1 đến Phase 6.