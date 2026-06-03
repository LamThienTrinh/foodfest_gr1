param(
  [string]$BaseUrl = "http://localhost:8080",
  [switch]$SkipRegister,
  [string]$Username,
  [string]$Password = "password123",
  [string]$FullName = "API Test User"
)

$ErrorActionPreference = "Stop"

function Write-Step($msg) {
  Write-Host "`n=== $msg ===" -ForegroundColor Cyan
}

function Assert-True($condition, [string]$message) {
  if (-not $condition) {
    throw "ASSERT FAILED: $message"
  }
}

function Invoke-Json($method, $url, $token = $null, $body = $null) {
  $headers = @{
    "Accept" = "application/json"
  }
  if ($token) {
    $headers["Authorization"] = "Bearer $token"
  }

  if ($null -ne $body) {
    $json = ($body | ConvertTo-Json -Depth 20)
    return Invoke-RestMethod -Method $method -Uri $url -Headers $headers -ContentType "application/json" -Body $json
  }

  return Invoke-RestMethod -Method $method -Uri $url -Headers $headers
}

function Invoke-Raw($method, $url, $token = $null, $body = $null) {
  $headers = @{
    "Accept" = "application/json"
  }
  if ($token) {
    $headers["Authorization"] = "Bearer $token"
  }

  $params = @{
    Method = $method
    Uri = $url
    Headers = $headers
    SkipHttpErrorCheck = $true
  }

  if ($null -ne $body) {
    $params["ContentType"] = "application/json"
    $params["Body"] = ($body | ConvertTo-Json -Depth 20)
  }

  return Invoke-WebRequest @params
}

function Get-FirstDishId($baseUrl) {
  $resp = Invoke-Json GET "$baseUrl/api/dishes?page=1"
  Assert-True ($resp.success -eq $true) "GET /api/dishes should succeed"
  Assert-True ($resp.data -ne $null) "GET /api/dishes should have data"
  $items = $resp.data.data
  Assert-True ($items -ne $null -and $items.Count -gt 0) "Need at least 1 dish in database to run tests"
  return [int]$items[0].id
}

try {
  Write-Step "Health check"
  $health = Invoke-RestMethod -Method GET -Uri "$BaseUrl/health"
  Assert-True ($health -eq "OK") "GET /health should return OK"

  if (-not $Username -or $Username.Trim().Length -eq 0) {
    $suffix = (Get-Random -Minimum 10000 -Maximum 99999)
    $Username = "api_test_$suffix"
  }

  Write-Step "Auth (register/login)"
  if (-not $SkipRegister) {
    $regBody = @{ username = $Username; password = $Password; fullName = $FullName }
    $reg = Invoke-Json POST "$BaseUrl/api/auth/register" $null $regBody
    Assert-True ($reg.success -eq $true) "Register should succeed"
    Assert-True ($reg.data.token) "Register should return token"
  }

  $loginBody = @{ username = $Username; password = $Password }
  $login = Invoke-Json POST "$BaseUrl/api/auth/login" $null $loginBody
  Assert-True ($login.success -eq $true) "Login should succeed"
  $token = $login.data.token
  Assert-True ($token) "Login should return token"

  Write-Step "Pick a dish to test"
  $dishId = Get-FirstDishId $BaseUrl
  Write-Host "Using dishId=$dishId" -ForegroundColor Yellow

  Write-Step "Verify new routes exist on this running server"
  $probeFav = Invoke-Raw GET "$BaseUrl/api/favorites?page=1" $token
  if ($probeFav.StatusCode -eq 404) {
    throw "Server returned 404 for /api/favorites. Restart server with './gradlew :server:run' to load new routes."
  }
  $probeMy = Invoke-Raw GET "$BaseUrl/api/my-dishes?page=1" $token
  if ($probeMy.StatusCode -eq 404) {
    throw "Server returned 404 for /api/my-dishes. Restart server with './gradlew :server:run' to load new routes."
  }
  $probeFamily = Invoke-Raw GET "$BaseUrl/api/families" $token
  if ($probeFamily.StatusCode -eq 404) {
    throw "Server returned 404 for /api/families. Restart server with './gradlew :server:run' to load new routes."
  }

  # -----------------------
  # FAVORITES
  # -----------------------
  Write-Step "Favorites: GET list (should be OK)"
  $favList1 = Invoke-Json GET "$BaseUrl/api/favorites?page=1" $token
  Assert-True ($favList1.success -eq $true) "GET /api/favorites should succeed"

  Write-Step "Favorites: check -> toggle on -> check -> toggle off"
  $chk1 = Invoke-Json GET "$BaseUrl/api/favorites/check/$dishId" $token
  Assert-True ($chk1.success -eq $true) "GET /api/favorites/check/{dishId} should succeed"

  $toggle1 = Invoke-Json POST "$BaseUrl/api/favorites/toggle/$dishId" $token
  Assert-True ($toggle1.success -eq $true) "POST /api/favorites/toggle/{dishId} should succeed"
  Assert-True ($toggle1.data.isFavorite -eq $true) "After first toggle, isFavorite should be true"

  $chk2 = Invoke-Json GET "$BaseUrl/api/favorites/check/$dishId" $token
  Assert-True ($chk2.success -eq $true) "Check after toggle should succeed"
  Assert-True ($chk2.data.isFavorite -eq $true) "Check should show isFavorite=true"

  $toggle2 = Invoke-Json POST "$BaseUrl/api/favorites/toggle/$dishId" $token
  Assert-True ($toggle2.success -eq $true) "Second toggle should succeed"
  Assert-True ($toggle2.data.isFavorite -eq $false) "After second toggle, isFavorite should be false"

  Write-Step "Favorites: add -> ids -> remove"
  $add = Invoke-Json POST "$BaseUrl/api/favorites/add/$dishId" $token
  Assert-True ($add.success -eq $true) "POST /api/favorites/add/{dishId} should succeed"

  $ids = Invoke-Json GET "$BaseUrl/api/favorites/ids" $token
  Assert-True ($ids.success -eq $true) "GET /api/favorites/ids should succeed"
  $idList = @($ids.data.ids)
  Assert-True ($idList -contains $dishId) "Favorite ids should contain dishId"

  $del = Invoke-Json DELETE "$BaseUrl/api/favorites/remove/$dishId" $token
  Assert-True ($del.success -eq $true) "DELETE /api/favorites/remove/{dishId} should succeed"

  # -----------------------
  # MY DISHES (personal)
  # -----------------------
  Write-Step "My dishes: list (should be OK)"
  $my1 = Invoke-Json GET "$BaseUrl/api/my-dishes?page=1" $token
  Assert-True ($my1.success -eq $true) "GET /api/my-dishes should succeed"

  Write-Step "My dishes: check saved (should be false)"
  $saved1 = Invoke-Json GET "$BaseUrl/api/my-dishes/check/$dishId" $token
  Assert-True ($saved1.success -eq $true) "GET /api/my-dishes/check/{originalDishId} should succeed"
  Assert-True ($saved1.data.hasSaved -eq $false) "Expected hasSaved=false before creating"

  Write-Step "My dishes: create"
  # Load original dish details to build request
  $dish = Invoke-Json GET "$BaseUrl/api/dishes/$dishId"
  Assert-True ($dish.success -eq $true) "GET /api/dishes/{id} should succeed"
  
  $createBody = @{
    originalDishId = $dishId
    dishName = $dish.data.name
    imageUrl = $dish.data.imageUrl
    description = $dish.data.description
    ingredients = $dish.data.ingredients
    instructions = $dish.data.instructions
    prepTime = $dish.data.prepTime
    cookTime = $dish.data.cookTime
    serving = $dish.data.serving
    note = "Saved by automated test"
    tagIds = @()
  }
  $created = Invoke-Json POST "$BaseUrl/api/my-dishes" $token $createBody
  Assert-True ($created.success -eq $true) "POST /api/my-dishes should succeed"
  $personalId = [int]$created.data.id
  Assert-True ($personalId -gt 0) "Created personal dish should have id"
  Write-Host "Created personalDishId=$personalId" -ForegroundColor Yellow

  Write-Step "My dishes: check saved (should be true)"
  $saved2 = Invoke-Json GET "$BaseUrl/api/my-dishes/check/$dishId" $token
  Assert-True ($saved2.success -eq $true) "Check after create should succeed"
  Assert-True ($saved2.data.hasSaved -eq $true) "Expected hasSaved=true after creating"

  Write-Step "My dishes: get by id"
  $get1 = Invoke-Json GET "$BaseUrl/api/my-dishes/$personalId" $token
  Assert-True ($get1.success -eq $true) "GET /api/my-dishes/{id} should succeed"
  Assert-True ($get1.data.id -eq $personalId) "Returned personal dish id should match"

  Write-Step "My dishes: update"
  $newName = "$($get1.data.dishName) (My Version)"
  $updateBody = @{ dishName = $newName }
  $upd = Invoke-Json PUT "$BaseUrl/api/my-dishes/$personalId" $token $updateBody
  Assert-True ($upd.success -eq $true) "PUT /api/my-dishes/{id} should succeed"
  Assert-True ($upd.data.dishName -eq $newName) "Dish name should be updated"

  Write-Step "My dishes: delete"
  $delMy = Invoke-Json DELETE "$BaseUrl/api/my-dishes/$personalId" $token
  Assert-True ($delMy.success -eq $true) "DELETE /api/my-dishes/{id} should succeed"

  # -----------------------
  # COMMENTS (post flow)
  # -----------------------
  Write-Step "Comments: create post for test"
  $commentPostBody = @{
    postType = "recipe"
    title = "Comment Regression $((Get-Random -Minimum 1000 -Maximum 9999))"
    content = "Post used for comment API regression"
  }
  $commentPost = Invoke-Json POST "$BaseUrl/api/posts" $token $commentPostBody
  Assert-True ($commentPost.success -eq $true) "POST /api/posts should succeed for comment regression"
  $commentPostId = [int]$commentPost.data.id
  Assert-True ($commentPostId -gt 0) "Created post should have id"

  Write-Step "Comments: add 3 top-level comments"
  $commentTexts = @(
    "Comment #1 from regression",
    "Comment #2 from regression",
    "Comment #3 from regression"
  )
  $topLevelCommentIds = @()
  foreach ($text in $commentTexts) {
    $newComment = Invoke-Json POST "$BaseUrl/api/posts/$commentPostId/comments" $token @{ content = $text }
    Assert-True ($newComment.success -eq $true) "POST /api/posts/{postId}/comments should succeed"
    Assert-True ($newComment.data.postId -eq $commentPostId) "Created comment should belong to target post"
    Assert-True ($null -eq $newComment.data.parentCommentId) "Top-level comment should have parentCommentId=null"
    $topLevelCommentIds += [int]$newComment.data.id
  }

  Write-Step "Comments: verify pagination page=1 limit=2"
  $commentsPage1 = Invoke-Json GET "$BaseUrl/api/posts/$commentPostId/comments?page=1&limit=2" $token
  Assert-True ($commentsPage1.success -eq $true) "GET comments page 1 should succeed"
  Assert-True ($commentsPage1.data.page -eq 1) "Comments page should be 1"
  Assert-True ($commentsPage1.data.limit -eq 2) "Comments limit should be 2"
  Assert-True ($commentsPage1.data.total -eq 3) "Comments total should be 3"
  Assert-True ((@($commentsPage1.data.data).Count) -eq 2) "Comments page 1 should contain 2 items"

  Write-Step "Comments: verify pagination page=2 limit=2"
  $commentsPage2 = Invoke-Json GET "$BaseUrl/api/posts/$commentPostId/comments?page=2&limit=2" $token
  Assert-True ($commentsPage2.success -eq $true) "GET comments page 2 should succeed"
  Assert-True ($commentsPage2.data.page -eq 2) "Comments page should be 2"
  Assert-True ($commentsPage2.data.limit -eq 2) "Comments limit should be 2"
  Assert-True ($commentsPage2.data.total -eq 3) "Comments total should stay 3"
  Assert-True ((@($commentsPage2.data.data).Count) -eq 1) "Comments page 2 should contain 1 item"

  Write-Step "Comments: add 1 reply (level 2)"
  $parentCommentId = [int]$topLevelCommentIds[0]
  $newReply = Invoke-Json POST "$BaseUrl/api/posts/$commentPostId/comments" $token @{ content = "Reply #1 from regression"; parentCommentId = $parentCommentId }
  Assert-True ($newReply.success -eq $true) "POST reply should succeed"
  Assert-True ([int]$newReply.data.postId -eq $commentPostId) "Reply should belong to target post"
  Assert-True ([int]$newReply.data.parentCommentId -eq $parentCommentId) "Reply should point to parent comment"
  Assert-True ([int]$newReply.data.depth -eq 1) "Reply depth should be 1"
  $replyId = [int]$newReply.data.id

  Write-Step "Comments: top-level list should expose replyCount"
  $topLevelAll = Invoke-Json GET "$BaseUrl/api/posts/$commentPostId/comments?page=1&limit=20" $token
  Assert-True ($topLevelAll.success -eq $true) "GET top-level comments should succeed"
  Assert-True ($topLevelAll.data.total -eq 3) "Top-level total should remain 3"
  $parentInTopLevel = @($topLevelAll.data.data | Where-Object { [int]$_.id -eq $parentCommentId })
  Assert-True ($parentInTopLevel.Count -eq 1) "Parent comment should exist in top-level list"
  Assert-True ([int]$parentInTopLevel[0].replyCount -eq 1) "Parent replyCount should be updated"

  Write-Step "Comments: get replies by parent comment"
  $replies = Invoke-Json GET "$BaseUrl/api/comments/$parentCommentId/replies?page=1&limit=10" $token
  Assert-True ($replies.success -eq $true) "GET /api/comments/{commentId}/replies should succeed"
  Assert-True ($replies.data.total -eq 1) "Replies total should be 1"
  Assert-True ((@($replies.data.data).Count) -eq 1) "Replies list should contain 1 item"
  Assert-True ([int]$replies.data.data[0].parentCommentId -eq $parentCommentId) "Reply item should keep correct parentCommentId"

  Write-Step "Comments: level-3 reply should be rejected"
  $level3Reply = Invoke-Raw POST "$BaseUrl/api/posts/$commentPostId/comments" $token @{ content = "Level 3 should fail"; parentCommentId = $replyId }
  Assert-True ($level3Reply.StatusCode -eq 400) "Creating level-3 reply should return 400"

  Write-Step "Comments: reply with parent from another post should be rejected"
  $crossPostBody = @{
    postType = "recipe"
    title = "Cross Reply Validation $((Get-Random -Minimum 1000 -Maximum 9999))"
    content = "Post used for cross-reply validation"
  }
  $crossPost = Invoke-Json POST "$BaseUrl/api/posts" $token $crossPostBody
  Assert-True ($crossPost.success -eq $true) "Create cross-validation post should succeed"
  $crossPostId = [int]$crossPost.data.id

  $crossReply = Invoke-Raw POST "$BaseUrl/api/posts/$crossPostId/comments" $token @{ content = "Cross post reply should fail"; parentCommentId = $parentCommentId }
  Assert-True ($crossReply.StatusCode -eq 400) "Reply with parent in another post should return 400"

  $deleteCrossPost = Invoke-Json DELETE "$BaseUrl/api/posts/$crossPostId" $token
  Assert-True ($deleteCrossPost.success -eq $true) "Cleanup cross-validation post should succeed"

  Write-Step "Comments: verify post comment_count"
  $postAfterComments = Invoke-Json GET "$BaseUrl/api/posts/$commentPostId" $token
  Assert-True ($postAfterComments.success -eq $true) "GET /api/posts/{id} should succeed"
  Assert-True ($postAfterComments.data.commentCount -eq 4) "post.commentCount should include top-level + replies"

  Write-Step "Comments: delete reply and verify counts rollback"
  $deleteReply = Invoke-Json DELETE "$BaseUrl/api/comments/$replyId" $token
  Assert-True ($deleteReply.success -eq $true) "DELETE /api/comments/{commentId} should succeed"
  Assert-True ($deleteReply.data.deleted -eq $true) "Delete reply result should be true"
  Assert-True ($deleteReply.data.deletedCount -eq 1) "Deleting 1 reply should report deletedCount=1"

  $postAfterDeleteReply = Invoke-Json GET "$BaseUrl/api/posts/$commentPostId" $token
  Assert-True ($postAfterDeleteReply.success -eq $true) "GET post after reply delete should succeed"
  Assert-True ($postAfterDeleteReply.data.commentCount -eq 3) "post.commentCount should decrease after deleting reply"

  $topLevelAfterDeleteReply = Invoke-Json GET "$BaseUrl/api/posts/$commentPostId/comments?page=1&limit=20" $token
  $parentAfterDeleteReply = @($topLevelAfterDeleteReply.data.data | Where-Object { [int]$_.id -eq $parentCommentId })
  Assert-True ($parentAfterDeleteReply.Count -eq 1) "Parent should remain after deleting reply"
  Assert-True ([int]$parentAfterDeleteReply[0].replyCount -eq 0) "Parent replyCount should decrement after deleting reply"

  Write-Step "Comments: blank content should be rejected"
  $blankComment = Invoke-Raw POST "$BaseUrl/api/posts/$commentPostId/comments" $token @{ content = "   " }
  Assert-True ($blankComment.StatusCode -eq 400) "Blank comment content should return 400"

  Write-Step "Comments: cleanup created post"
  $deleteCommentPost = Invoke-Json DELETE "$BaseUrl/api/posts/$commentPostId" $token
  Assert-True ($deleteCommentPost.success -eq $true) "DELETE created comment regression post should succeed"

  # -----------------------
  # PUBLIC PROFILE + FOLLOW STATE
  # -----------------------
  Write-Step "Public profile: register secondary user"
  $profileSuffix = (Get-Random -Minimum 10000 -Maximum 99999)
  $profileUsername = "api_profile_$profileSuffix"
  $profilePassword = "password123"
  $profileFullName = "Profile Target $profileSuffix"

  $profileRegister = Invoke-Json POST "$BaseUrl/api/auth/register" $null @{
    username = $profileUsername
    password = $profilePassword
    fullName = $profileFullName
  }
  Assert-True ($profileRegister.success -eq $true) "Register secondary profile user should succeed"
  $profileUserId = [int]$profileRegister.data.user.id
  $profileUserToken = $profileRegister.data.token
  Assert-True ($profileUserId -gt 0) "Secondary profile user id should be valid"
  Assert-True ($profileUserToken) "Secondary profile user token should exist"

  Write-Step "Public profile: create post as secondary user"
  $profilePost = Invoke-Json POST "$BaseUrl/api/posts" $profileUserToken @{
    postType = "recipe"
    title = "Profile Regression Post $profileSuffix"
    content = "Post used for public profile regression"
  }
  Assert-True ($profilePost.success -eq $true) "Secondary user should create post successfully"
  $profilePostId = [int]$profilePost.data.id
  Assert-True ($profilePostId -gt 0) "Secondary profile post id should be valid"

  Write-Step "Public profile: like secondary post from primary user"
  $profileLike = Invoke-Json POST "$BaseUrl/api/posts/$profilePostId/like" $token
  Assert-True ($profileLike.success -eq $true) "Primary user should like secondary post"
  Assert-True ($profileLike.data.isLiked -eq $true) "Secondary post should be liked"

  Write-Step "Public profile: unauthenticated profile read"
  $publicProfileUnauth = Invoke-Json GET "$BaseUrl/api/users/$profileUserId/profile"
  Assert-True ($publicProfileUnauth.success -eq $true) "GET /api/users/{userId}/profile without token should succeed"
  Assert-True ([int]$publicProfileUnauth.data.id -eq $profileUserId) "Unauth profile should return correct user"
  Assert-True ($publicProfileUnauth.data.postCount -ge 1) "Unauth profile postCount should be at least 1"
  Assert-True ($publicProfileUnauth.data.totalReceivedLikes -ge 1) "Unauth profile totalReceivedLikes should be at least 1"
  Assert-True ($null -eq $publicProfileUnauth.data.isFollowing) "Unauth profile isFollowing should be null"

  Write-Step "Public profile: authenticated read before follow"
  $publicProfileBeforeFollow = Invoke-Json GET "$BaseUrl/api/users/$profileUserId/profile" $token
  Assert-True ($publicProfileBeforeFollow.success -eq $true) "Authenticated profile read should succeed"
  Assert-True ($publicProfileBeforeFollow.data.isFollowing -eq $false) "Before follow, isFollowing should be false"
  $beforeFollowerCount = [int]$publicProfileBeforeFollow.data.followerCount

  Write-Step "Public profile: follow secondary user and verify state"
  $followProfileUser = Invoke-Json POST "$BaseUrl/api/users/$profileUserId/follow" $token
  Assert-True ($followProfileUser.success -eq $true) "Follow secondary profile user should succeed"
  Assert-True ($followProfileUser.data.isFollowing -eq $true) "Follow result should set isFollowing=true"

  $publicProfileAfterFollow = Invoke-Json GET "$BaseUrl/api/users/$profileUserId/profile" $token
  Assert-True ($publicProfileAfterFollow.success -eq $true) "Authenticated profile read after follow should succeed"
  Assert-True ($publicProfileAfterFollow.data.isFollowing -eq $true) "After follow, isFollowing should be true"
  Assert-True ([int]$publicProfileAfterFollow.data.followerCount -eq [int]$followProfileUser.data.followerCount) "Profile followerCount should match follow API"
  Assert-True ([int]$publicProfileAfterFollow.data.followerCount -ge ($beforeFollowerCount + 1)) "Follower count should increase after follow"
  Assert-True ([int]$publicProfileAfterFollow.data.postCount -ge 1) "Profile postCount should remain valid"
  Assert-True ([int]$publicProfileAfterFollow.data.totalReceivedLikes -ge 1) "Profile totalReceivedLikes should remain valid"

  Write-Step "Public profile: invalid userId and not found checks"
  $invalidProfile = Invoke-Raw GET "$BaseUrl/api/users/0/profile"
  Assert-True ($invalidProfile.StatusCode -eq 400) "GET /api/users/0/profile should return 400"

  $missingProfile = Invoke-Raw GET "$BaseUrl/api/users/2147483647/profile"
  Assert-True ($missingProfile.StatusCode -eq 404) "GET /api/users/{missingId}/profile should return 404"

  Write-Step "Public profile: cleanup follow + post"
  $unfollowProfileUser = Invoke-Json POST "$BaseUrl/api/users/$profileUserId/follow" $token
  Assert-True ($unfollowProfileUser.success -eq $true) "Unfollow secondary profile user should succeed"
  Assert-True ($unfollowProfileUser.data.isFollowing -eq $false) "Unfollow result should set isFollowing=false"

  $cleanupUnlike = Invoke-Json POST "$BaseUrl/api/posts/$profilePostId/like" $token
  Assert-True ($cleanupUnlike.success -eq $true) "Cleanup unlike should succeed"
  Assert-True ($cleanupUnlike.data.isLiked -eq $false) "Cleanup unlike should set isLiked=false"

  $cleanupProfilePost = Invoke-Json DELETE "$BaseUrl/api/posts/$profilePostId" $profileUserToken
  Assert-True ($cleanupProfilePost.success -eq $true) "Cleanup secondary profile post should succeed"

  # -----------------------
  # FAMILY + MENU + VOTE
  # -----------------------
  Write-Step "Family: register secondary user for invite"
  $familySuffix = (Get-Random -Minimum 10000 -Maximum 99999)
  $familyUsername = "api_family_$familySuffix"
  $familyPassword = "password123"
  $familyFullName = "Family Member $familySuffix"

  $familyRegister = Invoke-Json POST "$BaseUrl/api/auth/register" $null @{
    username = $familyUsername
    password = $familyPassword
    fullName = $familyFullName
  }
  Assert-True ($familyRegister.success -eq $true) "Register family secondary user should succeed"
  $familyMemberUserId = [int]$familyRegister.data.user.id
  $familyMemberToken = $familyRegister.data.token
  Assert-True ($familyMemberUserId -gt 0) "Family secondary user id should be valid"
  Assert-True ($familyMemberToken) "Family secondary user token should exist"

  Write-Step "Family: create group"
  $familyName = "Family Regression $familySuffix"
  $createdFamily = Invoke-Json POST "$BaseUrl/api/families" $token @{ name = $familyName }
  Assert-True ($createdFamily.success -eq $true) "POST /api/families should succeed"
  $familyId = [int]$createdFamily.data.id
  Assert-True ($familyId -gt 0) "Created family id should be valid"
  Assert-True ($createdFamily.data.name -eq $familyName) "Created family name should match"

  Write-Step "Family: list my families should include created group"
  $myFamilies = Invoke-Json GET "$BaseUrl/api/families" $token
  Assert-True ($myFamilies.success -eq $true) "GET /api/families should succeed"
  $familyInList = @($myFamilies.data | Where-Object { [int]$_.id -eq $familyId })
  Assert-True ($familyInList.Count -eq 1) "Created family should appear in owner's family list"

  Write-Step "Family: rename group"
  $renamedFamilyName = "$familyName (Renamed)"
  $renamedFamily = Invoke-Json PUT "$BaseUrl/api/families/$familyId" $token @{ name = $renamedFamilyName }
  Assert-True ($renamedFamily.success -eq $true) "PUT /api/families/{familyId} should succeed"
  Assert-True ($renamedFamily.data.name -eq $renamedFamilyName) "Renamed family name should match"

  Write-Step "Family: members should include owner initially"
  $familyMembersBeforeInvite = Invoke-Json GET "$BaseUrl/api/families/$familyId/members" $token
  Assert-True ($familyMembersBeforeInvite.success -eq $true) "GET /api/families/{familyId}/members should succeed"
  $ownerMemberRow = @($familyMembersBeforeInvite.data | Where-Object { [int]$_.userId -eq [int]$login.data.user.id })
  Assert-True ($ownerMemberRow.Count -eq 1) "Owner should be in family member list"
  Assert-True ($ownerMemberRow[0].role -eq "owner") "Owner role should be owner"

  Write-Step "Family: invite member"
  $addedFamilyMember = Invoke-Json POST "$BaseUrl/api/families/$familyId/members" $token @{ userId = $familyMemberUserId }
  Assert-True ($addedFamilyMember.success -eq $true) "POST /api/families/{familyId}/members should succeed"
  Assert-True ([int]$addedFamilyMember.data.userId -eq $familyMemberUserId) "Added member userId should match"

  Write-Step "Family: invited member should see family in their list"
  $memberFamilies = Invoke-Json GET "$BaseUrl/api/families" $familyMemberToken
  Assert-True ($memberFamilies.success -eq $true) "Invited member GET /api/families should succeed"
  $familyInMemberList = @($memberFamilies.data | Where-Object { [int]$_.id -eq $familyId })
  Assert-True ($familyInMemberList.Count -eq 1) "Invited member should see family in list"

  Write-Step "Family: create menu"
  $menuDateObj = (Get-Date).Date.AddDays(1)
  $menuDate = $menuDateObj.ToString("yyyy-MM-dd")
  $createdMenu = Invoke-Json POST "$BaseUrl/api/families/$familyId/menus" $token @{
    menuDate = $menuDate
    mealType = "dinner"
    status = "voting"
  }
  Assert-True ($createdMenu.success -eq $true) "POST /api/families/{familyId}/menus should succeed"
  $familyMenuId = [int]$createdMenu.data.id
  Assert-True ($familyMenuId -gt 0) "Created family menu id should be valid"

  Write-Step "Family: add menu item by dishId"
  $createdMenuItem = Invoke-Json POST "$BaseUrl/api/families/$familyId/menus/$familyMenuId/items" $token @{
    dishId = $dishId
    note = "Family menu regression item"
  }
  Assert-True ($createdMenuItem.success -eq $true) "POST /api/families/{familyId}/menus/{menuId}/items should succeed"
  $familyMenuItemId = [int]$createdMenuItem.data.id
  Assert-True ($familyMenuItemId -gt 0) "Created family menu item id should be valid"

  Write-Step "Family: weekly menus should include created menu and item"
  $dayOffsetToMonday = (([int]$menuDateObj.DayOfWeek + 6) % 7)
  $weekStart = $menuDateObj.AddDays(-$dayOffsetToMonday).ToString("yyyy-MM-dd")
  $weeklyMenus = Invoke-Json GET "$BaseUrl/api/families/$familyId/menus/week?weekStart=$weekStart" $token
  Assert-True ($weeklyMenus.success -eq $true) "GET /api/families/{familyId}/menus/week should succeed"
  $menuInWeek = @($weeklyMenus.data | Where-Object { [int]$_.menu.id -eq $familyMenuId })
  Assert-True ($menuInWeek.Count -eq 1) "Weekly menus should include created menu"
  $itemInWeek = @($menuInWeek[0].items | Where-Object { [int]$_.id -eq $familyMenuItemId })
  Assert-True ($itemInWeek.Count -eq 1) "Weekly menu items should include created item"

  # Recent menu items for picker
  Write-Step "Family: recent menu items should include created dish"
  $recentItems = Invoke-Json GET "$BaseUrl/api/families/$familyId/menus/recent?limit=5" $token
  Assert-True ($recentItems.success -eq $true) "GET /api/families/{familyId}/menus/recent should succeed"
  $recentRows = @($recentItems.data)
  Assert-True ($recentRows.Count -le 5) "Recent menu items should respect limit"
  $recentMatch = @($recentRows | Where-Object { [int]$_.dishId -eq $dishId })
  Assert-True ($recentMatch.Count -ge 1) "Recent menu items should include created dish"

  Write-Step "Family Vote: member vote up"
  $memberVoteUp = Invoke-Json POST "$BaseUrl/api/families/$familyId/menus/$familyMenuId/items/$familyMenuItemId/vote" $familyMemberToken @{ voteType = "up" }
  Assert-True ($memberVoteUp.success -eq $true) "Member vote up should succeed"
  Assert-True ($memberVoteUp.data.voted -eq $true) "Vote result should indicate voted=true"
  Assert-True ($memberVoteUp.data.voteType -eq "up") "Vote type should be up"
  Assert-True ([int]$memberVoteUp.data.upVotes -eq 1) "upVotes should be 1 after first member vote"

  Write-Step "Family Vote: owner vote down"
  $ownerVoteDown = Invoke-Json POST "$BaseUrl/api/families/$familyId/menus/$familyMenuId/items/$familyMenuItemId/vote" $token @{ voteType = "down" }
  Assert-True ($ownerVoteDown.success -eq $true) "Owner vote down should succeed"
  Assert-True ($ownerVoteDown.data.voted -eq $true) "Owner vote result should indicate voted=true"
  Assert-True ($ownerVoteDown.data.voteType -eq "down") "Owner vote type should be down"
  Assert-True ([int]$ownerVoteDown.data.downVotes -eq 1) "downVotes should be 1 after owner vote"

  Write-Step "Family Vote: summary by owner/member perspective"
  $voteSummaryOwner = Invoke-Json GET "$BaseUrl/api/families/$familyId/menus/$familyMenuId/votes" $token
  Assert-True ($voteSummaryOwner.success -eq $true) "GET vote summary as owner should succeed"
  $ownerSummaryRow = @($voteSummaryOwner.data | Where-Object { [int]$_.familyMenuItemId -eq $familyMenuItemId })
  Assert-True ($ownerSummaryRow.Count -eq 1) "Vote summary should include current menu item"
  Assert-True ([int]$ownerSummaryRow[0].upVotes -eq 1) "Owner summary upVotes should be 1"
  Assert-True ([int]$ownerSummaryRow[0].downVotes -eq 1) "Owner summary downVotes should be 1"
  Assert-True ($ownerSummaryRow[0].userVoteType -eq "down") "Owner userVoteType should be down"

  $voteSummaryMember = Invoke-Json GET "$BaseUrl/api/families/$familyId/menus/$familyMenuId/votes" $familyMemberToken
  Assert-True ($voteSummaryMember.success -eq $true) "GET vote summary as member should succeed"
  $memberSummaryRow = @($voteSummaryMember.data | Where-Object { [int]$_.familyMenuItemId -eq $familyMenuItemId })
  Assert-True ($memberSummaryRow.Count -eq 1) "Member summary should include current menu item"
  Assert-True ($memberSummaryRow[0].userVoteType -eq "up") "Member userVoteType should be up"

  Write-Step "Family Vote: member unvote"
  $memberUnvote = Invoke-Json DELETE "$BaseUrl/api/families/$familyId/menus/$familyMenuId/items/$familyMenuItemId/vote" $familyMemberToken
  Assert-True ($memberUnvote.success -eq $true) "Member unvote should succeed"
  Assert-True ($memberUnvote.data.voted -eq $false) "After unvote, voted should be false"
  Assert-True ($null -eq $memberUnvote.data.voteType) "After unvote, voteType should be null"
  Assert-True ([int]$memberUnvote.data.upVotes -eq 0) "After member unvote, upVotes should be 0"
  Assert-True ([int]$memberUnvote.data.downVotes -eq 1) "After member unvote, downVotes should remain 1"

  Write-Step "Family: remove menu item"
  $removeMenuItem = Invoke-Json DELETE "$BaseUrl/api/families/$familyId/menus/$familyMenuId/items/$familyMenuItemId" $token
  Assert-True ($removeMenuItem.success -eq $true) "DELETE /api/families/{familyId}/menus/{menuId}/items/{itemId} should succeed"

  $voteSummaryAfterItemRemoval = Invoke-Json GET "$BaseUrl/api/families/$familyId/menus/$familyMenuId/votes" $token
  Assert-True ($voteSummaryAfterItemRemoval.success -eq $true) "GET vote summary after removing item should succeed"
  $removedItemSummary = @($voteSummaryAfterItemRemoval.data | Where-Object { [int]$_.familyMenuItemId -eq $familyMenuItemId })
  Assert-True ($removedItemSummary.Count -eq 0) "Removed menu item should no longer appear in vote summary"

  Write-Step "Family: member leaves family"
  $memberLeaveFamily = Invoke-Json DELETE "$BaseUrl/api/families/$familyId/leave" $familyMemberToken
  Assert-True ($memberLeaveFamily.success -eq $true) "Member leave family should succeed"

  $familyMembersAfterLeave = Invoke-Json GET "$BaseUrl/api/families/$familyId/members" $token
  Assert-True ($familyMembersAfterLeave.success -eq $true) "Owner should still load members after member leaves"
  $leftMemberRow = @($familyMembersAfterLeave.data | Where-Object { [int]$_.userId -eq $familyMemberUserId })
  Assert-True ($leftMemberRow.Count -eq 0) "Member should be removed from family after leave"

  Write-Step "Family: owner leave should be rejected"
  $ownerLeaveRaw = Invoke-Raw DELETE "$BaseUrl/api/families/$familyId/leave" $token
  Assert-True ($ownerLeaveRaw.StatusCode -eq 409) "Owner leave family should return 409"

  # -----------------------
  # PHASE C: SEARCH, TRENDING, MY-POSTS
  # -----------------------
  Write-Step "Phase C: Create post for CRUD testing"
  $crudPostBody = @{
    postType = "review"
    title = "Test CRUD Title"
    content = "Test CRUD Content"
  }
  $crudPost = Invoke-Json POST "$BaseUrl/api/posts" $token $crudPostBody
  Assert-True ($crudPost.success -eq $true) "Create post should succeed"
  $crudPostId = [int]$crudPost.data.id

  Write-Step "Phase C: Update Post as Owner"
  $updatePostBody = @{
    title = "Updated Title"
    content = "Updated Content"
  }
  $updatedPost = Invoke-Json PUT "$BaseUrl/api/posts/$crudPostId" $token $updatePostBody
  Assert-True ($updatedPost.success -eq $true) "Owner update post should succeed"
  Assert-True ($updatedPost.data.title -eq "Updated Title") "Title should be updated"

  Write-Step "Phase C: Update Post as Non-Owner (Should fail)"
  $unauthUpdate = Invoke-Raw PUT "$BaseUrl/api/posts/$crudPostId" $profileUserToken $updatePostBody
  Assert-True ($unauthUpdate.StatusCode -eq 403) "Non-owner update should return 403"

  Write-Step "Phase C: My-Post Date Range Filter"
  $startDate = (Get-Date).AddDays(-1).ToString("yyyy-MM-dd")
  $endDate = (Get-Date).AddDays(1).ToString("yyyy-MM-dd")
  $myUserId = [int]$login.data.user.id
  $myPostsDate = Invoke-Json GET "$BaseUrl/api/users/$myUserId/posts?startDate=$startDate&endDate=$endDate" $token
  Assert-True ($myPostsDate.success -eq $true) "Date range filter should succeed"
  $hasCrudPost = @($myPostsDate.data.data | Where-Object { [int]$_.id -eq $crudPostId })
  Assert-True ($hasCrudPost.Count -gt 0) "Date range filter should include recently created post"

  Write-Step "Phase C: My-Post Date Range invalid format"
  $invalidDateRange = Invoke-Raw GET "$BaseUrl/api/users/$myUserId/posts?startDate=not-a-date&endDate=$endDate" $token
  Assert-True ($invalidDateRange.StatusCode -eq 400) "Invalid startDate format should return 400"

  Write-Step "Phase C: SearchType=post"
  $searchPost = Invoke-Json GET "$BaseUrl/api/posts?searchType=post&search=Updated" $token
  Assert-True ($searchPost.success -eq $true) "Search by post should succeed"
  $foundPost = @($searchPost.data.data | Where-Object { [int]$_.id -eq $crudPostId })
  Assert-True ($foundPost.Count -gt 0) "Search by post should find updated title"

  Write-Step "Phase C: SearchType=user"
  $searchUser = Invoke-Json GET "$BaseUrl/api/posts?searchType=user&search=$Username" $token
  Assert-True ($searchUser.success -eq $true) "Search by user should succeed"
  $foundUserPost = @($searchUser.data.data | Where-Object { [int]$_.id -eq $crudPostId })
  Assert-True ($foundUserPost.Count -gt 0) "Search by user should include post authored by matched username"

  Write-Step "Phase C: Trending Order (includeTrending=true)"
  $trendingAll = Invoke-Json GET "$BaseUrl/api/posts?includeTrending=true" $token
  Assert-True ($trendingAll.success -eq $true) "Get trending posts should succeed"
  Assert-True ($trendingAll.data.data -ne $null) "Trending result should not be null"
  $trendingRows = @($trendingAll.data.data)
  $firstNonTrendingIndex = -1
  $firstTrendingAfterNonTrending = -1
  for ($i = 0; $i -lt $trendingRows.Count; $i++) {
    if (-not $trendingRows[$i].isTrending) {
      $firstNonTrendingIndex = $i
      break
    }
  }
  if ($firstNonTrendingIndex -ge 0) {
    for ($j = $firstNonTrendingIndex + 1; $j -lt $trendingRows.Count; $j++) {
      if ($trendingRows[$j].isTrending) {
        $firstTrendingAfterNonTrending = $j
        break
      }
    }
  }
  Assert-True ($firstTrendingAfterNonTrending -eq -1) "Trending posts should not appear after first non-trending post"

  Write-Step "Phase C: Delete Post as Non-Owner (Should fail)"
  $unauthDelete = Invoke-Raw DELETE "$BaseUrl/api/posts/$crudPostId" $profileUserToken
  Assert-True ($unauthDelete.StatusCode -eq 403) "Non-owner delete should return 403"

  Write-Step "Phase C: Delete Post as Owner"
  $deleteCrudPost = Invoke-Json DELETE "$BaseUrl/api/posts/$crudPostId" $token
  Assert-True ($deleteCrudPost.success -eq $true) "Owner delete post should succeed"

  Write-Host "`n========================================" -ForegroundColor Green
  Write-Host "ALL TESTS PASSED" -ForegroundColor Green
  Write-Host "========================================" -ForegroundColor Green
  Write-Host "User: $Username" -ForegroundColor DarkGray
  Write-Host "DishId: $dishId" -ForegroundColor DarkGray
}
catch {
  Write-Host "`n========================================" -ForegroundColor Red
  Write-Host "TESTS FAILED" -ForegroundColor Red
  Write-Host "========================================" -ForegroundColor Red
  Write-Host $_ -ForegroundColor Red
  exit 1
}
