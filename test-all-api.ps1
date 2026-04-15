param(
    [string]$BaseUrl = "http://127.0.0.1:8080"
)

$ErrorActionPreference = "Stop"

function Write-Step($msg) {
    Write-Host "`n=== $msg ===" -ForegroundColor Yellow
}

function Assert-True($condition, [string]$message) {
    if (-not $condition) {
        throw "ASSERT FAILED: $message"
    }
}

function Invoke-Json($method, $url, $token = $null, $body = $null) {
    $headers = @{ "Accept" = "application/json" }
    if ($token) {
        $headers["Authorization"] = "Bearer $token"
    }

    if ($null -ne $body) {
        $json = ($body | ConvertTo-Json -Depth 20)
        return Invoke-RestMethod -Method $method -Uri $url -Headers $headers -ContentType "application/json" -Body $json
    }

    return Invoke-RestMethod -Method $method -Uri $url -Headers $headers
}

try {
    Write-Host "============================================" -ForegroundColor Cyan
    Write-Host "       FOODFEST API TEST SCRIPT            " -ForegroundColor Cyan
    Write-Host "============================================" -ForegroundColor Cyan

    Write-Step "Health check"
    $health = Invoke-RestMethod -Method GET -Uri "$BaseUrl/health"
    Assert-True ($health -eq "OK") "GET /health should return OK"

    $suffix = (Get-Random -Minimum 10000 -Maximum 99999)
    $username1 = "all_api_user_$suffix"
    $password = "password123"

    Write-Step "Auth: register/login primary user"
    $reg1 = Invoke-Json POST "$BaseUrl/api/auth/register" $null @{
        username = $username1
        password = $password
        fullName = "All API User $suffix"
    }
    Assert-True ($reg1.success -eq $true) "Register primary user should succeed"
    $token = $reg1.data.token
    $userId = [int]$reg1.data.user.id
    Assert-True ($token) "Register should return token"

    $login1 = Invoke-Json POST "$BaseUrl/api/auth/login" $null @{
        username = $username1
        password = $password
    }
    Assert-True ($login1.success -eq $true) "Login primary user should succeed"
    Assert-True ($login1.data.token) "Login should return token"

    Write-Step "Posts: list/create/get/like/save/comment"
    $posts = Invoke-Json GET "$BaseUrl/api/posts" $token
    Assert-True ($posts.success -eq $true) "GET /api/posts should succeed"

    $createPost = Invoke-Json POST "$BaseUrl/api/posts" $token @{
        postType = "recipe"
        title = "All API Post $suffix"
        content = "Post for test-all script"
    }
    Assert-True ($createPost.success -eq $true) "Create post should succeed"
    $postId = [int]$createPost.data.id

    $singlePost = Invoke-Json GET "$BaseUrl/api/posts/$postId" $token
    Assert-True ($singlePost.success -eq $true) "GET /api/posts/{id} should succeed"

    $likeOn = Invoke-Json POST "$BaseUrl/api/posts/$postId/like" $token
    Assert-True ($likeOn.success -eq $true) "Like post should succeed"
    Assert-True ($likeOn.data.isLiked -eq $true) "Post should be liked"

    $saveOn = Invoke-Json POST "$BaseUrl/api/posts/$postId/save" $token
    Assert-True ($saveOn.success -eq $true) "Save post should succeed"
    Assert-True ($saveOn.data.isSaved -eq $true) "Post should be saved"

    $comment = Invoke-Json POST "$BaseUrl/api/posts/$postId/comments" $token @{ content = "Comment from test-all" }
    Assert-True ($comment.success -eq $true) "Create comment should succeed"

    $comments = Invoke-Json GET "$BaseUrl/api/posts/$postId/comments?page=1&limit=20" $token
    Assert-True ($comments.success -eq $true) "Get comments should succeed"
    Assert-True ($comments.data.total -ge 1) "Comments total should be at least 1"

    Write-Step "Auth: register/login secondary user"
    $username2 = "all_api_user2_$suffix"
    $reg2 = Invoke-Json POST "$BaseUrl/api/auth/register" $null @{
        username = $username2
        password = $password
        fullName = "All API User 2 $suffix"
    }
    Assert-True ($reg2.success -eq $true) "Register secondary user should succeed"
    $user2Id = [int]$reg2.data.user.id

    $login2 = Invoke-Json POST "$BaseUrl/api/auth/login" $null @{
        username = $username2
        password = $password
    }
    Assert-True ($login2.success -eq $true) "Login secondary user should succeed"

    Write-Step "Follow: follow/check/followers/following/unfollow"
    $followOn = Invoke-Json POST "$BaseUrl/api/users/$user2Id/follow" $token
    Assert-True ($followOn.success -eq $true) "Follow user should succeed"
    Assert-True ($followOn.data.isFollowing -eq $true) "isFollowing should be true after follow"

    $isFollowing = Invoke-Json GET "$BaseUrl/api/users/$user2Id/is-following" $token
    Assert-True ($isFollowing.success -eq $true) "Check is-following should succeed"
    Assert-True ($isFollowing.data.isFollowing -eq $true) "Check should return true"

    $followers = Invoke-Json GET "$BaseUrl/api/users/$user2Id/followers?page=1" $token
    Assert-True ($followers.success -eq $true) "Get followers should succeed"
    Assert-True ((@($followers.data.data | Where-Object { [int]$_.id -eq $userId }).Count) -ge 1) "Followers should include primary user"

    $following = Invoke-Json GET "$BaseUrl/api/users/$userId/following?page=1" $token
    Assert-True ($following.success -eq $true) "Get following should succeed"
    Assert-True ((@($following.data.data | Where-Object { [int]$_.id -eq $user2Id }).Count) -ge 1) "Following should include secondary user"

    $followOff = Invoke-Json POST "$BaseUrl/api/users/$user2Id/follow" $token
    Assert-True ($followOff.success -eq $true) "Unfollow user should succeed"
    Assert-True ($followOff.data.isFollowing -eq $false) "isFollowing should be false after unfollow"

    Write-Step "Cleanup toggles and post"
    $saveOff = Invoke-Json POST "$BaseUrl/api/posts/$postId/save" $token
    Assert-True ($saveOff.success -eq $true) "Unsave post should succeed"

    $likeOff = Invoke-Json POST "$BaseUrl/api/posts/$postId/like" $token
    Assert-True ($likeOff.success -eq $true) "Unlike post should succeed"

    $deletePost = Invoke-Json DELETE "$BaseUrl/api/posts/$postId" $token
    Assert-True ($deletePost.success -eq $true) "Delete test post should succeed"

    Write-Host "`n============================================" -ForegroundColor Green
    Write-Host "         ALL TEST-ALL CHECKS PASSED        " -ForegroundColor Green
    Write-Host "============================================" -ForegroundColor Green
    Write-Host "Primary user: $username1 (id=$userId)" -ForegroundColor DarkGray
    Write-Host "Secondary user: $username2 (id=$user2Id)" -ForegroundColor DarkGray
}
catch {
    Write-Host "`n============================================" -ForegroundColor Red
    Write-Host "             TEST-ALL FAILED               " -ForegroundColor Red
    Write-Host "============================================" -ForegroundColor Red
    Write-Host $_ -ForegroundColor Red
    exit 1
}
