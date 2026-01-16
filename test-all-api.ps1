# Test All API Script for FoodFest
$baseUrl = "http://127.0.0.1:8080"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "       FOODFEST API TEST SCRIPT            " -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# 1. LOGIN
Write-Host "`n=== 1. LOGIN ===" -ForegroundColor Yellow
try {
    $loginBody = '{"email":"test@test.com","password":"123456"}'
    $login = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody
    $token = $login.token
    $userId = $login.user.id
    Write-Host "SUCCESS: Login OK" -ForegroundColor Green
    Write-Host "User ID: $userId"
    Write-Host "Token: $($token.Substring(0,50))..."
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# 2. GET POSTS (empty initially)
Write-Host "`n=== 2. GET POSTS ===" -ForegroundColor Yellow
try {
    $posts = Invoke-RestMethod -Uri "$baseUrl/api/posts" -Method GET -Headers $headers
    Write-Host "SUCCESS: Got $($posts.posts.Count) posts" -ForegroundColor Green
    $posts | ConvertTo-Json -Depth 3
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# 3. CREATE POST
Write-Host "`n=== 3. CREATE POST ===" -ForegroundColor Yellow
try {
    $postBody = @{
        postType = "recipe"
        title = "Pho Bo Ha Noi"
        content = "Day la cong thuc nau pho ngon nhat!"
        imageUrl = "https://example.com/pho.jpg"
        tags = @("pho", "vietnamese", "soup")
    } | ConvertTo-Json
    
    $newPost = Invoke-RestMethod -Uri "$baseUrl/api/posts" -Method POST -Headers $headers -Body $postBody
    Write-Host "SUCCESS: Created post" -ForegroundColor Green
    $newPost | ConvertTo-Json -Depth 3
    $postId = $newPost.id
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host $_.ErrorDetails.Message -ForegroundColor Red
}

# 4. GET SINGLE POST
Write-Host "`n=== 4. GET SINGLE POST ===" -ForegroundColor Yellow
if ($postId) {
    try {
        $singlePost = Invoke-RestMethod -Uri "$baseUrl/api/posts/$postId" -Method GET -Headers $headers
        Write-Host "SUCCESS: Got post $postId" -ForegroundColor Green
        $singlePost | ConvertTo-Json -Depth 3
    } catch {
        Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 5. LIKE POST
Write-Host "`n=== 5. LIKE POST ===" -ForegroundColor Yellow
if ($postId) {
    try {
        $likeResult = Invoke-RestMethod -Uri "$baseUrl/api/posts/$postId/like" -Method POST -Headers $headers
        Write-Host "SUCCESS: Liked post" -ForegroundColor Green
        $likeResult | ConvertTo-Json
    } catch {
        Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 6. SAVE POST
Write-Host "`n=== 6. SAVE POST ===" -ForegroundColor Yellow
if ($postId) {
    try {
        $saveResult = Invoke-RestMethod -Uri "$baseUrl/api/posts/$postId/save" -Method POST -Headers $headers
        Write-Host "SUCCESS: Saved post" -ForegroundColor Green
        $saveResult | ConvertTo-Json
    } catch {
        Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 7. GET SAVED POSTS
Write-Host "`n=== 7. GET SAVED POSTS ===" -ForegroundColor Yellow
try {
    $savedPosts = Invoke-RestMethod -Uri "$baseUrl/api/posts/saved" -Method GET -Headers $headers
    Write-Host "SUCCESS: Got $($savedPosts.posts.Count) saved posts" -ForegroundColor Green
    $savedPosts | ConvertTo-Json -Depth 3
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# 8. ADD COMMENT
Write-Host "`n=== 8. ADD COMMENT ===" -ForegroundColor Yellow
if ($postId) {
    try {
        $commentBody = @{
            content = "Nhin ngon qua! Cam on ban da chia se cong thuc!"
        } | ConvertTo-Json
        
        $comment = Invoke-RestMethod -Uri "$baseUrl/api/posts/$postId/comments" -Method POST -Headers $headers -Body $commentBody
        Write-Host "SUCCESS: Added comment" -ForegroundColor Green
        $comment | ConvertTo-Json -Depth 3
    } catch {
        Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 9. GET COMMENTS
Write-Host "`n=== 9. GET COMMENTS ===" -ForegroundColor Yellow
if ($postId) {
    try {
        $comments = Invoke-RestMethod -Uri "$baseUrl/api/posts/$postId/comments" -Method GET -Headers $headers
        Write-Host "SUCCESS: Got $($comments.comments.Count) comments" -ForegroundColor Green
        $comments | ConvertTo-Json -Depth 3
    } catch {
        Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 10. FOLLOW USER (follow yourself for testing, or need another user)
Write-Host "`n=== 10. FOLLOW USER ===" -ForegroundColor Yellow
# First, register another user if needed
try {
    $registerBody = '{"email":"testuser2@test.com","password":"123456","name":"Test User 2"}'
    $register = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method POST -ContentType "application/json" -Body $registerBody
    $user2Id = $register.user.id
    Write-Host "Created test user 2: $user2Id" -ForegroundColor Gray
} catch {
    # User might already exist, try login
    try {
        $loginBody2 = '{"email":"testuser2@test.com","password":"123456"}'
        $login2 = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -ContentType "application/json" -Body $loginBody2
        $user2Id = $login2.user.id
        Write-Host "User 2 exists: $user2Id" -ForegroundColor Gray
    } catch {
        Write-Host "Could not get user 2" -ForegroundColor Red
    }
}

if ($user2Id) {
    try {
        $followResult = Invoke-RestMethod -Uri "$baseUrl/api/users/$user2Id/follow" -Method POST -Headers $headers
        Write-Host "SUCCESS: Follow result" -ForegroundColor Green
        $followResult | ConvertTo-Json
    } catch {
        Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 11. CHECK IS FOLLOWING
Write-Host "`n=== 11. CHECK IS FOLLOWING ===" -ForegroundColor Yellow
if ($user2Id) {
    try {
        $isFollowing = Invoke-RestMethod -Uri "$baseUrl/api/users/$user2Id/is-following" -Method GET -Headers $headers
        Write-Host "SUCCESS: Is following check" -ForegroundColor Green
        $isFollowing | ConvertTo-Json
    } catch {
        Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 12. GET FOLLOWERS
Write-Host "`n=== 12. GET FOLLOWERS ===" -ForegroundColor Yellow
try {
    $followers = Invoke-RestMethod -Uri "$baseUrl/api/users/$userId/followers" -Method GET -Headers $headers
    Write-Host "SUCCESS: Got followers" -ForegroundColor Green
    $followers | ConvertTo-Json -Depth 3
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# 13. GET FOLLOWING
Write-Host "`n=== 13. GET FOLLOWING ===" -ForegroundColor Yellow
try {
    $following = Invoke-RestMethod -Uri "$baseUrl/api/users/$userId/following" -Method GET -Headers $headers
    Write-Host "SUCCESS: Got following" -ForegroundColor Green
    $following | ConvertTo-Json -Depth 3
} catch {
    Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

# 14. UNLIKE POST (toggle)
Write-Host "`n=== 14. UNLIKE POST (toggle) ===" -ForegroundColor Yellow
if ($postId) {
    try {
        $unlikeResult = Invoke-RestMethod -Uri "$baseUrl/api/posts/$postId/like" -Method POST -Headers $headers
        Write-Host "SUCCESS: Unlike result" -ForegroundColor Green
        $unlikeResult | ConvertTo-Json
    } catch {
        Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 15. UNSAVE POST (toggle)
Write-Host "`n=== 15. UNSAVE POST (toggle) ===" -ForegroundColor Yellow
if ($postId) {
    try {
        $unsaveResult = Invoke-RestMethod -Uri "$baseUrl/api/posts/$postId/save" -Method POST -Headers $headers
        Write-Host "SUCCESS: Unsave result" -ForegroundColor Green
        $unsaveResult | ConvertTo-Json
    } catch {
        Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 16. UNFOLLOW USER (toggle)
Write-Host "`n=== 16. UNFOLLOW USER (toggle) ===" -ForegroundColor Yellow
if ($user2Id) {
    try {
        $unfollowResult = Invoke-RestMethod -Uri "$baseUrl/api/users/$user2Id/follow" -Method POST -Headers $headers
        Write-Host "SUCCESS: Unfollow result" -ForegroundColor Green
        $unfollowResult | ConvertTo-Json
    } catch {
        Write-Host "FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n============================================" -ForegroundColor Cyan
Write-Host "           TEST COMPLETED!                 " -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
